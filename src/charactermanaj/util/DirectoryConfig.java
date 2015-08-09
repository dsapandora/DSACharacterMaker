package charactermanaj.util;

import java.io.File;

/**
 * 起動時に選択するキャラクターデータを格納する親ディレクトリ
 * 
 * @author seraphy
 */
public class DirectoryConfig {

	/**
	 * シングルトン
	 */
	private static final DirectoryConfig inst = new DirectoryConfig();
	
	private File charactersDir;
	
	private DirectoryConfig() {
		super();
	}
	
	/**
	 * キャラクターデータを格納するディレクトリを取得する.<br>
	 * まだ未設定であればIllegalStateException例外が発生する.<br>
	 * 
	 * @return キャラクターデータを格納するディレクトリ
	 */
	public File getCharactersDir() {
		if (charactersDir == null) {
			throw new IllegalStateException("キャラクターディレクトリが設定されていません.");
		}
		return charactersDir;
	}
	
	public void setCharactersDir(File charactersDir) {
		this.charactersDir = charactersDir;
	}
	
	public static DirectoryConfig getInstance() {
		return inst;
	}
}
