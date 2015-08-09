package charactermanaj.graphics.io;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileFilter;

import charactermanaj.graphics.io.OutputOption.PictureMode;
import charactermanaj.graphics.io.OutputOption.ZoomRenderingType;
import charactermanaj.util.LocalizedMessageComboBoxRender;
import charactermanaj.util.LocalizedResourcePropertyLoader;


/**
 * イメージを保存するためのヘルパークラス.<br>
 */
public class ImageSaveHelper {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ImageSaveHelper.class.getName());

	/**
	 * リソース
	 */
	protected static final String STRINGS_RESOURCE = "languages/imageSaveHelper";

	/**
	 * このヘルパクラス用のファイルフィルタの抽象実装
	 * @author seraphy
	 */
	protected static abstract class ImageSaveHelperFilter extends FileFilter {

		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				// ディレクトリは選択可
				return true;
			}

			return isSupported(f);
		}
		
		protected boolean isSupported(File f) {
			// サポートしている拡張子のいずれかにマッチするか?
			// (大文字・小文字は区別しない.)
			String lcName = f.getName().toLowerCase();
			for (String ext : getSupprotedExtension()) {
				if (lcName.endsWith("." + ext.toLowerCase())) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 現在の選択されたファイル名を取得し、そのファイル名がデフォルトの拡張子で終端していなければ
		 * デフォルトの拡張子を設定してファイルチューザに設定し直す.<Br> 
		 * @param fileChooser ファイルチューザ
		 * @return デフォルトの拡張子で終端されたファイル
		 */
		public File supplyDefaultExtension(JFileChooser fileChooser) {
			File outFile = fileChooser.getSelectedFile();
			if (outFile == null) {
				return null;
			}
			
			if ( !isSupported(outFile)) {
				String extName = "." + getSupprotedExtension()[0];
				outFile = new File(outFile.getPath() + extName);
				fileChooser.setSelectedFile(outFile);
			}
			return outFile;
		}
		
		/**
		 * サポートするファイルの拡張子を取得する.<br>
		 * 最初のものがデフォルトの拡張子として用いられる.<br>
		 * @return ファイルの拡張子
		 */
		protected abstract String[] getSupprotedExtension();
	}
	
	/**
	 * PNGファイルフィルタ
	 */
	protected static final FileFilter pngFilter = new ImageSaveHelperFilter() {
		@Override
		public String getDescription() {
			return "PNG(*.png)";
		}
		@Override
		protected String[] getSupprotedExtension() {
			return new String[] {"png"};
		}
	};

	/**
	 * JPEGファイルフィルタ
	 */
	protected static final FileFilter jpegFilter = new ImageSaveHelperFilter() {
		@Override
		public String getDescription() {
			return "JPEG(*.jpg;*.jpeg)";
		}
		@Override
		protected String[] getSupprotedExtension() {
			return new String[] {"jpeg", "jpg"};
		}
	};

	/**
	 * BMPファイルフィルタ
	 */
	protected static final FileFilter bmpFilter = new ImageSaveHelperFilter() {
		@Override
		public String getDescription() {
			return "Bitmap(*.bmp)";
		}
		@Override
		protected String[] getSupprotedExtension() {
			return new String[] {"bmp"};
		}
	};
	
	/**
	 * このヘルパクラスで定義されているファイルフィルタのリスト
	 */
	protected static final List<FileFilter> fileFilters = Arrays.asList(
			pngFilter, jpegFilter, bmpFilter);
	
	/**
	 * イメージビルダファクトリ
	 */
	protected OutputImageBuilderFactory imageBuilderFactory;

	/**
	 * 最後に使用した出力オプション.<br>
	 * 未使用であれば規定値.<br>
	 */
	protected OutputOption outputOption;
	
	/**
	 * 最後に使用したディレクトリ
	 */
	protected File lastUseSaveDir;
	
	/**
	 * 最後に使用したフィルタ
	 */
	protected FileFilter lastUseFilter = pngFilter;
	
	/**
	 * 最後に使用したディレクトリを設定する
	 * @param lastUseSaveDir 最後に使用したディレクトリ、設定しない場合はnull
	 */
	public void setLastUseSaveDir(File lastUseSaveDir) {
		this.lastUseSaveDir = lastUseSaveDir;
	}
	
	/**
	 * 最後に使用したディレクトリを取得する
	 * @return 最後に使用したディレクトリ、なければnull
	 */
	public File getLastUsedSaveDir() {
		return lastUseSaveDir;
	}
	

	/**
	 * コンストラクタ
	 */
	public ImageSaveHelper() {
		imageBuilderFactory = new OutputImageBuilderFactory();
		outputOption = imageBuilderFactory.createDefaultOutputOption();
	}

	/**
	 * 画像ファイルの保存用ダイアログを表示する.
	 * @param parent 親ウィンドウ
	 * @return ファイル名
	 */
	public File showSaveFileDialog(Component parent) {

		// 最後に使用したディレクトリを指定してファイルダイアログを構築する.
		JFileChooser fileChooser = new JFileChooser(lastUseSaveDir) {
			private static final long serialVersionUID = -9091369410030011886L;

			/**
			 * OKボタン押下時の処理.
			 */
			@Override
			public void approveSelection() {
				File outFile = getSelectedFile();
				if (outFile == null) {
					return;
				}

				// 選択したファイルフィルタに従ってデフォルトの拡張子を付与する.
				FileFilter selfilter = getFileFilter();
				if (selfilter instanceof ImageSaveHelperFilter) {
					outFile = ((ImageSaveHelperFilter) selfilter).supplyDefaultExtension(this);
				}
				
				// ファイルが存在すれば上書き確認する.
				if (outFile.exists()) {
					Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
						.getLocalizedProperties(STRINGS_RESOURCE);

					if (JOptionPane.showConfirmDialog(this,
							strings.getProperty("confirmOverwrite"),
							strings.getProperty("confirm"),
							JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
						return;
					}
				}
				
				super.approveSelection();
			}
		};
		
//		// アクセサリパネルの追加˙
//		final OutputOptionPanel accessoryPanel = new OutputOptionPanel(this.outputOption);
//		fileChooser.setAccessory(accessoryPanel);
		
		// ファイルフィルタ設定
		fileChooser.setAcceptAllFileFilterUsed(false);
		for (FileFilter fileFilter : fileFilters) {
			fileChooser.addChoosableFileFilter(fileFilter);
		}

		// 最後に使用したファイルフィルタが既定のフィルタでないか未設定であればPNGにする.
		if (lastUseFilter == null || !fileFilters.contains(lastUseFilter)) {
			lastUseFilter = pngFilter;
		}

		// 最後に使用したフィルタをデフォルトのフィルタに設定する.
		fileChooser.setFileFilter(lastUseFilter);

		// ファイルダイアログを開く.
		int ret = fileChooser.showSaveDialog(parent);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		
//		// 出力オプションの保存
//		OutputOption outputOption = accessoryPanel.getOutputOption();
//		this.outputOption = outputOption;

		// 最後に使用したフィルタ、および選択したディレクトリを記憶する.
		File outFile = fileChooser.getSelectedFile();
		lastUseSaveDir = outFile.getParentFile();
		lastUseFilter = fileChooser.getFileFilter();
		
		// 選択したファイルを返す.
		return outFile; 
	}
	
	public OutputOption getOutputOption() {
		return outputOption.clone();
	}
	
	public void setOutputOption(OutputOption outputOption) {
		if (outputOption == null) {
			throw new IllegalArgumentException();
		}
		this.outputOption = outputOption.clone();
	}

	/**
	 * ファイル名を指定してイメージをファイルに出力します.<br>
	 * 出力形式は拡張子より判定します.<br>
	 * サポートされていない拡張子の場合はIOException例外が発生します.<br>
	 * @param img イメージ
	 * @param imgBgColor JPEGの場合の背景色
	 * @param outFile 出力先ファイル(拡張子が必須)
	 * @param warnings 警告を記録するバッファ、必要なければnull
	 * @throws IOException 失敗
	 */
	public void savePicture(BufferedImage img, Color imgBgColor,
			File outFile, final StringBuilder warnings) throws IOException {
		if (img == null || outFile == null) {
			throw new IllegalArgumentException();
		}
		
		// ファイル名から拡張子を取り出します.
		String fname = outFile.getName();
		int extpos = fname.lastIndexOf(".");
		if (extpos < 0) {
			throw new IOException("missing file extension.");
		}
		String ext = fname.substring(extpos + 1).toLowerCase();

		// 拡張子に対するImageIOのライタを取得します.
		Iterator<ImageWriter> ite = ImageIO.getImageWritersBySuffix(ext);
		if (!ite.hasNext()) {
			throw new IOException("unsupported file extension: " + ext);
		}
		
		ImageWriter iw = ite.next();
		
		// ライタを使いイメージを書き込みます.
		savePicture(img, imgBgColor, iw, outFile, warnings);
	}
	
	/**
	 * イメージをMIMEで指定された形式で出力します.
	 * @param img イメージ
	 * @param imgBgColor JPEGの場合の背景色
	 * @param outstm 出力先
	 * @param mime MIME
	 * @param warnings 警告を書き込むバッファ、必要なければnull
	 * @throws IOException 例外
	 */
	public void savePicture(BufferedImage img, Color imgBgColor,
			OutputStream outstm, String mime, final StringBuilder warnings)
			throws IOException {
		if (img == null || outstm == null || mime == null) {
			throw new IllegalArgumentException();
		}

		// mimeがパラメータ付きの場合は、パラメータを除去する.
		int pt = mime.indexOf(';');
		if (pt >= 0) {
			mime = mime.substring(0, pt).trim();
		}

		// サポートしているmimeタイプを検出.
		Iterator<ImageWriter> ite = ImageIO.getImageWritersByMIMEType(mime);
		if (!ite.hasNext()) {
			throw new IOException("unsupported mime: " + mime);
		}

		ImageWriter iw = ite.next();
		savePicture(img, imgBgColor, iw, outstm, warnings);
		outstm.flush();
	}
	
	protected void savePicture(BufferedImage img, Color imgBgColor, ImageWriter iw,
			Object output, final StringBuilder warnings)
			throws IOException {
		try {
			iw.addIIOWriteWarningListener(new IIOWriteWarningListener() {
				public void warningOccurred(ImageWriter source, int imageIndex,
						String warning) {
					if (warnings.length() > 0) {
						warnings.append(System.getProperty("line.separator"));
					}
					if (warnings != null) {
						warnings.append(warning);
					}
					logger.log(Level.WARNING, warning);
				}
			});

			boolean jpeg = false;
			boolean bmp = false;
			for (String mime : iw.getOriginatingProvider().getMIMETypes()) {
				if (mime.contains("image/jpeg") || mime.contains("image/jpg")) {
					jpeg = true;
					break;
				}
				if (mime.contains("image/bmp") || mime.contains("image/x-bmp")
						|| mime.contains("image/x-windows-bmp")) {
					bmp = true;
					break;
				}
			}

			ImageWriteParam iwp = iw.getDefaultWriteParam();
			IIOImage ioimg;

			if (jpeg) {
				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwp.setCompressionQuality((float) outputOption.getJpegQuality());

				// JPEGは透過色をサポートしていないので背景色を設定する.
				ioimg = new IIOImage(createJpegFormatPicture(img, imgBgColor), null, null);

			} else if (bmp) {
				// BMPは透過色をサポートしていないので背景色を設定する.
				ioimg = new IIOImage(createBMPFormatPicture(img, imgBgColor), null, null);

			} else if (outputOption.isForceBgColor()) {
				// 背景色強制
				// JPEG, BMP以外(PNGを想定)
				ioimg = new IIOImage(createOpaquePNGFormatPicture(img, imgBgColor), null, null);

			} else {
				// 透過有効のまま
				// JPEG, BMP以外(PNGを想定)
				ioimg = new IIOImage(img, null, null);
			}

			ImageOutputStream imgstm = ImageIO.createImageOutputStream(output);
			try {
				iw.setOutput(imgstm);
				iw.write(null, ioimg, iwp);

			} finally {
				imgstm.close();
			}

		} finally {
			iw.dispose();
		}
	}
	
	/**
	 * ARGB形式から、アルファチャンネルを削除し、かわりに背景色を設定したBGR形式画像を返します.<br>
	 * JPEG画像として用いることを想定しています.<br>
	 * @param img 変換するイメージ
	 * @param imgBgColor 背景色
	 * @return 変換されたイメージ
	 */
	public BufferedImage createJpegFormatPicture(BufferedImage img, Color imgBgColor) {
		if (imgBgColor == null) {
			imgBgColor = Color.WHITE;
		}
		return createFormatPicture(img, imgBgColor, BufferedImage.TYPE_INT_BGR);
	}

	/**
	 * ARGB形式から、アルファチャンネルを削除し、かわりに背景色を設定したBGR形式画像を返します.<br>
	 * BMP画像として用いることを想定しています.<br>
	 * @param img 変換するイメージ
	 * @param imgBgColor 背景色
	 * @return 変換されたイメージ
	 */
	public BufferedImage createBMPFormatPicture(BufferedImage img, Color imgBgColor) {
		if (imgBgColor == null) {
			imgBgColor = Color.WHITE;
		}
		return createFormatPicture(img, imgBgColor, BufferedImage.TYPE_3BYTE_BGR);
	}

	/**
	 * ARGB形式から、アルファチャンネルを削除し、かわりに背景色を設定したRGB形式画像を返します.<br>
	 * 背景付きPNG画像として用いることを想定しています.<br>
	 * @param img 変換するイメージ
	 * @param imgBgColor 背景色
	 * @return 変換されたイメージ
	 */
	public BufferedImage createOpaquePNGFormatPicture(BufferedImage img, Color imgBgColor) {
		if (imgBgColor == null) {
			imgBgColor = Color.WHITE;
		}
		return createFormatPicture(img, imgBgColor, BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * ARGB形式から、アルファチャンネルを削除し、かわりに背景色を設定したBGR形式画像を返します.<br>
	 * JPEG画像として用いることを想定しています.<br>
	 * @param img 変換するイメージ
	 * @param imgBgColor 背景色
	 * @return 変換されたイメージ
	 */
	protected BufferedImage createFormatPicture(BufferedImage img, Color imgBgColor, int type) {
		if (img == null) {
			throw new IllegalArgumentException();
		}
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage tmpImg = new BufferedImage(w, h, type);
		Graphics2D g = tmpImg.createGraphics();
		try {
			g.setRenderingHint(
					RenderingHints.KEY_ALPHA_INTERPOLATION,
					RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

			if (imgBgColor != null) {
				g.setColor(imgBgColor);
				g.fillRect(0, 0, w, h);
			}
			g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);

		} finally {
			g.dispose();
		}
		return tmpImg;
	}
}

/**
 * 出力オプションパネル
 * @author seraphy
 */
class OutputOptionPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JSpinner jpegQualitySpinner;
	
	private JCheckBox lblZoom;
	
	private JSpinner zoomSpinner;
	
	private JComboBox zoomAlgoCombo;
	
	private JComboBox pictureModeCombo;
	
	private JCheckBox checkForceBgColor;

	
	public OutputOptionPanel() {
		this(new OutputOption());
	}

	public OutputOptionPanel(OutputOption outputOption) {

		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(ImageSaveHelper.STRINGS_RESOURCE);

		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(3, 3, 3, 3),
				BorderFactory.createTitledBorder(
						strings.getProperty("ouputOption.caption"))));
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill  = GridBagConstraints.BOTH;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.gridheight = 1;
		gbc.ipadx = 0;
		gbc.ipady = 0;
		gbc.weighty = 0;

		// 左端余白
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		add(Box.createHorizontalStrut(6), gbc);
		
		// JPEG
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.weightx = 1.;
		JLabel lblJpeg = new JLabel(
				strings.getProperty("outputOption.jpeg.caption"));
		lblJpeg.setFont(lblJpeg.getFont().deriveFont(Font.BOLD));
		add(lblJpeg, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		add(new JLabel(
				strings.getProperty("outputOption.jpeg.quality"),
				JLabel.RIGHT), gbc);
		
		SpinnerNumberModel spmodel = new SpinnerNumberModel(100, 10, 100, 1);
		this.jpegQualitySpinner = new JSpinner(spmodel);
		
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		add(jpegQualitySpinner, gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		add(new JLabel("%"), gbc);

		// ZOOM
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.weightx = 1.;
		gbc.insets = new Insets(10, 3, 3, 3);
		lblZoom = new JCheckBox(strings.getProperty("outputOption.zoom.caption"));
		lblZoom.setFont(lblJpeg.getFont().deriveFont(Font.BOLD));
		add(lblZoom, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.weightx = 0.;
		gbc.insets = new Insets(3, 3, 3, 3);
		add(new JLabel(
				strings.getProperty("outputOption.zoom.factor"), JLabel.RIGHT), gbc);
		
		SpinnerNumberModel zoomSpModel = new SpinnerNumberModel(100, 20, 800, 1);
		this.zoomSpinner = new JSpinner(zoomSpModel);
		
		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		add(zoomSpinner, gbc);
		
		gbc.gridx = 3;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		add(new JLabel("%"), gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.weightx = 1.;
		add(new JLabel(
				strings.getProperty("outputOption.zoom.renderingMode"),
				JLabel.RIGHT), gbc);
		
		this.zoomAlgoCombo = new JComboBox(ZoomRenderingType.values());
		this.zoomAlgoCombo.setRenderer(new LocalizedMessageComboBoxRender(strings));
		gbc.gridx = 2;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.weightx = 0.;
		add(zoomAlgoCombo, gbc);
		
		// 画像モード
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 3;
		gbc.insets = new Insets(10, 3, 3, 3);
		JLabel lblPictureMode = new JLabel(
				strings.getProperty("outputOption.picture"));
		lblPictureMode.setFont(lblJpeg.getFont().deriveFont(Font.BOLD));
		add(lblPictureMode, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(3, 3, 3, 3);
		add(new JLabel(
				strings.getProperty("outputOption.picture.type"),
				JLabel.RIGHT), gbc);
		
		this.pictureModeCombo = new JComboBox(PictureMode.values());
		this.pictureModeCombo.setRenderer(
				new LocalizedMessageComboBoxRender(strings));
		gbc.gridx = 2;
		gbc.gridy = 6;
		gbc.gridwidth = 2;
		gbc.weightx = 1.;
		add(pictureModeCombo, gbc);		
		
		gbc.gridx = 1;
		gbc.gridy = 7;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;
		checkForceBgColor = new JCheckBox(
				strings.getProperty("outputOption.picture.forceBgColor"));  
		add(checkForceBgColor, gbc);
		
		JPanel pnlBgAlpha = new JPanel(new BorderLayout(3, 3));
		pnlBgAlpha.add(new JLabel("背景アルファ"), BorderLayout.WEST);
		
		SpinnerNumberModel bgAlphaModel = new SpinnerNumberModel(255, 0, 255, 1);
		JSpinner bgAlphaSpinner = new JSpinner(bgAlphaModel);

		pnlBgAlpha.add(bgAlphaSpinner, BorderLayout.CENTER);
		
		gbc.gridx = 1;
		gbc.gridy = 8;
		gbc.gridwidth = 3;
		gbc.weightx = 0.;
		add(pnlBgAlpha, gbc);

		// 余白
		gbc.gridx = 0;
		gbc.gridy = 9;
		gbc.gridwidth = 4;
		gbc.weightx = 1.;
		gbc.weighty = 1.;
		add(Box.createGlue(), gbc);
		
		// update
		setOutputOption(outputOption);
	}
	
	public void setOutputOption(OutputOption outputOption) {
		if (outputOption == null) {
			outputOption = new OutputOption();
		}
		
		jpegQualitySpinner.setValue((int) (outputOption.getJpegQuality() * 100));
		lblZoom.setSelected(outputOption.isEnableZoom());
		zoomSpinner.setValue((int) (outputOption.getZoomFactor() * 100));
		zoomAlgoCombo.setSelectedItem(outputOption.getZoomRenderingType());
		pictureModeCombo.setSelectedItem(outputOption.getPictureMode());
		checkForceBgColor.setSelected(outputOption.isForceBgColor());
	}

	public OutputOption getOutputOption() {
		OutputOption outputOption = new OutputOption();

		outputOption.setJpegQuality(((Integer) jpegQualitySpinner.getValue() / 100.));
		outputOption.setEnableZoom(lblZoom.isSelected());
		outputOption.setZoomFactor(((Integer) zoomSpinner.getValue() / 100.));
		outputOption.setZoomRenderingType((ZoomRenderingType) zoomAlgoCombo.getSelectedItem());
		outputOption.setPictureMode((PictureMode) pictureModeCombo.getSelectedItem());
		outputOption.setForceBgColor(checkForceBgColor.isSelected());
		
		return outputOption;
	}
}

