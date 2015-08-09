package charactermanaj.ui.util;

import java.io.File;
import java.util.List;

/**
 * ファイルがドロップされたことを通知されるリスナ.<br>
 * @author seraphy
 *
 */
public interface FileDropListener {

	/**
	 * ファイルがドロップされたことを通知する.
	 * @param dropFiles ドロップされたファイル
	 */
	void onDropFiles(List<File> dropFiles);
	
}
