package charactermanaj.ui;

import java.awt.Component;
import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * エクスポート・インポート用ファイルダイアログ
 * @author seraphy
 *
 */
public class ArchiveFileDialog {

	/**
	 * 最後に使ったディレクトリ
	 */
	protected File lastUsedDir;
	
	/**
	 * 最後に使用したフィルタ
	 */
	protected FileFilter lastUsedFileFiler;

	/**
	 * 保存ダイアログを開く
	 * @param parent 親
	 * @return 保存先ファイル、キャンセルした場合はnull
	 */
	public File showSaveDialog(Component parent) {
		ArchiveFileChooser fileChooser = new ArchiveFileChooser(lastUsedDir, true);
		
		int ret = fileChooser.showSaveDialog(parent);
		if (ret != ArchiveFileChooser.APPROVE_OPTION) {
			return null;
		}
		
		File outFile = fileChooser.getSelectedFile();
		lastUsedDir = outFile.getParentFile();
		
		return outFile;
	}
	
	/**
	 * 開くダイアログを開く.<br>
	 * initFileがnullの場合は前回選択したディレクトリが初期状態となる.
	 * @param parent 親
	 * @param initFile 初期選択ファイル、もしくはディレクトリ、もしくはnull
	 * @return 選択ファイル、キャンセルした場合はnull
	 */
	public File showOpenDialog(Component parent, File initFile) {
		
		// 初期ファイル名が指定されている場合、そのディレクトリをカレントにしてみる.
		// 指定されてなければ最後に使ったディレクトリを設定する.
		File initDir = null;
		if (initFile != null) {
			if (initFile.isDirectory()) {
				initDir = initFile;
			} else {
				initDir = initFile.getParentFile();
			}
		}
		// 初期ファイルの指定がなければ前回の最後に使用したディレクトリを使用
		if (initDir == null) {
			initDir = lastUsedDir;
		}
		
		ArchiveFileChooser fileChooser = new ArchiveFileChooser(initDir, false);
		
		// 最後に使用したフィルタがあれば、それを選択状態とする.
		if (lastUsedFileFiler != null) {
			fileChooser.setFileFilter(lastUsedFileFiler);
		}

		// 初期ファイル名が指定されていれば、それを選択状態としてみる.
		if (initFile != null) {
			fileChooser.setSelectedFile(initFile);
		}
		
		// ファイル選択ダイアログの表示
		int ret = fileChooser.showOpenDialog(parent);
		if (ret != ArchiveFileChooser.APPROVE_OPTION) {
			// キャンセル
			return null;
		}
		
		// 選択ファイルの取得
		File outFile = fileChooser.getSelectedFile();

		// 最後に選択したディレクトリとフィルタを記憶する.
		lastUsedDir = outFile.getParentFile();
		lastUsedFileFiler = fileChooser.getFileFilter();
		
		return outFile;
	}
	
	/**
	 * 最後に使用したディレクトリを取得する.
	 * @return
	 */
	public File getLastUSedDir() {
		return lastUsedDir;
	}
	
	/**
	 * 最後に使用したディレクトリを設定する.
	 * @param lastUSedDir
	 */
	public void setLastUSedDir(File lastUSedDir) {
		this.lastUsedDir = lastUSedDir;
	}

}
