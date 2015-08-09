package charactermanaj.model.io;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.ImageSaveHelper;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsFiles;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.PartsManageDataConverter;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpec;

public abstract class AbstractCharacterDataArchivedFileWriter extends AbstractCharacterDataFileWriter {

	protected AbstractCharacterDataArchivedFileWriter(File outFile) throws IOException {
		super(outFile);
	}
	
	protected abstract OutputStream getOutputStream() throws IOException;
	
	protected abstract void putNextEntry(String name, long lastModified) throws IOException;
	
	protected abstract void closeEntry() throws IOException;

	@Override
	protected void internalWriteExportProp(Properties prop) throws IOException {
		// export prop
		putNextEntry("export-info.xml", 0);
		prop.storeToXML(getOutputStream(), "exportProp");
	}

	@Override
	protected void internalWriteCharacterData(CharacterData characterData)
			throws IOException {
		CharacterDataXMLWriter xmlWriter = new CharacterDataXMLWriter();
		
		// character.xmlの出力
		putNextEntry(CharacterDataPersistent.CONFIG_FILE, 0);
		xmlWriter.writeXMLCharacterData(characterData, getOutputStream());
		closeEntry();
		
		// character.iniの出力
		internalWriteCharacterIni(characterData);
	}

	/**
	 * character.iniを出力します.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @throws IOException
	 *             出力に失敗した場合
	 */
	protected void internalWriteCharacterIni(CharacterData characterData) throws IOException {
		StringBuilder buf = new StringBuilder();

		buf.append("; created by charactermanaj "
				+ new Timestamp(System.currentTimeMillis()) + "\r\n");
		
		buf.append("[Size]\r\n");
		Dimension dim = characterData.getImageSize();
		if (dim == null) {
			dim = new Dimension(300, 400);
		}
		buf.append("size_x=" + dim.width + "\r\n");
		buf.append("size_y=" + dim.height + "\r\n");

		buf.append("\r\n");
		buf.append("[Parts]\r\n");
		
		Map<String, String> partsMap = new HashMap<String, String>();
		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			String categoryId = partsCategory.getCategoryId();
			partsMap.put(categoryId, "");
		}
		
		Map<String, PartsSet> partsSets = characterData.getPartsSets();
		PartsSet partsSet = partsSets.get(characterData.getDefaultPartsSetId());
		if (partsSet == null && !partsSets.isEmpty()) {
			// デフォルトのパーツセットが指定されていない場合は、どれか1つを選択する.
			partsSet = partsSets.values().iterator().next();
		}
		if (partsSet != null) {
			for (Map.Entry<PartsCategory, List<PartsIdentifier>> entry : partsSet
					.entrySet()) {
				PartsCategory partsCategory = entry.getKey();
				StringBuilder partsNames = new StringBuilder();
				for (PartsIdentifier partsIdentifier : entry.getValue()) {
					if (partsNames.length() > 0) {
						partsNames.append(",");
					}
					partsNames.append(partsIdentifier.getPartsName());
				}
				String categoryId = partsCategory.getCategoryId();
				partsMap.put(categoryId, partsNames.toString());
			}
		}
		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			String categoryId = partsCategory.getCategoryId();
			String partsNames = partsMap.get(categoryId);
			buf.append(categoryId + "=" + partsNames + "\r\n");
		}

		// 色情報はすべてダミー(character.iniは色情報を省略しても問題ないようだが、一応)
		buf.append("\r\n");
		buf.append("[Color]\r\n");
		buf.append("hair_rgb=0\r\n");
		buf.append("hair_gray=0\r\n");
		buf.append("eye_rgb=0\r\n");
		buf.append("eye_gray=0\r\n");
		buf.append("skin_rgb=0\r\n");
		buf.append("skin_gray=0\r\n");
		buf.append("body_rgb=0\r\n");
		buf.append("body_gray=0\r\n");

		// UTF16LEで出力する.
		internalWriteTextUTF16LE(CharacterDataPersistent.COMPATIBLE_CONFIG_NAME, buf.toString());
	}
	
	@Override
	protected void internalWriteTextUTF16LE(String name, String contents) throws IOException {
		if (contents == null) {
			contents = "";
		}

		// LFまたはCR改行であればCR/LF改行に変換.
		contents = contents.replace("\r\n", "\n");
		contents = contents.replace("\r", "\n");
		contents = contents.replace("\n", "\r\n");
		
		putNextEntry(name, 0);
		OutputStream os = getOutputStream();
		os.write((byte) 0xff);
		os.write((byte) 0xfe);
		os.flush();
		Writer wr = new OutputStreamWriter(os, Charset.forName("UTF-16LE")) {
			@Override
			public void close() throws IOException {
				// ZipのOutputStreamをクローズしてはならないため
				// OutputStreamWriter自身はクローズは呼び出さない.
				flush();
				closeEntry();
			}
		};
		try {
			wr.append(contents);
			wr.flush();
		} finally {
			wr.close();
		}
	}
	
	@Override
	protected void internalWriteSamplePicture(BufferedImage samplePicture)
			throws IOException {
		putNextEntry("preview.png", 0);
		ImageSaveHelper imageSaveHelper = new ImageSaveHelper();
		imageSaveHelper.savePicture(samplePicture, Color.white, getOutputStream(), "image/png", null);
		closeEntry();
	}
	
	@Override
	protected void internalWritePartsImages(
			Map<PartsIdentifier, PartsSpec> partsImages) throws IOException {
		AppConfig appConfig = AppConfig.getInstance();
		byte[] buf = new byte[appConfig.getJarTransferBufferSize()];
		
		for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsImages.entrySet()) {
			PartsIdentifier partsIdentifier = entry.getKey();
			PartsSpec partsSpec = entry.getValue();
			PartsFiles partsFiles = partsSpec.getPartsFiles();
			
			for (Map.Entry<Layer, ImageResource> imageEntry : partsFiles.entrySet()) {
				Layer layer = imageEntry.getKey();
				ImageResource imageResource = imageEntry.getValue();

				String name = layer.getDir() + "/" + partsIdentifier.getPartsName() + ".png";
				name = name.replace("//", "/");
				
				putNextEntry(name, imageResource.lastModified());
				OutputStream os = getOutputStream();
				InputStream is = imageResource.openStream();
				try {
					int rd;
					while ((rd = is.read(buf)) >= 0) {
						os.write(buf, 0, rd);
					}
				} finally {
					is.close();
				}
				closeEntry();
			}
		}
	}
	
	@Override
	protected void internalWritePartsManageData(
			Map<PartsIdentifier, PartsSpec> partsImages) throws IOException {
		
		PartsManageDataConverter partsManageDataConverter = new PartsManageDataConverter();
		
		for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsImages.entrySet()) {
			PartsIdentifier partsIdentifier = entry.getKey();
			PartsSpec partsSpec = entry.getValue();
			partsManageDataConverter.convert(partsIdentifier, partsSpec);
		}
		
		PartsManageData partsManageData = partsManageDataConverter.getPartsManageData();
		PartsInfoXMLWriter xmlWriter = new PartsInfoXMLWriter();

		putNextEntry("parts-info.xml", 0);
		xmlWriter.savePartsManageData(partsManageData, getOutputStream());
		closeEntry();
	}
}
