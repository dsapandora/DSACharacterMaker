package charactermanaj.model;

import java.sql.Timestamp;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * パーツ管理情報.<br>
 * パーツ識別子のかわりに、パーツキーを用いる.<br>
 * パーツキーは、CategoryIdがnullであることを許可している.<br>
 * カテゴリを省略し、パーツ名だけでパーツ管理情報を検索できるようにするためのもの.<br>
 * 
 * @author seraphy
 */
public class PartsManageData extends AbstractCollection<PartsManageData.PartsKey> {

	/**
	 * パーツキー.<br>
	 * パーツ識別子 {@link PartsIdentifier} とほぼ同等であるが、カテゴリがnullであることを許可している点が異なる.<br>
	 * 
	 * @author seraphy
	 */
	public static final class PartsKey implements Comparable<PartsKey> {
		
		private final String partsName;
		
		private final String categoryId;
		
		public PartsKey(String partsName) {
			this(partsName, null);
		}
		
		public PartsKey(String partsName, String categoryId) {
			if (partsName == null || partsName.length() == 0) {
				throw new IllegalArgumentException();
			}
			if (categoryId != null && categoryId.trim().length() == 0) {
				categoryId = null;
			}
			this.partsName = partsName;
			this.categoryId = categoryId;
		}
		
		public PartsKey(PartsIdentifier partsIdentifier) {
			if (partsIdentifier == null) {
				throw new IllegalArgumentException();
			}
			this.partsName = partsIdentifier.getPartsName();
			this.categoryId = partsIdentifier.getPartsCategory().getCategoryId();
		}
		
		public int compareTo(PartsKey o) {
			int ret = partsName.compareTo(o.partsName);
			if (ret == 0) {
				if (categoryId == null || o.categoryId == null) {
					ret = (categoryId == o.categoryId) ? 0 : (categoryId == null ? -1 : 1); 
				}
			}
			return ret;
		}
		
		public String getCategoryId() {
			return categoryId;
		}
		
		public String getPartsName() {
			return partsName;
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
			if (obj != null && obj instanceof PartsKey) {
				PartsKey o = (PartsKey) obj;
				if (partsName.equals(o.partsName)) {
					return categoryId == null ? (o.categoryId == null)
							: categoryId.equals(o.categoryId);
				}
			}
			return false;
		}
	}

	/**
	 * パーツごとのバージョンとダウンロードURL情報を保持するホルダークラス.<br>
	 * 
	 * @author seraphy
	 */
	public static final class PartsVersionInfo {
		
		private double version;
		
		private String downloadURL;
		
		private Timestamp lastModified;

		public PartsVersionInfo() {
			super();
		}
		
		public PartsVersionInfo(double version, String downloadURL) {
			this(version, downloadURL, null);
		}
		
		public PartsVersionInfo(double version, String downloadURL,
				Timestamp lastModified) {
			this.version = version;
			this.downloadURL = downloadURL;
			this.lastModified = lastModified;
		}

		public double getVersion() {
			return version;
		}
		
		public String getDownloadURL() {
			return downloadURL;
		}
		
		public void setVersion(double version) {
			this.version = version;
		}
		
		public void setDownloadURL(String downloadURL) {
			this.downloadURL = downloadURL;
		}

		public Timestamp getLastModified() {
			return lastModified;
		}

		public void setLastModified(Timestamp lastModified) {
			this.lastModified = lastModified;
		}
	}

	
	/**
	 * パーツキーと、それに対する作者情報
	 */
	private HashMap<PartsKey, PartsAuthorInfo> partsAuthorInfoMap = new HashMap<PartsKey, PartsAuthorInfo>();
	
	/**
	 * パーツキーと、それに対するローカライズ名
	 */
	private HashMap<PartsKey, String> partsLocalizedNameMap = new HashMap<PartsKey, String>();

	/**
	 * パーツキーと、それに対するバージョン情報
	 */
	private HashMap<PartsKey, PartsVersionInfo> partsVersionInfoMap = new HashMap<PartsKey, PartsVersionInfo>();


	/**
	 * すべてクリアする.<br>
	 */
	@Override
	public void clear() {
		partsAuthorInfoMap.clear();
		partsLocalizedNameMap.clear();
		partsVersionInfoMap.clear();
	}
	
	/**
	 * パーツキーに結びつく、各種情報を登録する.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @param localizedName
	 *            ローカライズ名(なければnull)
	 * @param partsAuthorInfo
	 *            作者情報 (なければnull)
	 * @param versionInfo
	 *            バージョン情報 (なければnull)
	 */
	public void putPartsInfo(PartsKey partsKey, String localizedName, PartsAuthorInfo partsAuthorInfo, PartsVersionInfo versionInfo) {
		if (partsKey == null) {
			throw new IllegalArgumentException();
		}
		partsAuthorInfoMap.put(partsKey, partsAuthorInfo);
		partsLocalizedNameMap.put(partsKey, localizedName);
		partsVersionInfoMap.put(partsKey, versionInfo);
	}
	
	/**
	 * パーツキーを指定して該当する作者情報を取得する.<br>
	 * 完全に一致する作者情報がない場合は、カテゴリを無視して、パーツキーのパーツ名(ID)の一致する、いずれかの作者情報を返す.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return 作者情報、完全に一致するものがなく、且つ、パーツ名(ID)に一致する情報もない場合はnull
	 */
	public PartsAuthorInfo getPartsAuthorInfo(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		PartsAuthorInfo authorInfo = partsAuthorInfoMap.get(partsKey);
		if (authorInfo == null) {
			for (Map.Entry<PartsKey, PartsAuthorInfo> entry : partsAuthorInfoMap.entrySet()) {
				PartsKey key = entry.getKey();
				if (key.getPartsName().equals(partsKey.getPartsName())) {
					authorInfo = entry.getValue();
					break;
				}
			}
		}
		return authorInfo;
	}

	/**
	 * パーツキーと完全に一致する作者情報を取得する.<br>
	 * 存在しない場合はnullを返す.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return 作者情報、もしくはnull
	 */
	public PartsAuthorInfo getPartsAuthorInfoStrict(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		return partsAuthorInfoMap.get(partsKey);
	}
	
	/**
	 * パーツキーを指定して該当するバージョン情報を取得する.<br>
	 * 完全に一致するバージョン情報がない場合は、カテゴリを無視して、パーツキーのパーツ名(ID)の一致する、いずれかのバージョン情報を返す.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return バージョン情報、完全に一致するものがなく、且つ、パーツ名(ID)に一致する情報もない場合はnull
	 */
	public PartsVersionInfo getVersion(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		PartsVersionInfo versionInfo = partsVersionInfoMap.get(partsKey);
		if (versionInfo == null) {
			for (Map.Entry<PartsKey, PartsVersionInfo> entry : partsVersionInfoMap.entrySet()) {
				PartsKey key = entry.getKey();
				if (key.getPartsName().equals(partsKey.getPartsName())) {
					versionInfo = entry.getValue();
					break;
				}
			}
		}
		return versionInfo;
	}
	
	/**
	 * パーツキーを指定して該当するローカライズ名を取得する.<br>
	 * 完全に一致するバージョン情報がない場合は、カテゴリを無視して、パーツキーのパーツ名(ID)の一致する、いずれかのローカライズ名を返す.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return バージョン情報、完全に一致するものがなく、且つ、パーツ名(ID)に一致する情報もない場合はnull
	 */
	public String getLocalizedName(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		String localizedName = partsLocalizedNameMap.get(partsKey);
		if (localizedName == null) {
			for (Map.Entry<PartsKey, String> entry : partsLocalizedNameMap.entrySet()) {
				PartsKey key = entry.getKey();
				if (key.getPartsName().equals(partsKey.getPartsName())) {
					localizedName = entry.getValue();
					break;
				}
			}
		}
		return localizedName;
	}

	/**
	 * パーツキーと完全に一致するバージョン情報を取得する.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return パーツキーに完全に一致するバージョン情報、該当がなければnull
	 */
	public PartsVersionInfo getVersionStrict(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		return partsVersionInfoMap.get(partsKey);
	}
	
	/**
	 * パーツキーと完全に一致するローカライズ名を取得する.<br>
	 * 
	 * @param partsKey
	 *            パーツキー
	 * @return パーツキーに完全に一致するローカライズ名、該当がなければnull
	 */
	public String getLocalizedNameStrict(PartsKey partsKey) {
		if (partsKey == null) {
			return null;
		}
		return partsLocalizedNameMap.get(partsKey);
	}

	/**
	 * すべての作者情報を返す.<br>
	 * 
	 * @return 作者情報のコレクション
	 */
	public Collection<PartsAuthorInfo> getAuthorInfos() {
		HashMap<String, PartsAuthorInfo> authorInfos = new HashMap<String, PartsAuthorInfo>();
		for (PartsAuthorInfo authorInfo : partsAuthorInfoMap.values()) {
			if (authorInfo != null) {
				String author = authorInfo.getAuthor();
				if (author != null && author.length() > 0) {
					authorInfos.put(author, authorInfo);
				}
			}
		}
		return authorInfos.values();
	}

	/**
	 * 指定した作者に該当する登録されているパーツキーの一覧を返す.<Br>
	 * 
	 * @param author
	 *            作者
	 * @return 作者に該当するパーツキー
	 */
	public Collection<PartsKey> getPartsKeysByAuthor(String author) {
		if (author == null) {
			return Collections.emptyList();
		}
		ArrayList<PartsKey> partsKeys = new ArrayList<PartsKey>();
		for (Map.Entry<PartsKey, PartsAuthorInfo> entry : partsAuthorInfoMap.entrySet()) {
			PartsKey partsKey = entry.getKey();
			PartsAuthorInfo partsAuthorInfo = entry.getValue();
			if (partsAuthorInfo != null) {
				String author2 = partsAuthorInfo.getAuthor();
				if (author2 == null) {
					author2 = "";
				}
				if (author2.equals(author)) {
					partsKeys.add(partsKey);
				}
			}
		}
		return partsKeys;
	}
	
	@Override
	public Iterator<PartsKey> iterator() {
		return partsAuthorInfoMap.keySet().iterator();
	}
	
	@Override
	public int size() {
		return partsAuthorInfoMap.size();
	}
}
