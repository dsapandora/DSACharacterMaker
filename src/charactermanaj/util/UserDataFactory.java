package charactermanaj.util;

import java.io.File;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ユーザーデータの保存先を生成するファクトリ
 * 
 * @author seraphy
 */
public class UserDataFactory {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(UserDataFactory.class.getName());

	/**
	 * シングルトン
	 */
	private static UserDataFactory inst = new UserDataFactory();

	/**
	 * インスタンスを取得する.
	 * 
	 * @return インスタンス
	 */
	public static UserDataFactory getInstance() {
		return inst;
	}

	/**
	 * プライベートコンストラクタ
	 */
	private UserDataFactory() {
		super();
	}

	/**
	 * 拡張子を含むファイル名を指定し、そのファイルが保存されるべきユーザディレクトリを判定して返す.<br>
	 * nullまたは空の場合、もしくは拡張子がない場合はユーザディレクトリのルートを返します.<br>
	 * フォルダがなければ作成されます.<br>
	 * 
	 * @param name
	 *            ファイル名、もしくはnull
	 * @return ファィルの拡張子に対応したデータ保存先フォルダ
	 */
	public File getSpecialDataDir(String name) {
		File userDataDir = ConfigurationDirUtilities.getUserDataDir();
		
		if (name != null && name.length() > 0) {
			int seppos = name.lastIndexOf('-');
			if (name.endsWith(".xml") && seppos >= 0) {
				// 「foo-????.xml」形式の場合は「????」でグループ化する
				String groupName = name.substring(seppos + 1, name.length() - 4);
				if (groupName.length() > 0) {
					userDataDir = new File(userDataDir, groupName);
				}

			} else {
				// 拡張子によるグループ化
				int pos = name.lastIndexOf('.');
				if (pos >= 0) {
					String ext = name.substring(pos + 1);
					if (ext.length() > 0) {
						if ("ser".equals(ext)) {
							userDataDir = new File(userDataDir, "caches");
						} else {
							userDataDir = new File(userDataDir, ext + "s");
						}
					}
				}
			}
		}

		// フォルダがなければ作成する.
		if (!userDataDir.exists()) {
			boolean result = userDataDir.mkdirs();
			logger.log(Level.INFO, "makeDir: " + userDataDir + " /succeeded=" + result);
		}
		
		return userDataDir;
	}

	/**
	 * 指定した名前のユーザーデータ保存先を作成する.
	 * 
	 * @param name
	 *            ファイル名
	 * @return 保存先
	 */
	public UserData getUserData(String name) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		return new FileUserData(new File(getSpecialDataDir(name), name));
	}

	/**
	 * docBaseの名前ベースのUUIDをプレフィックスをもつユーザーデータ保存先を作成する.<br>
	 * 
	 * @param docBase
	 *            URI、null可
	 * @param name
	 *            ファイル名
	 * @return 保存先
	 */
	public UserData getMangledNamedUserData(URI docBase, String name) {
		String mangledName = getMangledName(docBase);
		return getUserData(mangledName + "-" + name);
	}

	/**
	 * docBaseをハッシュ値化文字列にした、名前ベースのUUIDを返す.<br>
	 * docBaseがnullの場合は空文字とみなして変換する.<br>
	 * (衝突の可能性は無視する。)<br>
	 * 
	 * @param docBase
	 *            URI、null可
	 * @return 名前ベースのUUID
	 */
	private String getMangledName(URI docBase) {
		String docBaseStr;
		if (docBase == null) {
			docBaseStr = "";
		} else {
			docBaseStr = docBase.toString();
		}
		String mangledName = UUID.nameUUIDFromBytes(docBaseStr.getBytes()).toString();

		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, "mangledName " + docBase + "=" + mangledName);
		}
		
		return mangledName;
	}
}

