package charactermanaj.model.io;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import charactermanaj.graphics.io.FileImageResource;
import charactermanaj.graphics.io.ImageLoader;
import charactermanaj.graphics.io.ImageSaveHelper;
import charactermanaj.graphics.io.LoadedImage;
import charactermanaj.model.AppConfig;
import charactermanaj.model.CharacterData;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.io.CharacterDataDefaultProvider.DefaultCharacterDataVersion;
import charactermanaj.util.DirectoryConfig;
import charactermanaj.util.FileNameNormalizer;
import charactermanaj.util.FileUserData;
import charactermanaj.util.UserData;

public class CharacterDataPersistent {

	/**
	 * キャラクター定義ファイル名
	 */
	public static final String CONFIG_FILE = "character.xml";

	/**
	 * キャラクターなんとか機用のiniファイル名
	 */
	public static final String COMPATIBLE_CONFIG_NAME = "character.ini";

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(CharacterDataPersistent.class.getName());

	/**
	 * キャラクターデータを格納したXMLのリーダー
	 */
	private final CharacterDataXMLReader characterDataXmlReader = new CharacterDataXMLReader();

	/**
	 * キャラクターデータを格納したXMLのライタ
	 */
	private final CharacterDataXMLWriter characterDataXmlWriter = new CharacterDataXMLWriter();

	/**
	 * サンプルイメージファイル名
	 */
	private static final String SAMPLE_IMAGE_FILENAME = "preview.png";

	/**
	 * プロファイルの列挙時のエラーハンドラ.<br>
	 * 
	 * @author seraphy
	 */
	public interface ProfileListErrorHandler {

		/**
		 * エラーが発生したことを通知される
		 * 
		 * @param baseDir
		 *            読み込み対象のXMLのファイル
		 * @param ex
		 *            例外
		 */
		void occureException(File baseDir, Throwable ex);
	}

	/**
	 * プライベートコンストラクタ.<br>
	 * シングルトン実装であるため、一度だけ呼び出される.
	 */
	private CharacterDataPersistent() {
		super();
	}

	/**
	 * シングルトン
	 */
	private static final CharacterDataPersistent singleton = new CharacterDataPersistent();

	/**
	 * インスタンスを取得する
	 * 
	 * @return インスタンス
	 */
	public static CharacterDataPersistent getInstance() {
		return singleton;
	}

	/**
	 * キャラクターデータを新規に保存する.<br>
	 * REVがnullである場合は保存に先立ってランダムにREVが設定される.<br>
	 * 保存先ディレクトリはユーザー固有のキャラクターデータ保存先のディレクトリにキャラクター定義のIDを基本とする ディレクトリを作成して保存される.<br>
	 * ただし、そのディレクトリがすでに存在する場合はランダムな名前で決定される.<br>
	 * 実際のxmlの保存先にあわせてDocBaseが設定されて返される.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ (IDは設定済みであること.それ以外はvalid, editableであること。)
	 * @throws IOException
	 *             失敗
	 */
	public void createProfile(CharacterData characterData) throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		String id = characterData.getId();
		if (id == null || id.trim().length() == 0) {
			throw new IOException("missing character-id:" + characterData);
		}

		// ユーザー個別のキャラクターデータ保存先ディレクトリを取得
		DirectoryConfig dirConfig = DirectoryConfig.getInstance();
		File charactersDir = dirConfig.getCharactersDir();
		if (!charactersDir.exists()) {
			if (!charactersDir.mkdirs()) {
				throw new IOException("can't create the characters directory. "
						+ charactersDir);
			}
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "check characters-dir: " + charactersDir
					+ ": exists=" + charactersDir.exists());
		}

		// 新規に保存先ディレクトリを作成.
		// 同じ名前のディレクトリがある場合は日付+連番をつけて衝突を回避する
		File baseDir = null;
		String suffix = "";
		String name = characterData.getName();
		if (name == null) {
			// 表示名が定義されていなければIDで代用する.(IDは必須)
			name = characterData.getId();
		}
		for (int retry = 0;; retry++) {
			baseDir = new File(charactersDir, name + suffix);
			if (!baseDir.exists()) {
				break;
			}
			if (retry > 100) {
				throw new IOException("character directory conflict.:"
						+ baseDir);
			}
			// 衝突回避の末尾文字を設定
			suffix = generateSuffix(retry);
		}
		if (!baseDir.exists()) {
			if (!baseDir.mkdirs()) {
				throw new IOException("can't create directory. " + baseDir);
			}
			logger.log(Level.INFO, "create character-dir: " + baseDir);
		}

		// 保存先を確認
		File characterPropXML = new File(baseDir, CONFIG_FILE);
		if (characterPropXML.exists() && !characterPropXML.isFile()) {
			throw new IOException("character.xml is not a regular file.:"
					+ characterPropXML);
		}
		if (characterPropXML.exists() && !characterPropXML.canWrite()) {
			throw new IOException("character.xml is not writable.:"
					+ characterPropXML);
		}

		// DocBaseを実際の保存先に更新
		URI docBase = characterPropXML.toURI();
		characterData.setDocBase(docBase);

		// リビジョンが指定されてなければ新規にリビジョンを割り当てる。
		if (characterData.getRev() == null) {
			characterData.setRev(generateRev());
		}

		// 保存する.
		saveCharacterDataToXML(characterData);

		// ディレクトリを準備する
		preparePartsDir(characterData);
	}

	/**
	 * リビジョンを生成して返す.
	 * 
	 * @return リビジョン用文字列
	 */
	public String generateRev() {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		return fmt.format(new Date());
	}

	/**
	 * 衝突回避用の末尾文字を生成する.
	 * 
	 * @param retryCount
	 *            リトライ回数
	 * @return 末尾文字
	 */
	protected String generateSuffix(int retryCount) {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		String suffix = "_" + fmt.format(new Date());
		if (retryCount > 0) {
			suffix = suffix + "_" + retryCount;
		}
		return suffix;
	}

	/**
	 * キャラクターデータを更新する.
	 * 
	 * @param characterData
	 *            キャラクターデータ(有効かつ編集可能であること)
	 * @throws IOException
	 *             失敗
	 */
	public void updateProfile(CharacterData characterData) throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		characterData.checkWritable();
		if (!characterData.isValid()) {
			throw new IOException("invalid profile: " + characterData);
		}

		// 保存する
		saveCharacterDataToXML(characterData);

		// ディレクトリを準備する
		preparePartsDir(characterData);
	}

	/**
	 * キャラクターデータのパーツイメージを保存するディレクトリを準備する
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @param baseDir
	 *            ベースディレクトリ
	 * @throws IOException
	 *             失敗
	 */
	protected void preparePartsDir(CharacterData characterData)
			throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		characterData.checkWritable();
		if (!characterData.isValid()) {
			throw new IOException("invalid profile: " + characterData);
		}

		URI docBase = characterData.getDocBase();
		if (!"file".equals(docBase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません。:" + docBase);
		}
		File docBaseFile = new File(docBase);
		File baseDir = docBaseFile.getParentFile();

		if (!baseDir.exists()) {
			if (!baseDir.mkdirs()) {
				throw new IOException("can't create directory. " + baseDir);
			}
		}
		for (PartsCategory partsCategory : characterData.getPartsCategories()) {
			for (Layer layer : partsCategory.getLayers()) {
				File layerDir = new File(baseDir, layer.getDir());
				if (!layerDir.exists()) {
					if (!layerDir.mkdirs()) {
						throw new IOException("can't create directory. "
								+ layerDir);
					}
				}
			}
		}
	}

	/**
	 * キャラクターデータを読み込んだ場合に返されるコールバック
	 */
	public interface ListProfileCallback {

		/**
		 * キャラクターデータを読み込んだ場合.<br>
		 * 戻り値がfalseの場合は読み込みを以降の読み込みを中断します.<br>
		 * (ただし、すでに読み込み開始している分については中断されません.)
		 * 
		 * @param characterData
		 * @return 継続する場合はtrue、中止する場合はfalse
		 */
		boolean receiveCharacterData(CharacterData characterData);

		/**
		 * キャラクターデータの読み込みに失敗した場合.<br>
		 * 戻り値がfalseの場合は読み込みを以降の読み込みを中断します.<br>
		 * (ただし、すでに読み込み開始している分については中断されません.)
		 * 
		 * @param dir
		 *            読み込み対象ディレクトリ
		 * @param ex
		 *            例外の内容
		 * @return 継続する場合はtrue、中止する場合はfalse
		 */
		boolean occureException(File dir, Exception ex);
	}

	/**
	 * 指定したディレクトリの下のサブフォルダに、character.iniがある場合は、
	 * キャラクターなんとか機のディレクトリとして、標準のcharacter.xmlを生成する.<br>
	 * ただし、書き込み禁止の場合は何もしない.<br>
	 * すでにcharacter.xmlがある場合も何もしない.<br>
	 * 
	 * @param dataDir
	 *            キャラクターなんとか機のデータディレクトリ
	 */
	public void convertCharacterNantokaIniToXml(File dataDir) {
		if (dataDir == null || !dataDir.isDirectory() || !dataDir.canWrite()) {
			return;
		}

		File[] dirs = dataDir.listFiles();
		if (dirs == null) {
			dirs = new File[0];
		}
		for (File dir : dirs) {
			if (!dir.isDirectory()) {
				continue;
			}
			File characterXmlFile = new File(dir,
					CharacterDataPersistent.CONFIG_FILE);
			if (characterXmlFile.exists()) {
				// すでにキャラクター定義XMLが存在する場合はスキップする.
				continue;
			}

			File characterIniFile = new File(dir,
					CharacterDataPersistent.COMPATIBLE_CONFIG_NAME);
			if (characterIniFile.exists() && characterIniFile.canWrite()
					&& dir.canWrite()) {
				// character.iniが存在し、書き込み可能であれば、それをcharacter.xmlに変換する.

				// eye_colorフォルダがあるか？
				File eyeColorFolder = new File(dir, "eye_color");
				boolean hasEyeColorFolder = eyeColorFolder.exists()
						&& eyeColorFolder.isDirectory();

				DefaultCharacterDataVersion version;
				if (hasEyeColorFolder) {
					// eye_colorフォルダがあればver3形式とみなす.
					version = DefaultCharacterDataVersion.V3;
				} else {
					version = DefaultCharacterDataVersion.V2;
				}

				// readmeがあるか？
				String readme = null;
				File readmeFile = new File(dir, "readme.txt");
				if (readmeFile.exists() && readmeFile.canRead()) {
					try {
						readme = TextReadHelper
								.readTextTryEncoding(new FileInputStream(
										readmeFile));

					} catch (IOException ex) {
						logger.log(Level.WARNING, ex.toString(), ex);
					}
				}

				try {
					convertFromCharacterIni(characterIniFile, characterXmlFile,
							version, readme);

				} catch (Exception ex) {
					logger.log(Level.WARNING, "character.xmlの生成に失敗しました。:"
							+ characterXmlFile, ex);
				}
			}
		}
	}

	/**
	 * キャラクターデータを非同期に読み込む.<br>
	 * 読み込み完了したものが随時、コールバックに渡される.
	 * 
	 * @param callback
	 * @return すべての読み込みが完了したか判定し待機することのできるFuture
	 */
	public Future<?> listProfileAsync(final ListProfileCallback callback) {
		if (callback == null) {
			throw new IllegalArgumentException();
		}

		// キャラクターデータが格納されている親ディレクトリ
		DirectoryConfig dirConfig = DirectoryConfig.getInstance();
		File baseDir = dirConfig.getCharactersDir();

		// キャラクターなんとか機のcharacter.iniがあれば、character.xmlに変換する.
		convertCharacterNantokaIniToXml(baseDir);

		// ファイル名をノーマライズする
		FileNameNormalizer normalizer = FileNameNormalizer.getDefault();

		// キャンセルしたことを示すフラグ
		final AtomicBoolean cancelled = new AtomicBoolean(false);

		// 有効な論理CPU(CORE)数のスレッドで同時実行させる
		int numOfProcessors = Runtime.getRuntime().availableProcessors();
		final ExecutorService executorSrv = Executors
				.newFixedThreadPool(numOfProcessors);
		try {
			// キャラクターデータ対象ディレクトリを列挙し、並列に解析する
			File[] dirs = baseDir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					boolean accept = pathname.isDirectory()
							&& !pathname.getName().startsWith(".");
					if (accept) {
						File configFile = new File(pathname, CONFIG_FILE);
						accept = configFile.exists() && configFile.canRead();
					}
					return accept;
				}
			});
			if (dirs == null) {
				dirs = new File[0];
			}
			for (File dir : dirs) {
				String path = normalizer.normalize(dir.getPath());
				final File normDir = new File(path);

				executorSrv.submit(new Runnable() {
					public void run() {
						boolean terminate = false;
						File characterDataXml = new File(normDir, CONFIG_FILE);
						if (characterDataXml.exists()) {
							try {
								File docBaseFile = new File(normDir,
										CONFIG_FILE);
								URI docBase = docBaseFile.toURI();
								CharacterData characterData = loadProfile(docBase);
								terminate = !callback
										.receiveCharacterData(characterData);

							} catch (Exception ex) {
								terminate = !callback.occureException(normDir,
										ex);
							}
						}
						if (terminate) {
							// 中止が指示されたらスレッドプールを終了する
							logger.log(Level.FINE, "shutdownNow listProfile");
							executorSrv.shutdownNow();
							cancelled.set(true);
						}
					}
				});
			}
		} finally {
			// タスクの登録を受付終了し、現在のすべてのタスクが完了したらスレッドは終了する.
			executorSrv.shutdown();
		}

		// タスクの終了を待機できる疑似フューチャーを作成する.
		Future<Object> awaiter = new Future<Object>() {
			public boolean cancel(boolean mayInterruptIfRunning) {
				if (executorSrv.isTerminated()) {
					// すでに停止完了済み
					return false;
				}
				executorSrv.shutdownNow();
				cancelled.set(true);
				return true;
			}

			public boolean isCancelled() {
				return cancelled.get();
			}

			public boolean isDone() {
				return executorSrv.isTerminated();
			}

			public Object get() throws InterruptedException, ExecutionException {
				try {
					return get(300, TimeUnit.SECONDS);

				} catch (TimeoutException ex) {
					throw new ExecutionException(ex);
				}
			}

			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				executorSrv.shutdown();
				if (!executorSrv.isTerminated()) {
					executorSrv.awaitTermination(timeout, unit);
				}
				return null;
			}
		};

		return awaiter;
	}

	/**
	 * プロファイルを列挙する.<br>
	 * 読み取りに失敗した場合はエラーハンドラに通知されるが例外は返されない.<br>
	 * 一つも正常なプロファイルがない場合は空のリストが返される.<br>
	 * エラーハンドラの通知は非同期に行われる.
	 * 
	 * @param errorHandler
	 *            エラーハンドラ、不要ならばnull
	 * @return プロファイルのリスト(表示名順)、もしくは空
	 */
	public List<CharacterData> listProfiles(
			final ProfileListErrorHandler errorHandler) {

		final List<CharacterData> profiles = new ArrayList<CharacterData>();

		Future<?> awaiter = listProfileAsync(new ListProfileCallback() {

			public boolean receiveCharacterData(CharacterData characterData) {
				synchronized (profiles) {
					profiles.add(characterData);
				}
				return true;
			}

			public boolean occureException(File dir, Exception ex) {
				if (errorHandler != null) {
					errorHandler.occureException(dir, ex);
				}
				return true;
			}
		});

		// すべてのキャラクターデータが読み込まれるまで待機する.
		try {
			awaiter.get();

		} catch (Exception ex) {
			logger.log(Level.WARNING, "listProfile abort.", ex);
		}

		Collections.sort(profiles, CharacterData.SORT_DISPLAYNAME);

		return Collections.unmodifiableList(profiles);
	}

	public CharacterData loadProfile(URI docBase) throws IOException {
		if (docBase == null) {
			throw new IllegalArgumentException();
		}

		// XMLから読み取る
		CharacterData characterData = characterDataXmlReader
				.loadCharacterDataFromXML(docBase);

		return characterData;
	}

	protected void saveCharacterDataToXML(CharacterData characterData)
			throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		characterData.checkWritable();
		if (!characterData.isValid()) {
			throw new IOException("invalid profile: " + characterData);
		}

		URI docBase = characterData.getDocBase();
		if (!"file".equals(docBase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません: " + docBase);
		}

		// XML形式で保存(メモリへ)
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			characterDataXmlWriter.writeXMLCharacterData(characterData, bos);
		} finally {
			bos.close();
		}

		// 成功したら実際にファイルに出力
		File characterPropXML = new File(docBase);
		File baseDir = characterPropXML.getParentFile();
		if (!baseDir.exists()) {
			if (!baseDir.mkdirs()) {
				logger.log(Level.WARNING, "can't create directory. " + baseDir);
			}
		}

		FileOutputStream fos = new FileOutputStream(characterPropXML);
		try {
			fos.write(bos.toByteArray());
		} finally {
			fos.close();
		}
	}

	public void saveFavorites(CharacterData characterData) throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		// xml形式
		UserData favoritesData = getFavoritesUserData(characterData);
		OutputStream os = favoritesData.getOutputStream();
		try {
			characterDataXmlWriter.saveFavorites(characterData, os);

		} finally {
			os.close();
		}
	}

	private UserData getFavoritesUserData(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		// xml形式の場合、キャラクターディレクトリ上に設定する.
		URI docBase = characterData.getDocBase();
		File characterDir = new File(docBase).getParentFile();
		return new FileUserData(new File(characterDir, "favorites.xml"));
	}


	/**
	 * お気に入り(Favorites)を読み込む.<br>
	 * 現在のパーツセットに追加する形で読み込まれ、同じパーツセットIDのものは上書きされます.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public void loadFavorites(CharacterData characterData) throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}

		UserData favoritesXml = getFavoritesUserData(characterData);
		if (favoritesXml.exists()) {
			InputStream is = favoritesXml.openStream();
			try {
				characterDataXmlReader.loadPartsSet(characterData, is);

			} finally {
				is.close();
			}
		}
	}


	/**
	 * 既存のキャラクター定義を削除する.<br>
	 * 有効なdocBaseがあり、そのxmlファイルが存在するものについて、削除を行う.<br>
	 * forceRemoveがtrueでない場合はキャラクター定義 character.xmlファイルの拡張子を
	 * リネームすることでキャラクター定義として認識させなくする.<br>
	 * forceRevmoeがtrueの場合は実際にファイルを削除する.<br>
	 * character.xml、favorites、workingsetのキャッシュも削除される.<br>
	 * 
	 * @param cd
	 *            キャラクター定義
	 * @param forceRemove
	 *            ファイルを削除する場合はtrue、リネームして無効にするだけならfalse
	 * @throws IOException
	 *             削除またはリネームできなかった場合
	 */
	public void remove(CharacterData cd, boolean forceRemove)
			throws IOException {
		if (cd == null || cd.getDocBase() == null) {
			throw new IllegalArgumentException();
		}

		URI docBase = cd.getDocBase();
		File xmlFile = new File(docBase);
		if (!xmlFile.exists() || !xmlFile.isFile()) {
			// すでに存在しない場合
			return;
		}

		// favories.xmlの削除
		if (forceRemove) {
			UserData[] favoritesDatas = new UserData[]{getFavoritesUserData(cd)};
			for (UserData favoriteData : favoritesDatas) {
				if (favoriteData != null && favoriteData.exists()) {
					logger.log(Level.INFO, "remove file: " + favoriteData);
					favoriteData.delete();
				}
			}
		}

		// ワーキングセットの削除
		// XML形式でのワーキングセットの保存
		WorkingSetPersist workingSetPersist = WorkingSetPersist.getInstance();
		workingSetPersist.removeWorkingSet(cd);

		// xmlファイルの拡張子を変更することでキャラクター定義として認識させない.
		// (削除に失敗するケースに備えて先にリネームする.)
		String suffix = "." + System.currentTimeMillis() + ".deleted";
		File bakFile = new File(xmlFile.getPath() + suffix);
		if (!xmlFile.renameTo(bakFile)) {
			throw new IOException("can not rename configuration file.:"
					+ xmlFile);
		}

		// ディレクトリ
		File baseDir = xmlFile.getParentFile();

		if (!forceRemove) {
			// 削除されたディレクトリであることを識別できるようにディレクトリ名も変更する.
			File parentBak = new File(baseDir.getPath() + suffix);
			if (!baseDir.renameTo(parentBak)) {
				throw new IOException("can't rename directory. " + baseDir);
			}

		} else {
			// 完全に削除する
			removeRecursive(baseDir);
		}
	}

	/**
	 * 指定したファイルを削除します.<br>
	 * 指定したファイルがディレクトリを示す場合、このディレクトリを含む配下のすべてのファイルとディレクトリを削除します.<br>
	 * 
	 * @param file
	 *            ファイル、またはディレクトリ
	 * @throws IOException
	 *             削除できない場合
	 */
	protected void removeRecursive(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					removeRecursive(child);
				}
			}
		}
		if (!file.delete()) {
			throw new IOException("can't delete file. " + file);
		}
	}

	protected Iterable<Node> iterable(final NodeList nodeList) {
		final int mx;
		if (nodeList == null) {
			mx = 0;
		} else {
			mx = nodeList.getLength();
		}
		return new Iterable<Node>() {
			public Iterator<Node> iterator() {
				return new Iterator<Node>() {
					private int idx = 0;
					public boolean hasNext() {
						return idx < mx;
					}
					public Node next() {
						if (idx >= mx) {
							throw new NoSuchElementException();
						}
						return nodeList.item(idx++);
					}
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	protected URL getEmbeddedResourceURL(String schemaName) {
		return this.getClass().getResource(schemaName);
	}

	/**
	 * サンプルピクチャを読み込む.<br>
	 * ピクチャが存在しなければnullを返す. キャラクター定義がValidでない場合は常にnullを返す.<br>
	 * 
	 * @param characterData
	 *            キャラクター定義、null不可
	 * @param loader
	 *            イメージのローダー、null不可
	 * @return ピクチャのイメージ、もしくはnull
	 * @throws IOException
	 *             ピクチャの読み取りに失敗した場合
	 */
	public BufferedImage loadSamplePicture(CharacterData characterData,
			ImageLoader loader) throws IOException {
		if (characterData == null || loader == null) {
			throw new IllegalArgumentException();
		}
		if (!characterData.isValid()) {
			return null;
		}

		File sampleImageFile = getSamplePictureFile(characterData);
		if (sampleImageFile != null && sampleImageFile.exists()) {
			LoadedImage loadedImage = loader.load(new FileImageResource(
					sampleImageFile));
			return loadedImage.getImage();
		}
		return null;
	}

	/**
	 * キャラクターのサンプルピクチャが登録可能であるか?<br>
	 * キャラクターデータが有効であり、且つ、ファイルの書き込みが可能であればtrueを返す.<br>
	 * キャラクターデータがnullもしくは無効であるか、ファイルプロトコルでないか、ファイルが書き込み禁止であればfalseょ返す.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @return 書き込み可能であればtrue、そうでなければfalse
	 */
	public boolean canSaveSamplePicture(CharacterData characterData) {
		if (characterData == null || !characterData.isValid()) {
			return false;
		}
		File sampleImageFile = getSamplePictureFile(characterData);
		if (sampleImageFile != null) {
			if (sampleImageFile.exists() && sampleImageFile.canWrite()) {
				return true;
			}
			if (!sampleImageFile.exists()) {
				File parentDir = sampleImageFile.getParentFile();
				if (parentDir != null) {
					return parentDir.canWrite();
				}
			}
		}
		return false;
	}

	/**
	 * サンプルピクチャとして認識されるファイル位置を返す.<br>
	 * ファイルが実在するかは問わない.<br>
	 * DocBaseが未設定であるか、ファィルプロトコルとして返せない場合はnullを返す.<br>
	 * 
	 * @param characterData
	 *            キャラクター定義
	 * @return サンプルピクチャの保存先のファイル位置、もしくはnull
	 */
	protected File getSamplePictureFile(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		URI docBase = characterData.getDocBase();
		if (docBase != null && "file".endsWith(docBase.getScheme())) {
			File docBaseFile = new File(docBase);
			return new File(docBaseFile.getParentFile(), SAMPLE_IMAGE_FILENAME);
		}
		return null;
	}

	/**
	 * サンプルピクチャを保存する.
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @param samplePicture
	 *            サンプルピクチャ
	 * @throws IOException
	 *             保存に失敗した場合
	 */
	public void saveSamplePicture(CharacterData characterData,
			BufferedImage samplePicture) throws IOException {
		if (!canSaveSamplePicture(characterData)) {
			throw new IOException("can not write a sample picture.:"
					+ characterData);
		}
		File sampleImageFile = getSamplePictureFile(characterData); // canSaveSamplePictureで書き込み先検証済み

		if (samplePicture != null) {
			// 登録または更新

			// pngで保存するので背景色は透過になるが、一応、コードとしては入れておく。
			AppConfig appConfig = AppConfig.getInstance();
			Color sampleImageBgColor = appConfig.getSampleImageBgColor();

			ImageSaveHelper imageSaveHelper = new ImageSaveHelper();
			imageSaveHelper.savePicture(samplePicture, sampleImageBgColor,
					sampleImageFile, null);

		} else {
			// 削除
			if (sampleImageFile.exists()) {
				if (!sampleImageFile.delete()) {
					throw new IOException("sample pucture delete failed. :"
							+ sampleImageFile);
				}
			}
		}
	}




	/**
	 * character.iniを読み取り、character.xmlを生成します.<br>
	 * すでにcharacter.xmlがある場合は上書きされます.<br>
	 * 途中でエラーが発生した場合はcharacter.xmlは削除されます.<br>
	 * 
	 * @param characterIniFile
	 *            読み取るcharatcer.iniファイル
	 * @param characterXmlFile
	 *            書き込まれるcharacter.xmlファイル
	 * @param version
	 *            デフォルトキャラクターセットのバージョン
	 * @param description
	 *            説明
	 * @throws IOException
	 *             失敗した場合
	 */
	public void convertFromCharacterIni(File characterIniFile,
			File characterXmlFile, DefaultCharacterDataVersion version,
			String description)
			throws IOException {
		if (characterIniFile == null || characterXmlFile == null
				|| version == null) {
			throw new IllegalArgumentException();
		}

		// character.iniから、character.xmlの内容を構築する.
		FileInputStream is = new FileInputStream(characterIniFile);
		CharacterData characterData;
		try {
			CharacterDataIniReader iniReader = new CharacterDataIniReader();
			characterData = iniReader.readCharacterDataFromIni(is, version);

		} finally {
			is.close();
		}

		// 説明文を設定する
		if (description != null) {
			characterData.setDescription(description);
		}

		// docBase
		URI docBase = characterXmlFile.toURI();
		characterData.setDocBase(docBase);

		// character.xmlの書き込み
		boolean succeeded = false;
		try {
			FileOutputStream outstm = new FileOutputStream(characterXmlFile);
			try {
				characterDataXmlWriter.writeXMLCharacterData(characterData,
						outstm);
			} finally {
				outstm.close();
			}

			succeeded = true;

		} finally {
			if (!succeeded) {
				// 途中で失敗した場合は生成ファイルを削除しておく.
				try {
					if (characterXmlFile.exists()) {
						characterXmlFile.delete();
					}

				} catch (Exception ex) {
					logger.log(Level.WARNING, "ファイルの削除に失敗しました。:"
							+ characterXmlFile, ex);
				}
			}
		}
	}

	/**
	 * お勧めリンクリストが設定されていない場合(nullの場合)、デフォルトのお勧めリストを設定する.<br>
	 * すでに設定されている場合(空を含む)は何もしない.<br>
	 * <br>
	 * おすすめリンクがサポートされてなかったころのデータは、おすすめリンク用のタグそのものが存在せずnullとなる.<br>
	 * サポート後のデータでリンクを未設定にしている場合は、空のリストとなる.<br>
	 * したがって、nullの場合のみ、おすすめリンクを補完する.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 */
	public void compensateRecommendationList(CharacterData characterData) {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		if (characterData.getRecommendationURLList() != null) {
			// 補填の必要なし
			return;
		}
		CharacterDataDefaultProvider defProv = new CharacterDataDefaultProvider();
		CharacterData defaultCd = defProv
				.createDefaultCharacterData(DefaultCharacterDataVersion.V3);
		characterData.setRecommendationURLList(defaultCd
				.getRecommendationURLList());
	}
}
