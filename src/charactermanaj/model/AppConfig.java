package charactermanaj.model;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import charactermanaj.util.ApplicationLogHandler;
import charactermanaj.util.BeanPropertiesUtilities;
import charactermanaj.util.ConfigurationDirUtilities;

/**
 * アプリケーションの全域にわたる設定.<br>
 * アプリケーション設定は、クラスパス上のリソース、コートベース直下のappConfig.xml、ユーザーごとのappConfig.xmlの順に読み込まれます
 * .<br>
 *
 * @author seraphy
 */
public final class AppConfig {

	/**
	 * アプリケーション設定ファイルの名前
	 */
	private static final String CONFIG_NAME = "appConfig.xml";

	/**
	 * 全ユーザー用キャラクターディレクトリのシステムプロパティのキー名.<br>
	 */
	public static final String COMMON_CHARACTER_DIR_PROPERTY_NAME = "character.dir";

	/**
	 * 開発用仕様バージョン番号
	 */
	private static final String DEFAULT_SPECIFICATION_VERSION = "1.0";


	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(AppConfig.class.getName());


	/**
	 * シングルトンインスタンス
	 */
	private static final AppConfig singleton = new AppConfig();


	/**
	 * インスタンスを取得する.
	 *
	 * @return インスタンス
	 */
	public static AppConfig getInstance() {
		return singleton;
	}

	/**
	 * プライベートコンストラクタ
	 */
	private AppConfig() {
		loadAppVersions();
	}

	private String implementationVersion;

	private String specificationVersion;

	/**
	 * 実装バージョンを取得する.<br>
	 * ビルドされたjarパッケージからバージョン情報を取得する.<br>
	 * クラスパスの実行からのバージョンは常に「develop」となる.<br>
	 *
	 * @return 実装バージョン
	 */
	public String getImplementationVersion() {
		return implementationVersion;
	}

	/**
	 * 仕様バージョンを取得する.<br>
	 * ビルドされたjarパッケージからバージョン情報を取得する.<br>
	 * クラスパスの実行からのバージョンは常に「develop」となる.<br>
	 *
	 * @return 仕様バージョン
	 */
	public String getSpecificationVersion() {
		return specificationVersion;
	}

	/**
	 * ビルドされたjarパッケージからバージョン情報を取得する.<br>
	 * クラスパスの実行からのバージョンは常に「develop」となる.<br>
	 */
	private void loadAppVersions() {
		Package pak = this.getClass().getPackage();
		String implementationVersion = "develop";
		String specificationVersion = DEFAULT_SPECIFICATION_VERSION;
		if (pak != null) {
			String vInfo = pak.getImplementationVersion();
			if (vInfo != null && implementationVersion.trim().length() > 0) {
				implementationVersion = vInfo.trim();
			}
			String specVInfo = pak.getSpecificationVersion();
			if (specVInfo != null && specVInfo.trim().length() > 0) {
				specificationVersion = specVInfo.trim();
			}
		}

		this.implementationVersion = implementationVersion;
		this.specificationVersion = specificationVersion;
	}

	/**
	 * 設定ファイルのロケール固有版へのファイル末尾の修飾文字列を読み込み順に取得する.
	 * @param locale ロケール、nullの場合はデフォルト
	 * @return ロケールを表すファイル末尾の修飾文字列の読み込み順のリスト
	 */
	private String[] getLocalizedSuffix(Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}

		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		
		return new String[] {
			"",
			"_" + language,
			"_" + language + "_" + country,
			"_" + language + "_" + country + "_" + variant,
		};
	}

	/**
	 * 指定されたファイル名の拡張子の前にロケール固有の修飾文字列を付与したリスト作成して返す.
	 * @param base ファイル名
	 * @param locale ロケール、nullの場合はデフォルト
	 * @return ロケールの検索順序でのロケール固有の修飾文字列が付与されたファイルのリスト
	 */
	private List<File> expandLocalizedSuffix(File base, Locale locale) {
		String path = base.getPath();

		int pt = path.lastIndexOf(".");
		String left, right;
		if (pt >= 0) {
			left = path.substring(0, pt);
			right = path.substring(pt);

		} else {
			left = path;
			right = "";
		}
		
		ArrayList<File> files = new ArrayList<File>();
		for (String suffix : getLocalizedSuffix(locale)) {
			String newPath = left + suffix + right;
			System.out.println("newpath=" + newPath);
			files.add(new File(newPath));
		}
		return files;
	}

	/**
	 * 設定ファイルの読み込み順序で、読み込むべきURIのリストを返す.<br>
	 * <ul>
	 * <li>(1) リソース上の/appConfig.xml</li>
	 * <li>(2) appConfigFileシステムプロパティで指定されたファイル</li>
	 * <li>(3) コードベース下のappConfig.xml</li>
	 * <li>(4) アプリケーションデータ保存先のappConfig.xml</li>
	 * </ul>
	 * appConfigFileシステムプロパティがある場合は、(1)(2)の順。 <br>
	 * 指定がない場合は、(1)(3)(4)の順に読み取る.<br>
	 *
	 * @return 優先順位での設定ファイルの読み込み先URIのリスト
	 * @throws IOException
	 */
	public List<URI> getCandidateURIs() throws IOException {
		List<URI> uris = new ArrayList<URI>();
		// リソース中の既定 (ロケール識別あり)
		for (File localizedFile : expandLocalizedSuffix(new File(getClass()
				.getResource("/" + CONFIG_NAME).getPath()), null)) {
			uris.add(localizedFile.toURI());
		}

		String specifiedAppConfig = System.getProperty("appConfigFile");
		if (specifiedAppConfig != null) {
			// システムプロパティでappConfig.xmlを明示している場合は、それを読み込む。
			// (appConfigFileシステムプロパティが空の場合は、リソース埋め込みの既定の設定だけをよみこむ)
			if (specifiedAppConfig.trim().length() > 0) {
				File specifiedAppConfigFile = new File(specifiedAppConfig);
				uris.add(specifiedAppConfigFile.toURI());
			}

		} else {
			// システムプロパティて明示していない場合は、まずコードベースを使用する.(ロケール識別あり)
			File codeBase = ConfigurationDirUtilities.getApplicationBaseDir();
			for (File localizedFile : expandLocalizedSuffix(new File(codeBase,
					CONFIG_NAME).getCanonicalFile(), null)) {
				uris.add(localizedFile.toURI());
			}

			// システムプロパティて明示していない場合は、次にユーザディレクトリを使用する.
			File userDataDir = ConfigurationDirUtilities.getUserDataDir();
			uris.add(new File(userDataDir, CONFIG_NAME).toURI());
		}
		return uris;
	}

	/**
	 * 保存先の試行順序ごとのファイルのリスト。
	 *
	 * @return 保存先(優先順)
	 */
	public List<File> getPrioritySaveFileList() {
		ArrayList<File> saveFiles = new ArrayList<File>();

		String specifiedAppConfig = System.getProperty("appConfigFile");
		if (specifiedAppConfig != null) {
			// システムプロパティでappConfig.xmlを明示している場合
			if (specifiedAppConfig.trim().length() > 0) {
				File specifiedAppConfigFile = new File(specifiedAppConfig);
				if (!specifiedAppConfigFile.exists()
						|| specifiedAppConfigFile.canWrite()) {
					// まだ存在しないか、書き込み可能である場合のみ候補とする.
					saveFiles.add(specifiedAppConfigFile);
				}
			}
		} else {
			// システムプロパティappConfigFileがなければユーザディレクトリへ書き込む
			// ユーザディレクトリは常に候補とする.
			File userDataDir = ConfigurationDirUtilities.getUserDataDir();
			saveFiles.add(new File(userDataDir, CONFIG_NAME));
		}

		return saveFiles;
	}

	/**
	 * プロパティをロードする.<br>
	 * 存在しないか、読み取りに失敗した場合は、該当ファイルはスキップされる.<br>
	 */
	public void loadConfig() {
		Properties config = new Properties();
		try {
			for (URI uri : getCandidateURIs()) {
				if (uri == null) {
					continue; // リソースがない場合はnullになる
				}
				// ファイルの実在チェック (チェックできる場合のみ)
				if ("file".equals(uri.getScheme())) {
					File file = new File(uri);
					if (!file.exists()) {
						logger.log(Level.CONFIG, "appConfig.xml is not found.:" + file);
						continue;
					}
				}
				// appConfig.xmlの読み込みを行う.
				// Properties#loadFromXML() はXMLからキーを読み取り、既存のキーに対して上書きする.
				// XMLに存在しないキーは読み込み前のままなので、繰り返し呼び出すことで「重ね合わせ」することができる.
				try {
					URL resourceURL = uri.toURL();
					InputStream is = resourceURL.openStream();
					try {
						config.loadFromXML(is);
						logger.log(Level.CONFIG, "appConfig.xml is loaded.:" + uri);
					} finally {
						is.close();
					}

				} catch (FileNotFoundException ex) {
					logger.log(Level.CONFIG, "appConfig.xml is not found.: " + uri, ex);
					// 無視する (無い場合は十分にありえるので「情報」レベルでログ。)
				} catch (Exception ex) {
					logger.log(Level.WARNING, "appConfig.xml loading failed.: " + uri, ex);
					// 無視する
				}
			}

		} catch (IOException ex) {
			throw new RuntimeException("appConfig.xml loading failed.", ex);
		} catch (RuntimeException ex) {
			throw new RuntimeException("appConfig.xml loading failed.", ex);
		}
		BeanPropertiesUtilities.loadFromProperties(this, config);
	}

	/**
	 * プロパティをアプリケーションデータの指定した保存先に保存する.
	 *
	 * @throws IOException
	 *             保存に失敗した場合
	 */
	public void saveConfig(List<File> prioritySaveFiles) throws IOException {
		Properties config = getProperties();
		IOException oex = null;
		for (File configStore : prioritySaveFiles) {
			try {
				OutputStream os = new BufferedOutputStream(
						new FileOutputStream(configStore));
				try {
					config.storeToXML(os, CONFIG_NAME, "UTF-8");
					return; // 成功した時点で終了

				} finally {
					os.close();
				}

			} catch (IOException ex) {
				logger.log(Level.WARNING, "アプリケーション設定の保存に失敗しました" + ex, ex);
				oex = ex;
			}
		}

		// 例外が発生していれば、最後の例外を返す.
		if (oex != null) {
			throw oex;
		}
	}

	/**
	 * プロパティをアプリケーションデータの保存先に保存する.
	 *
	 * @throws IOException
	 *             保存に失敗した場合
	 */
	public void saveConfig() throws IOException {
		saveConfig(getPrioritySaveFileList());
	}

	/**
	 * Propertiesの値を設定した場合に設定できない項目があるかチェックする.<br>
	 * このメソッドを呼び出しても、アプリケーション設定自身は何も影響されない.<br>
	 *
	 * @param props
	 *            適用するプロパティ
	 * @return 設定できなかったプロパティキーのコレクション、問題なければ空が返される.
	 */
	public static Set<String> checkProperties(Properties props) {
		if (props == null) {
			throw new IllegalArgumentException();
		}
		AppConfig dummy = new AppConfig(); // アプリケーションから参照されないダミーのインスタンスを作成する.
		return BeanPropertiesUtilities.loadFromProperties(dummy, props);
	}

	/**
	 * Propertiesの値で設定を更新する.<br>
	 *
	 * @param props
	 *            適用するプロパティ
	 * @return 設定できなかったプロパティキーのコレクション、問題なければ空が返される.
	 */
	public Set<String> update(Properties props) {
		if (props == null) {
			throw new IllegalArgumentException();
		}
		return BeanPropertiesUtilities.loadFromProperties(this, props);
	}

	/**
	 * このアプリケーション設定をプロパティに書き出して返します.<br>
	 *
	 * @return プロパティ
	 */
	public Properties getProperties() {
		Properties config = new Properties();
		BeanPropertiesUtilities.saveToProperties(this, config);
		return config;
	}


	/**
	 * プロファイル選択ダイアログのプロファイルのサンプルイメージの背景色
	 *
	 * @return サンプルイメージの背景色
	 */
	public Color getSampleImageBgColor() {
		return sampleImageBgColor;
	}

	public void setSampleImageBgColor(Color sampleImageBgColor) {
		if (sampleImageBgColor == null) {
			throw new IllegalArgumentException();
		}
		this.sampleImageBgColor = sampleImageBgColor;
	}

	private Color sampleImageBgColor = Color.white;


	/**
	 * デフォルトのイメージ背景色を取得する.
	 *
	 * @return デフォルトのイメージ背景色
	 */
	public Color getDefaultImageBgColor() {
		return defaultImageBgColor;
	}

	public void setDefaultImageBgColor(Color defaultImageBgColor) {
		if (defaultImageBgColor == null) {
			throw new IllegalArgumentException();
		}
		this.defaultImageBgColor = defaultImageBgColor;
	}

	private Color defaultImageBgColor = Color.white;

	/**
	 * 使用中アイテムの背景色を取得する.
	 *
	 * @return 使用中アイテムの背景色
	 */
	public Color getCheckedItemBgColor() {
		return checkedItemBgColor;
	}

	public void setCheckedItemBgColor(Color checkedItemBgColor) {
		if (checkedItemBgColor == null) {
			throw new IllegalArgumentException();
		}
		this.checkedItemBgColor = checkedItemBgColor;
	}

	private Color checkedItemBgColor = Color.cyan.brighter();


	/**
	 * 　選択アイテムの背景色を取得する
	 *
	 * @return 選択アイテムの背景色
	 */
	public Color getSelectedItemBgColor() {
		return selectedItemBgColor;
	}

	public void setSelectedItemBgColor(Color selectedItemBgColor) {
		this.selectedItemBgColor = selectedItemBgColor;
	}

	private Color selectedItemBgColor = Color.orange;

	/**
	 * 不備のあるデータ行の背景色を取得する.
	 *
	 * @return 不備のあるデータ行の背景色
	 */
	public Color getInvalidBgColor() {
		return invalidBgColor;
	}

	public void setInvalidBgColor(Color invalidBgColor) {
		if (invalidBgColor == null) {
			throw new IllegalArgumentException();
		}
		this.invalidBgColor = invalidBgColor;
	}

	private Color invalidBgColor = Color.red.brighter().brighter();

	/**
	 * JPEG画像変換時の圧縮率を取得する.
	 *
	 * @return 圧縮率
	 */
	public float getCompressionQuality() {
		return compressionQuality;
	}

	public void setCompressionQuality(float compressionQuality) {
		if (compressionQuality < .1f || compressionQuality > 1f) {
			throw new IllegalArgumentException();
		}
		this.compressionQuality = compressionQuality;
	}

	private float compressionQuality = .8f;

	/**
	 * エクスポートウィザードのプリセットにパーツ不足時の警告色(前景色)を取得する.
	 *
	 * @return エクスポートウィザードのプリセットにパーツ不足時の警告色(前景色)
	 */
	public Color getExportPresetWarningsForegroundColor() {
		return exportPresetWarningsForegroundColor;
	}

	public void setExportPresetWarningsForegroundColor(
			Color exportPresetWarningsForegroundColor) {
		this.exportPresetWarningsForegroundColor = exportPresetWarningsForegroundColor;
	}

	private Color exportPresetWarningsForegroundColor = Color.red;

	/**
	 * JARファイル転送用バッファサイズ.<br>
	 *
	 * @return JARファイル転送用バッファサイズ.
	 */
	public int getJarTransferBufferSize() {
		return jarTransferBufferSize;
	}

	public void setJarTransferBufferSize(int jarTransferBufferSize) {
		if (jarTransferBufferSize <= 0) {
			throw new IllegalArgumentException();
		}
		this.jarTransferBufferSize = jarTransferBufferSize;
	}

	private int jarTransferBufferSize = 4096;

	/**
	 * ZIPファイル名のエンコーディング.<br>
	 *
	 * @return ZIPファイル名のエンコーディング.<br>
	 */
	public String getZipNameEncoding() {
		return zipNameEncoding;
	}

	public void setZipNameEncoding(String zipNameEncoding) {
		if (zipNameEncoding == null) {
			throw new IllegalArgumentException();
		}
		try {
			Charset.forName(zipNameEncoding);
		} catch (Exception ex) {
			throw new RuntimeException("unsupported charset: " + zipNameEncoding);
		}
		this.zipNameEncoding = zipNameEncoding;
	}

	private String zipNameEncoding = "csWindows31J";

	/**
	 * ディセーブルなテーブルのセルのフォアグラウンドカラーを取得する.
	 *
	 * @return ディセーブルなテーブルのセルのフォアグラウンドカラー
	 */
	public Color getDisabledCellForgroundColor() {
		return disabledCellForegroundColor;
	}

	public void setDisabledCellForegroundColor(Color disabledCellForegroundColor) {
		if (disabledCellForegroundColor == null) {
			throw new IllegalArgumentException();
		}
		this.disabledCellForegroundColor = disabledCellForegroundColor;
	}

	private Color disabledCellForegroundColor = Color.gray;


	/**
	 * ディレクトリを監視する間隔(mSec)を取得する.
	 *
	 * @return ディレクトリを監視する間隔(mSec)
	 */
	public int getDirWatchInterval() {
		return dirWatchInterval;
	}

	public void setDirWatchInterval(int dirWatchInterval) {
		if (dirWatchInterval <= 0) {
			throw new IllegalArgumentException();
		}
		this.dirWatchInterval = dirWatchInterval;
	}

	private int dirWatchInterval = 7 * 1000;

	/**
	 * ディレクトリの監視を有効にするか?
	 *
	 * @return ディレクトリの監視を有効にする場合はtrue
	 */
	public boolean isEnableDirWatch() {
		return enableDirWatch;
	}

	public void setEnableDirWatch(boolean enableDirWatch) {
		this.enableDirWatch = enableDirWatch;
	}

	private boolean enableDirWatch = true;

	/**
	 * ファイル転送に使うバッファサイズ.<br>
	 *
	 * @return バッファサイズ
	 */
	public int getFileTransferBufferSize() {
		return fileTransferBufferSize;
	}

	public void setFileTransferBufferSize(int fileTransferBufferSize) {
		if (fileTransferBufferSize <= 0) {
			throw new IllegalArgumentException();
		}
		this.fileTransferBufferSize = fileTransferBufferSize;
	}

	private int fileTransferBufferSize = 4096;

	/**
	 * プレビューのインジケータを表示するまでのディレイ(mSec)を取得する.
	 *
	 * @return プレビューのインジケータを表示するまでのディレイ(mSec)
	 */
	public long getPreviewIndicatorDelay() {
		return previewIndeicatorDelay;
	}

	public void setPreviewIndeicatorDelay(long previewIndeicatorDelay) {
		if (previewIndeicatorDelay < 0) {
			throw new IllegalArgumentException();
		}
		this.previewIndeicatorDelay = previewIndeicatorDelay;
	}

	private long previewIndeicatorDelay = 300;

	/**
	 * 情報ダイアログの編集ボタンを「開く」アクションにする場合はtrue、「編集」アクションにする場合はfalse
	 *
	 * @return trueならばOpen、falseならばEdit
	 */
	public boolean isInformationDialogOpenMethod() {
		return informationDialogOpenMethod;
	}

	public void setInformationDialogOpenMethod(
			boolean informationDialogOpenMethod) {
		this.informationDialogOpenMethod = informationDialogOpenMethod;
	}

	private boolean informationDialogOpenMethod = true;

	/**
	 * ログを常に残すか?<br>
	 * falseの場合は{@link ApplicationLogHandler}の実装に従って終了時に 必要なければログは削除される.<br>
	 *
	 * @return 常に残す場合はtrue、そうでなければfalse
	 */
	public boolean isNoRemoveLog() {
		return noRemoveLog;
	}

	public void setNoRemoveLog(boolean noRemoveLog) {
		this.noRemoveLog = noRemoveLog;
	}

	private boolean noRemoveLog = false;


	/**
	 * テーブルのグリッド色.<br>
	 *
	 * @return テーブルのグリッド色
	 */
	public Color getGridColor() {
		return gridColor;
	}

	public void setGridColor(Color gridColor) {
		if (gridColor == null) {
			throw new IllegalArgumentException();
		}
		this.gridColor = gridColor;
	}

	private Color gridColor = Color.gray;

	/**
	 * カラーダイアログの値が変更されたら、自動的にプレビューを更新するか?
	 *
	 * @return カラーダイアログの値が変更されたら、自動的にプレビューを更新する場合はtrue (デフォルトはtrue)
	 */
	public boolean isEnableAutoColorChange() {
		return enableAutoColorChange;
	}

	public void setEnableAutoColorChange(boolean enableAutoColorChange) {
		this.enableAutoColorChange = enableAutoColorChange;
	}

	private boolean enableAutoColorChange = true;

	public void setAuthorEditConflictBgColor(Color authorEditConflictBgColor) {
		if (authorEditConflictBgColor == null) {
			throw new IllegalArgumentException();
		}
		this.authorEditConflictBgColor = authorEditConflictBgColor;
	}

	/**
	 * パーツの作者編集時に複数作者を選択した場合のに入力ボックスの背景色
	 *
	 * @return 背景色
	 */
	public Color getAuthorEditConflictBgColor() {
		return authorEditConflictBgColor;
	}

	Color authorEditConflictBgColor = Color.yellow;


	public void setMainFrameMaxWidth(int width) {
		this.mainFrameMaxWidth = width;
	}

	/**
	 * メインフレームの初期表示時の最大幅
	 *
	 * @return メインフレームの初期表示時の最大幅
	 */
	public int getMainFrameMaxWidth() {
		return mainFrameMaxWidth;
	}

	private int mainFrameMaxWidth = 800;

	public void setMainFrameMaxHeight(int height) {
		this.mainFrameMaxHeight = height;
	}

	/**
	 * メインフレームの初期表示時の最大高さ
	 *
	 * @return メインフレームの初期表示時の最大高さ
	 */
	public int getMainFrameMaxHeight() {
		return mainFrameMaxHeight;
	}

	private int mainFrameMaxHeight = 600;


	/**
	 * カラーダイアログで存在しないレイヤーをディセーブルにしない.
	 *
	 * @return ディセーブルにしない場合はtrue
	 */
	public boolean isNotDisableLayerTab() {
		return notDisableLayerTab;
	}

	public void setNotDisableLayerTab(boolean notDisableLayerTab) {
		this.notDisableLayerTab = notDisableLayerTab;
	}

	private boolean notDisableLayerTab;


	/**
	 * ログを消去する日数.<br>
	 * この指定日を経過した古いログは削除される.<br>
	 * 0の場合は削除されない.
	 *
	 * @return
	 */
	public long getPurgeLogDays() {
		return purgeLogDays;
	}

	public void setPurgeLogDays(long purgeLogDays) {
		this.purgeLogDays = purgeLogDays;
	}

	private long purgeLogDays = 10;

	public String getPartsColorGroupPattern() {
		return partsColorGroupPattern;
	}

	public void setPartsColorGroupPattern(String pattern) {
		if (pattern != null && pattern.trim().length() > 0) {
			Pattern.compile(pattern);
		}
		partsColorGroupPattern = pattern;
	}

	private String partsColorGroupPattern = "^.*\\(@\\).*$";

	private Color selectPanelTitleColor = Color.BLUE;

	public Color getSelectPanelTitleColor() {
		return selectPanelTitleColor;
	}

	public void setSelectPanelTitleColor(Color color) {
		if (color == null) {
			throw new IllegalArgumentException();
		}
		selectPanelTitleColor = color;
	}

	private boolean enableAutoShrinkPanel;

	public boolean isEnableAutoShrinkPanel() {
		return enableAutoShrinkPanel;
	}

	public void setEnableAutoShrinkPanel(boolean enableAutoShrinkPanel) {
		this.enableAutoShrinkPanel = enableAutoShrinkPanel;
	}

	public boolean isDisableWatchDirIfNotWritable() {
		return disableWatchDirIfNotWritable;
	}

	public void setDisableWatchDirIfNotWritable(boolean disableWatchDirIfNotWritable) {
		this.disableWatchDirIfNotWritable = disableWatchDirIfNotWritable;
	}

	private boolean disableWatchDirIfNotWritable = true;

	public void setEnablePNGSupportForWindows(boolean enablePNGSupportForWindows) {
		this.enablePNGSupportForWindows = enablePNGSupportForWindows;
	}

	public boolean isEnablePNGSupportForWindows() {
		return enablePNGSupportForWindows;
	}

	private boolean enablePNGSupportForWindows = true;

	/**
	 * 画像表示(通常モード)でオプティマイズを有効にする最大倍率.
	 */
	private double renderingOptimizeThresholdForNormal = 2.;

	public void setRenderingOptimizeThresholdForNormal(
			double renderingOptimizeThresholdForNormal) {
		this.renderingOptimizeThresholdForNormal = renderingOptimizeThresholdForNormal;
	}

	public double getRenderingOptimizeThresholdForNormal() {
		return renderingOptimizeThresholdForNormal;
	}
	/**
	 * 画像表示(チェックモード)でオプティマイズを有効にする最大倍率.
	 */
	private double renderingOptimizeThresholdForCheck = 0.;

	public void setRenderingOptimizeThresholdForCheck(
			double renderingOptimizeThresholdForCheck) {
		this.renderingOptimizeThresholdForCheck = renderingOptimizeThresholdForCheck;
	}

	public double getRenderingOptimizeThresholdForCheck() {
		return renderingOptimizeThresholdForCheck;
	}

	/**
	 * バイキュービックをサポートする場合
	 */
	private boolean enableInterpolationBicubic = true;

	public void setEnableInterpolationBicubic(boolean enableInterpolationBicubic) {
		this.enableInterpolationBicubic = enableInterpolationBicubic;
	}

	public boolean isEnableInterpolationBicubic() {
		return enableInterpolationBicubic;
	}

	/**
	 * 事前定義済みの倍率候補.<br>
	 */
	private String predefinedZoomRanges = "20, 50, 80, 100, 120, 150, 200, 300, 400, 800";

	public String getPredefinedZoomRanges() {
		return predefinedZoomRanges;
	}

	public void setPredefinedZoomRanges(String predefinedZoomRanges) {
		this.predefinedZoomRanges = predefinedZoomRanges;
	}

	/**
	 * ズームパネルを初期状態で表示するか?
	 */
	private boolean enableZoomPanel = true;

	public boolean isEnableZoomPanel() {
		return enableZoomPanel;
	}

	public void setEnableZoomPanel(boolean enableZoomPanel) {
		this.enableZoomPanel = enableZoomPanel;
	}

	/**
	 * ズームパネルをアクティブにする下部範囲
	 */
	private int zoomPanelActivationArea = 30;

	public int getZoomPanelActivationArea() {
		return zoomPanelActivationArea;
	}

	public void setZoomPanelActivationArea(int zoomPanelActivationArea) {
		this.zoomPanelActivationArea = zoomPanelActivationArea;
	}

	/**
	 * レンダリングヒントを使用するか?
	 */
	private boolean enableRenderingHints = true;

	public void setEnableRenderingHints(boolean enableRenderingHints) {
		this.enableRenderingHints = enableRenderingHints;
	}

	public boolean isEnableRenderingHints() {
		return enableRenderingHints;
	}

	/**
	 * グリッド描画とマスク
	 */
	private int drawGridMask = 2;

	public int getDrawGridMask() {
		return drawGridMask;
	}

	public void setDrawGridMask(int drawGridMask) {
		this.drawGridMask = drawGridMask & 0x03;
	}

	private int previewGridColor = 0x7f7f0000;

	public int getPreviewGridColor() {
		return previewGridColor;
	}

	public void setPreviewGridColor(int previewGridColor) {
		this.previewGridColor = previewGridColor;
	}

	private int previewGridSize = 20;

	public int getPreviewGridSize() {
		return previewGridSize;
	}

	public void setPreviewGridSize(int previewGridSize) {
		this.previewGridSize = previewGridSize;
	}

	/**
	 * チェックモード時の余白サイズ(片側)
	 */
	private int previewUnfilledSpaceForCheckMode = 0;

	public int getPreviewUnfilledSpaceForCheckMode() {
		return previewUnfilledSpaceForCheckMode;
	}

	public void setPreviewUnfilledSpaceForCheckMode(
			int previewUnfilledSpaceForCheckMode) {
		this.previewUnfilledSpaceForCheckMode = previewUnfilledSpaceForCheckMode;
	}

	/**
	 * チェックモードでツールチップを表示するか?
	 */
	private boolean enableCheckInfoTooltip = true;

	public boolean isEnableCheckInfoTooltip() {
		return enableCheckInfoTooltip;
	}

	public void setEnableCheckInfoTooltip(boolean enableCheckInfoTooltip) {
		this.enableCheckInfoTooltip = enableCheckInfoTooltip;
	}

	/**
	 * ホイールによるスクロールの単位.<br>
	 */
	private int wheelScrollUnit = 10;

	public int getWheelScrollUnit() {
		return wheelScrollUnit;
	}

	public void setWheelScrollUnit(int wheelScrollUnit) {
		this.wheelScrollUnit = wheelScrollUnit;
	}

	/**
	 * 壁紙にオフスクリーン描画を使用するか?.<br>
	 * (あまり劇的なパフォーマンス効果はない.)
	 */
	private boolean enableOffscreenWallpaper = false;

	public boolean isEnableOffscreenWallpaper() {
		return enableOffscreenWallpaper;
	}

	public void setEnableOffscreenWallpaper(boolean enableOffscreenWallpaper) {
		this.enableOffscreenWallpaper = enableOffscreenWallpaper;
	}

	/**
	 * 壁紙のオフスクリーンの既定サイズ.
	 */
	private int offscreenWallpaperSize = 300;

	public int getOffscreenWallpaperSize() {
		return offscreenWallpaperSize;
	}

	public void setOffscreenWallpaperSize(int offscreenWallpaperSize) {
		this.offscreenWallpaperSize = offscreenWallpaperSize;
	}

	/**
	 * ランダム選択パーツの履歴数
	 */
	private int randomChooserMaxHistory = 10;

	public int getRandomChooserMaxHistory() {
		return randomChooserMaxHistory;
	}

	public void setRandomChooserMaxHistory(int randomChooserMaxHistory) {
		this.randomChooserMaxHistory = randomChooserMaxHistory;
	}

	/**
	 * デフォルトのフォントサイズ、0以下の場合はシステム既定のまま
	 */
	private int defaultFontSize = 12;

	public int getDefaultFontSize() {
		return defaultFontSize;
	}

	public void setDefaultFontSize(int defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
	}

	/**
	 * デフォルトのフォントファミリー、カンマ区切り
	 */
	private String fontPriority = "Lucida Grande";
	
	public String getFontPriority() {
		return fontPriority;
	}
	
	public void setFontPriority(String fontPriority) {
		this.fontPriority = fontPriority;
	}
}
