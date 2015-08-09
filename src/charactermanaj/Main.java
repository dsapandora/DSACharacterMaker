package charactermanaj;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import charactermanaj.clipboardSupport.ImageSelection;
import charactermanaj.graphics.io.ImageCacheMBeanImpl;
import charactermanaj.model.AppConfig;
import charactermanaj.model.util.StartupSupport;
import charactermanaj.ui.MainFrame;
import charactermanaj.ui.ProfileListManager;
import charactermanaj.ui.SelectCharatersDirDialog;
import charactermanaj.util.AWTExceptionLoggingHandler;
import charactermanaj.util.ApplicationLoggerConfigurator;
import charactermanaj.util.ConfigurationDirUtilities;
import charactermanaj.util.DirectoryConfig;
import charactermanaj.util.ErrorMessageHelper;
import charactermanaj.util.JavaVersionUtils;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * エントリポイント用クラス
 * 
 * @author seraphy
 */
public final class Main implements Runnable {

	/**
	 * ロガー.<br>
	 */
	private static final Logger logger = Logger.getLogger(Main.class.getName());

	/**
	 * Mac OS Xであるか?
	 */
	private static final boolean isMacOSX;

	/**
	 * Linuxであるか?
	 */
	private static final boolean isLinux;

	/**
	 * クラスイニシャライザ.<br>
	 * 実行環境に関する定数を取得・設定する.<br>
	 */
	static {
		// Mac OS Xでの実行判定
		// システムプロパティos.nameは、すべてのJVM実装に存在する.
		// 基本ディレクトリの位置の決定に使うため、
		// なによりも、まず、これを判定しないとダメ.(順序が重要)
		String lcOS = System.getProperty("os.name").toLowerCase();
		isMacOSX = lcOS.startsWith("mac os x");
		isLinux = lcOS.indexOf("linux") >= 0;
	}

	/**
	 * ロガーの初期化.<br>
	 * 失敗しても継続する.<br>
	 */
	private static void initLogger() {
		try {
			// ロガーの準備

			// ローカルファイルシステム上のユーザ定義ディレクトリから
			// ログの設定を読み取る.(OSにより、設定ファイルの位置が異なることに注意)
			ApplicationLoggerConfigurator.configure();

			if (JavaVersionUtils.getJavaVersion() >= 1.7) {
				// java7以降は、sun.awt.exception.handlerが使えないので、
				// EDTスレッドで未処理例外ハンドラを明示的に設定する.
				final AWTExceptionLoggingHandler logHandler = new AWTExceptionLoggingHandler();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						final UncaughtExceptionHandler handler = Thread
								.getDefaultUncaughtExceptionHandler();
						Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
							public void uncaughtException(Thread t, Throwable ex) {
								logHandler.handle(ex);
								if (handler != null) {
									handler.uncaughtException(t, ex);
								}
							}
						});
					}
				});

			} else {
				// SwingのEDT内の例外ハンドラの設定 (ロギングするだけ)
				// (ただし、unofficial trickである.)
				System.setProperty("sun.awt.exception.handler",
						AWTExceptionLoggingHandler.class.getName());
			}

		} catch (Throwable ex) {
			// ロガーの準備に失敗した場合はロガーがないかもなので
			// コンソールに出力する.
			ex.printStackTrace();
			logger.log(Level.SEVERE, "logger initiation failed. " + ex, ex);
		}
	}


	/**
	 * UIをセットアップする.
	 * 
	 * @throws Exception
	 *             いろいろな失敗
	 */
	private static void setupUIManager(AppConfig appConfig) throws Exception {
		// System.setProperty("swing.aatext", "true");
		// System.setProperty("awt.useSystemAAFontSettings", "on");

		if (isMacOSX()) {
			// MacOSXであれば、スクリーンメニューを有効化
			System.setProperty("apple.laf.useScreenMenuBar", "true");
			System.setProperty(
					"com.apple.mrj.application.apple.menu.about.name",
					"CharacterManaJ");

			// Java7以降であればノーマライズをセットアップする.
			if (JavaVersionUtils.getJavaVersion() >= 1.7) {
				charactermanaj.util.FileNameNormalizer.setupNFCNormalizer();
			}
		}

		// 実行プラットフォームのネイティブな外観にする.
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		// JSpliderのvalueを非表示 (GTKではデフォルトで有効のため)
		UIManager.put("Slider.paintValue", Boolean.FALSE);


		// 優先するフォントファミリ中の実在するフォントファミリのセット(大文字小文字の区別なし)
		TreeSet<String> availablePriorityFontSets = new TreeSet<String>(
				String.CASE_INSENSITIVE_ORDER);
		
		// 少なくともメニューが表示できるようなフォントを選択する
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties("menu/menu");
		HashSet<Character> useChars = new HashSet<Character>();
		Enumeration<?> enmStrings = strings.propertyNames();
		while (enmStrings.hasMoreElements()) {
			String propertyName = (String) enmStrings.nextElement();
			String propertyValue = strings.getProperty(propertyName);
			for (char ch : propertyValue.toCharArray()) {
				useChars.add(ch);
			}
		}

		// 優先するフォントファミリの実在チェックと、もっとも優先されるフォントファミリの確定
		String selectedFontFamily = null;
		String fontPriorityStr = appConfig.getFontPriority();
		if (fontPriorityStr.trim().length() > 0) {
			String[] fontPriority = fontPriorityStr.split(",");
			for (String availableFontFamily : GraphicsEnvironment
					.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH)) {
				for (String fontFamily : fontPriority) {
					fontFamily = fontFamily.trim();
					if (fontFamily.length() > 0) {
						if (availableFontFamily.equalsIgnoreCase(fontFamily)) {
							// 見つかった実在フォントが、現在のロケールのメニューを正しく表示できるか？
							Font font = Font.decode(availableFontFamily);
							logger.log(Level.INFO, "実在するフォントの確認:" + availableFontFamily);
							boolean canDisplay = false;
							for (char ch : useChars) {
								canDisplay = font.canDisplay(ch);
								if (!canDisplay) {
									logger.log(Level.INFO,
											"このフォントはメニュー表示に使用できません: "
													+ selectedFontFamily + "/ch=" + ch);
									break;
								}
							}
							if (canDisplay) {
								if (selectedFontFamily == null) {
									// 最初に見つかったメニューを表示可能な優先フォント
									selectedFontFamily = availableFontFamily;
								}
								// メニューを表示可能なフォントのみ候補に入れる
								availablePriorityFontSets.add(fontFamily);
							}
						}
					}
				}
			}
			if (selectedFontFamily == null) {
				// フォールバック用フォントとして「Dialog」を用いる.
				// 仮想フォントファミリである「Dialog」は日本語も表示可能である.
				selectedFontFamily = "Dialog";
			}
		}
		
		// デフォルトのフォントサイズ、0以下の場合はシステム標準のまま
		int defFontSize = appConfig.getDefaultFontSize();

		// UIデフォルトのフォント設定で、優先フォント以外のフォントファミリが指定されているものを
		// すべて最優先フォントファミリに設定する.
		// また、設定されたフォントサイズが0よりも大きければ、そのサイズに設定する.
		for (java.util.Map.Entry<?, ?> entry : UIManager.getDefaults()
				.entrySet()) {
			Object key = entry.getKey();
			Object val = UIManager.get(key);
			if (val instanceof FontUIResource) {
				FontUIResource fontUIResource = (FontUIResource) val;
				int fontSize = fontUIResource.getSize();
				String fontFamily = fontUIResource.getFamily();
				
				if (defFontSize > 0) {
					fontSize = defFontSize;
				}

				if (selectedFontFamily != null
						&& !availablePriorityFontSets.contains(fontFamily)) {
					// 現在のデフォルトUIに指定された優先フォント以外が設定されており、
					// 且つ、優先フォントの指定があれば、優先フォントに差し替える.
					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, "UIDefaultFont: " + key +
								"= " + fontFamily + " -> " + selectedFontFamily);
					}
					fontFamily = selectedFontFamily;
				}
				
				fontUIResource = new FontUIResource(fontFamily,
						fontUIResource.getStyle(), fontSize);
				UIManager.put(entry.getKey(), fontUIResource);
			}
		}
	}

	/**
	 * 初期処理およびメインフレームを構築する.<br>
	 * SwingのUIスレッドで実行される.<br>
	 */
	public void run() {
		try {
			// アプリケーション設定の読み込み
			AppConfig appConfig = AppConfig.getInstance();
			appConfig.loadConfig();

			// UIManagerのセットアップ.
			try {
				setupUIManager(appConfig);

			} catch (Exception ex) {
				// UIManagerの設定に失敗した場合はログに書いて継続する.
				ex.printStackTrace();
				logger.log(Level.WARNING, "UIManager setup failed.", ex);
			}

			// クリップボードサポートの設定
			if (!ImageSelection.setupSystemFlavorMap()) {
				logger.log(Level.WARNING,
						"failed to set the clipboard-support.");
			}

			// LANG, またはLC_CTYPEが設定されていない場合はエラーを表示する
			// OSXのJava7(Oracle)を実行する場合、環境変数LANGまたはLC_CTYPEに正しくファイル名の文字コードが設定されていないと
			// ファイル名を正しく取り扱えず文字化けするため、実行前に確認し警告を表示する。
			// ただし、この挙動はJava7u60では修正されているので、それ以降であれば除外する.
			int[] versions = JavaVersionUtils.getJavaVersions();
			if (isMacOSX()
					&& (versions[0] == 1 && versions[1] == 7 && versions[3] < 60)) {
				String lang = System.getenv("LANG");
				String lcctype = System.getenv("LC_CTYPE");
				if ((lang == null || lang.trim().length() == 0)
						&& (lcctype == null || lcctype.trim().length() == 0)) {
					JOptionPane
							.showMessageDialog(
									null,
									"\"LANG\" or \"LC_CTYPE\" environment variable must be set.",
									"Configuration Error",
									JOptionPane.ERROR_MESSAGE);
				}
			}

			// 起動時のシステムプロパティでキャラクターディレクトリが指定されていて実在すれば、それを優先する.
			File currentCharacterDir = null;
			String charactersDir = System.getProperty("charactersDir");
			if (charactersDir != null && charactersDir.length() > 0) {
				File charsDir = new File(charactersDir);
				if (charsDir.exists() && charsDir.isDirectory()) {
					currentCharacterDir = charsDir;
				}
			}

			if (currentCharacterDir == null) {
				// キャラクターセットディレクトリの選択
				File defaultCharacterDir = ConfigurationDirUtilities
						.getDefaultCharactersDir();
				currentCharacterDir = SelectCharatersDirDialog
						.getCharacterDir(defaultCharacterDir);
				if (currentCharacterDir == null) {
					// キャンセルされたので終了する.
					logger.info("luncher canceled.");
					return;
				}
			}

			// キャラクターデータフォルダの設定
			DirectoryConfig.getInstance().setCharactersDir(currentCharacterDir);

			// スタートアップ時の初期化
			StartupSupport.getInstance().doStartup();

			// デフォルトのプロファイルを開く.
			// (最後に使ったプロファイルがあれば、それが開かれる.)
			MainFrame mainFrame = ProfileListManager.openDefaultProfile();
			if (isMacOSX()) {
				try {
					// MacOSXであればスクリーンメニューからのイベントをハンドルできるようにする.
					// OSXにしか存在しないクラスを利用するためリフレクションとしている.
					// ただしJDKによっては、Apple Java Extensionsがないことも予想されるので、
					// その場合はエラーにしない。
					Class<?> clz = Class
							.forName("charactermanaj.ui.MainFramePartialForMacOSX");
					Method mtd = clz.getMethod("setupScreenMenu",
							MainFrame.class);
					mtd.invoke(null, mainFrame);

				} catch (Throwable ex) {
					logger.log(Level.CONFIG,
							"The Apple Java Extensions is not found.", ex);
				}
			}

			// 表示(および位置あわせ)
			mainFrame.showMainFrame();

		} catch (Throwable ex) {
			// なんらかの致命的な初期化エラーがあった場合、ログとコンソールに表示
			// ダイアログが表示されるかどうかは状況次第.
			ex.printStackTrace();
			logger.log(Level.SEVERE, "Application initiation failed.", ex);
			ErrorMessageHelper.showErrorDialog(null, ex);

			// メインフレームを破棄します.
			MainFrame.closeAllProfiles();
		}
	}

	/**
	 * エントリポイント.<br>
	 * 最初のメインフレームを開いたときにMac OS Xであればスクリーンメニューの登録も行う.<br>
	 * 
	 * @param args
	 *            引数(未使用)
	 */
	public static void main(String[] args) {
		// ロガー等の初期化
		initLogger();

		// MBeanのセットアップ
		try {
			ImageCacheMBeanImpl.setupMBean();

		} catch (JMException ex) {
			// 失敗しても無視して継続する.
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}

		// フレームの生成等は、SwingのEDTで実行する.
		SwingUtilities.invokeLater(new Main());
	}

	/**
	 * Mac OS Xで動作しているか?
	 * 
	 * @return Max OS X上であればtrue
	 */
	public static boolean isMacOSX() {
		return isMacOSX;
	}

	/**
	 * Mac OS X、もしくはlinuxで動作しているか?
	 * 
	 * @return Mac OS X、もしくはlinuxで動作していればtrue
	 */
	public static boolean isLinuxOrMacOSX() {
		return isLinux || isMacOSX;
	}
}
