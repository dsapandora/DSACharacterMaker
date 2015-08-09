package charactermanaj.model.io;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.io.AbstractCharacterDataArchiveFile.CategoryLayerPair;
import charactermanaj.model.io.AbstractCharacterDataArchiveFile.PartsImageContent;
import charactermanaj.ui.progress.ProgressHandle;

public class ImportModel {

	/**
	 * ロガー.<br>
	 */
	private static final Logger logger = Logger.getLogger(ImportModel.class.getName());

	/**
	 * インポートもとファイル
	 */
	private URI importSource;
	
	/**
	 * プロファイル先のキャラクター定義、新規の場合はnull
	 */
	private CharacterData currentCharacterData;
	
	
	/**
	 * インポートもとアーカイブ. ロードされた場合に非nullとなる.
	 */
	private CharacterDataArchiveFile archiveFile;
	
	
	/**
	 * インポートされたキャラクター定義、なければnull
	 */
	private CharacterData sourceCharacterData;
	
	/**
	 * インポートされたサンプルピクチャ、なければnull
	 */
	private BufferedImage samplePicture;
	
	/**
	 * インポートされたreadme
	 */
	private String readme;
	
	/**
	 * インポート先のキャラクター定義、もしくは現在のキャラクター定義のディレクトリ構成から 読み取ることのできるパーツのコレクション、なければ空.<br>
	 */
	private Collection<PartsImageContent> partsImageContentsMap;
	
	/**
	 * パーツ管理データ、なければ空
	 */
	private PartsManageData partsManageData;
	

	public void openImportSource(URI importSource, CharacterData currentCharacterData) throws IOException {
		if (archiveFile != null || importSource == null) {
			throw new IllegalStateException("既にアーカイブがオープンされています。");
		}
		this.importSource = importSource;
		this.currentCharacterData = currentCharacterData;
	}
	
	public void closeImportSource() throws IOException {
		if (archiveFile != null) {
			try {
				archiveFile.close();
			} finally {
				// クローズに失敗しても閉じたことにする.
				reset();
			}
		}
	}
	
	public void loadContents(ProgressHandle progressHandle) throws IOException {
		if (archiveFile != null) {
			throw new IllegalStateException("既にアーカイブがオープンされています。");
		}
		if (importSource == null) {
			throw new IllegalStateException("インポートファィルが指定されていません。");
		}

		CharacterDataFileReaderWriterFactory factory = CharacterDataFileReaderWriterFactory.getInstance();
		
		progressHandle.setCaption("open archive...");
		archiveFile = factory.openArchive(importSource);

		readme = archiveFile.readReadMe();

		progressHandle.setCaption("search the character definition...");
		sourceCharacterData = archiveFile.readCharacterData();
		if (sourceCharacterData == null) {
			// character.xmlがない場合は、character.iniで試行する.

			sourceCharacterData = archiveFile.readCharacterINI();
			if (sourceCharacterData != null) {
				// readmeがあれば、それを説明として登録しておく
				if (readme != null && readme.trim().length() > 0) {
					sourceCharacterData.setDescription(readme);
				}
			}
			
		} else {
			// character.xmlがあった場合、favorites.xmlもあれば読み込む.
			archiveFile.readFavorites(sourceCharacterData);
		}

		// サンプルピクチャの読み込み、なければnull
		progressHandle.setCaption("load sample picture...");
		samplePicture = archiveFile.readSamplePicture();
		
		// パーツセットの読み込み、なければ空
		progressHandle.setCaption("load partssets...");
		if (currentCharacterData != null) {
			// 既存のキャラクターセットの定義をもとにパーツディレクトリを探索する場合
			// (インポートもと・インポート先が同一であればパーツは除外される.)
			partsImageContentsMap = archiveFile.getPartsImageContents(currentCharacterData, false);
		} else {
			// インポート元にあるキャラクター定義をもとにパーツディレクトリを探索する場合
			partsImageContentsMap = archiveFile.getPartsImageContents(sourceCharacterData, true);
		}
		
		// パーツ管理データの読み込み
		progressHandle.setCaption("load parts definitions...");
		partsManageData = archiveFile.getPartsManageData();
	}

	protected void reset() {
		importSource = null;
		archiveFile = null;
		sourceCharacterData = null;
		samplePicture = null;
		readme = null;
		partsImageContentsMap = null;
		partsManageData = null;
	}
	


	public URI getImportSource() {
		return importSource;
	}
	
	protected void checkArchiveOpened() {
		if (archiveFile == null) {
			throw new IllegalStateException("アーカイブはオープンされていません。");
		}
	}
	
	public CharacterData getCharacterData() {
		checkArchiveOpened();
		return sourceCharacterData;
	}

	public BufferedImage getSamplePicture() {
		checkArchiveOpened();
		return samplePicture;
	}

	public String getReadme() {
		checkArchiveOpened();
		return readme;
	}

	public Collection<PartsImageContent> getPartsImageContents() {
		checkArchiveOpened();
		return partsImageContentsMap;
	}

	public PartsManageData getPartsManageData() {
		checkArchiveOpened();
		return partsManageData;
	}

	
	
	/**
	 * パーツデータをプロファイルの画像ディレクトリに一括コピーする.<br>
	 * 
	 * @param partsImageContents
	 *            コピー対象のパーツデータ
	 * @param cd
	 *            コピー先のプロファイル
	 * @throws IOException
	 *             失敗
	 */
	public void copyPartsImageContents(
			Collection<PartsImageContent> partsImageContents, CharacterData cd)
			throws IOException {
		if (cd == null || cd.getDocBase() == null) {
			throw new IllegalArgumentException("invalid character data");
		}

		// コピー先ディレクトリの確定
		URI docbase = cd.getDocBase();
		if ( !"file".equals(docbase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません: " + docbase);
		}
		File configFile = new File(docbase);
		File baseDir = configFile.getParentFile();
		if (baseDir == null || !baseDir.isDirectory()) {
			throw new IOException("親フォルダがディレクトリではありません: " + baseDir);
		}

		AppConfig appConfig = AppConfig.getInstance();
		byte[] stmbuf = new byte[appConfig.getFileTransferBufferSize()];
		
		// ファイルコピー
		for (PartsImageContent content : partsImageContents) {
			InputStream is = new BufferedInputStream(content.openStream());
			try {
				File outDir = new File(baseDir, content.getDirName());
				if (!outDir.exists()) {
					if (!outDir.mkdirs()) {
						logger.log(Level.WARNING, "can't create the directory. " + outDir);
					}
				}
				File outFile = new File(outDir, content.getFileName());
				OutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));
				try {
					for (;;) {
						int rd = is.read(stmbuf);
						if (rd < 0) {
							break;
						}
						os.write(stmbuf, 0, rd);
					}
				} finally {
					os.close();
				}
				
				if (!outFile.setLastModified(content.lastModified())) {
					logger.log(Level.WARNING, "can't change the modified-date: " + outFile);
				}
				
			} finally {
				is.close();
			}
		}
	}

	/**
	 * パーツ管理情報を更新または作成する.<br>
	 * パーツ管理情報がnullまたは空であれば何もしない.<br>
	 * そうでなければインポートするパーツに該当するパーツ管理情報を、現在のプロファイルのパーツ管理情報に追記・更新し、 それを書き出す.<br>
	 * インポートもとにパーツ管理情報がなく、既存にある場合、インポートしても管理情報は削除されません.(上書きセマンティクスのため)
	 * 
	 * @param partsImageContents
	 *            インポートするパーツ
	 * @param partsManageData
	 *            パーツ管理データ
	 * @param current
	 *            現在のパーツ管理データを保持しているプロファイル、新規の場合はnull
	 * @param target
	 *            書き込み先のプロファイル
	 * @throws IOException
	 *             書き込みに失敗した場合
	 */
	public void updatePartsManageData(
			Collection<PartsImageContent> partsImageContents,
			PartsManageData partsManageData, CharacterData current,
			CharacterData target) throws IOException {
		if (target == null || !target.isValid()) {
			throw new IllegalArgumentException();
		}
		
		if (partsImageContents == null || partsImageContents.isEmpty()
				|| partsManageData == null || partsManageData.isEmpty()) {
			// インポートするパーツが存在しないか、管理情報がない場合は更新しようがないので何もしないで戻る.
			return;
		}

		PartsInfoXMLReader xmlReader = new PartsInfoXMLReader();
		
		PartsManageData mergedPartsManagedData;
		if (current != null && current.isValid()) {
			// 現在のプロファイルからパーツ管理情報を取得する.
			mergedPartsManagedData = xmlReader.loadPartsManageData(current.getDocBase());
		} else {
			// 新規の場合は空
			mergedPartsManagedData = new PartsManageData();
		}
		
		// インポート対象のパーツに該当するパーツ管理情報のみを取り出して追記する.
		for (PartsImageContent partsImageContent : partsImageContents) {
			String partsName = partsImageContent.getPartsName();
			for (CategoryLayerPair catLayerPair : partsImageContent.getCategoryLayerPairs()) {
				PartsCategory partsCategory = catLayerPair.getPartsCategory();
				String categoryId = partsCategory.getCategoryId();
				
				PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(partsName, categoryId);
				
				PartsAuthorInfo partsAuthorInfo = partsManageData.getPartsAuthorInfo(partsKey);
				PartsManageData.PartsVersionInfo versionInfo = partsManageData.getVersion(partsKey);
				String localizedName = partsManageData.getLocalizedName(partsKey);
				
				if (partsAuthorInfo != null || versionInfo != null || localizedName != null) {
					// いずれかの情報の登録がある場合、パーツ管理情報として追記する.
					mergedPartsManagedData.putPartsInfo(partsKey, localizedName, partsAuthorInfo, versionInfo);
				}
			}
		}

		// パーツ管理情報を更新する.
		PartsInfoXMLWriter partsInfoXMLWriter = new PartsInfoXMLWriter();
		partsInfoXMLWriter.savePartsManageData(target.getDocBase(),
				mergedPartsManagedData);
	}
}
