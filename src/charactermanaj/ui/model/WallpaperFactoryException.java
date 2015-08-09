package charactermanaj.ui.model;

/**
 * 壁紙の構築エラー.<br>
 * @author seraphy
 */
public class WallpaperFactoryException extends Exception {
	
	private static final long serialVersionUID = 6160297739997949904L;

	public WallpaperFactoryException() {
		super();
	}
	
	public WallpaperFactoryException(String message) {
		super(message);
	}
	
	public WallpaperFactoryException(String message, Throwable cause) {
		super(message, cause);
	}

}
