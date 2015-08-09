package charactermanaj.model.io;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.WorkingSet;
import charactermanaj.ui.model.WallpaperInfo;
import charactermanaj.ui.model.WallpaperInfo.WallpaperResourceType;

/**
 * WorkingSetのXMLへの書き込み
 */
public class WorkingSetXMLWriter {
	
	/**
	 * WorkingSetのバージョン
	 */
	private static final String VERSION_SIG_1_0 = "1.0";

	/**
	 * WorkingSetのXMLファイルの名前空間
	 */
	private static final String NS = "http://charactermanaj.sourceforge.jp/schema/charactermanaj-workingset";

	/**
	 * キャラクターデータのXML化
	 */
	private CharacterDataXMLWriter characterDataXmlWriter =
			new CharacterDataXMLWriter(NS);

	/**
	 * ワーキングセットをXML表現で出力ストリームに出力します.<br>
	 * 
	 * @param ws
	 *            ワーキングセット
	 * @param outstm
	 *            出力先ストリーム
	 * @throws IOException
	 *             失敗
	 */
	public void writeWorkingSet(WorkingSet ws, OutputStream outstm) throws IOException {
		if (ws == null || outstm == null) {
			throw new IllegalArgumentException();
		}
		
		Document doc = createWorkingSetXML(ws);

		// output xml
		TransformerFactory txFactory = TransformerFactory.newInstance();
		txFactory.setAttribute("indent-number", Integer.valueOf(4));
		Transformer tfmr;
		try {
			tfmr = txFactory.newTransformer();
		} catch (TransformerConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration Failed.", ex);
		}
		tfmr.setOutputProperty(OutputKeys.INDENT, "yes");

		// JDK-4504745 : javax.xml.transform.Transformer encoding does not work properly
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504745
		final String encoding = "UTF-8";
		tfmr.setOutputProperty("encoding", encoding);
		try {
			tfmr.transform(new DOMSource(doc), new StreamResult(
					new OutputStreamWriter(outstm, Charset.forName(encoding))));

		} catch (TransformerException ex) {
			IOException ex2 = new IOException("XML Convert failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	/**
	 * ワーキングセットのXMLドキュメントを生成します.
	 * 
	 * @param ws
	 *            ワーキングセット
	 * @return XMLドキュメント
	 */
	public Document createWorkingSetXML(WorkingSet ws) {
		if (ws == null) {
			throw new IllegalArgumentException();
		}

		Document doc;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException("JAXP Configuration failed.", ex);
		}

		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		Element root = doc.createElementNS(NS, "character-workingset");
		root.setAttribute("version", VERSION_SIG_1_0);

		root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi",
				XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);		
		root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xml",
				XMLConstants.XML_NS_URI);		
		root.setAttribute("xsi:schemaLocation", NS + " character_ws.xsd");

		// ドキュメントベース
		URI docbase = ws.getCharacterDocBase();
		root.setAttribute("characterDocBase", docbase == null ? "" : docbase.toString());
		
		// キャラクターデータのシグネチャ
		CharacterData cd = ws.getCharacterData();
		Element characterDataSigElm = doc.createElementNS(NS, "characterDataSig");
		if (cd == null || !cd.isValid()) {
			// 指定されていないか有効でない場合は無しとみなす.
			characterDataSigElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
		} else {
			characterDataSigElm.setTextContent(cd.toSignatureString());
		}
		root.appendChild(characterDataSigElm);

		// パーツカラー情報
		root.appendChild(writePartsColorInfoMap(doc, ws.getPartsColorInfoMap()));
		
		// 現在のパーツセット
		PartsSet currentPartsSet = ws.getPartsSet();
		Element partsSetElm = doc.createElementNS(NS, "currentPartsSet");
		if (currentPartsSet == null || currentPartsSet.isEmpty()) {
			partsSetElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			
		} else {
			Element elm = characterDataXmlWriter.createPartsSetXML(doc, lang,
					currentPartsSet);
			partsSetElm.appendChild(elm);
		}
		root.appendChild(partsSetElm);
		
		// 最後に使用した保存先ディレクトリ
		Element lastUsedSaveDirElm = doc.createElementNS(NS, "lastUsedSaveDir");
		File lastUsedSaveDir = ws.getLastUsedSaveDir();
		if (lastUsedSaveDir == null) {
			lastUsedSaveDirElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			
		} else {
			lastUsedSaveDirElm.setTextContent(lastUsedSaveDir.getPath());
		}
		root.appendChild(lastUsedSaveDirElm);
		
		// 最後に使用したエクスポート先ディレクトリ
		Element lastUsedExportDirElm = doc.createElementNS(NS, "lastUsedExportDir");
		File lastUsedExportDir = ws.getLastUsedExportDir();
		if (lastUsedExportDir == null) {
			lastUsedExportDirElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			
		} else {
			lastUsedExportDirElm.setTextContent(lastUsedExportDir.getPath());
		}
		root.appendChild(lastUsedExportDirElm);
		
		// 最後に使用したパーツセット情報、なければnull
		PartsSet lastUsePresetParts = ws.getLastUsePresetParts();
		Element lastUsePresetPartsElm = doc.createElementNS(NS, "lastUsePresetParts");
		if (lastUsePresetParts == null || lastUsePresetParts.isEmpty()) {
			lastUsePresetPartsElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			
		} else {
			Element elm = characterDataXmlWriter.createPartsSetXML(doc, lang, lastUsePresetParts);
			lastUsePresetPartsElm.appendChild(elm);
		}
		root.appendChild(lastUsePresetPartsElm);
		
		// 壁紙情報
		root.appendChild(writeWallpaper(doc, ws.getWallpaperInfo()));
		
		doc.appendChild(root);
		return doc;
	}
	
	/**
	 * パーツごとのカラー情報のXML要素を生成して返します.
	 * 
	 * @param doc
	 *            要素のファクトリ
	 * @param partsColorMap
	 *            パーツごとのカラー情報のマップ
	 * @return パーツごとのカラー情報のXML要素
	 */
	public Element writePartsColorInfoMap(Document doc, Map<PartsIdentifier, PartsColorInfo> partsColorMap) {
		Element partsColorInfoMapElm = doc.createElementNS(NS, "partsColorInfoMap");
		if (partsColorMap != null) {
			// 使用しているカラーの設定ごとに番号を振る
			// (同じカラー設定であれば同じ番号とする.)
			LinkedHashMap<PartsColorInfo, String> colorMap = new LinkedHashMap<PartsColorInfo, String>();
			for (Map.Entry<PartsIdentifier, PartsColorInfo> partsColorEntry : partsColorMap.entrySet()) {
				PartsColorInfo partsColorInfo = partsColorEntry.getValue();
				if (partsColorInfo != null && !partsColorInfo.isEmpty()) {
					if (!colorMap.containsKey(partsColorInfo)) {
						colorMap.put(partsColorInfo, Integer.toString(colorMap.size() + 1));
					}
				}
			}
			
			// すべてのカラー設定を出力する.
			Element colorsElm = doc.createElementNS(NS, "colors");
			for (Map.Entry<PartsColorInfo, String> colorMapEntry : colorMap.entrySet()) {
				PartsColorInfo partsColorInfo = colorMapEntry.getKey();
				String id = colorMapEntry.getValue();
				Element partsColorElm = characterDataXmlWriter.createPartsColorInfoXML(doc, partsColorInfo);
				partsColorElm.setAttribute("id", id);
				colorsElm.appendChild(partsColorElm);
			}
			partsColorInfoMapElm.appendChild(colorsElm);
			
			// パーツと、そのパーツのカラー設定番号の一覧を出力する.
			Element partsListElm = doc.createElementNS(NS, "partsList");
			for (Map.Entry<PartsIdentifier, PartsColorInfo> partsColorEntry : partsColorMap.entrySet()) {
				PartsIdentifier partsIdentifier = partsColorEntry.getKey();
				PartsColorInfo partsColorInfo = partsColorEntry.getValue();
				if (partsColorInfo != null && !partsColorInfo.isEmpty()) {
					String colorId = colorMap.get(partsColorInfo);
					if (colorId == null) {
						throw new RuntimeException("colorMapが不整合です");
					}
					Element partsElm = doc.createElementNS(NS, "partsIdentifier");
					String categoryId = partsIdentifier.getPartsCategory().getCategoryId();
					partsElm.setAttribute("categoryId", categoryId);
					partsElm.setAttribute("name", partsIdentifier.getPartsName());
					partsElm.setAttribute("colorId", colorId);
					partsListElm.appendChild(partsElm);
				}
			}
			partsColorInfoMapElm.appendChild(partsListElm);
		}
		return partsColorInfoMapElm;
	}
	
	/**
	 * 壁紙情報をXML要素として生成する.
	 * 
	 * @param doc
	 *            XML要素のファクトリ
	 * @param wallpaperInfo
	 *            壁紙情報
	 * @return 壁紙情報のXML要素
	 */
	public Element writeWallpaper(Document doc, WallpaperInfo wallpaperInfo) {
		Element elm = doc.createElementNS(NS, "wallpaperInfo");
		
		if (wallpaperInfo == null) {
			elm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");

		} else {
			// タイプ
			WallpaperResourceType typ = wallpaperInfo.getType();
			Element typElm = doc.createElementNS(NS, "type");
			typElm.setTextContent(typ.name());
			elm.appendChild(typElm);
			
			// リソース
			String res = wallpaperInfo.getResource();
			Element resElm = doc.createElementNS(NS, "resource");
			if (res == null || res.trim().length() == 0) {
				resElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			} else {
				resElm.setTextContent(res);
			}
			elm.appendChild(resElm);
			
			// ファイル
			File file = wallpaperInfo.getFile();
			Element fileElm = doc.createElementNS(NS, "file");
			if (file == null) {
				fileElm.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:nil", "true");
			} else {
				fileElm.setTextContent(file.getPath());
			}
			elm.appendChild(fileElm);
			
			// アルファ
			float alpha = wallpaperInfo.getAlpha();
			Element alphaElm = doc.createElementNS(NS, "alpha");
			alphaElm.setTextContent(Float.toString(alpha));
			elm.appendChild(alphaElm);
			
			// 背景色
			Color backgroundColor = wallpaperInfo.getBackgroundColor();
			Element bgColorElm = doc.createElementNS(NS, "backgroundColor");
			bgColorElm.setTextContent("#" + Integer.toHexString(backgroundColor.getRGB() & 0xffffff));
			elm.appendChild(bgColorElm);
		}
		
		return elm;
	}
	
}
