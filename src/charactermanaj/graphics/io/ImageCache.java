package charactermanaj.graphics.io;

import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 画像のキャッシュ.<br>
 * キャッシュは自動的にガベージコレクタにより回収されます.<br>
 * ただし、{@link #unlockImages()}が呼び出されるまで、{@link #set(Object, BufferedImage)}されたイメージは
 * ガベージコレクトの対象にはなりません。
 * @author seraphy
 *
 * @param <K>
 */
public class ImageCache<K> {

	private static final Logger logger = Logger.getLogger(ImageCache.class.getName());

	private static final ImageCacheMBeanImpl imageCacheMBean = ImageCacheMBeanImpl.getSingleton();

	private HashMap<K, BufferedImageWithKeyReference<K>> lockedImages
		= new HashMap<K, BufferedImageWithKeyReference<K>>();

	private ReferenceQueue<LoadedImage> queue = new ReferenceQueue<LoadedImage>();

	private HashMap<K, BufferedImageWithKeyReference<K>> caches
		= new HashMap<K, BufferedImageWithKeyReference<K>>();

	public ImageCache() {
	    imageCacheMBean.incrementInstance();
	}

	@Override
	protected void finalize() throws Throwable {
	    clear();
        imageCacheMBean.decrementInstance();
	    super.finalize();
	}

	public LoadedImage get(K key) {
		if (key == null) {
			return null;
		}
		synchronized (caches) {
			BufferedImageWithKeyReference<K> ref = caches.get(key);
			LoadedImage img = null;
			if (ref != null) {
				img = ref.get();
			}
			imageCacheMBean.incrementReadCount(img != null);
			sweep();
			return img;
		}
	}

	public void set(K key, LoadedImage img) {
		if (key == null) {
			return;
		}
		synchronized (caches) {
		    // 現在キャッシュされているものがあれば、いったん解放する.
            BufferedImageWithKeyReference<K> ref = caches.get(key);
            if (ref != null) {
                ref.enqueue();
            }

            if (img == null) {
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE, "remove cache: " + key);
				}
				caches.remove(key);

			} else {
				BufferedImageWithKeyReference<K> cacheData = new BufferedImageWithKeyReference<K>(key, img, queue);
				lockedImages.put(key, cacheData);
				caches.put(key, cacheData);

				imageCacheMBean.cacheIn(cacheData.getImageSize());
			}

            // 解放済みのアイテムエントリを除去する.
            sweep();
		}
	}

	public void unlockImages() {
		synchronized (caches) {
			lockedImages.clear();
			sweep();
		}
	}

	/**
	 * すべてのエントリをキャッシュアウトしてクリアする.
	 */
	public void clear() {
	    synchronized (caches) {
            lockedImages.clear();
	        for (BufferedImageWithKeyReference<K> ref : caches.values()) {
	            ref.enqueue();
	        }
            sweep();
            caches.clear();
	    }
	}

	public void sweep() {
		synchronized (caches) {
			// ガベージコレクト済みアイテムを除去する
			Reference<?> ref = null;
			boolean removed = false;
			while ((ref = queue.poll()) != null) {
				@SuppressWarnings("unchecked")
				BufferedImageWithKeyReference<K> r =
				    (BufferedImageWithKeyReference<K>) ref;
				K key = r.getKey();
				if (key != null) {
					if (caches.get(key).get() == null) {
						if (logger.isLoggable(Level.FINE)) {
							logger.log(Level.FINE, "removed cache: " + key);
						}
						removed = true;
						caches.remove(key);
					}
				}

				int imageSize = r.getImageSize();
				imageCacheMBean.cacheOut(imageSize);
			}
			if (removed) {
				if (logger.isLoggable(Level.FINE)) {
					logger.log(Level.FINE,
							"cache[" + Integer.toHexString(this.hashCode())
									+ "] size:" + caches.size());
				}
			}
		}
	}
}

/**
 * キー情報つきSoftReference
 * @author seraphy
 *
 * @param <K> キー
 */
class BufferedImageWithKeyReference<K> extends SoftReference<LoadedImage> {

	private final K key;

	private final int imageSize;

	public BufferedImageWithKeyReference(K key, LoadedImage img, ReferenceQueue<? super LoadedImage> queue) {
		super(img, queue);
		this.key = key;
		this.imageSize = (img == null) ? 0 : img.getImageSize();
	}

	public K getKey() {
		return key;
	}

	public int getImageSize() {
        return imageSize;
    }
}
