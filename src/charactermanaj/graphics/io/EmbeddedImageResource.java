package charactermanaj.graphics.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.util.ResourceLoader;


/**
 * クラスローダからリソースを読み込むイメージリソース.<br>
 * @author seraphy
 */
public class EmbeddedImageResource extends ResourceLoader implements ImageResource, Serializable {
	
	/**
	 * シリアライズバージョン
	 */
	private static final long serialVersionUID = 703707046457343373L;

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(EmbeddedImageResource.class.getName());

	/**
	 * ファイル
	 */
	private String resourceName;
	
	
	public EmbeddedImageResource(String resourceName) {
		if (resourceName == null) {
			throw new IllegalArgumentException();
		}
		this.resourceName = resourceName;
	}
	
	public int compareTo(ImageResource o) {
		return getFullName().compareTo(o.getFullName());
	}
	
	@Override
	public int hashCode() {
		return getFullName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ImageResource) {
			ImageResource o = (ImageResource) obj;
			return getFullName().equals(o.getFullName());
		}
		return false;
	}
	
	public String getFullName() {
		return resourceName;
	}
	
	public URI getURI() {
		URL url = getResource(resourceName);
		if (url != null) {
			try {
				return url.toURI();

			} catch(URISyntaxException ex) {
				logger.log(Level.WARNING, "resource name is invalid. " + resourceName, ex);
				// 何もしない.
			}
		}
		return null;
	}
	
	/**
	 * リソースが実在すれば日付は常に1を返す.<br>
	 * リソースが存在しなければ0を返す.<br>
	 */
	public long lastModified() {
		URL url = getResource(resourceName);
		if (url == null) {
			return 1;
		}
		return 0;
	}
	
	public InputStream openStream() throws IOException {
		URL url = getResource(resourceName);
		if (url == null) {
			return null;
		}
		return url.openStream();
	}
	
	@Override
	public String toString() {
		return getFullName();
	}
}
