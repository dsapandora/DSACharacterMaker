package charactermanaj.model.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.util.FileNameNormalizer;

/**
 * ディレクトリをアーカイブと見立てる
 * 
 * @author seraphy
 */
public class CharacterDataDirectoryFile extends AbstractCharacterDataArchiveFile {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(CharacterDataDirectoryFile.class.getName());

	
	/**
	 * 対象ディレクトリ
	 */
	protected File baseDir;
	
	/**
	 * ディレクトリ上のファイルコンテンツ
	 * 
	 * @author seraphy
	 */
	protected static class DirFileContent implements FileContent {

		/**
		 * ディレクトリ名 + ファイル名からなるエントリ名.<br>
		 * エントリ名の区切り文字は常に「/」とする.<br>
		 */
		private String entryName;

		/**
		 * 実際のファイルへのパス
		 */
		private File entry;
		
		protected DirFileContent(File entry, String entryName) {
			this.entry = entry;
			this.entryName = entryName;
		}
		
		public String getEntryName() {
			return entryName;
		}
		
		public long lastModified() {
			return entry.lastModified();
		}

		public InputStream openStream() throws IOException {
			return new FileInputStream(entry);
		}
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
		return new File(baseDir, name).toURI();
	}
	
	@Override
	public Collection<PartsImageContent> getPartsImageContents(
			CharacterData characterData, boolean newly) {
		if (!newly && isOverlapped(characterData)) {
			// 既存のプロファイルへのインポートで指定されたインポートもととなるディレクトリが
			// 既存のプロファイルのディレクトリと重なっていれば自分自身のインポートであるとして
			// 空のパーツリストを返す.
			return Collections.emptyList();
		}
		return super.getPartsImageContents(characterData, newly);
	}
	
	/**
	 * このディレクトリに対してターゲットプロファイルのディレクトリがかぶっているか? つまり、ターゲット自身のディレクトリをソースとしていないか?
	 * 
	 * @param characterData
	 *            ソースプロファイル
	 * @return 被っている場合はtrue、被っていない場合はfalse
	 */
	protected boolean isOverlapped(CharacterData characterData) {
		if (characterData == null) {
			return false;
		}
		URI docBase = characterData.getDocBase();
		if (docBase == null || !"file".equals(docBase.getScheme())) {
			return false;
		}

		String folderPlace = File.separator;
		String basePath = new File(docBase).getParent() + folderPlace;
		String sourcePath = baseDir.getPath() + folderPlace;
		
		// インポートもとディレクトリがインポート先のキャラクター定義のディレクトリを含むか?
		boolean result = basePath.contains(sourcePath);
		logger.log(Level.FINE, "checkOverlapped: " + basePath + " * " + sourcePath + " :" + result);
		
		return result;
	}

	public void close() throws IOException {
		// ディレクトリなのでクローズ処理は必要ない.
	}
	
	public CharacterDataDirectoryFile(File file) throws IOException {
		super(file);
		baseDir = file;
		load(baseDir, "");
		searchRootPrefix();
	}
	
	private void load(File dir, String prefix) {
		if (!dir.exists() || !dir.isDirectory()) {
			// ディレクトリでなければ何もせず戻る
			return;
		}
		
		// ファイル名をノーマライズする
		FileNameNormalizer normalizer = FileNameNormalizer.getDefault();
		
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				String name = normalizer.normalize(file.getName());
				String entryName = prefix + name;
				if (file.isDirectory()) {
					// エントリ名の区切り文字は常に「/」とする. (ZIP/JARのentry互換のため)
					load(file, entryName + "/");
				} else {
					addEntry(new DirFileContent(file, entryName));
				}
			}
		}
	}

}
