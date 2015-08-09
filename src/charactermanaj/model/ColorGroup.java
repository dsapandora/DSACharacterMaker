package charactermanaj.model;

import java.io.Serializable;

/**
 * カラーグループ.<br>
 * カラーグループはimmutableであり、構築された値は変更されることはない.<br>
 * @author seraphy
 */
public final class ColorGroup implements Serializable {

	private static final long serialVersionUID = -2127943872189828172L;

	private final String id;
	
	private final boolean enabled;
	
	private final String localizedName;

	public static final ColorGroup NA = new ColorGroup("n/a", "", false);
	
	public ColorGroup(final String id, final String localizedName) {
		this(id, localizedName, true);
	}
	
	private ColorGroup(final String id, final String localizedName, final boolean enabled) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		this.id = id.trim();
		this.localizedName = (localizedName == null || localizedName.trim().length() == 0) ? id : localizedName;
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public String getId() {
		return id;
	}
	
	public String getLocalizedName() {
		return localizedName;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ColorGroup) {
			ColorGroup o = (ColorGroup) obj;
			return id.equals(o.getId());
		}
		return false;
	}
	
	public static boolean equals(ColorGroup v1, ColorGroup v2) {
		if (v1 == v2) {
			return true;
		}
		if (v1 == null || v2 == null) {
			return false;
		}
		return v1.equals(v2);
	}
	
	@Override
	public String toString() {
		return getLocalizedName();
	}

}
