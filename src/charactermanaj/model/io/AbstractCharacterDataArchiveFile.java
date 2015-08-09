package charactermanaj.model.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import charactermanaj.graphics.io.PNGFileImageHeader;
import charactermanaj.graphics.io.PNGFileImageHeaderReader;
import charactermanaj.model.CharacterData;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.io.CharacterDataDefaultProvider.DefaultCharacterDataVersion;

public abstract class AbstractCharacterDataArchiveFile
		implements
			CharacterDataArchiveFile {

	private static final Logger logger = Logger
			.getLogger(AbstractCharacterDataArchiveFile.class.getName());

	protected File archiveFile;

	protected String rootPrefix = "";

	public interface FileContent {

		String getEntryName();

		long lastModified();

		InputStream openStream() throws IOException;

	}

	@Override
	public String toString() {
		return "CharacterDataArchiveFile(file=" + archiveFile + ")";
	}

	public static final class CategoryLayerPair {

		private PartsCategory partsCategory;

		private Layer layer;

		public CategoryLayerPair(PartsCategory partsCategory, Layer layer) {
			if (partsCategory == null || layer == null) {
				throw new IllegalArgumentException();
			}
			this.partsCategory = partsCategory;
			this.layer = layer;
		}

		@Override
		public int hashCode() {
			return partsCategory.hashCode() ^ layer.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj != null && obj instanceof CategoryLayerPair) {
				CategoryLayerPair o = (CategoryLayerPair) obj;
				return partsCategory.equals(o.partsCategory)
						&& layer.equals(o.layer);
			}
			return false;
		}

		public Layer getLayer() {
			return layer;
		}

		public PartsCategory getPartsCategory() {
			return partsCategory;
		}

		@Override
		public String toString() {
			return "(" + partsCategory + ":" + layer + ")";
		}
	}

	public static final class PartsImageContent implements FileContent {

		private final FileContent fileContent;

		private final Collection<CategoryLayerPair> categoryLayerPairs;

		private final String dirName;

		private final String partsName;

		private final String fileName;

		private final PNGFileImageHeader pngFileImageHeader;

		/**
		 * パーツイメージを示す.<br>
		 * 
		 * @param fileContent
		 *            ファイルコンテンツ
		 * @param categoryLayerPairs
		 *            カテゴリとレイヤーのペア
		 * @param partsName
		 *            ファイル名(ファイル名のみ。拡張子を含まない。パーツ名の元として用いることを想定。)
		 * @param pngFileImageHeader
		 *            PNGファイルヘッダ
		 */
		protected PartsImageContent(FileContent fileContent,
				Collection<CategoryLayerPair> categoryLayerPairs,
				String fileName, String partsName,
				PNGFileImageHeader pngFileImageHeader) {
			if (fileContent == null || categoryLayerPairs == null
					|| categoryLayerPairs.isEmpty() || fileName == null
					|| partsName == null || pngFileImageHeader == null) {
				throw new IllegalArgumentException();
			}
			this.fileContent = fileContent;
			this.categoryLayerPairs = Collections
					.unmodifiableCollection(categoryLayerPairs);
			this.fileName = fileName;
			this.partsName = partsName;
			this.pngFileImageHeader = pngFileImageHeader;

			CategoryLayerPair categoryLayerPair = categoryLayerPairs.iterator()
					.next();
			dirName = categoryLayerPair.getLayer().getDir();
		}

		@Override
		public int hashCode() {
			return getEntryName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj != null && obj instanceof PartsImageContent) {
				return getEntryName().equals(
						((PartsImageContent) obj).getEntryName());
			}
			return false;
		}

		public Collection<CategoryLayerPair> getCategoryLayerPairs() {
			return categoryLayerPairs;
		}

		public String getDirName() {
			return dirName;
		}

		public String getEntryName() {
			return fileContent.getEntryName();
		}

		public String getFileName() {
			return fileName;
		}

		public String getPartsName() {
			return partsName;
		}

		public PNGFileImageHeader getPngFileImageHeader() {
			return pngFileImageHeader;
		}

		public long lastModified() {
			return fileContent.lastModified();
		}

		public InputStream openStream() throws IOException {
			return fileContent.openStream();
		}

		@Override
		public String toString() {
			return fileContent.getEntryName();
		}
	}

	protected HashMap<String, FileContent> entries = new HashMap<String, FileContent>();

	protected AbstractCharacterDataArchiveFile(File archiveFile) {
		if (archiveFile == null) {
			throw new IllegalArgumentException();
		}
		this.archiveFile = archiveFile;
	}

	public File getArchiveFile() {
		return this.archiveFile;
	}

	protected void addEntry(FileContent fileContent) {
		if (fileContent == null) {
			throw new IllegalArgumentException();
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, fileContent.getEntryName());
		}
		entries.put(fileContent.getEntryName(), fileContent);
	}

	/**
	 * アーカイブファイルをベースとしたURIを返す.<br>
	 * 
	 * @param name
	 *            コンテンツ名
	 * @return URI
	 * @throws IOException
	 *             URIを生成できない場合
	 */
	protected URI getContentURI(String name) throws IOException {
		try {
			URI baseURI = archiveFile.toURI();
			return new URI("jar:" + baseURI.toString() + "/!" + name);

		} catch (URISyntaxException ex) {
			IOException iex = new IOException(ex.getMessage());
			iex.initCause(ex);
			throw iex;
		}
	}

	/**
	 * 指定したコンテンツが存在するか?
	 * 
	 * @param name
	 *            コンテンツ名
	 * @return 存在すればtrue、存在しなければfalse
	 */
	public boolean hasContent(String name) {
		return entries.containsKey(name);
	}

	/**
	 * 指定したコンテンツを取得する.<br>
	 * 存在しない場合はnullを返す.<br>
	 * 
	 * @param name
	 *            コンテンツ名
	 * @return 存在すればコンテンツ、存在しなければnull
	 */
	public FileContent getContent(String name) {
		return entries.get(name);
	}

	public String getRootPrefix() {
		return this.rootPrefix;
	}

	/**
	 * アーカイブのルート上に単一のフォルダしかない場合、そのフォルダを真のルートとして設定する.<br>
	 * 返されるルート名には末尾に「/」を含む.<br>
	 * ルート上に複数のフォルダがあるかファイルが存在する場合は空文字を設定する.<br>
	 */
	protected void searchRootPrefix() {
		HashSet<String> dirs = new HashSet<String>();
		for (String name : entries.keySet()) {
			int pos = name.indexOf('/');
			if (pos < 0) {
				// ルート上にファイルがあるので、ここがルート
				rootPrefix = "";
				return;
			}
			if (pos >= 0) {
				String dir = name.substring(0, pos + 1);
				dirs.add(dir);
			}
		}
		if (dirs.size() == 1) {
			// ルート上に単一のフォルダしかないので、
			// このフォルダが真のルート
			rootPrefix = dirs.iterator().next();
			return;
		}
		// ルート上に複数のフォルダがあるので、ここがルート
		rootPrefix = "";
	}

	/**
	 * 指定したディレクトリ(フルパス指定)のファイルのみを取り出す.<br>
	 * サブフォルダは含まない.<br>
	 * ディレクトリに空文字またはnullまたは「/」を指定した場合はルートを意味する.<br>
	 * 
	 * @param dir
	 *            ディレクトリ
	 * @return ファイルへのフルパスのコレクション
	 */
	public Map<String, FileContent> getFiles(String dir) {
		if (dir == null) {
			dir = "";
		}
		if (dir.length() > 0 && !dir.endsWith("/")) {
			dir += "/";
		}
		if (dir.equals("/")) {
			dir = ""; // アーカイブ内コンテンツのパスは先頭は「/」ではないため。
		}

		HashMap<String, FileContent> files = new HashMap<String, FileContent>();

		int ep = dir.length();
		for (Map.Entry<String, FileContent> entry : entries.entrySet()) {
			String name = entry.getKey();
			FileContent fileContent = entry.getValue();
			if (name.startsWith(dir)) {
				String suffix = name.substring(ep);
				int sep = suffix.indexOf('/');
				if (sep < 0) {
					files.put(name, fileContent);
				}
			}
		}

		return files;
	}

	/**
	 * キャラクター定義を読み込む.<br>
	 * アーカイブに存在しないか、妥当性がない場合はnullを返す.<br>
	 * 
	 * @return キャラクター定義、もしくはnull
	 */
	public CharacterData readCharacterData() {
		FileContent characterFile = entries.get(rootPrefix
				+ CharacterDataPersistent.CONFIG_FILE);
		if (characterFile == null) {
			return null;
		}

		CharacterDataXMLReader xmlReader = new CharacterDataXMLReader();

		try {
			// character.xmlを読み込む
			CharacterData cd;
			InputStream is = characterFile.openStream();
			try {
				URI docBase = getContentURI(rootPrefix
						+ CharacterDataPersistent.CONFIG_FILE);
				cd = xmlReader.loadCharacterDataFromXML(is, docBase);

			} finally {
				is.close();
			}

			return cd;

		} catch (Exception ex) {
			logger.log(Level.INFO, "character.xml load failed.", ex);
			return null;
		}
	}

	/**
	 * キャラクター定義をINIファイルより読み取る.<br>
	 * アーカイブのコンテンツルート上のcharacter.iniを探す.<br>
	 * それがなければ、アーカイブ上のどこかにある/character.iniを探して、もっとも短い場所にある1つを採用する.<br>
	 * character.iniが何処にも存在しないか、読み取り時にエラーとなった場合はnullを返す.<br>
	 * 「キャラクターなんとか機 v2.2」の設定ファイルを想定している.<br>
	 * ただし、character.iniの下にeye_colorフォルダがある場合は「ver3」とみなす。<br>
	 * (設定ファイルの形式はv2, v3の間で変わらず)
	 * 
	 * @return キャラクター定義、もしくはnull
	 */
	public CharacterData readCharacterINI() {
		FileContent characterFile = null;
		characterFile = entries.get(rootPrefix
				+ CharacterDataPersistent.COMPATIBLE_CONFIG_NAME);

		// どこかにあるcharacter.iniを探す
		// および、eye_colorフォルダがあるか？(あればver3形式とみなす。)
		boolean hasEyeColorFolder = false;
		ArrayList<String> characterInis = new ArrayList<String>();
		for (Map.Entry<String, FileContent> entry : entries.entrySet()) {
			String entryName = entry.getKey();
			if (entryName.endsWith("/"
					+ CharacterDataPersistent.COMPATIBLE_CONFIG_NAME)) {
				characterInis.add(entryName);
			}
			if (entryName.contains("/eye_color/")
					|| entryName.endsWith("/eye_color")) {
				hasEyeColorFolder = true;
			}
		}

		if (characterFile == null) {
			// もっとも短い名前のものを採用
			Collections.sort(characterInis);
			if (characterInis.size() > 0) {
				characterFile = entries.get(characterInis.get(0));
			}
		}
		if (characterFile == null) {
			// character.iniがないので何もしない.
			return null;
		}

		DefaultCharacterDataVersion version;
		if (hasEyeColorFolder) {
			// "eye_color"フォルダがあればver3形式とみなす
			version = DefaultCharacterDataVersion.V3;
		} else {
			version = DefaultCharacterDataVersion.V2;
		}

		try {
			// デフォルトのキャラクター定義を構築する.
			CharacterData cd;
			InputStream is = characterFile.openStream();
			try {
				CharacterDataIniReader iniReader = new CharacterDataIniReader();
				cd = iniReader.readCharacterDataFromIni(is, version);
			} finally {
				is.close();
			}

			// docBaseを設定する.
			URI docBase = getContentURI(rootPrefix
					+ CharacterDataPersistent.COMPATIBLE_CONFIG_NAME);
			cd.setDocBase(docBase);

			return cd;

		} catch (Exception ex) {
			logger.log(Level.INFO, "character.ini load failed", ex);
			return null;
		}
	}

	/**
	 * お気に入りを読み込みキャラクター定義に追加する.<br>
	 * アーカイブにお気に入り(favorites.xml)が存在しなければ何もしない.<br>
	 * 読み取り中に失敗した場合は、その時点で読み込みを止めて戻る.<br>
	 * 
	 * @param characterData
	 *            キャラクター定義(お気に入りが追加される)
	 */
	public void readFavorites(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException("characterDataにnullは指定できません。");
		}
		FileContent favoritesXml = entries.get(rootPrefix + "favorites.xml");
		if (favoritesXml == null) {
			// favorites.xmlがなければ何もしない
			return;
		}

		CharacterDataXMLReader xmlReader = new CharacterDataXMLReader();

		try {
			// favorites.xmlを読み込む
			InputStream is = favoritesXml.openStream();
			try {
				xmlReader.loadPartsSet(characterData, is);

			} catch (Exception ex) {
				logger.log(Level.INFO, "favorites.xml load failed.", ex);

			} finally {
				is.close();
			}

		} catch (Exception ex) {
			logger.log(Level.INFO, "favorites.xml load failed", ex);
		}
	}

	/**
	 * サンプルピクチャを読み込む.<br>
	 * アーカイブに存在しないか、画像として読み取れなかった場合はnull
	 * 
	 * @return サンプルピクチャ、もしくはnull
	 */
	public BufferedImage readSamplePicture() {
		FileContent samplePictureFile = entries.get(rootPrefix + "preview.png");
		if (samplePictureFile == null) {
			Map<String, FileContent> files = getFiles(rootPrefix);

			samplePictureFile = files.get("preview.jpg");
			if (samplePictureFile == null) {
				samplePictureFile = files.get("preview.jpeg");
			}

			if (samplePictureFile == null) {
				for (Map.Entry<String, FileContent> entry : files.entrySet()) {
					String name = entry.getKey();
					if (name.endsWith(".jpg") || name.endsWith(".jpeg")
							|| name.endsWith(".png")) {
						samplePictureFile = entry.getValue();
						break;
					}
				}
			}
		}
		if (samplePictureFile == null) {
			return null;
		}

		try {
			BufferedImage pic;
			InputStream is = samplePictureFile.openStream();
			try {
				pic = ImageIO.read(is);
			} finally {
				is.close();
			}

			return pic;

		} catch (Exception ex) {
			logger.log(Level.INFO, "sample picture load failed.", ex);
			return null;
		}
	}

	/**
	 * アーカイブにある「readme.txt」、もしくは「readme」というファイルをテキストファイルとして読み込む.<br>
	 * readme.txtが優先される。ともに存在しない場合はnull.<br>
	 * 改行コードはプラットフォーム固有の改行コードに変換して返される.<br>
	 * 読み取れなかった場合はnull.<br>
	 * 
	 * @return テキスト、もしくはnull
	 */
	public String readReadMe() {

		Locale locale = Locale.getDefault();
		String lang = locale.getLanguage();

		ArrayList<FileContent> candiates = new ArrayList<FileContent>();

		Map<String, FileContent> files = getFiles(rootPrefix);
		for (String findName : new String[]{"readme_" + lang + ".txt",
				"readme_" + lang, "readme.txt", "readme", null}) {
			for (Map.Entry<String, FileContent> entry : files.entrySet()) {
				String name = entry.getKey().toLowerCase();
				if (findName == null && name.endsWith(".txt")) {
					candiates.add(entry.getValue());
					break;
				}
				if (name.equals(findName)) {
					candiates.add(entry.getValue());
				}
			}
		}
		if (candiates.isEmpty()) {
			return null;
		}

		// みつかったファイルについて読み込みを優先順で試行する.
		for (FileContent file : candiates) {
			try {
				return readTextUTF16(file);

			} catch (Exception ex) {
				logger.log(Level.WARNING, "read file failed. :" + file, ex);
				// 無視して次の候補を試行する.
			}
		}

		// すべて失敗したか、そもそもファイルがみつからなかった。
		return null;
	}

	/**
	 * ファイルをテキストとして読み取り、返す.<br>
	 * UTF-16LE/BE/UTF-8についてはBOMにより判定する.<br>
	 * BOMがない場合はUTF-16/8ともに判定できない.<br>
	 * BOMがなければMS932もしくはEUC_JPであると仮定して読み込む.<br>
	 * 改行コードはプラットフォーム固有の改行コードに変換して返される.<br>
	 * 
	 * @param name
	 *            コンテンツ名
	 * @return テキスト、コンテンツが存在しない場合はnull
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public String readTextFile(String name) throws IOException {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		FileContent content = entries.get(name);
		if (content == null) {
			return null;
		}
		return readTextUTF16(content);
	}

	/**
	 * ファイルをテキストとして読み取り、返す.<br>
	 * UTF-16LE/BE/UTF-8についてはBOMにより判定する.<br>
	 * BOMがない場合はUTF-16/8ともに判定できない.<br>
	 * BOMがなければMS932もしくはEUC_JPであると仮定して読み込む.<br>
	 * 
	 * @param content
	 *            コンテンツ
	 * @return テキスト
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public String readTextUTF16(FileContent content) throws IOException {
		if (content == null) {
			throw new IllegalArgumentException();
		}
		return TextReadHelper.readTextTryEncoding(content.openStream());
	}

	/**
	 * キャラクター定義のカテゴリと、そのレイヤー情報から、画像のディレクトリの一覧をアーカイブ上のディレクトリの一覧として返す.<br>
	 * ディレクトリの末尾は常にスラ付きとなる.<br>
	 * enabledRootPefixがtrueの場合、ディレクトリの先頭はアーカイブのコンテンツルートとなる.<br>
	 * 同一のディレクトリに対して複数のレイヤー(複数カテゴリを含む)が参照している場合、それらを含めて返す.<br>
	 * 参照されているディレクトリがない場合は返される結果には含まれない.<br>
	 * 
	 * @param characterData
	 *            キャラクター定義
	 * @param enabledRootPrefix
	 *            ルートプレフィックス(アーカイブのコンテンツルート)を付与する場合
	 * @return 
	 *         パーツで使用する画像のディレクトリとして認識されるディレクトリの一覧、キーはアーカイブのディレクトリ位置、値は参照する1つ以上のレイヤー
	 */
	protected Map<String, Collection<CategoryLayerPair>> getLayerDirs(
			CharacterData characterData, boolean enabledRootPrefix) {
		if (characterData == null) {
			return Collections.emptyMap();
		}
		// イメージディレクトリの一覧
		String rootPrefix = getRootPrefix();
		HashMap<String, Collection<CategoryLayerPair>> layerDirs = new HashMap<String, Collection<CategoryLayerPair>>();
		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			for (Layer layer : partsCategory.getLayers()) {
				String dir = layer.getDir();
				if (!dir.endsWith("/")) {
					dir += "/"; // スラ付きにする.
				}
				if (enabledRootPrefix) {
					// アーカイブのルートコンテキストからのディレクトリ位置とする.
					dir = rootPrefix + dir;
				}
				Collection<CategoryLayerPair> sameDirLayers = layerDirs
						.get(dir);
				if (sameDirLayers == null) {
					sameDirLayers = new ArrayList<CategoryLayerPair>();
					layerDirs.put(dir, sameDirLayers);
				}
				sameDirLayers.add(new CategoryLayerPair(partsCategory, layer));
			}
		}
		return layerDirs;
	}

	/**
	 * アーカイブに含まれるフォルダをもつpngファイルからパーツイメージを取得する.<br>
	 * 
	 * @param インポート先のキャラクターデータ
	 *            、フォルダ名などを判別するため。nullの場合はディレクトリなしとみなす.<br>
	 * @param newly
	 *            新規インポート用であるか?(新規でない場合は引数で指定したキャラクターセットと同じパーツは読み込まれない).
	 *            (アーカイブファイルからの読み込みでは無視される)
	 * @return パーツイメージコンテンツのコレクション、なければ空
	 */
	public Collection<PartsImageContent> getPartsImageContents(
			CharacterData characterData, boolean newly) {
		// コンテンツルートからの絶対位置指定でパーツイメージを取得する.
		Collection<PartsImageContent> results = getPartsImageContentsStrict(characterData);
		if (results.isEmpty()) {
			// コンテンツルートからの絶対位置にパーツがない場合は、任意のディレクトリ位置からパーツイメージを推定する.
			results = getPartsImageContentsLazy(characterData);
		}
		return results;
	}

	/**
	 * コンテンツルートからの絶対位置のフォルダからpngファイルからパーツイメージを取得する.<br>
	 * 
	 * @param インポート先のキャラクターデータ
	 *            、フォルダ名などを判別するため。nullの場合はディレクトリなしとみなす.<br>
	 * @return パーツイメージコンテンツのコレクション、なければ空
	 */
	protected Collection<PartsImageContent> getPartsImageContentsStrict(
			CharacterData characterData) {
		final Map<String, Collection<CategoryLayerPair>> layerDirMap = getLayerDirs(
				characterData, true);

		CategoryLayerPairResolveStrategy strategy = new CategoryLayerPairResolveStrategy() {
			public Collection<CategoryLayerPair> resolveCategoryLayerPairs(
					String dir) {
				Collection<CategoryLayerPair> categoryLayerPairs = layerDirMap
						.get(dir);
				if (categoryLayerPairs == null || categoryLayerPairs.isEmpty()) {
					// ディレクトリ名に一致するものがないので、この画像は無視する
					return null;
				}
				return categoryLayerPairs;
			}
		};

		return getPartsImageContents(strategy);
	}

	/**
	 * アーカイブに含まれる任意のフォルダからpngファイルからパーツイメージを取得する.<br>
	 * ディレクトリ名の大文字・小文字は区別されません.<br>
	 * 
	 * @param インポート先のキャラクターデータ
	 *            、フォルダ名などを判別するため。nullの場合はディレクトリなしとみなす.<br>
	 * @return パーツイメージコンテンツのコレクション、なければ空
	 */
	protected Collection<PartsImageContent> getPartsImageContentsLazy(
			CharacterData characterData) {
		final Map<String, Collection<CategoryLayerPair>> layerDirMap = getLayerDirs(
				characterData, false);

		CategoryLayerPairResolveStrategy strategy = new CategoryLayerPairResolveStrategy() {
			public Collection<CategoryLayerPair> resolveCategoryLayerPairs(
					String dir) {
				dir = (dir == null) ? "" : dir.toLowerCase();
				for (Map.Entry<String, Collection<CategoryLayerPair>> entry : layerDirMap
						.entrySet()) {
					String dirSuffix = entry.getKey().toLowerCase();
					Collection<CategoryLayerPair> categoryLayerPairs = entry
							.getValue();
					if (dir.endsWith(dirSuffix)) {
						return categoryLayerPairs;
					}
				}
				return null;
			}
		};

		return getPartsImageContents(strategy);
	}

	/**
	 * ディレクトリ名からカテゴリとレイヤーを取得するためのインターフェイス.<br>
	 * 
	 * @author seraphy
	 */
	protected interface CategoryLayerPairResolveStrategy {

		/**
		 * ディレクトリを指定して、それに該当するカテゴリとレイヤーペアのコレクションを返します.<br>
		 * 同一のディレクトリに対して複数のレイヤーが割り当てられている可能性があるためコレクションで返されます.<br>
		 * 空のコレクションにはなりません.<br>
		 * レイヤーとして認識されていないディレクトリの場合はnullを返します.<br>
		 * 
		 * @param dir
		 *            ディレクトリ
		 * @return カテゴリ・レイヤーのペアのコレクション、またはnull (空のコレクションにはならない。)
		 */
		Collection<CategoryLayerPair> resolveCategoryLayerPairs(String dir);

	}

	/**
	 * アーカイブに含まれるフォルダをもつpngファイルからパーツイメージを取得する。
	 * 
	 * @param strategy
	 *            ディレクトリが売れ入れ可能であるか判断するストラテジー
	 * @return パーツイメージコンテンツのコレクション、なければ空
	 */
	protected Collection<PartsImageContent> getPartsImageContents(
			CategoryLayerPairResolveStrategy strategy) {
		if (strategy == null) {
			throw new IllegalArgumentException();
		}

		ArrayList<PartsImageContent> results = new ArrayList<PartsImageContent>();
		for (Map.Entry<String, FileContent> entry : entries.entrySet()) {
			String name = entry.getKey();
			FileContent fileContent = entry.getValue();

			String[] split = name.split("/");
			if (split.length < 2) {
				// 最低でもフォルダ下になければならない
				continue;
			}
			String lastName = split[split.length - 1];
			if (!lastName.toLowerCase().endsWith(".png")) {
				// png拡張子でなければならない
				continue;
			}

			// ディレクトリ名
			String dir = name.substring(0, name.length() - lastName.length());

			// ディレクトリ名から対応するレイヤーを取得します.
			Collection<CategoryLayerPair> categoryLayerPairs = strategy
					.resolveCategoryLayerPairs(dir);
			if (categoryLayerPairs == null) {
				// パーツイメージのディレクトリとして定義されていない場合は、この画像は無視される.
				continue;
			}

			// PNGファイルヘッダの取得と確認
			PNGFileImageHeader pngFileHeader = readPNGFileHeader(fileContent);
			if (pngFileHeader == null) {
				// PNGファイルとして不正なものは無視する.
				logger.log(Level.WARNING, "invalid png: " + name);
				continue;
			}

			// パーツ名(拡張子を除いたもの)
			String partsName;
			int extpos = lastName.lastIndexOf('.');
			partsName = lastName.substring(0, extpos);

			PartsImageContent partsImageContent = new PartsImageContent(
					fileContent, categoryLayerPairs, lastName, partsName,
					pngFileHeader);

			results.add(partsImageContent);
		}
		return results;
	}

	/**
	 * PNGファイルとしてファイルを読み込みPNGヘッダ情報を返します.<br>
	 * PNGでないか、ファイルの読み込みに失敗した場合はnullを返します.<br>
	 * 
	 * @param fileContent
	 *            画像ファイル
	 * @return PNGヘッダ情報、またはnull
	 */
	protected PNGFileImageHeader readPNGFileHeader(FileContent fileContent) {
		PNGFileImageHeaderReader pngHeaderReader = PNGFileImageHeaderReader
				.getInstance();
		PNGFileImageHeader pngFileHeader = null;
		try {
			InputStream is = fileContent.openStream();
			try {
				pngFileHeader = pngHeaderReader.readHeader(is);
			} finally {
				is.close();
			}
		} catch (IOException ex) {
			logger.log(Level.WARNING, "not png header. " + fileContent, ex);
			pngFileHeader = null;
			// 無視する.
		}
		return pngFileHeader;
	}

	/**
	 * アーカイブに含まれるparts-info.xmlを読み込み返す.<br>
	 * 存在しなければ空のインスタンスを返す.<br>
	 * 
	 * @return パーツ管理データ
	 * @throws IOException
	 */
	public PartsManageData getPartsManageData() throws IOException {
		FileContent content = entries.get(rootPrefix + "parts-info.xml");
		if (content == null) {
			return new PartsManageData();
		}

		PartsManageData partsManageData;

		InputStream is = content.openStream();
		try {
			PartsInfoXMLReader xmlWriter = new PartsInfoXMLReader();
			partsManageData = xmlWriter.loadPartsManageData(is);
		} finally {
			is.close();
		}

		return partsManageData;
	}
}
