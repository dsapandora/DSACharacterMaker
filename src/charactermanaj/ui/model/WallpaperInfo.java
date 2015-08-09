package charactermanaj.ui.model;

import java.awt.Color;
import java.io.File;
import java.io.Serializable;

/**
 * 壁紙情報.<br>
 * @author seraphy
 */
public class WallpaperInfo implements Serializable, Cloneable {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = -3661482624140948074L;


	/**
	 * 壁紙のリソースタイプ
	 * @author seraphy
	 */
	public enum WallpaperResourceType {
		
		/**
		 * なし
		 */
		NONE,
		
		/**
		 * ファイル
		 */
		FILE,
		
		/**
		 * 定義済み
		 */
		PREDEFINED
	}
	
	/**
	 * 壁紙リソースのタイプ
	 */
	private WallpaperResourceType type = WallpaperResourceType.NONE;
	
	/**
	 * ファイル
	 */
	private File file;
	
	/**
	 * リソース
	 */
	private String resource;
	
	/**
	 * 壁紙のアルファ値
	 */
	private float alpha = 1.f;

	
	/**
	 * 背景色
	 */
	private Color backgroundColor = Color.WHITE;

	
	@Override
	public WallpaperInfo clone() {
		try {
			return (WallpaperInfo) super.clone();

		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	

	public WallpaperResourceType getType() {
		return type;
	}


	public void setType(WallpaperResourceType type) {
		if (type == null) {
			type = WallpaperResourceType.NONE;
		}

		this.type = type;
	}


	public File getFile() {
		return file;
	}


	public void setFile(File file) {
		this.file = file;
	}


	public String getResource() {
		return resource;
	}


	public void setResource(String resource) {
		if (resource != null) {
			resource = resource.trim();

			if (resource.length() == 0) {
				resource = null;
			}
		}

		this.resource = resource;
	}


	/**
	 * 背景画像のアルファ値を取得する.
	 * @return アルファ値
	 */
	public float getAlpha() {
		return alpha;
	}

	/**
	 * 背景画像のアルファ値を設定する.<br>
	 * 範囲は0から1の間であり、それを超えた場合は制限される.<br>
	 * @param alpha アルファ値
	 */
	public void setAlpha(float alpha) {
		if (alpha < 0) {
			alpha = 0;

		} else if (alpha > 1.f) {
			alpha = 1.f;
		}
		
		this.alpha = alpha;
	}

	/**
	 * 背景色を取得する.
	 * @return 背景色
	 */
	public Color getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * 背景色を設定する.<br>
	 * nullを指定した場合は白とみなす.<br>
	 * @param backgroundColor 背景色
	 */
	public void setBackgroundColor(Color backgroundColor) {
		if (backgroundColor == null) {
			backgroundColor = Color.WHITE;
		}
		this.backgroundColor = backgroundColor;
	}

	@Override
	public String toString() {
		return "(WallpaperInfo:(type:" + type + ")(file:" + file
				+ ")(resource:" + resource + ")(alpha:" + alpha
				+ ")(bgColor:" + backgroundColor + "))";
	}
}
