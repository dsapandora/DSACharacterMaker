package charactermanaj.graphics.io;

import java.io.Serializable;

import charactermanaj.util.LocalizedMessageAware;

/**
 * 出力オプションモデル.
 * @author seraphy
 */
public class OutputOption implements Serializable, Cloneable {

	private static final long serialVersionUID = -879599688492364852L;

	/**
	 * 拡大縮小時に使用するレンダリングオプション.
	 * @author seraphy
	 */
	public enum ZoomRenderingType implements LocalizedMessageAware {
		NONE,
		BILINER,
		BICUBIC;
		
		public String getLocalizedResourceId() {
			return "outputOption.zoomRenderingType." + name();
		}
	}

	/**
	 * 出力する画像モード.
	 * @author seraphy
	 */
	public enum PictureMode implements LocalizedMessageAware {
		NORMAL,
		OPAQUE,
		GRAY,
		ALPHA;

		public String getLocalizedResourceId() {
			return "outputOption.pictureMode." + name();
		}
	}

	/**
	 * JPEG品質
	 */
	private double jpegQuality = 1.;
	
	/**
	 * ズームの使用可否
	 */
	private boolean enableZoom;
	
	/**
	 * 拡大率
	 */
	private double zoomFactor = 1.;
	
	/**
	 * 拡大縮小に使うアルゴリズム
	 */
	private ZoomRenderingType zoomRenderingType = ZoomRenderingType.NONE;
	
	/**
	 * 出力画像のタイプ
	 */
	private PictureMode pictureMode = PictureMode.NORMAL;
	
	/**
	 * 背景色を強制するか?
	 */
	private boolean forceBgColor;

	@Override
	public OutputOption clone() {
		try {
			return (OutputOption) super.clone();

		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public double getJpegQuality() {
		return jpegQuality;
	}

	public void setJpegQuality(double jpegQuality) {
		if (jpegQuality < 0.1) {
			jpegQuality = 0.1;
		}
		if (jpegQuality > 1) {
			jpegQuality = 1.;
		}
		this.jpegQuality = jpegQuality;
	}

	public double getZoomFactor() {
		return zoomFactor;
	}

	public void setZoomFactor(double zoomFactor) {
		if (zoomFactor < 0.1) {
			zoomFactor = 0.1;
		}
		if (zoomFactor > 10.) {
			zoomFactor = 10.;
		}
		this.zoomFactor = zoomFactor;
	}

	public ZoomRenderingType getZoomRenderingType() {
		return zoomRenderingType;
	}

	public void setZoomRenderingType(ZoomRenderingType zoomRenderingType) {
		if (zoomRenderingType == null) {
			zoomRenderingType = ZoomRenderingType.NONE;
		}
		this.zoomRenderingType = zoomRenderingType;
	}

	public PictureMode getPictureMode() {
		return pictureMode;
	}

	public void setPictureMode(PictureMode pictureMode) {
		if (pictureMode == null) {
			pictureMode = PictureMode.NORMAL;
		}
		this.pictureMode = pictureMode;
	}
	
	public boolean isForceBgColor() {
		return forceBgColor;
	}
	
	public void setForceBgColor(boolean forceBgColor) {
		this.forceBgColor = forceBgColor;
	}
	
	public void setEnableZoom(boolean enableZoom) {
		this.enableZoom = enableZoom;
	}
	
	public boolean isEnableZoom() {
		return enableZoom;
	}
	
	/**
	 * 推奨値に変更する.
	 */
	public void changeRecommend() {
		if (zoomFactor > 1.) {
			zoomRenderingType = ZoomRenderingType.BICUBIC;
		
		} else if (zoomFactor < 1.) {
			zoomRenderingType = ZoomRenderingType.BILINER;

		} else {
			zoomRenderingType = ZoomRenderingType.NONE;
		}
	}

	@Override
	public String toString() {
		return "(OutputOption:(jpegQuality:" + jpegQuality + ")(enableZoom:" + enableZoom
			+ ")(zoomFactor:" + zoomFactor + ")(renderingType:" + zoomRenderingType
			+ ")(pictureMode:" + pictureMode + ")(forceBgColor:" + forceBgColor + "))";
	}
}