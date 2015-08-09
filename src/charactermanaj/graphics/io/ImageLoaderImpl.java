package charactermanaj.graphics.io;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;


/**
 * 画像を読み取ります.<br>
 * @author seraphy
 */
public class ImageLoaderImpl implements ImageLoader {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(ImageLoaderImpl.class.getName());
	
	/**
	 * 画像リソースからBufferedImageを返します.<br>
	 * 返される形式はARGBに変換されています.<br>
	 * @param imageResource 画像リソース
	 * @throws IOException 読み取りに失敗した場合、もしくは画像の形式が不明な場合
	 */
	public LoadedImage load(ImageResource imageResource) throws IOException {
		if (imageResource == null) {
			throw new IllegalArgumentException();
		}

		BufferedImage img;
		InputStream is = imageResource.openStream();
		try {
			img = ImageIO.read(is);

		} finally {
			is.close();
		}
		if (img == null) {
			logger.log(Level.WARNING, "unsuppoted image: " + imageResource);
			throw new IOException("unsupported image");
		}
		
		// ARGB形式でなければ変換する.
		img = convertARGB(img);
		
		long lastModified = imageResource.lastModified();

		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "load image: " + imageResource + " ;lastModified=" + new Timestamp(lastModified));
		}
		return new LoadedImage(img, lastModified);
	}
	
	/**
	 * イメージがARGB形式でなければ、ARGB形式に変換して返す.<br>
	 * そうでなければ、そのまま返す. 
	 * @param image イメージ
	 * @return ARGB形式のイメージ
	 */
	protected BufferedImage convertARGB(BufferedImage image) {
		if (image == null) {
			throw new IllegalArgumentException();
		}
		int typ = image.getType();
		if (typ == BufferedImage.TYPE_INT_ARGB) {
			return image;
		}
		// ARGB形式でなければ変換する.
		BufferedImage img2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img2.getGraphics();
		try {
			g.drawImage(image, 0, 0, null);
		} finally {
			g.dispose();
		}
		return img2;
	}
}
