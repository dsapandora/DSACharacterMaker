package charactermanaj.graphics.io;

import java.io.Closeable;
import java.io.IOException;


/**
 * 一度読み込んだ画像をキャッシュする画像ローダ.<br>
 * すでに読み込まれており、ファイルの更新日に変更がなければ読み込み済みの画像をかえす.<br>
 * @author seraphy
 */
public class ImageCachedLoader extends ImageLoaderImpl implements Closeable {

	/**
	 * リソースに対するイメージキャッシュ.<br>
	 * リソースは複数のプロファイルで共有しえるのでstaticとしている。
	 */
	private static ImageCache<ImageResourceCacheKey> caches = new ImageCache<ImageResourceCacheKey>();

	@Override
    public LoadedImage load(ImageResource imageResource) throws IOException {
		if (imageResource == null) {
			throw new IllegalArgumentException();
		}

		ImageResourceCacheKey key = new ImageResourceCacheKey(imageResource);

		synchronized (caches) {
			LoadedImage loadedImage = caches.get(key);

			if (loadedImage != null) {
				long lastModified = loadedImage.getLastModified();
				if (lastModified != imageResource.lastModified()) {
					// キャッシュされているが、すでに古い場合は破棄する.
					loadedImage = null;
				}
			}

			if (loadedImage == null) {
				loadedImage = super.load(imageResource);
				caches.set(key, loadedImage);
				caches.unlockImages(); // 即時解放許可
			}

			return loadedImage;
		}
	}

	public void close() {
	    caches.clear();
	}
}

final class ImageResourceCacheKey {

	private final ImageResource imageResource;

	private final int hashCode;

	public ImageResourceCacheKey(ImageResource imageResource) {
		if (imageResource == null) {
			throw new IllegalArgumentException();
		}
		this.imageResource = imageResource;
		this.hashCode = imageResource.hashCode();
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
		if (obj != null && obj instanceof ImageResourceCacheKey) {
			ImageResourceCacheKey other = (ImageResourceCacheKey) obj;
			return imageResource.equals(other.imageResource);
		}
		return false;
	}
}
