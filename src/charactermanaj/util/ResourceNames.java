package charactermanaj.util;

import java.util.AbstractList;
import java.util.Arrays;

/**
 * 関連もしくは類似するリソースをまとめて取り扱うためにグループ化するためのクラス.<br>
 * 
 * @author seraphy
 */
public class ResourceNames extends AbstractList<String> {
	
	private final String[] resourceNames;
	
	public ResourceNames(String[] resourceNames) {
		if (resourceNames == null) {
			throw new IllegalArgumentException();
		}
		this.resourceNames = resourceNames;
	}
	
	/**
	 * 順次を逆転させた新しいインスタンスを返す
	 * 
	 * @return 順序を逆転させたインスタンス
	 */
	public ResourceNames reverse() {
		int len = resourceNames.length;
		String[] tmp = new String[len];
		for (int idx = 0; idx < len; idx++) {
			tmp[len - idx - 1] = resourceNames[idx];
		}
		return new ResourceNames(tmp);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(resourceNames);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ResourceNames) {
			ResourceNames o = (ResourceNames) obj;
			return Arrays.equals(resourceNames, o.resourceNames);
		}
		return false;
	}
	
	@Override
	public int size() {
		return resourceNames.length;
	}
	
	@Override
	public String get(int index) {
		return resourceNames[index];
	}
}
