package charactermanaj.clipboardSupport;

import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import charactermanaj.graphics.io.ImageSaveHelper;
import charactermanaj.model.AppConfig;

/**
 * クリップボードに画像をコピーするためのセレクション.<br>
 * @author seraphy
 */
public class ImageSelection implements Transferable {
	
	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ImageSelection.class.getName());

	/**
	 * 実行環境がWindowsであるか?
	 */
	private static final boolean platformWindows;
	
	/**
	 * 対象となるイメージ
	 */
	private BufferedImage img;
	
	/**
	 * 背景色(jpeg画像変換時)
	 */
	private Color bgColor;
	
	/**
	 * MIME汎用(PNG).
	 */
    private static final DataFlavor PNG_FLAVOR = new DataFlavor("image/png", "image/png");

	/**
	 * MIME汎用(JPEG).
	 */
    private static final DataFlavor JPEG_FLAVOR = new DataFlavor("image/jpeg", "image/jpeg");
    
	/**
	 * MIME汎用(BMP).
	 */
    private static final DataFlavor BMP_FLAVOR = new DataFlavor("image/bmp", "image/bmp");
    

    /**
     * サポートされている形式.<br>
     * 順序は優先順.<br>
     */
    private static final List<DataFlavor> SUPPORTED_FLAVORS;
    
    /**
     * クラスイニシャライザ
     */
    static {
    	String lcOS = System.getProperty("os.name").toLowerCase(); 
    	platformWindows = lcOS.indexOf("windows") >= 0;
    	if (platformWindows) {
    		// Windowsの場合
        	SUPPORTED_FLAVORS = Arrays.asList(new DataFlavor[] {
        			PNG_FLAVOR,
        			DataFlavor.imageFlavor,
        	});

    	} else {
    		// Linux, Mac OS Xの場合を想定
        	SUPPORTED_FLAVORS = Arrays.asList(new DataFlavor[] {
        			PNG_FLAVOR,
        			JPEG_FLAVOR,
        			BMP_FLAVOR,
        			DataFlavor.imageFlavor,
        	});
    	}
    }

    /**
     * システムのフレーバーマップを設定する.<br>
     * @return 正常にセットアップできた場合はtrue、そうでなければfalse
     */
    public static boolean setupSystemFlavorMap() {
    	try {
			AppConfig appConfig = AppConfig.getInstance();
			if (appConfig.isEnablePNGSupportForWindows()) {
    			// "PNG"へのマップを明示的に設定する.
    			// (Windowsの場合、デフォルトでは、画像はDBI転送となり透過情報を持つことができないため.)
           		FlavorMap defFlavorMap = SystemFlavorMap.getDefaultFlavorMap();
        		if (defFlavorMap instanceof SystemFlavorMap) {
        			SystemFlavorMap sysFlavorMap = (SystemFlavorMap) defFlavorMap;
					sysFlavorMap.setNativesForFlavor(PNG_FLAVOR, new String[] {"PNG"});
					sysFlavorMap.setNativesForFlavor(JPEG_FLAVOR, new String[] {"JFIF"});
        		}
			}
    		return true;

    	} catch (Exception ex) {
    		logger.log(Level.SEVERE, "systemFlavorMap setup failed.", ex);
    	}
    	return false;
    }
    
	/**
	 * セレクションを構築する.
	 * @param img 対象となるイメージ
	 */
	public ImageSelection(BufferedImage img, Color bgColor) {
		if (img == null) {
			throw new IllegalArgumentException();
		}
		this.img = img;
		this.bgColor = (bgColor == null) ? Color.white : bgColor;
	}
	
	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (flavor != null) {
			logger.log(Level.FINE, "getTransferData flavor=" + flavor);
			try {
				ImageSaveHelper imageSaveHelper = new ImageSaveHelper();
	
				if (flavor.equals(PNG_FLAVOR) || flavor.equals(JPEG_FLAVOR) || flavor.equals(BMP_FLAVOR)) {
					// image/png, image/jpeg, image/bmpの場合は、
					// そのファイル形式のデータを生成して、それを返す.
				    ByteArrayOutputStream bos = new ByteArrayOutputStream();
				    try {
				    	imageSaveHelper.savePicture(img, bgColor, bos, flavor.getMimeType(), null);

				    } finally {
				    	bos.close();
				    }
			    	return new ByteArrayInputStream(bos.toByteArray());
				}

				if (flavor.equals(DataFlavor.imageFlavor)) {
					// "image/x-java-image"の場合
	    			AppConfig appConfig = AppConfig.getInstance();
		    		if (platformWindows || !appConfig.isEnablePNGSupportForWindows()) {
						// Windowsの場合は、背景色で塗りつぶしたBMP画像に変換して返す. 
						// JDK5/6のシステムクリップボードへのコピーでは透過画像をサポートしておらず透過部分が黒色になるため.
		    			// ネイティブPNGとのマッピングが有効であれば、Windowsでは、そちらで対応する.
						return imageSaveHelper.createBMPFormatPicture(img, bgColor);

		    		} else {
		    			// Windows以外、且つ、透過サポートが有効の場合
		    			return img;
		    		}
				}
	
			} catch (RuntimeException ex) {
				logger.log(Level.WARNING, "The exception occurred during the data transfer of a clipboard.", ex);
				throw ex;
				
			} catch (IOException ex) {
				logger.log(Level.WARNING, "The exception occurred during the data transfer of a clipboard.", ex);
				throw ex;
			}
		}
		
		throw new UnsupportedFlavorException(flavor);
	}

	public DataFlavor[] getTransferDataFlavors() {
		return SUPPORTED_FLAVORS.toArray(new DataFlavor[SUPPORTED_FLAVORS.size()]);
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor != null && SUPPORTED_FLAVORS.contains(flavor);
	}
	
	public static boolean isSupprotedFlavorAvailable(Clipboard cb) {
		if (cb != null) {
			for (DataFlavor flavor : SUPPORTED_FLAVORS) {
				if (cb.isDataFlavorAvailable(flavor)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static BufferedImage getImage(Clipboard cb) throws IOException {
		if (cb == null) {
			return null;
		}
		try {
			// サポートされている形式をチェックする.
			for (DataFlavor flavor : cb.getAvailableDataFlavors()) {
				logger.log(Level.FINE, "dataFlavor(in Clipboard)=" + flavor);
			}
			DataFlavor availableFlavor = null;
			for (DataFlavor flavor : SUPPORTED_FLAVORS) {
				// 優先順にチェックし最初に見つかったサポートされている形式を採用する.
				if (cb.isDataFlavorAvailable(flavor)) {
					availableFlavor = flavor;
					break;
				}
			}
			logger.log(Level.FINE, "selected flavor=" + availableFlavor);
			if (availableFlavor != null) {
				
				if (availableFlavor.equals(DataFlavor.imageFlavor)) {
					// 汎用の画像形式で取得を試みる。
					// 透過画像は使えないため、ここで取得されるものは非透過画像である。
					return (BufferedImage) cb.getData(DataFlavor.imageFlavor);
				}
				
				if (availableFlavor.equals(PNG_FLAVOR)
						|| availableFlavor.equals(JPEG_FLAVOR)
						|| availableFlavor.equals(BMP_FLAVOR)) {
					// image/png, image/bmp, image/jpegのいずれか
					
					InputStream is = (InputStream) cb.getData(availableFlavor);
					if (is != null) {
						BufferedImage img;
						try {
							img = ImageIO.read(is);
						} finally {
							is.close();
						}
						return img;
					}
				}
			}

		} catch (IOException ex) {
			logger.log(Level.WARNING, "The exception occurred in access to a clipboard.", ex);
			throw ex;

		} catch (UnsupportedFlavorException ex) {
			// 直前にisDataFlavorAvailableで確認しているので、
			// よほどタイミングが悪くなければエラーは発生しないはず。
			logger.log(Level.WARNING, "The exception occurred in access to a clipboard.", ex);
			throw new IOException(ex.getMessage());
		}

		// サポートしているものが無い場合.
		return null;
	}
}
