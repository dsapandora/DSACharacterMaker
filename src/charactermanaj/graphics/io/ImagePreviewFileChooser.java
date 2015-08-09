package charactermanaj.graphics.io;

import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import charactermanaj.Main;
import charactermanaj.ui.MiniPictureBox;


/**
 * 画像ファイルを選択するファイルチューザ.<br>
 * 選択したファイルの画像のサムネイルを表示する.
 * @author seraphy
 */
public class ImagePreviewFileChooser extends JFileChooser {

	private static final long serialVersionUID = -66951985128705674L;

	/***
	 * プレビューパネル
	 */
	private MiniPictureBox previewPanel;
	
	/**
	 * デフォルトコンストラクタ
	 */
	public ImagePreviewFileChooser() {
		super();
		initAccessory();
		initFileFilter();
	}
	
	/**
	 * 初期ディレクトり指定コンストラクタ
	 * @param initDir 初期ディレクトリ
	 */
	public ImagePreviewFileChooser(File initDir) {
		super(initDir);
		initAccessory();
		initFileFilter();
	}
	
	protected Image getSelectedImage() {
		return previewPanel.getImage();
	}
	
	protected void setSelectedImage(Image selectedImage) {
		previewPanel.setImage(selectedImage);
	}

	/**
	 * イメージをロードする.<br>
	 * ファィルパスがnullまたはファイルを示さないか実在しない場合はnull.<br>
	 * 既定ではイメージは非同期読み込みとなる.<br>
	 * @param file ファイルパス
	 * @return イメージ、もしくはnull
	 */
	protected Image loadImage(File file) {
		if (file == null || !file.exists() || !file.isFile()) {
			return null;
		}
		Toolkit tk = Toolkit.getDefaultToolkit();
		return tk.createImage(file.getPath());
	}
	
	protected void initAccessory() {
		previewPanel = createAccessory();
		previewPanel.setVisible(true);
		setAccessory(previewPanel);
		addPropertyChangeListener(
				JFileChooser.SELECTED_FILE_CHANGED_PROPERTY,
				new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				previewPanel.setImage(loadImage(getSelectedFile()));
			}
		});
	}
	
	protected MiniPictureBox createAccessory() {
		MiniPictureBox pictureBox = new MiniPictureBox();
		pictureBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(
						0,
						Main.isLinuxOrMacOSX() ? 5 : 10,
						0,
						Main.isLinuxOrMacOSX() ? 5 : 0),
				pictureBox.getBorder()));
		return pictureBox;
	}
	
	protected void initFileFilter() {
		setAcceptAllFileFilterUsed(false);
		setFileFilter(createImageFileFilter());
	}
	
	protected FileFilter createImageFileFilter() {
		return new FileFilter() {
			private final String[] acceptExts = {".png", ".jpeg", ".jpg", ".gif", ".bmp"};
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				String lcName = f.getName().toLowerCase();
				for (String acceptExt : acceptExts) {
					if (lcName.endsWith(acceptExt)) {
						return true;
					}
				}
				return false;
			}
			public String getDescription() {
				return "Picture(*.jpeg;*.png;*.gif;*.bmp)";
			};
		};
	}
}

