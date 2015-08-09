package charactermanaj.ui.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import charactermanaj.graphics.io.EmbeddedImageResource;
import charactermanaj.graphics.io.FileImageResource;
import charactermanaj.graphics.io.ImageLoaderImpl;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.LoadedImage;
import charactermanaj.ui.Wallpaper;
import charactermanaj.ui.model.WallpaperInfo.WallpaperResourceType;

/**
 * 壁紙情報から壁紙オブジェクトを作成するファクトリクラス.<br>
 * @author seraphy
 */
public class WallpaperFactory {
	
	/**
	 * エラーが発生した場合にハンドリングするインターフェィス.<br>
	 * ハンドラは、そのままエラーを送出するか、もしくは回復して続行させることができる.<br>
	 * @author seraphy
	 */
	public interface ErrorHandler {

		/**
		 * 指定された壁紙ファイルが、ファイルとして実在しない場合のエラー.<br>
		 * 代わりのファイルを指定するかnullを返して画像なしとして扱うか、
		 * もしくは例外を送出することができる.<br>
		 * @param wallpaperInfo 設定対象(回復時には更新可能)
		 * @param file 対象となったファイル
		 * @return 読み替えるファイル、nullの場合は画像なしとみなす.
		 * @throws WallpaperFactoryException 例外とする場合
		 */
		File missingImageFile(WallpaperInfo wallpaperInfo, File file)
				throws WallpaperFactoryException;

		/**
		 * 指定した画像リソースの読み込みに失敗した場合のエラー.<br>
		 * 代わりの画像を指定するかnullを返して画像なしとして扱うか、
		 * もしくは例外を送出することができる.<br>
		 * @param wallpaperInfo 設定対象(回復時には更新可能)
		 * @param imageResource 対象となった画像リソース
		 * @param ex 失敗事由
		 * @return 代わりの画像、もしくは画像なしとするためにnullを返すことができる
		 * @throws WallpaperFactoryException 例外とする場合
		 */
		BufferedImage imageCreationFailed(WallpaperInfo wallpaperInfo,
				ImageResource imageResource, Throwable ex)
				throws WallpaperFactoryException;

		/**
		 * その他の内部例外(RuntimeException)が発生した場合のハンドラ.<br>
		 * 代わりの壁紙オブジェクトを返すか、もしくは例外を送出することができる.<br>
		 * @param wallpaperInfo 壁紙情報
		 * @param wallpaper 構築中の壁紙オブジェクト
		 * @param ex 発生した例外
		 * @return 代わりの壁紙オブジェクト (nullは返してはならない.)
		 * @throws WallpaperFactoryException 例外とする場合
		 */
		Wallpaper internalError(WallpaperInfo wallpaperInfo,
				Wallpaper wallpaper, Throwable ex)
				throws WallpaperFactoryException;
	}
	
	/**
	 * 壁紙画像を読み取るためのイメージローダ.<br>
	 */
	private ImageLoaderImpl imageLoader = new ImageLoaderImpl();

	/**
	 * シングルトン
	 */
	private static final WallpaperFactory inst = new WallpaperFactory();

	/**
	 * エラーを例外として送出する既定のハンドラ.<br>
	 */
	public final ErrorHandler defaultErrorHandler = new WallpaperFactoryDefaultErrorHandler();
	
	private WallpaperFactory() {
		super();
	}
	
	public static WallpaperFactory getInstance() {
		return inst;
	}

	/**
	 * 壁紙情報から壁紙オブジェクトを作成して返します.<br>
	 * 壁紙情報に不備があるか何らかの問題により壁紙が作成できない場合はエラーハンドラが呼び出されます.<br>
	 * エラーハンドラは例外を送出するか、もしくは回復して続行させることができます.<br>
	 * 引数に渡される壁紙情報はエラーハンドラにより修復される可能性があります.<br>
	 * @param wallpaperInfo 壁紙情報、nullの場合はデフォルト設定が用いられる.
	 * @param errorHandler エラーハンドラ、省略した場合は{@link #DEFAULT_ERROR_HANDLER}が用いられる.
	 * @return 生成された壁紙オブジェクト
	 * @throws WallpaperFactoryException 壁紙オブジェクトの生成に失敗したことを通知する例外
	 */
	public Wallpaper createWallpaper(WallpaperInfo wallpaperInfo,
			ErrorHandler errorHandler) throws WallpaperFactoryException {
		if (wallpaperInfo == null) {
			return new Wallpaper();
		}
		if (errorHandler == null) {
			errorHandler = defaultErrorHandler;
		}
		
		Wallpaper wallpaper = new Wallpaper();

		try {
			// 背景画像の設定.
			WallpaperResourceType typ = wallpaperInfo.getType();
			ImageResource imageResource = null;
			if (typ == WallpaperResourceType.FILE) {
				// 選択ファイルから
				File imageFile = wallpaperInfo.getFile();
				if (imageFile == null || !imageFile.exists() || !imageFile.isFile()
						|| !imageFile.canRead()) {
					// ハンドラによってエラーを通知するか、もしくは回復する.
					imageFile = errorHandler.missingImageFile(wallpaperInfo,
							imageFile);
				}
				if (imageFile != null) {
					imageResource = new FileImageResource(imageFile);
				}
	
			} else if (typ == WallpaperResourceType.PREDEFINED) {
				// リソースファイルから
				String resource = wallpaperInfo.getResource();
				if (resource != null && resource.trim().length() > 0) {
					imageResource = new EmbeddedImageResource(resource);
				}
			}
			BufferedImage wallpaperImg = null;
			if (imageResource != null) {
				try {
					LoadedImage wallpaperLoadedImage = imageLoader.load(imageResource);
					wallpaperImg = wallpaperLoadedImage.getImage();
	
				} catch (IOException ex) {
					// ハンドラによってエラーを通知するか、もしくは回復する.
					wallpaperImg = errorHandler.imageCreationFailed(wallpaperInfo, imageResource, ex);
				}
			}
			wallpaper.setWallpaperImage(wallpaperImg);
	
			// アルファ値
			wallpaper.setWallpaperAlpha(wallpaperInfo.getAlpha());
			
			// 背景色
			wallpaper.setBackgroundColor(wallpaperInfo.getBackgroundColor());
		
		} catch (RuntimeException ex) {
			// ハンドラによってエラーを通知するか、もしくは回復する.
			wallpaper = errorHandler.internalError(wallpaperInfo, wallpaper, ex);
			if (wallpaper == null) {
				throw ex;
			}
		}

		return wallpaper;
	}
}

/**
 * 壁紙を構築時に問題があった場合に例外を送出するエラーハンドラ.<br>
 * @author seraphy
 */
class WallpaperFactoryDefaultErrorHandler implements WallpaperFactory.ErrorHandler {

	public BufferedImage imageCreationFailed(WallpaperInfo wallpaperInfo,
			ImageResource imageResource, Throwable ex) throws WallpaperFactoryException {
		throw new WallpaperFactoryException("image creation failed: " + imageResource, ex);
	}
	
	public Wallpaper internalError(WallpaperInfo wallpaperInfo,
			Wallpaper wallpaper, Throwable ex) throws WallpaperFactoryException {
		throw new WallpaperFactoryException("internal error: " + (ex == null ? "" : ex.getMessage()), ex);
	}
	
	public File missingImageFile(WallpaperInfo wallpaperInfo, File file)
			throws WallpaperFactoryException {
		throw new WallpaperFactoryException("missing image file: " + file);
	}
}

