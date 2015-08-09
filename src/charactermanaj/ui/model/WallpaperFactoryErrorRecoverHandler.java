package charactermanaj.ui.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.graphics.io.ImageResource;
import charactermanaj.ui.Wallpaper;
import charactermanaj.ui.model.WallpaperInfo.WallpaperResourceType;

/**
 * 壁紙オブジェクトの構築に回復しながら継続するためのハンドラ.<br> 
 * @author seraphy
 */
public class WallpaperFactoryErrorRecoverHandler implements WallpaperFactory.ErrorHandler {
	
	protected static final Logger logger = Logger.getLogger(WallpaperFactoryErrorRecoverHandler.class.getName());

	private boolean errorOccured = false;
	
	private boolean recovered = false;
	
	/**
	 * 何らかのエラーが発生し回復できなかった場合
	 * @return
	 */
	public boolean isErrorOccured() {
		return errorOccured;
	}
	
	/**
	 * 何らかのエラーが発生したが回復された場合
	 * @return
	 */
	public boolean isRecovered() {
		return recovered;
	}
	
	public void setErrorOccured(boolean errorOccured) {
		this.errorOccured = errorOccured;
	}
	
	public void setRecovered(boolean recovered) {
		this.recovered = recovered;
	}

	public File missingImageFile(WallpaperInfo wallpaperInfo, File file)
			throws WallpaperFactoryException {
		if (file == null) {
			logger.log(Level.FINE, "壁紙ファイルの指定がありません.");
		} else {
			logger.log(Level.WARNING, "壁紙ファイルが存在しないか読み込みできません:" + file);
		}
		
		// ファイルは、もとより指定されていなかったものとして回復する.
		wallpaperInfo.setType(WallpaperResourceType.NONE);
		setRecovered(true);
		return null;
	}

	public BufferedImage imageCreationFailed(WallpaperInfo wallpaperInfo,
			ImageResource imageResource, Throwable ex)
			throws WallpaperFactoryException {
		logger.log(Level.WARNING, "壁紙ファイルの読み込みに失敗しました。:" + imageResource, ex);

		// ファイルは、もとより指定されていなかったものとして回復する.
		wallpaperInfo.setType(WallpaperResourceType.NONE);
		setRecovered(true);
		return null;
	}
	
	public Wallpaper internalError(WallpaperInfo wallpaperInfo,
			Wallpaper wallpaper, Throwable ex) throws WallpaperFactoryException {
		logger.log(Level.WARNING, "壁紙の構築に失敗しました。" + wallpaperInfo, ex);
		setErrorOccured(true);
		throw new WallpaperFactoryException("internal error: " + ex, ex);
	}
	
}
