package charactermanaj.graphics.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import charactermanaj.ui.UkagakaConvertDialog;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 伺か用PNG/PNA出力ヘルパ.
 * @author seraphy
 *
 */
public class UkagakaImageSaveHelper {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(UkagakaImageSaveHelper.class.getName());

	/**
	 * リソース
	 */
	protected static final String STRINGS_RESOURCE = "languages/ukagakaImageSaveHelper";

	/**
	 * PNGファイルフィルタ
	 */
	protected static final FileFilter pngFilter = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().endsWith(".png");
		}
		@Override
		public String getDescription() {
			return "PNG(*.png)";
		}
	};

	/**
	 * 最後に開いたディレクトリ.<br>
	 * まだ使用していなければnull.<br>
	 */
	protected File lastUseOpenDir;
	
	/**
	 * 最後に保存したディレクトリ.<br>
	 * まだ使用していなければnull.<br>
	 */
	protected File lastUseSaveDir;
	
	/**
	 * 最後に保存したファイル名
	 */
	protected String lastUseSaveName = "surface";

	/**
	 * 最後に使用した透過色キー.<br>
	 * まだ使用していなければnull.<br>
	 */
	protected Color transparentColorKey;
	
	/**
	 * 最後に使用した透過色キーモード.
	 */
	protected boolean autoTransparentColor = true;
	
	/**
	 * コンストラクタ
	 */
	public UkagakaImageSaveHelper() {
		Properties strings = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(STRINGS_RESOURCE);

		lastUseSaveName = strings.getProperty("default.lastUseSaveName"); 
	}
	
	protected JFileChooser createFileChooser(final boolean save) {
		JFileChooser fileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			@Override
			public void approveSelection() {
				File outFile = getSelectedFile();
				if (outFile == null) {
					return;
				}
				
				String lcName = outFile.getName().toLowerCase();
				FileFilter selfilter = getFileFilter();
				if (selfilter == pngFilter) {
					if (!lcName.endsWith(".png")) {
						outFile = new File(outFile.getPath() + ".png");
						setSelectedFile(outFile);
					}
				}

				if (save && outFile.exists()) {
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

		if (lastUseSaveDir != null) {
			fileChooser.setCurrentDirectory(lastUseSaveDir);
		}
		
		fileChooser.setFileFilter(pngFilter);
		if ( !save) {
			fileChooser.setAcceptAllFileFilterUsed(false);
		}
		fileChooser.addChoosableFileFilter(pngFilter);
		
		return fileChooser;
	}
	
	/**
	 * 透過画像(TYPE_INT_ARGB)から、伺か用のPNG/PNAファイルに出力する.
	 * @param parent 親フレーム
	 * @param img 対象イメージ
	 * @param colorKey マニュアル指定時の候補(前回のものを優先)
	 * @throws IOException 出力に失敗した場合
	 */
	public void save(JFrame parent, BufferedImage img, Color colorKey) throws IOException {
		final UkagakaConvertDialog dlg = new UkagakaConvertDialog(parent);
		if (!autoTransparentColor && transparentColorKey != null) {
			// 前回マニュアル透過色キー指定であれば、それを使う.
			colorKey = transparentColorKey;
		}
		dlg.setExportImage(img, colorKey);
		dlg.setAutoTransparentColor(autoTransparentColor);
		dlg.setSaveActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = createFileChooser(true);
				fileChooser.setSelectedFile(new File(lastUseSaveDir, lastUseSaveName));

				int ret = fileChooser.showSaveDialog(dlg);
				if (ret != JFileChooser.APPROVE_OPTION) {
					return;
				}

				// 結果
				dlg.setResult(fileChooser.getSelectedFile());
				dlg.dispose();
			}
		});
		dlg.setVisible(true);
		
		File selectedFile = (File) dlg.getResult();
		if (selectedFile == null) {
			return;
		}
		
		lastUseSaveName = selectedFile.getName();
		lastUseSaveDir = selectedFile.getParentFile();
		
		File pngFile = selectedFile;
		File pnaFile = makePNAFileName(pngFile);

		File[] outfiles = {pngFile, pnaFile};
		BufferedImage[] outimages = {
				dlg.getOpaqueImage(),
				dlg.getAlphaImage()
		};

		savePNGImages(outfiles, outimages);
	}

	/**
	 * 複数のファイルとイメージを指定して書き込みます.<br>
	 * ファイルとイメージの個数は一致していなければなりません.<br>
	 * 同じ添え字のファイルに対して、その添え字のイメージが出力されます.<br>
	 * いずれかで失敗した場合、その時点で処理は打ち切られて例外が返されます.<br>
	 * (すでに出力されたファイル、もしくは書き込み中のファイルは放置されます.)<br>
	 * @param outfiles ファイルの配列
	 * @param outimages イメージの配列
	 * @throws IOException 失敗
	 */
	protected void savePNGImages(File[] outfiles, BufferedImage[] outimages) throws IOException {
		if (outfiles == null || outimages == null) {
			throw new IllegalArgumentException("引数にnullは指定でまきせん。");
		}
		if (outfiles.length != outimages.length) {
			throw new IllegalArgumentException("ファイルおよびイメージの個数は一致していなければなりません.");
		}
		
		ImageWriter iw = ImageIO.getImageWritersByFormatName("png").next();
		try {
			iw.addIIOWriteWarningListener(new IIOWriteWarningListener() {
				public void warningOccurred(ImageWriter source, int imageIndex,
						String warning) {
					logger.log(Level.WARNING, warning);
				}
			});

			for (int idx = 0; idx < outfiles.length; idx++) {
				File outfile = outfiles[idx];
				BufferedImage outimage = outimages[idx];
			
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(outfile));
				try {
					ImageWriteParam iwp = iw.getDefaultWriteParam();
					IIOImage ioimg = new IIOImage(outimage, null, null);
					
					ImageOutputStream imgstm = ImageIO.createImageOutputStream(bos);
					try {
						iw.setOutput(imgstm);
						iw.write(null, ioimg, iwp);
					
					} finally {
						imgstm.close();
					}
					
				} finally {
					bos.close();
				}
			}

		} finally {
			iw.dispose();
		}
	}
	
	/**
	 * 複数ファイルを指定して既存のファイルから伺かPNG/PNAに変換して出力する.(ユーテリティ)
	 * @param parent 親フレーム
	 * @param colorKey 透過色キー(候補)
	 * @throws IOException 失敗
	 */
	public void convertChooseFiles(JFrame parent, Color colorKey) throws IOException {
		JFileChooser fileChooser = createFileChooser(false);
		fileChooser.setCurrentDirectory(lastUseOpenDir);
		fileChooser.setMultiSelectionEnabled(true);
		
		int ret = fileChooser.showOpenDialog(parent);
		if (ret != JFileChooser.APPROVE_OPTION) {
			return;
		}

		// 選択したディレクトリを記憶する.
		File[] files = fileChooser.getSelectedFiles();
		if (files == null || files.length == 0) {
			return;
		}
		lastUseOpenDir = files[0].getParentFile();
		
		final UkagakaConvertDialog dlg = new UkagakaConvertDialog(parent, null, true);
		if (!autoTransparentColor && transparentColorKey != null) {
			// 前回マニュアル透過色キー指定であれば、それを使う.
			colorKey = transparentColorKey;
		}
		dlg.setAutoTransparentColor(autoTransparentColor);

		ImageReader ir = ImageIO.getImageReadersByFormatName("png").next();
		try {
			for (final File file : files) {
				String fname = file.getName();
				
				ImageReadParam param = ir.getDefaultReadParam();
				
				BufferedImage img;
				ImageInputStream iis = ImageIO.createImageInputStream(file);
				try {
					ir.setInput(iis);
					img = ir.read(0, param);

				} finally {
					iis.close();
				}
				
				img = convertIntARGB(img);

				dlg.setCaption(fname);
				dlg.setExportImage(img, colorKey);
				dlg.setSaveActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if ( !dlg.isOverwriteOriginalFile()) {
							JFileChooser fileChooser = createFileChooser(true);
							fileChooser.setCurrentDirectory(file.getParentFile());
							fileChooser.setSelectedFile(file);

							int ret = fileChooser.showSaveDialog(dlg);
							if (ret != JFileChooser.APPROVE_OPTION) {
								return;
							}

							// 選択結果
							dlg.setResult(fileChooser.getSelectedFile());

						} else {
							// ソースと同じ (上書き)
							dlg.setResult(file);
						}

						dlg.dispose();
					}
				});
				dlg.setVisible(true);
				
				File selectedFile = (File) dlg.getResult();
				if (selectedFile == null) {
					// キャンセルされた場合
					break;
				}

				File pngFile = selectedFile;
				File pnaFile = makePNAFileName(pngFile);

				File[] outfiles = {pngFile, pnaFile};
				BufferedImage[] outimages = {
						dlg.getOpaqueImage(),
						dlg.getAlphaImage()
				};

				savePNGImages(outfiles, outimages);
			}
			
		} finally {
			ir.dispose();
		}
	}
	
	/**
	 * BufferedImageをINT_TYPE_ARGBに設定する.<br>
	 * @param img イメージ
	 * @return 形式をINT_ARGBに変換されたイメージ
	 */
	protected BufferedImage convertIntARGB(BufferedImage img) {
		if (img == null || img.getType() == BufferedImage.TYPE_INT_ARGB) {
			// nullであるか、変換不要であれば、そのまま返す.
			return img;
		}
		
		int w = img.getWidth();
		int h = img.getHeight();
		
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = dst.createGraphics();
		try {
			g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);
			
		} finally {
			g.dispose();
		}
		
		return dst;
	}
		
	
	/**
	 * 拡張子をPNAに変換して返す.
	 * @param pngFile PNGファイル名
	 * @return PNAファイル名
	 */
	protected File makePNAFileName(File pngFile) {
		if (pngFile == null) {
			return null;
		}
		String fname = pngFile.getName();
		int extpos = fname.lastIndexOf('.');
		if (extpos >= 0) {
			fname = fname.substring(0, extpos);
		}
		fname += ".pna";
		return new File(pngFile.getParent(), fname);
	}
}

