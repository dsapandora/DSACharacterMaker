package charactermanaj.model;

import java.io.Serializable;

/**
 * お勧めリンク
 * @author seraphy
 */
public class RecommendationURL implements Serializable, Cloneable {

	private static final long serialVersionUID = 3568122645473390201L;

	private String displayName;
	
	private String url;
	
	@Override
	public RecommendationURL clone() {
		try {
			return (RecommendationURL) super.clone();

		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public int hashCode() {
		if (url == null) {
			return 0;
		}
		return url.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof RecommendationURL) {
			RecommendationURL o = (RecommendationURL) obj;
			return (displayName == null ? (o.displayName == null) : displayName.equals(o.displayName))
				&& (url == null ? (o.url == null) : url.equals(o.url));
		}
		return false;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return "displayName=" + displayName + "/url=" + url;
	}
}
