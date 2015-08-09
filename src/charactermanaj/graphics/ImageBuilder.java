package charactermanaj.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import charactermanaj.graphics.colormodel.ColorModel;
import charactermanaj.graphics.colormodel.ColorModels;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.graphics.io.LoadedImage;
import charactermanaj.model.AppConfig;
import charactermanaj.model.Layer;

/**
 * 各パーツの各レイヤーごとの画像を色変換したのちレイヤーの順序に従い重ね合わせ合成する。
 * 
 * @author seraphy
 */
public class ImageBuilder {

	/**
	 * 各パーツ情報の読み取りタイムアウト
	 */
	private static final int MAX_TIMEOUT = 20; // Secs

	/**
	 * 各パーツ情報を設定するためのインターフェイス.<br>
	 * パーツ登録が完了したら、{@link #setComplite()}を呼び出す必要がある.<br>
	 * 
	 * @author seraphy
	 * 
	 */
	public interface ImageSourceCollector {
		
		/**
		 * 画像サイズを設定する.<br>
		 * 
		 * @param size
		 *            サイズ
		 */
		void setSize(Dimension size);
		
		/**
		 * 画像の背景色を設定する.<br>
		 * 画像生成処理そのものは背景色を使用しないが、画像の生成完了と同じタイミングで背景色を変えるために ホルダーとして用いることを想定している.<br>
		 * 
		 * @param color
		 */
		void setImageBgColor(Color color);
		
		/**
		 * アフィン変換処理のためのパラメータを指定する.<br>
		 * 配列サイズは4または6でなければならない.<br>
		 * 
		 * @param param
		 *            パラメータ、変換しない場合はnull
		 */
		void setAffineTramsform(double[] param);

		/**
		 * 各パーツを登録する.<br>
		 * 複数パーツある場合は、これを繰り返し呼び出す.<br>
		 * すべて呼び出したらsetCompliteを呼び出す.<br>
		 * 
		 * @param layer
		 *            レイヤー
		 * @param imageResource
		 *            イメージソース
		 * @param param
		 *            色変換情報
		 */
		void setImageSource(Layer layer, ImageResource imageResource, ColorConvertParameter param);

		/**
		 * パーツの登録が完了したことを通知する。
		 */
		void setComplite();
	}

	/**
	 * 合成が完了した画像を通知するインターフェイス
	 * 
	 * @author seraphy
	 */
	public interface ImageOutput {
		
		/**
		 * 画像の背景色を取得する.
		 * 
		 * @return 背景色
		 */
		Color getImageBgColor();
		
		/**
		 * 　画像を取得する.
		 * 
		 * @return 画像
		 */
		BufferedImage getImageOutput();
		
	}

	/**
	 * イメージを構築するためのジョブ定義.<br>
	 * イメージを構築するためのパーツを登録するハンドラと、合成されたイメージを取り出すハンドラ、および エラーハンドラからなる.<br>
	 * 
	 * @author seraphy
	 */
	public interface ImageBuildJob {

		/**
		 * 合成する、各パーツを登録するためのハンドラ.<br>
		 * 
		 * @param collector
		 *            登録するためのインターフェイス
		 */
		void loadParts(ImageSourceCollector collector) throws IOException;
		
		/**
		 * 合成されたイメージを取得するハンドラ
		 * 
		 * @param output
		 *            イメージを取得するためのインターフェイス
		 */
		void buildImage(ImageOutput output);

		/**
		 * 例外ハンドラ
		 * 
		 * @param ex
		 *            例外
		 */
		void handleException(Throwable ex);
	}
	
	/**
	 * イメージ構築に使用したパーツ情報
	 * 
	 * @author seraphy
	 */
	private static final class BuildedPartsInfo {
		
		private final ImageBuildPartsInfo partsInfo;
		
		private final long lastModified;
		
		public BuildedPartsInfo(ImageBuildPartsInfo partsInfo, LoadedImage loadedImage) {
			this.partsInfo = partsInfo;
			this.lastModified = loadedImage.getLastModified();
		}
		
		public ImageBuildPartsInfo getPartsInfo() {
			return partsInfo;
		}
		
		public long getLastModified() {
			return lastModified;
		}
	}
	
	/**
	 * イメージ構築用情報.<br>
	 * イメージ構築結果も格納される.<br>
	 * 
	 * @author seraphy
	 */
	private static final class ImageBuildInfo {
		
		private ArrayList<ImageBuildPartsInfo> partsInfos = new ArrayList<ImageBuildPartsInfo>();
		
		private ArrayList<BuildedPartsInfo> buildPartsInfos = new ArrayList<BuildedPartsInfo>(); 
		
		private BufferedImage canvas;

		private Rectangle rct = new Rectangle(0, 0, 0, 0);
		
		private Color imageBgColor;
		
		private double[] affineParamHolder;
		
		private boolean sorted;
		
		@Override
		public int hashCode() {
			return partsInfos.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof ImageBuildInfo) {
				ImageBuildInfo other = (ImageBuildInfo) obj;
				
				if (!getPartsInfos().equals(other.getPartsInfos())) {
					// パーツ情報を重ね順をあわせて比較している.
					return false;
				}
				if (!rct.equals(other.rct)) {
					return false;
				}
				if (!(imageBgColor == null ? other.imageBgColor == null
						: imageBgColor.equals(other.imageBgColor))) {
					return false;
				}
				if (!(affineParamHolder == null ? other.affineParamHolder == null
						: Arrays.equals(affineParamHolder, other.affineParamHolder))) {
					return false;
				}
				return true;
			}
			return false;
		}
		
		/**
		 * このイメージ構築情報と、すでに構築したイメージ情報を比較し、 同一であるか判定する.<br>
		 * 引数に指定したイメージ構築情報が、まだ構築されていない場合は常にfalseとなる.<br>
		 * イメージリソースが更新されていれば同一構成であってもfalseとなる.<br>
		 * 
		 * @param usedInfo
		 *            すでに構築済みのイメージ情報(結果が入っているもの)
		 * @return 同一であればtrue、そうでなければfalse
		 */
		public boolean isAlreadyLoaded(ImageBuildInfo usedInfo) {
			if (usedInfo == null || usedInfo.getCanvas() == null) {
				return false;
			}
			if ( !usedInfo.equals(this)) {
				// 構成が違うのでfalse
				return false;
			}

			// 要求されているパーツ情報と、読み込み済みのパーツ情報が同一であるか判定する.
			int mx = partsInfos.size();
			int mxUsed = usedInfo.buildPartsInfos.size();
			if (mx != mxUsed) {
				return false; // 念のため
			}
			for (int idx = 0; idx < mx; idx++) {
				ImageBuildPartsInfo partsInfo = partsInfos.get(idx);
				BuildedPartsInfo buildedPartsInfo = usedInfo.buildPartsInfos.get(idx);
				if ( !partsInfo.equals(buildedPartsInfo.getPartsInfo())) {
					// パーツ構成が一致しない.(念のため)
					return false;
				}
				long lastModified = partsInfo.getFile().lastModified();
				if (lastModified != buildedPartsInfo.getLastModified()) {
					// 画像ファイルが更新されている.
					return false;
				}
			}
			
			return true;
		}
		
		/**
		 * イメージ構築に使用したパーツ情報を記録する.
		 * 
		 * @param partsInfo
		 *            パーツ情報
		 * @param loadedImage
		 *            イメージ
		 */
		public void addUsedPartsInfo(ImageBuildPartsInfo partsInfo, LoadedImage loadedImage) {
			buildPartsInfos.add(new BuildedPartsInfo(partsInfo, loadedImage));
		}
		
		/**
		 * イメージ構築結果を取得する.
		 * 
		 * @return イメージ構築結果
		 */
		public BufferedImage getCanvas() {
			return canvas;
		}
		
		/**
		 * イメージ構築結果を格納する.
		 * 
		 * @param canvas
		 *            イメージ構築結果
		 */
		public void setCanvas(BufferedImage canvas) {
			this.canvas = canvas;
		}
		
		public double[] getAffineParamHolder() {
			return affineParamHolder;
		}
		
		public void setAffineParamHolder(double[] affineParamHolder) {
			this.affineParamHolder = affineParamHolder;
		}
		
		public Color getImageBgColor() {
			return imageBgColor;
		}
		
		public void setImageBgColor(Color imageBgColor) {
			this.imageBgColor = imageBgColor;
		}
		
		public Rectangle getRct() {
			return rct;
		}
		
		public void setRect(int w, int h) {
			rct.width = w;
			rct.height = h;
		}
		
		/**
		 * パーツのリストを取得する.<Br>
		 * 取得された時点で、パーツ情報は重ね合わせ順にソートされている.<br>
		 * リストは変更不可です.<br>
		 * 
		 * @return パーツ情報のリスト
		 */
		public List<ImageBuildPartsInfo> getPartsInfos() {
			if ( !sorted) {
				Collections.sort(partsInfos);
				sorted = true;
			}
			return Collections.unmodifiableList(partsInfos);
		}
		
		public void add(ImageBuildPartsInfo imageBuildPartsInfo) {
			sorted = false;
			partsInfos.add(imageBuildPartsInfo);
		}
		
		public int getPartsCount() {
			return partsInfos.size();
		}
	}
	

	/**
	 * イメージのローダー
	 */
	private ColorConvertedImageCachedLoader imageLoader;
	
	/**
	 * 最後に使用したイメージビルド情報.(初回ならばnull)
	 */
	private ImageBuildInfo lastUsedImageBuildInfo;
	
	/**
	 * イメージのローダーを指定して構築します.<br>
	 * 
	 * @param imageLoader
	 *            イメージローダー
	 */
	public ImageBuilder(ColorConvertedImageCachedLoader imageLoader) {
		if (imageLoader == null) {
			throw new IllegalArgumentException();
		}
		this.imageLoader = imageLoader;
	}

	
	/**
	 * イメージビルドジョブより、構築すべきイメージの情報を取得する.
	 * 
	 * @param imageBuildJob
	 *            イメージビルドジョブ
	 * @return 取得されたイメージ構築情報
	 * @throws IOException
	 *             失敗
	 * @throws InterruptedException
	 *             割り込み
	 */
	protected ImageBuildInfo getPartsInfo(ImageBuildJob imageBuildJob) throws IOException, InterruptedException {
		final ImageBuildInfo imageBuildInfo = new ImageBuildInfo();
		final Semaphore compliteLock = new Semaphore(0);
		// ジョブリクエスト側に合成するイメージの情報を引き渡すように要求する.
		// loadPartsが非同期に行われる場合、すぐに制御を戻す.
		imageBuildJob.loadParts(new ImageSourceCollector() {
			// ジョブリクエスト側よりイメージサイズの設定として呼び出される
			public void setSize(Dimension size) {
				synchronized (imageBuildInfo) {
					imageBuildInfo.setRect(size.width, size.height);
				}
			}
			public void setImageBgColor(Color color) {
				synchronized (imageBuildInfo) {
					imageBuildInfo.setImageBgColor(color);
				}
			}
			public void setAffineTramsform(double[] param) {
				if (param != null && !(param.length == 4 || param.length == 6)) {
					throw new IllegalArgumentException("affineTransformParameter invalid length.");
				}
				synchronized (imageBuildInfo) {
					imageBuildInfo.setAffineParamHolder(param);
				}
			}
			// ジョブリクエスト側よりパーツの登録として呼び出される
			public void setImageSource(Layer layer, ImageResource imageResource, ColorConvertParameter param) {
				synchronized (imageBuildInfo) {
					imageBuildInfo.add(new ImageBuildPartsInfo(
							imageBuildInfo.getPartsCount(), layer, imageResource, param));
				}
			}
			// ジョブリクエスト側よりイメージサイズとパーツの登録が完了したことを通知される.
			public void setComplite() {
				compliteLock.release();
			}
		});

		// ImageCollectorは非同期に呼び出されても良いことを想定している.
		// MAX_TIMEOUTを経過してもsetCompliteが呼び出されない場合、処理を放棄する.
		if (!compliteLock.tryAcquire(MAX_TIMEOUT, TimeUnit.SECONDS)) {
			throw new RuntimeException("ImageCollector Timeout.");
		}
		return imageBuildInfo;
	}
	
	/**
	 * イメージビルド情報をもとにイメージを構築して返す.
	 * 
	 * @param imageBuildInfo
	 *            イメージビルド情報と、その結果
	 * @throws IOException
	 *             失敗
	 */
	protected void buildImage(ImageBuildInfo imageBuildInfo) throws IOException {

		// 出力画像のカンバスを作成
		int w = imageBuildInfo.getRct().width;
		int h = imageBuildInfo.getRct().height;

		final BufferedImage canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) canvas.getGraphics();
		try {
			// レンダリングヒント
			AppConfig appConfig = AppConfig.getInstance();
			if (appConfig.isEnableRenderingHints()) {
				g.setRenderingHint(
						RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
				g.setRenderingHint(
						RenderingHints.KEY_COLOR_RENDERING,
						RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				g.setRenderingHint(
						RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
			}

			// 各パーツを重ね合わせ順にカンバスに描画する
			imageLoader.unlockImages();
			for (ImageBuildPartsInfo partsInfo : imageBuildInfo.getPartsInfos()) {
				ImageResource imageFile = partsInfo.getFile();
				ColorConvertParameter colorConvParam = partsInfo.getColorParam();
				// カラーモデル
				Layer layer = partsInfo.getLayer();
				String colorModelName = layer.getColorModelName();
				ColorModel colorModel = ColorModels.safeValueOf(colorModelName);

				LoadedImage loadedImage = imageLoader.load(imageFile,
						colorConvParam, colorModel);

				// イメージ構築に使用した各パーツの結果を格納する.
				imageBuildInfo.addUsedPartsInfo(partsInfo, loadedImage);

				// イメージをキャンバスに重ねる.
				BufferedImage img = loadedImage.getImage();
				g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);
			}
		
		} finally {
			g.dispose();
		}
	
		// アフィン処理を行う.(パラメータが指定されていれば)
		final BufferedImage affineTransformedCanvas;
		double[] affineTransformParameter = imageBuildInfo.getAffineParamHolder();
		if (affineTransformParameter == null || affineTransformParameter.length != 6) {
			affineTransformedCanvas = canvas;

		} else {
			AffineTransform affineTransform = new AffineTransform(affineTransformParameter);
			AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
			affineTransformedCanvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			affineTransformOp.filter(canvas, affineTransformedCanvas);
		}

		// 最終的にできあがったキャンバスを結果として格納する.
		imageBuildInfo.setCanvas(affineTransformedCanvas);
	}
	
	/**
	 * イメージ構築ジョブを要求します.<br>
	 * 戻り値がtrueである場合は、ただちに完了したことを示します.<br>
	 * 
	 * @param imageBuildJob
	 *            イメージを構築するジョブ
	 * @return 画像がただちに得られた場合はtrue、そうでなければfalse
	 */
	public boolean requestJob(final ImageBuildJob imageBuildJob) {
		if (imageBuildJob == null) {
			throw new IllegalArgumentException();
		}
		
		// 合成する画像パーツの取得処理
		final ImageBuildInfo imageBuildInfo;
		try {
			imageBuildInfo = getPartsInfo(imageBuildJob);

		} catch (Throwable ex) {
			// 予期せぬ例外の通知
			imageBuildJob.handleException(ex);
			return false;
		}
		
		try {
			synchronized (imageBuildInfo) {

				final BufferedImage canvas;
				
				// 前回構築したパーツと同じであれば再構築せず、以前のものを使う
				if (imageBuildInfo.isAlreadyLoaded(lastUsedImageBuildInfo)) {
					canvas = lastUsedImageBuildInfo.getCanvas();

				} else {
					// パーツの合成処理
					buildImage(imageBuildInfo);
					canvas = imageBuildInfo.getCanvas();
					lastUsedImageBuildInfo = imageBuildInfo;
				}
				
				// 完成したカンバスを合成結果として通知する.
				imageBuildJob.buildImage(new ImageOutput() {
					public BufferedImage getImageOutput() {
						return canvas;
					}
					public Color getImageBgColor() {
						return imageBuildInfo.getImageBgColor();
					}
				});
			}

		} catch (Throwable ex) {
			// 予期せぬ例外の通知
			imageBuildJob.handleException(ex);
			return false;
		}

		// 完了
		return true;
	}
}

/**
 * 合成する個々のイメージ情報 .<br>
 * レイヤー順に順序づけられており、同一レイヤーであればOrder順に順序づけられます.<br>
 * 
 * @author seraphy
 */
final class ImageBuildPartsInfo implements Comparable<ImageBuildPartsInfo> {

	private int order;
	
	private Layer layer;
	
	private ImageResource imageResource;
	
	private ColorConvertParameter colorParam;

	public ImageBuildPartsInfo(int order, Layer layer, ImageResource imageResource, ColorConvertParameter colorParam) {
		this.order = order;
		this.layer = layer;
		this.imageResource = imageResource;
		this.colorParam = colorParam;
	}
	
	@Override
	public int hashCode() {
		return order ^ layer.hashCode() ^ imageResource.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ImageBuildPartsInfo) {
			ImageBuildPartsInfo o = (ImageBuildPartsInfo) obj;
			return order == o.order && layer.equals(o.layer)
					&& imageResource.equals(o.imageResource) && colorParam.equals(o.colorParam);
		}
		return false;
	}
	
	public int compareTo(ImageBuildPartsInfo o) {
		// レイヤー順
		int ret = layer.compareTo(o.layer);
		if (ret == 0) {
			// 同一レイヤーであれば定義順
			ret = order - o.order;
		}
		if (ret == 0) {
			// それでも同じであればイメージソースの固有の順序
			ret = imageResource.compareTo(o.imageResource);
		}
		return ret;
	}

	public int getOrder() {
		return order;
	}
	
	public Layer getLayer() {
		return layer;
	}
	
	public ColorConvertParameter getColorParam() {
		return colorParam;
	}
	
	public ImageResource getFile() {
		return imageResource;
	}
}
