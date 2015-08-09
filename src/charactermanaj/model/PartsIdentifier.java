package charactermanaj.model;

import java.io.Serializable;

/**
 * パーツ識別子.<br>
 * パーツ識別子の同値性は同一カテゴリID、且つ、同一のパーツ名(ID)であることによってのみ判定される.<br>
 * 表示名については不問.<br>
 * カテゴリIDのみ判定され、カテゴリの同値性についても問わない.<br>
 * @author seraphy
 *
 */
public final class PartsIdentifier implements Serializable, Comparable<PartsIdentifier> {

	private static final long serialVersionUID = 8943101890389091718L;

	private final PartsCategory partsCategory;
	
	private final String partsName;
	
	private final String localizedName;
	
	public PartsIdentifier(final PartsCategory partsCategory, final String partsName, final String localizedName) {
		if (partsName == null || partsCategory == null) {
			throw new IllegalArgumentException();
		}
		this.partsCategory = partsCategory;
		this.partsName = partsName;
		this.localizedName = (localizedName == null || localizedName.trim().length() == 0) ? partsName : localizedName;
	}
	
	public PartsCategory getPartsCategory() {
		return partsCategory;
	}
	
	public boolean hasLayer(Layer layer) {
		return partsCategory.hasLayer(layer);
	}
	
	@Override
	public int hashCode() {
		return partsName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof PartsIdentifier) {
			return partsName.equals(((PartsIdentifier) obj).partsName)
					&& partsCategory.isSameCategoryID(((PartsIdentifier) obj).getPartsCategory());
		}
		return false;
	}
	
	public static boolean equals(PartsIdentifier a, PartsIdentifier b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
	
	public int compareTo(PartsIdentifier o) {
		if (o == this) {
			return 0;
		}
		int ret = partsCategory.compareTo(o.partsCategory);
		if (ret == 0) {
			ret = localizedName.compareTo(o.localizedName);
		}
		if (ret == 0) {
			ret = partsName.compareTo(o.partsName);
		}
		return ret;
	}
	
	public String getPartsName() {
		return partsName;
	}
	
	public String getLocalizedPartsName() {
		return localizedName;
	}

	/**
	 * ローカライズされた名前を変更する.<br>
	 * [注意] このクラスは不変クラスなので、インスタンスを変更するのではなく、変更された状態の
	 * 新しいインスタンスを返します.<br>
	 * @param localizedName ローカライズされた名前
	 * @return 新しいインスタンス
	 */
	public PartsIdentifier setLocalizedPartsName(String localizedName) {
		if (localizedName == null || localizedName.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		return new PartsIdentifier(partsCategory, partsName, localizedName);
	}

	@Override
	public String toString() {
		return getLocalizedPartsName();
	}
	
}
