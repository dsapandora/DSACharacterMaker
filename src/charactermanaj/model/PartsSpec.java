package charactermanaj.model;

import java.io.Serializable;

/**
 * パーツの構成情報.<br>
 * @author seraphy
 */
public class PartsSpec implements Serializable {

	private static final long serialVersionUID = -5275967668710789314L;

	private PartsIdentifier partsIdentifier;
	
	private ColorGroup colorGroup = ColorGroup.NA;
	
	private PartsFiles partsFiles;
	
	/**
	 * パーツの作者情報、指定がなければnull
	 */
	private PartsAuthorInfo authorInfo;
	
	/**
	 * パーツのバージョン、指定がなければ0
	 */
	private double version;
	
	/**
	 * ダウンロードURL
	 */
	private String downloadURL;

	
	public PartsSpec(PartsIdentifier partsIdentifier) {
		if (partsIdentifier == null) {
			throw new IllegalArgumentException();
		}
		this.partsIdentifier = partsIdentifier;
		this.partsFiles = new PartsFiles(partsIdentifier);
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}
	
	public PartsFiles getPartsFiles() {
		return partsFiles;
	}

	public void setAuthorInfo(PartsAuthorInfo authorInfo) {
		this.authorInfo = authorInfo;
	}
	
	public PartsAuthorInfo getAuthorInfo() {
		return authorInfo;
	}
	
	public String getAuthor() {
		if (authorInfo != null) {
			return authorInfo.getAuthor();
		}
		return null;
	}
	
	public void setVersion(double version) {
		this.version = version;
	}
	
	public double getVersion() {
		return version;
	}
	
	public String getDownloadURL() {
		return downloadURL;
	}
	
	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}
	
	public void setColorGroup(ColorGroup colorGroup) {
		if (colorGroup == null) {
			colorGroup = ColorGroup.NA;
		}
		this.colorGroup = colorGroup;
	}
	
	public ColorGroup getColorGroup() {
		return colorGroup;
	}
	
}
