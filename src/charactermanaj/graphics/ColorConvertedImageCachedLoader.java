package charactermanaj.graphics;

import java.io.IOException;

import charactermanaj.graphics.colormodel.ColorModel;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageCache;
import charactermanaj.graphics.io.ImageCachedLoader;
import charactermanaj.graphics.io.ImageLoader;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.LoadedImage;

/**
 * 画像リソースに対する色変換後の画像イメージを返します.<br>
 * 一度読み込まれ色変換された画像は、画像ファイルの更新日が同一であり、且つ、色パラメータに変更がなければ
 * 読み込み済みの画像イメージを返します.<br>
 * @author seraphy
 *
 */
public class ColorConvertedImageCachedLoader extends ColorConvertedImageLoaderImpl {

	private ImageCache<ColorConvertedImageKey> caches = new ImageCache<ColorConvertedImageKey>();

	public ColorConvertedImageCachedLoader() {
		this(new ImageCachedLoader());
	}

	public ColorConvertedImageCachedLoader(ImageLoader imageLoader) {
		super(imageLoader);
	}

	@Override
	public LoadedImage load(ImageResource file,
			ColorConvertParameter colorConvParam, ColorModel colorModel)
			throws IOException {
		if (file == null) {
			throw new IllegalArgumentException();
		}

		ColorConvertParameter param;
		if (colorConvParam == null) {
			param = new ColorConvertParameter();
		} else {
			param = colorConvParam.clone();
		}
		ColorConvertedImageKey key = new ColorConvertedImageKey(param, file);

		synchronized (caches) {
			LoadedImage loadedImage = caches.get(key);
			if (loadedImage == null) {
				loadedImage = super.load(file, param, colorModel);
				caches.set(key, loadedImage);
			}
			return loadedImage;
		}
	}

	@Override
	public void close() {
	    caches.clear();
	    super.close();
	}

	public void unlockImages() {
		caches.unlockImages();
	}

}

final class ColorConvertedImageKey {

	private final ColorConvertParameter colorConvParameter;

	private final ImageResource imageResource;

	private final long lastModified;

	private final int hashCode;

	public ColorConvertedImageKey(ColorConvertParameter colorConvParameter, ImageResource imageResource) {
		if (colorConvParameter == null || imageResource == null) {
			throw new IllegalArgumentException();
		}
		this.colorConvParameter = colorConvParameter;
		this.imageResource = imageResource;
		this.lastModified = imageResource.lastModified();
		this.hashCode = imageResource.hashCode()
				^ colorConvParameter.hashCode() ^ (int) this.lastModified;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ColorConvertedImageKey) {
			ColorConvertedImageKey other = (ColorConvertedImageKey) obj;
			return lastModified == other.lastModified
					&& imageResource.equals(other.imageResource)
					&& colorConvParameter.equals(other.colorConvParameter);
		}
		return false;
	}
}
