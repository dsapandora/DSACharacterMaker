package charactermanaj.graphics.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;

/**
 * ファイルシステム上にある画像リソースを示す.
 * @author seraphy
 */
public class FileImageResource implements ImageResource, Serializable {

	/**
	 * シリアライズバージョン
	 */
	private static final long serialVersionUID = 5397113740824387869L;

	/**
	 * ファイル
	 */
	private File file;
	
	
	public FileImageResource(File file) {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
	}
	
	
	public long lastModified() {
		return file.lastModified();
	}
	
	public InputStream openStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}
	
	@Override
	public int hashCode() {
		return file.hashCode();
	}
	
	public int compareTo(ImageResource o) {
		return getFullName().compareTo(o.getFullName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof FileImageResource) {
			FileImageResource o = (FileImageResource) obj;
			return file.equals(o.file);
		}
		return false;
	}
	
	public String getFullName() {
		return file.getPath();
	}
	
	public URI getURI() {
		return file.toURI();
	}
	
	@Override
	public String toString() {
		return file.toString();
	}
}
