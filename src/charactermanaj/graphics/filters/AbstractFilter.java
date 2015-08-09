package charactermanaj.graphics.filters;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

/**
 * 色フィルタの抽象クラス.<br>
 * @author seraphy
 */
public abstract class AbstractFilter implements BufferedImageOp {

	/**
	 * 色変換を行うルーチン.<br>
	 * 派生クラスでオーバーライドする.<br>
	 * @param pixcels ARGB形式のピクセルデータ
	 */
	protected abstract void filter(int[] pixcels);

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		if (destCM == null) {
			destCM = src.getColorModel();
		}
		int w = src.getWidth();
		int h = src.getHeight();
		return new BufferedImage(destCM,
				destCM.createCompatibleWritableRaster(w, h),
				destCM.isAlphaPremultiplied(), null);
	}

	public BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest == null) {
			dest = createCompatibleDestImage(src, null);
		}
		int w = src.getWidth();
		int h = src.getHeight();
		
		int imageType = src.getType();
		int [] pixcels;
		boolean shared = false;
		if (src == dest && (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB)) {
			// 元イメージと出力先イメージが同一であり、且つ、
			// イメージがARGB/RGB形式であればピクセルデータはイメージが持つバッファそのものを共有アクセスする.
			// したがって、setPixcelsの呼び出しは不要.
			pixcels = null;
			shared = true;
		} else {
			// 元イメージと出力先イメージが異なるか、もしくは、
			// イメージがARGB/RGB形式以外であれば、RGB形式のint配列に変換して処理する.
			// イメージに書き戻すためにsetPixcelsの呼び出しが必要となる.
			int len = w * h;
			pixcels = new int[len];
		}
		pixcels = getPixcels(src, 0, 0, w, h, pixcels);
		
		filter(pixcels);

		if (!shared) {
			setPixcels(dest, 0, 0, w, h, pixcels);
		}

		return dest;
	}
	

	public Rectangle2D getBounds2D(BufferedImage src) {
		int w = src.getWidth();
		int h = src.getHeight();
		return new Rectangle(0, 0, w, h);
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		return (Point2D) srcPt.clone();
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

	/**
	 * ピクセルデータを取得する.<br>
	 * 画像のタイプがARGBもしくはRGBの場合はpixcelsにnullを指定して格納先を指定しない場合は
	 * 格納先を自動的に構築する.<br>
	 * そうでない場合はピクセルデータはイメージのピクセルバッファを、そのまま返す.(つまり、変更は即イメージの変更になる.)<br>
	 * ARGB,RGB以外のイメージは常に幅x高さ分のRGB(もしくはARGB)を格納できるだけのバッファを指定しなければならない.<br>
	 * @param img 対象のイメージ
	 * @param x 位置x
	 * @param y 位置y
	 * @param w 幅
	 * @param h 高さ
	 * @param pixcels 格納先バッファ、もしくはnull
	 * @return ピクセルデータ
	 */
	protected int[] getPixcels(BufferedImage img, int x, int y, int w, int h, int[] pixcels) {
		if (w <= 0 || h <= 0) {
			return new int[0];
		}
		int len = w * h;
		if (pixcels != null && pixcels.length < len) {
			throw new IllegalArgumentException("array too short.");
		}
		
		int imageType = img.getType();
		if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
			WritableRaster raster = img.getRaster();
			if (pixcels == null) {
				DataBufferInt buf = (DataBufferInt) raster.getDataBuffer();
				return buf.getData();
			}
			return (int[]) raster.getDataElements(x, y, w, h, pixcels);
		}

		if (pixcels == null) {
			throw new IllegalArgumentException("image type error.");
		}
		return img.getRGB(x, y, w, h, pixcels, 0, w);
	}

	/**
	 * ピクセルデータをイメージに書き戻す.<br>
	 * ピクセルデータがnullであるか幅または高さが0であれば何もしない.<br>
	 * @param img 対象のイメージ
	 * @param x 位置x
	 * @param y 位置y
	 * @param w 幅
	 * @param h 高さ
	 * @param pixcels ピクセルデータ、nullの場合は何もしない.
	 */
	protected void setPixcels(BufferedImage img, int x, int y, int w, int h, int[] pixcels) {
		int len = w * h;
		if (pixcels == null || w == 0 || h == 0) {
			return;
		}
		if (pixcels.length < len) {
			throw new IllegalArgumentException("array too short.");
		}
		int imageType = img.getType();
		if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
			WritableRaster raster = img.getRaster();
			raster.setDataElements(x, y, w, h, pixcels);
			return;
		}
		img.setRGB(x, y, w, h, pixcels, 0, w);
	}
	
}
