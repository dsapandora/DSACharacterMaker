package charactermanaj.model;

import java.io.Serializable;

import charactermanaj.graphics.filters.ColorConvertParameter;

/**
 * カラーグループおよび連動フラグを含む色情報.<br> 
 * @author seraphy
 */
public class ColorInfo implements Serializable, Cloneable {

	private static final long serialVersionUID = 2448550538711608223L;

	private ColorConvertParameter colorParameter = new ColorConvertParameter();
	
	private boolean syncColorGroup = false;
	
	private ColorGroup colorGroup = ColorGroup.NA;
	
	@Override
	public ColorInfo clone() {
		ColorInfo colorInfo;
		try {
			colorInfo = (ColorInfo) super.clone();

		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
		colorInfo.colorParameter = (ColorConvertParameter) this.colorParameter.clone();
		return colorInfo;
	}
	
	@Override
	public int hashCode() {
		return colorParameter.hashCode() ^ colorGroup.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ColorInfo) {
			ColorInfo o = (ColorInfo) obj;
			return colorGroup.equals(o.colorGroup)
					&& syncColorGroup == o.syncColorGroup
					&& colorParameter.equals(o.colorParameter);
		}
		return false;
	}

	public ColorConvertParameter getColorParameter() {
		return colorParameter;
	}

	public void setColorParameter(ColorConvertParameter colorParameter) {
		if (colorParameter == null) {
			this.colorParameter = new ColorConvertParameter();
		} else {
			this.colorParameter = colorParameter;
		}
	}

	public boolean isSyncColorGroup() {
		return syncColorGroup;
	}

	public void setSyncColorGroup(boolean syncColorGroup) {
		this.syncColorGroup = syncColorGroup;
	}

	public ColorGroup getColorGroup() {
		return colorGroup;
	}

	public void setColorGroup(ColorGroup colorGroup) {
		if (colorGroup == null) {
			this.colorGroup = ColorGroup.NA;
		} else {
			this.colorGroup = colorGroup;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)));
		buf.append("(");
		buf.append("(colorGroup: " + colorGroup + "(sync: " + syncColorGroup + ")), ");
		buf.append("(colorParameter: " + colorParameter + ")");
		buf.append(")");
		return buf.toString();
	}
}
