package charactermanaj.ui;

import java.io.File;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import charactermanaj.model.AppConfig;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * OK押下時に拡張子の補完とオーバーライドの警告を行うファイルチューザー.<br> 
 * @author seraphy
 */
public class ArchiveFileChooser extends JFileChooser {
	
	private static final long serialVersionUID = -3908688762049311010L;
	
	protected static final String STRINGS_RESOURCE = "languages/exportwizdialog";

	/**
	 * Jarファイルフィルタ
	 */
	public static final FileFilter cmjFileFilter = new FileFilter() {
		@Override
		public String getDescription() {
			return "CharacterManaJ (*.cmj)";
		}
		
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".cmj");
		}
	};
	
	/**
	 * Zipファイルフィルタ
	 */
	public static final FileFilter zipFileFilter = new FileFilter() {
		@Override
		public String getDescription() {
			AppConfig appConfig = AppConfig.getInstance();
			String zipNameEncoding = appConfig.getZipNameEncoding();
			return "ZIP (" + zipNameEncoding + ") (*.zip)";
		}
		
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".zip");
		}
	};

	protected boolean writeMode;
	
	protected ArchiveFileChooser(File initFile, boolean writeMode) {
		super(initFile);
		this.writeMode = writeMode;
		
		// フィルタの登録
		addChoosableFileFilter(zipFileFilter);
		addChoosableFileFilter(cmjFileFilter);

		// デフォルトのフィルタ
		setFileFilter(zipFileFilter);
	}
	
	@Override
	public void approveSelection() {
		File file = getSelectedFile();
		if (file == null) {
			return;
		}
		
		// ディレクトリ名を指定した場合は、そこに移動する.
		if (file.exists() && file.isDirectory()) {
			setCurrentDirectory(file);
			setSelectedFile(null);
			return;
		}

		String lcName = file.getName().toLowerCase();
		FileFilter selfilter = getFileFilter();
		if (selfilter == cmjFileFilter) {
			if (!lcName.endsWith(".cmj")) {
				file = new File(file.getPath() + ".cmj");
				setSelectedFile(file);
			}
		}
		if (selfilter == zipFileFilter) {
			if (!lcName.endsWith(".zip")) {
				file = new File(file.getPath() + ".zip");
				setSelectedFile(file);
			}
		}
		
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
				.getLocalizedProperties(STRINGS_RESOURCE);

		if (writeMode && file.exists()) {
			if (JOptionPane.showConfirmDialog(this, strings.getProperty("confirm.overwrite"),
					strings.getProperty("confirm"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				return;
			}
		}
		if (!writeMode && !file.exists()) {
			JOptionPane.showMessageDialog(this, strings.getProperty("requiredExists"));
			return;
		}
		super.approveSelection();
	}
};
