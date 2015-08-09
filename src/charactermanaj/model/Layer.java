package charactermanaj.model;

import java.io.File;
import java.io.Serializable;

/**
 * レイヤー情報.<br>
 * 重ね合わせ順による比較が可能.<br> 
 * immutableなクラスであり、構築された後に変更されることはない.<br>
 * 同値であるかは、ID、順序、Dirのみで判断され、それ以外の情報は無視される.<br>
 * @author seraphy
 */
public final class Layer implements Comparable<Layer>, Serializable {

	private static final long serialVersionUID = -6437516046486547811L;

	/**
	 * 重ね合わせ順
	 */
	private final int order;
	
	/**
	 * レイヤー識別名
	 */
	private final String id;
	
	/**
	 * レイヤー表示名
	 */
	private final String localizedName;
	
	/**
	 * カラーグループ
	 */
	private final ColorGroup colorGroup;
	
	/**
	 * カラーグループ同期(初期)
	 */
	private final boolean initSync;
	
	/**
	 * 対象ディレクトリ
	 */
	private final String dir;
	
	/**
	 * カラーモデル名
	 */
	private final String colorModelName;

	/**
	 * レイヤー情報を構築する
	 * 
	 * @param id
	 * @param localizedName
	 * @param order
	 * @param colorGroup
	 * @param initSync
	 * @param dir
	 * @param colorModelName
	 */
	public Layer(String id, String localizedName, int order,
			ColorGroup colorGroup, boolean initSync, String dir,
			String colorModelName) {
		if (id == null || id.length() == 0 || order < 0 || dir == null) {
			throw new IllegalArgumentException();
		}
		if (localizedName == null || localizedName.length() == 0) {
			localizedName = id;
		}
		if (colorGroup == null) {
			colorGroup = ColorGroup.NA;
		}
		if (colorModelName == null || colorModelName.trim().length() == 0) {
			colorModelName = null;
		}
		this.id = id;
		this.localizedName = localizedName;
		this.order = order;
		this.colorGroup = colorGroup;
		this.initSync = initSync;
		this.dir = dir;
		this.colorModelName = colorModelName;
	}
	
	/**
	 * 重ね合わせ順に比較する.
	 * 同順位の場合は名前・ディレクトリから順序を決める.
	 */
	public int compareTo(Layer o) {
		int ret = order - o.order;
		if (ret == 0) {
			ret = id.compareTo(o.id);
		}
		if (ret == 0) {
			File d1 = new File(dir);
			File d2 = new File(o.dir);
			ret = d1.compareTo(d2);
		}
		return ret;
	}

	/**
	 * レイヤー識別名を取得する
	 * @return レイヤー識別名
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * レイヤー表示名を取得する
	 * @return レイヤー表示名
	 */
	public String getLocalizedName() {
		return localizedName;
	}
	
	/**
	 * 重ね合わせ順を取得する.
	 * @return 重ね合わせ順
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * カラーグループを取得する.
	 * @return カラーグループ
	 */
	public ColorGroup getColorGroup() {
		return colorGroup;
	}

	/**
	 * カラーグループの同期フラグ(初期)を取得する.
	 * @return 同期フラグ
	 */
	public boolean isInitSync() {
		return initSync;
	}
	
	/**
	 * 対象ディレクトリを取得する.
	 * @return 対象ディレクトリ
	 */
	public String getDir() {
		return dir;
	}
	
	/**
	 * カラーモデル名を取得する.
	 * 
	 * @return カラーモデル名
	 */
	public String getColorModelName() {
		return colorModelName;
	}

	/**
	 * 同一レイヤーであるか判断する.<br>
	 * ID、順序、Dirで判断する.<br>
	 * (カラーグループ、カラーグループ同期、表示名、は無視される.)<br>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof Layer) {
			Layer o = (Layer) obj;
			File d1 = new File(dir);
			File d2 = new File(o.dir);
			return id.equals(o.id) && order == o.order && d1.equals(d2);
		}
		return false;
	}
	
	/**
	 * 同一レイヤーであるか判断する.<br>
	 * IDのみで判断する.<br>
	 * (カラーグループ、カラーグループ同期、表示名、順序、Dirは無視される.)<br>
	 * @param a 比較1
	 * @param b 比較2
	 * @return 等しければtrue、そうでなければfalse
	 */
	public static boolean equals(Layer a, Layer b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Layer(id=" + id + ", name=" + localizedName + ", order=" + order + ")";
	}
}
