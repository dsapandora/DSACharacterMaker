package charactermanaj.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.Main;

/**
 * アプリケーションの設定ファイル等の位置を取得するユーテリティクラス.<br>
 * Mainクラスのロード時に最も早くロードされるであろうクラスの一つである.<br>
 * @author seraphy
 */
public final class ConfigurationDirUtilities {

	public static final String CONFIGURATION_DIR_NAME = "CharacterManaJ";

	private static File userDataDir;
	
	private static File applicationBaseDir;
	
	private ConfigurationDirUtilities() {
		throw new RuntimeException("utilities class.");
	}
	
	/**
	 * ユーザーごとのアプリケーションデータ保存先を取得する.<br>
	 * 環境変数「APPDATA」もしくはシステムプロパティ「appdata.dir」からベース位置を取得する.<br>
	 * いずれも設定されておらず、Mac OS Xであれば「~/Library」をベース位置とする。
	 * Mac OS Xでなければ「~/」をベース位置とする.<br>
	 * これに対してシステムプロパティ「characterdata.dirname」(デフォルトは「CharacterManaJ」)という
	 * フォルダをユーザー毎のアプリケーションデータの保存先ディレクトリとする.<br>
	 */
	public synchronized static File getUserDataDir() {
		if (userDataDir == null) {
			
			String appData = null;
			// システムプロパティ「appdata.dir」を探す
			appData = System.getProperty("appdata.dir");
			if (appData == null) {
				// なければ環境変数APPDATAを探す
				// Windows2000/XP/Vista/Windows7には存在する.
				appData = System.getenv("APPDATA");
			}
			if (appData == null && Main.isMacOSX()) {
				// システムプロパティも環境変数にも設定がなく、実行環境がMac OS Xであれば
				// ~/Libraryをベースにする.(Mac OS Xならば必ずある。)
				appData = new File(System.getProperty("user.home"), "Library").getPath();
			}
			if (appData == null) {
				// なければシステムプロパティ「user.home」を使う
				// このプロパティは必ず存在する.
				appData = System.getProperty("user.home");
			}

			// システムプロパティ「characterdata.dirname」のディレクトリ名、なければ「CharacterManaJ」を設定する.
			String characterDirName = System.getProperty("characterdata.dirname", CONFIGURATION_DIR_NAME);
			userDataDir = new File(appData, characterDirName).getAbsoluteFile();

			// ディレクトリを準備する.
			if (!userDataDir.exists()) {
				if (!userDataDir.mkdirs()) {
					// ログ保管場所も設定されていないのでコンソールに出すしかない.
					System.err.println("can't create the user data directory. " + userDataDir);
				}
			}
		}
		return userDataDir;
	}

	/**
	 * アプリケーションディレクトリを取得する.<br>
	 * このクラスをコードソースから、ベースとなる位置を割り出す.<br>
	 * クラスが格納されているクラスパスのフォルダか、JARに固められている場合は、そのJARファイルの、その親ディレクトリを指し示す.<br>
	 * このクラスのプロテクションドメインのコードソースがnullでコードの位置が取得できないか、
	 * コードの位置を示すURLがファイルプロトコルでない場合は実行時例外が返される.<br>
	 * ただし、システムプロパティ「appbase.dir」が明示的に設定されていれば、それが優先される.<br>
	 */
	public synchronized static File getApplicationBaseDir() {
		if (applicationBaseDir == null) {
			
			String appbaseDir = System.getProperty("appbase.dir");
			if (appbaseDir != null && appbaseDir.length() > 0) {
				// 明示的にアプリケーションベースディレクトリが指定されている場合.
				try {
					applicationBaseDir = new File(appbaseDir).getCanonicalFile();
				} catch (IOException ex) {
					ex.printStackTrace();
					// 継続する.まだログの準備ができていない可能性が高いので標準エラー出力へ.
				}
			}
			if (applicationBaseDir == null) {
				// 明示的に指定されていない場合はコードの実行位置から割り出す.
				ProtectionDomain pdomain = ConfigurationDirUtilities.class.getProtectionDomain();

				CodeSource codeSource = pdomain.getCodeSource();
				if (codeSource == null) {
					throw new RuntimeException("codeSource is null: domain=" + pdomain);
				}
				
				URL codeBaseUrl = codeSource.getLocation();
				if (!codeBaseUrl.getProtocol().equals("file")) {
					throw new RuntimeException("codeLocation is not file protocol.: " + codeBaseUrl);
				}
				
				// クラスパスフォルダ、またはJARファイルの、その親
				applicationBaseDir = new File(codeBaseUrl.getPath()).getParentFile();
				
			}
		}
		return applicationBaseDir;
	}
	
	/**
	 * デフォルトのユーザー固有のキャラクターデータディレクトリを取得する.<br>
	 * ユーザー固有のキャラクターディレクトリがまだ存在しない場合は作成される.<br>
	 * @return ユーザー固有のキャラクターデータディレクトリ
	 */
	public static File getDefaultCharactersDir() {
		Logger logger = Logger.getLogger(ConfigurationDirUtilities.class.getName());
		File characterBaseDir = new File(ConfigurationDirUtilities.getUserDataDir(), "characters");
		if (!characterBaseDir.exists()) {
			if (!characterBaseDir.mkdirs()) {
				logger.log(Level.WARNING, "can't create the charatcer base directory. " + characterBaseDir);
			}
		}
		return characterBaseDir;
	}
	
}
