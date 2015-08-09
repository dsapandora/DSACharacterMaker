package charactermanaj.model.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.util.DirectoryConfig;

/**
 * 最後に使用したデータの使用状況を保存・復元するためのクラス
 * 
 * @author seraphy
 */
public final class RecentDataPersistent {

	/**
	 * キャラクターデータの親フォルダごとに保存する、最後に使用したキャラクターデータを保存するファイル名
	 */
	private static final String RECENT_CAHARCTER_INFO_XML = "recent-character.xml";

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(RecentDataPersistent.class.getName());

	/**
	 * シングルトン
	 */
	private static final RecentDataPersistent inst = new RecentDataPersistent();

	/**
	 * プライベートコンストラクタ
	 */
	private RecentDataPersistent() {
		super();
	}
	
	/**
	 * インスタンスを取得する
	 * 
	 * @return インスタンス
	 */
	public static RecentDataPersistent getInstance() {
		return inst;
	}

	/**
	 * キャラクターデータの親フォルダごとに保存する、最後に使用したキャラクターデータを保存するファイル
	 * 
	 * @return
	 */
	private File getRecentCharacterXML() {
		File currentCharactersDir = DirectoryConfig.getInstance()
				.getCharactersDir();
		File recentCharacterXML = new File(currentCharactersDir,
				RECENT_CAHARCTER_INFO_XML);
		return recentCharacterXML;
	}

	/**
	 * 最後に使用したキャラクターデータのフォルダ名を親ディレクトリからの相対パスとして保存する.<br>
	 * ただし、書き込み禁止である場合は何もしない.<br>
	 * 
	 * @param characterData
	 *            キャラクターデータ
	 * @throws IOException
	 *             書き込みに失敗した場合
	 */
	public boolean saveRecent(CharacterData characterData) throws IOException {
		URI docBase = null;
		if (characterData != null) {
			docBase = characterData.getDocBase();
		}

		String characterDataName = null;
		if (docBase != null) {
			File currentCharactersDir = DirectoryConfig.getInstance()
					.getCharactersDir();
			File characterXml = new File(docBase);
			File characterDir = characterXml.getParentFile();
			// "Foo/Bar/character.xml"の、「Foo」の部分を取得する.
			File baseDir = characterDir.getParentFile();
			if (currentCharactersDir.equals(baseDir)) {
				// "Foo/Bar/character.xml"の「Bar」の部分を取得する.
				// ※ キャラクターデータの親フォルダ上のフォルダ名だけを保存する.(相対パス)
				characterDataName = characterDir.getName();
			}
		}
		Properties props = new Properties();
		props.setProperty("lastUseCharacterData", characterDataName == null
				? ""
				: characterDataName);

		File recentCharacterXML = getRecentCharacterXML();
		if (recentCharacterXML.exists() && !recentCharacterXML.canWrite()) {
			// ファイルが書き込み禁止の場合は何もしない.
			logger.log(Level.FINE, "recent-character.xml is readonly.");
			return false;
		}

		// ファイルがまだ実在しないか、書き込み可能である場合のみ保存する.
		OutputStream os = new BufferedOutputStream(new FileOutputStream(
				recentCharacterXML));
		try {
			String comment = "recent-character: lastModified="
					+ (new Timestamp(System.currentTimeMillis()).toString());
			props.storeToXML(os, comment);
		} finally {
			os.close();
		}
		return true;
	}

	/**
	 * 親ディレクトリからの相対パスとして記録されている、最後に使用したキャラクターデータのフォルダ名から、
	 * 最後に使用したキャラクターデータをロードして返す.<br>
	 * 該当するキャラクターデータが存在しないか、読み込みに失敗した場合は「履歴なし」としてnullを返す.<br>
	 * 
	 * @return キャラクターデータ、もしくはnull
	 */
	public CharacterData loadRecent() {
		File recentCharacterXML = getRecentCharacterXML();
		if (recentCharacterXML.exists()) {
			try {
				Properties props = new Properties();
				InputStream is = new BufferedInputStream(new FileInputStream(
						recentCharacterXML));
				try {
					props.loadFromXML(is);
				} finally {
					is.close();
				}

				String characterDataName = props
						.getProperty("lastUseCharacterData");
				if (characterDataName != null
						&& characterDataName.trim().length() > 0) {
					// ※ キャラクターデータの親フォルダ上のフォルダ名だけを保存されているので
					// 相対パスから、character.xmlの絶対パスを求める
					File currentCharactersDir = DirectoryConfig.getInstance()
							.getCharactersDir();
					File characterDir = new File(currentCharactersDir,
							characterDataName);
					File characterXml = new File(characterDir,
							CharacterDataPersistent.CONFIG_FILE);
					if (characterXml.exists()) {
						// character.xmlが存在すれば復元を試行する.
						CharacterDataPersistent persist = CharacterDataPersistent
								.getInstance();
						return persist.loadProfile(characterXml.toURI());
					}
				}

			} catch (Exception ex) {
				// 失敗した場合は最後に使用したデータが存在しないものとみなす.
				logger.log(Level.WARNING, "recent data loading failed. "
						+ recentCharacterXML, ex);
			}
		}

		// 履歴がない場合、もしくは読み取れなかった場合はnullを返す.
		return null;
	}

	// /**
	// * 最後に使用したキャラクターデータを取得する.
	// *
	// * @return キャラクターデータ。最後に使用したデータが存在しない場合はnull
	// * @throws IOException
	// * 読み込みに失敗した場合
	// */
	// private CharacterData loadRecentSer() throws IOException {
	// UserData recentCharacterStore = getRecentCharacterStore();
	// if (!recentCharacterStore.exists()) {
	// return null;
	// }
	//
	// RecentData recentData;
	// try {
	// File currentCharactersDir =
	// DirectoryConfig.getInstance().getCharactersDir();
	//
	// Object rawRecentData = recentCharacterStore.load();
	// if (rawRecentData instanceof RecentData) {
	// // 旧形式 (単一)
	// recentData = (RecentData) rawRecentData;
	// logger.log(Level.INFO, "old-recentdata-type: " + recentData);
	//
	// // 旧形式で保存されているURIが、現在選択しているキャラクターデータと同じ親ディレクトリ
	// // でなければ復元しない.
	// URI uri = recentData.getDocBase();
	// File parentDir = new File(uri).getParentFile().getParentFile(); // 2段上
	// if (!currentCharactersDir.equals(parentDir)) {
	// logger.log(Level.INFO,
	// "unmatched characters-dir. current="
	// + currentCharactersDir + "/recent="
	// + parentDir);
	// recentData = null;
	// }
	//
	// } else if (rawRecentData instanceof Map) {
	// // 新形式 (複数のキャラクターディレクトリに対応)
	// @SuppressWarnings("unchecked")
	// Map<File, RecentData> recentDataMap = (Map<File, RecentData>)
	// rawRecentData;
	// recentData = recentDataMap.get(currentCharactersDir);
	// logger.log(Level.FINE, "recent-data: " + currentCharactersDir + "=" +
	// recentData);
	//
	// } else {
	// // 不明な形式
	// logger.log(Level.SEVERE,
	// "invalid file format. " + recentCharacterStore
	// + "/class=" + rawRecentData.getClass());
	// recentData = null;
	// }
	//
	// } catch (Exception ex) {
	// // RecentData情報の復元に失敗した場合は最後に使用したデータが存在しないものとみなす.
	// logger.log(Level.WARNING, "recent data loading failed. " +
	// recentCharacterStore, ex);
	// recentData = null;
	// }
	//
	// if (recentData != null) {
	// CharacterDataPersistent persist = CharacterDataPersistent.getInstance();
	// return persist.loadProfile(recentData.getDocBase());
	// }
	//
	// // 履歴がない場合、もしくは読み取れなかった場合はnullを返す.
	// return null;
	// }
	//
	// /**
	// * 最後に使用したキャラクタデータの保存先を取得する
	// *
	// * @return 保存先
	// */
	// protected UserData getRecentCharacterStore() {
	// UserDataFactory userDataFactory = UserDataFactory.getInstance();
	// UserData recentCharacterStore =
	// userDataFactory.getUserData(RECENT_CHARACTER_SER);
	// return recentCharacterStore;
	// }
	
	
}
