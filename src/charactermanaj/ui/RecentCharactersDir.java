package charactermanaj.ui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

import charactermanaj.util.ConfigurationDirUtilities;

/**
 * 最後に使用したキャラクターデータディレクトリと、その履歴情報.<br>
 * 
 * @author seraphy
 */
public class RecentCharactersDir implements Serializable {
	
	private static final long serialVersionUID = -5274310741380875405L;
	
	/**
	 * 使用したキャラクターデータディレクトリリストの保存先ファイル名
	 */
	private static final String RECENT_CHARACTERDIRS_XML = "recent-characterdirs.xml";

	/**
	 * キャラクターデータディレクトリ一覧のプロパティキーのプレフィックス
	 */
	private static final String DIRS_PREFIX = "characterDataDirs.";

	/**
	 * 最後に使用したディレクトリ
	 */
	private File lastUseCharacterDir;
	
	/**
	 * 過去に使用したディレクトリ情報
	 */
	private ArrayList<File> recentCharacterDirs = new ArrayList<File>();
	
	/**
	 * ディレクトリの問い合わせ不要フラグ.
	 */
	private boolean doNotAskAgain;

	
	/**
	 * 使用したキャラクターディレクトリの履歴、古いもの順
	 * 
	 * @return
	 */
	public List<File> getRecentCharacterDirs() {
		return recentCharacterDirs;
	}

	/**
	 * 使用したキャラクターディレクトリの履歴、新しいもの順.<br>
	 * 表示用であり、追加・削除・変更は不可.
	 * 
	 * @return
	 */
	public List<File> getRecentCharacterDirsOrderByNewly() {
		ArrayList<File> dirs = new ArrayList<File>(recentCharacterDirs);
		Collections.reverse(dirs);
		return Collections.unmodifiableList(dirs);
	}

	public void setLastUseCharacterDir(File lastUseCharacterDir) {
		this.lastUseCharacterDir = lastUseCharacterDir;
	}
	
	public File getLastUseCharacterDir() {
		return lastUseCharacterDir;
	}

	public void clrar() {
		doNotAskAgain = false;
		lastUseCharacterDir = null;
		recentCharacterDirs.clear();
	}
	
	public boolean isDoNotAskAgain() {
		return doNotAskAgain;
	}
	
	public void setDoNotAskAgain(boolean doNotAskAgain) {
		this.doNotAskAgain = doNotAskAgain;
	}

	/**
	 * ユーザーディレクトリ上の、過去に利用したキャラクターデータディレクトリの一覧をロードする.<br>
	 * 存在しなければ空を返す.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static RecentCharactersDir load() throws IOException {

		Properties props = new Properties();

		// ユーザーディレクトリのルート上に最後に使ったファイルリストをxml形式で保存する.
		File userDataDir = ConfigurationDirUtilities.getUserDataDir();
		File recentUseDirs = new File(userDataDir, RECENT_CHARACTERDIRS_XML);
		if (recentUseDirs.exists()) {
			InputStream is = new BufferedInputStream(new FileInputStream(
					recentUseDirs));
			try {
				props.loadFromXML(is);
			} finally {
				is.close();
			}
		}

		RecentCharactersDir inst = new RecentCharactersDir();

		inst.doNotAskAgain = Boolean.parseBoolean(props
				.getProperty("doNotAskAgain"));

		String lastUseCharacterDataDir = props
				.getProperty("lastUseCharacterDataDir");

		File lastUseCharacterDir = null;
		TreeMap<String, File> dirsMap = new TreeMap<String, File>();

		try {
			if (lastUseCharacterDataDir != null
					&& lastUseCharacterDataDir.trim().length() > 0) {
				lastUseCharacterDir = new File(new URI(
						lastUseCharacterDataDir));
			}

			Enumeration<?> enmKeys = props.propertyNames();
			while (enmKeys.hasMoreElements()) {
				String key = (String) enmKeys.nextElement();
				if (key.startsWith(DIRS_PREFIX)) {
					String value = props.getProperty(key);
					if (value != null && value.trim().length() > 0) {
						dirsMap.put(key, new File(new URI(value)));
					}
				}
			}

		} catch (URISyntaxException ex) {
			IOException ex2 = new IOException("invalid file name: " + ex);
			ex2.initCause(ex);
			throw ex2;
		}

		inst.lastUseCharacterDir = lastUseCharacterDir;

		for (File dir : dirsMap.values()) {
			if (!dir.equals(lastUseCharacterDir)) {
				inst.recentCharacterDirs.add(dir);
			}
		}
		if (lastUseCharacterDir != null) {
			inst.recentCharacterDirs.add(lastUseCharacterDir);
		}

		return inst;
	}

	/**
	 * 利用したキャラクターデータディレクトリの履歴情報をユーザーディレクトリに保存する.
	 * 
	 * @throws IOException
	 */
	public void saveRecents() throws IOException {
		Properties props = new Properties();

		// 最後に使用したディレクトリ
		if (lastUseCharacterDir != null) {
			props.put("lastUseCharacterDataDir", lastUseCharacterDir.toURI()
					.toString());

			// 最後に使用したディレクトリを末尾に移動
			recentCharacterDirs.remove(lastUseCharacterDir);
			recentCharacterDirs.add(lastUseCharacterDir);

		} else {
			props.put("lastUseCharacterDataDir", "");
		}

		// ディレクトリリスト
		int idx = 0;
		for (File dir : recentCharacterDirs) {
			String key = DIRS_PREFIX + String.format("%04d", idx);
			props.put(key, dir.toURI().toString());
			idx++;
		}

		// ディレクトリを再度問い合わせないか？
		props.put("doNotAskAgain", doNotAskAgain ? "true" : "false");

		// ユーザーディレクトリのルート上に最後に使ったファイルリストをxml形式で保存する.
		File userDataDir = ConfigurationDirUtilities.getUserDataDir();
		File recentUseDirs = new File(userDataDir, RECENT_CHARACTERDIRS_XML);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(
				recentUseDirs));
		try {
			props.storeToXML(os, "recent-characterdirs");
		} finally {
			os.close();
		}
	}
}