package charactermanaj.ui.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルドロップターゲット.<br>
 * Windows/Macと、Linuxの両方のデスクトップのドロップをサポートする.
 * @author seraphy
 */
public class FileDropTarget extends DropTargetAdapter {

	/**
	 * ロガー
	 */
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	protected FileDropListener fileDropListener;

	public FileDropTarget() {
		this(null);
	}
	
	public FileDropTarget(FileDropListener fileDropListener) {
		this.fileDropListener = fileDropListener;
	}
	
	public FileDropListener getFileDropListener() {
		return fileDropListener;
	}
	
	public void setFileDropListener(FileDropListener fileDropListener) {
		this.fileDropListener = fileDropListener;
	}
	
	protected void onDropFiles(List<File> dropFiles) {
		if (fileDropListener != null) {
			if ( !dropFiles.isEmpty()) {
				fileDropListener.onDropFiles(dropFiles);
			}
		}
	}
	
	public void drop(DropTargetDropEvent dtde) {
		try {
			// urlListFlavor (RFC 2483 for the text/uri-list format)
			DataFlavor uriListFlavor;
			try {
				uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
			} catch (ClassNotFoundException ex) {
				logger.log(Level.WARNING, "urlListFlavor is not supported.", ex);
				uriListFlavor = null;
			}

			final List<File> dropFiles = new ArrayList<File>();
			// ドロップされたものが1つのファイルであれば受け入れる。
			for (DataFlavor flavor : dtde.getCurrentDataFlavors()) {
				logger.log(Level.FINE, "flavor: " + flavor);
				
				if (DataFlavor.javaFileListFlavor.equals(flavor)) {
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					@SuppressWarnings({ "unchecked", "rawtypes" })
					List<File> files = (List) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
					logger.log(Level.FINER, "DragAndDrop files(javaFileListFlavor)=" + files);
					dropFiles.addAll(files);
					break;
				}
				if (uriListFlavor != null && uriListFlavor.equals(flavor)) {
					// LinuxではjavaFileListFlavorではなく、text/uri-listタイプで送信される.
					dtde.acceptDrop(DnDConstants.ACTION_COPY);
					String uriList = (String) dtde.getTransferable().getTransferData(uriListFlavor);
					logger.log(Level.FINER, "DragAndDrop files(text/uri-list)=" + uriList);
					for (String fileStr : uriList.split("\r\n")) { // RFC2483によると改行コードはCRLF
						fileStr = fileStr.trim();
						if (fileStr.startsWith("#")) {
							continue;
						}
						try {
							URI uri = new URI(fileStr);
							File dropFile = new File(uri);
							dropFiles.add(dropFile);
							break;
							
						} catch (URISyntaxException ex) {
							logger.log(Level.WARNING, "invalid drop file: " + fileStr, ex);
						}
					}
				}
			}
			
			// 存在しないファイルを除去する.
			for (Iterator<File> ite = dropFiles.iterator(); ite.hasNext();) {
				File dropFile = ite.next();
				if (dropFile == null || !dropFile.exists()) {
					ite.remove();
				}
			}

			// ドロップされたファイルを通知する.
			onDropFiles(dropFiles);
			
		} catch (UnsupportedFlavorException ex) {
			logger.log(Level.WARNING, "unsipported flovaor." , ex);
			onException(ex);

		} catch (IOException ex) {
			logger.log(Level.WARNING, "drop target failed." , ex);
			onException(ex);
		}
	}
	
	protected void onException(Exception ex) {
		// do nothing.
	}
}
