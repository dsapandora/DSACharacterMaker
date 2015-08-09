package charactermanaj.graphics;

import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.Closeable;
import java.io.IOException;

import charactermanaj.graphics.colormodel.ColorModel;
import charactermanaj.graphics.colormodel.ColorModels;
import charactermanaj.graphics.filters.ColorConvertFilter;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.filters.ContrastTableFactory;
import charactermanaj.graphics.filters.GammaTableFactory;
import charactermanaj.graphics.io.ImageLoader;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.LoadedImage;


/**
 * 画像リソースをロードし色変換された結果の画像イメージとして返す.<br>
 * @author seraphy
 */
public class ColorConvertedImageLoaderImpl implements ColorConvertedImageLoader, Closeable {

	private static final ColorConvertParameter NULL_COLORCONVPARAM = new ColorConvertParameter();

	private ImageLoader loader;


	public ColorConvertedImageLoaderImpl(ImageLoader loader) {
		if (loader == null) {
			throw new IllegalArgumentException();
		}
		this.loader = loader;
	}


	/**
	 * 画像リソースをロードし色変換した結果のBufferedImageを返します.<br>
	 * 返される形式はARGBに変換されています.<br>
	 *
	 * @param file
	 *            画像リソース
	 * @param colorConvParam
	 *            色変換パラメータ、nullの場合はデフォルト
	 * @param colorModel
	 *            カラーモデル、nullの場合はデフォルト
	 * @return 画像イメージ
	 * @throws IOException
	 *             形式が不明であるか、ファィルがないか読み取りに失敗した場合
	 */
	public LoadedImage load(ImageResource file,
			ColorConvertParameter colorConvParam, ColorModel colorModel)
			throws IOException {
		if (file == null) {
			throw new IllegalArgumentException();
		}

		if (colorConvParam == null) {
			colorConvParam = NULL_COLORCONVPARAM;
		}
		if (colorModel == null) {
			colorModel = ColorModels.DEFAULT;
		}

		LoadedImage loadedImage = loader.load(file);
		BufferedImage originalImage = loadedImage.getImage();
		BufferedImage image = colorConvert(originalImage, colorConvParam,
				colorModel);
		return new LoadedImage(image, loadedImage.getLastModified());
	}

	public void close() {
	    if (loader instanceof Closeable) {
	        try {
	            ((Closeable) loader).close();

	        } catch (RuntimeException ex) {
	            throw ex;
	        } catch (Exception ex) {
	            throw new RuntimeException(ex);
	        }
	    }
	}

	/**
	 * 色変換ロジック.
	 * @param img 元画像(ARGB形式)
	 * @param param 変換パラメータ
	 * @return 色変換後の画像
	 */
	private BufferedImage colorConvert(BufferedImage img,
			ColorConvertParameter param, ColorModel colorModel) {

		float[] factors = {
				param.getFactorR(),
				param.getFactorG(),
				param.getFactorB(),
				param.getFactorA(),
				};
		float[] offsets = {
				param.getOffsetR(),
				param.getOffsetG(),
				param.getOffsetB(),
				param.getOffsetA(),
				};
		RescaleOp rescale_op = new RescaleOp(factors, offsets, null);

		float[] gammas = {
				param.getGammaA(),
				param.getGammaR(),
				param.getGammaG(),
				param.getGammaB(),
				};
		float[] hsbs = {
				param.getHue(),
				param.getSaturation(),
				param.getBrightness()
				};

		float contrast = param.getContrast();

		ColorConvertFilter colorConvert_op = new ColorConvertFilter(
				colorModel,
				param.getColorReplace(),
				hsbs,
				param.getGrayLevel(),
				new GammaTableFactory(gammas),
				new ContrastTableFactory((float) Math.exp(contrast * 2.f)) // 対数補正
				);

		img = colorConvert_op.filter(img, null);
		img = rescale_op.filter(img, img);

		return img;
	}
}
