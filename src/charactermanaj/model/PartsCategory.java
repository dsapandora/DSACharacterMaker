package charactermanaj.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * パーツカテゴリ.<br> 
 * 同値であるかはカテゴリIDが一致するかによってのみ判定します.<br>
 * それ以外の情報は無視されます.<br>
 * @author seraphy
 */
public final class PartsCategory implements Comparable<PartsCategory>, Serializable {

	/**
	 * シリアライズバージョンID.
	 */
	private static final long serialVersionUID = -8652242530280056201L;

	/**
	 * 順序
	 */
	private final int order;
	
	/**
	 * カテゴリ識別名
	 */
	private final String categoryId;
	
	/**
	 * カテゴリ表示名
	 */
	private final String localizedCategoryName;

	/**
	 * 複数選択可能?
	 */
	private final boolean multipleSelectable;
	
	/**
	 * 表示行数
	 */
	private final int visibleRows;
	
	/**
	 * レイヤー情報
	 */
	private final List<Layer> layers;
	
	/**
	 * カテゴリを構築する.<br>
	 * @param order 順序
	 * @param categoryId カテゴリ識別名
	 * @param localizedCategoryName カテゴリ表示名
	 * @param multipleSelectable 複数選択可能?
	 * @param visibleRows 表示行数
	 * @param layers レイヤー情報の配列、nullの場合は空とみなす
	 */
	public PartsCategory(final int order, final String categoryId, String localizedCategoryName,
			boolean multipleSelectable, int visibleRows, Layer[] layers) {
		if (categoryId == null || categoryId.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		if (layers == null) {
			layers = new Layer[0];
		}
		if (localizedCategoryName == null || localizedCategoryName.trim().length() == 0) {
			localizedCategoryName = categoryId;
		}
		this.order = order;
		this.categoryId = categoryId.trim();
		this.localizedCategoryName = localizedCategoryName.trim();
		this.multipleSelectable = multipleSelectable;
		this.layers = Collections.unmodifiableList(Arrays.asList(layers.clone()));
		this.visibleRows = visibleRows;
	}
	
	/**
	 * カテゴリの順序を比較して返す.<br>
	 * 順序で比較し、同一順序であれば表示名で比較し、それでも同一であれば識別子で比較します.<br>
	 * @param o 比較対象.
	 * @return 順序
	 */
	public int compareTo(PartsCategory o) {
		if (o == this) {
			return 0;
		}
		int ret = order - o.order;
		if (ret == 0) {
			ret = localizedCategoryName.compareTo(o.localizedCategoryName);
		}
		if (ret == 0) {
			ret = categoryId.compareTo(o.categoryId);
		}
		return ret;
	}
	
	@Override
	public int hashCode() {
		return this.categoryId.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof PartsCategory) {
			// IDが等しいか?
			PartsCategory o = (PartsCategory) obj;
			if (categoryId.equals(o.getCategoryId())) {
				// それ以外の情報も等しいか?
				// (用法的に、異なるインスタンスで同じIDをもつことは希であり、
				// その上、IDが同一で、それ以外の内容が一致しないことは更に希である。)
				if (order == o.order
						&& localizedCategoryName
								.equals(o.localizedCategoryName)
						&& multipleSelectable == o.multipleSelectable
						&& visibleRows == o.visibleRows
						&& layers.equals(o.layers)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 同一カテゴリであるか判定します.<br>
	 * nullの場合は常にfalseを返します.<br>
	 * @param obj パーツカテゴリ、またはnull
	 * @return 同一のパーツカテゴリIDであればtrue、そうでなければfalse
	 */
	public boolean isSameCategoryID(PartsCategory obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null) {
			return categoryId.equals(obj.categoryId);
		}
		return false;
	}
	
	public static boolean equals(PartsCategory o1, PartsCategory o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		return o1.equals(o2);
	}
	
	/**
	 * 定義順を取得する
	 * @return 定義順
	 */
	public int getOrder() {
		return order;
	}

	/**
	 * 複数選択可能であるか?
	 * @return 複数選択可能であるか?
	 */
	public boolean isMultipleSelectable() {
		return multipleSelectable;
	}

	/**
	 * 表示行数を取得する.
	 * @return 表示行数
	 */
	public int getVisibleRows() {
		return visibleRows;
	}

	/**
	 * このカテゴリに指定したレイヤーが含まれるか検証する.
	 * @param layer レイヤー
	 * @return 含まれる場合はtrue、含まれない場合はfalse
	 */
	public boolean hasLayer(Layer layer) {
		if (layer == null) {
			return false;
		}
		for (Layer memberLayer : layers) {
			if (Layer.equals(memberLayer, layer)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * レイヤー情報
	 * @return レイヤー情報
	 */
	public List<Layer> getLayers() {
		return layers;
	}
	
	/**
	 * レイヤーを取得する.<br>
	 * 該当するレイヤーがなければnull
	 * @param layerId レイヤー名
	 * @return レイヤーもしくはnull
	 */
	public Layer getLayer(String layerId) {
		if (layerId == null) {
			return null;
		}
		for (Layer layer : layers) {
			if (layer.getId().equals(layerId)) {
				return layer;
			}
		}
		return null;
	}
	
	/**
	 * カテゴリ識別名を取得する.
	 * @return カテゴリ識別名
	 */
	public String getCategoryId() {
		return categoryId;
	}
	
	/**
	 * カテゴリ表示名を取得する.
	 * @return カテゴリ表示名
	 */
	public String getLocalizedCategoryName() {
		return this.localizedCategoryName;
	}

	@Override
	public String toString() {
		return getLocalizedCategoryName();
	}
}
