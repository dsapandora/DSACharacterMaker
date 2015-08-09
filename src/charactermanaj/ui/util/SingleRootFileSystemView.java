package charactermanaj.ui.util;

import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileSystemView;

/**
 * ファイルチューザのコンストラクタに指定することで、特定のディレクトリ以外に移動できないようにする<br>
 * ためのファイルシステムビューを構築します.<br>
 * 親ディレクトリへの移動、別のルートディレクトリの選択はできず、新規ディレクトリの作成もできません.<br>
 * 
 * @author seraphy
 */
public class SingleRootFileSystemView extends FileSystemView {

	/**
	 * 対象ディレクトリ
	 */
	private File dir;

	public SingleRootFileSystemView(File templDir) {
		if (templDir == null) {
			throw new IllegalArgumentException();
		}
		this.dir = templDir;
	}

	@Override
	public File createNewFolder(File containingDir) throws IOException {
		return null;
	}
	@Override
	public File getDefaultDirectory() {
		return dir;
	}
	@Override
	public File getHomeDirectory() {
		return dir;
	}
	@Override
	public File[] getRoots() {
		return new File[]{dir};
	}
}
