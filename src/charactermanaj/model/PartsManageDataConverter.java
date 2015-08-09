package charactermanaj.model;


/**
 * パーツ設定情報から、パーツ管理情報に変換する.<br>
 * @author seraphy
 *
 */
public class PartsManageDataConverter {

	private PartsManageData partsManageData;
	
	/**
	 * パーツ管理情報は自動作成される.<br>
	 * @param partsSpecResolver
	 */
	public PartsManageDataConverter() {
		this(null);
	}

	/**
	 * 書き込み先となるパーツ管理情報を指定して構築する.<br>
	 * パーツ管理情報がnullの場合は自動作成される.<br>
	 * @param partsManageData パーツ管理情報(書き込み先)
	 */
	public PartsManageDataConverter(PartsManageData partsManageData) {
		if (partsManageData == null) {
			this.partsManageData = new PartsManageData();
		} else {
			this.partsManageData = partsManageData;
		}
	}

	/**
	 * パーツ管理情報を取得する.
	 * @return パーツ管理情報
	 */
	public PartsManageData getPartsManageData() {
		return partsManageData;
	}
	
	/**
	 * パーツ識別子とパーツ設定情報を指定して、パーツ管理情報に変換して登録する.<br>
	 * @param partsIdentifier パーツ識別子
	 * @param partsSpec パーツ設定情報(null可)
	 */
	public void convert(PartsIdentifier partsIdentifier, PartsSpec partsSpec) {
		if (partsIdentifier == null) {
			throw new IllegalArgumentException();
		}

		String localizedName = partsIdentifier.getLocalizedPartsName();
		
		double version;
		String downloadURL;
		
		if (partsSpec != null) {
			version = partsSpec.getVersion();
			downloadURL = partsSpec.getDownloadURL();
		} else {
			version = 0;
			downloadURL = null;
		}
		
		PartsAuthorInfo partsAuthorInfo;
		if (partsSpec != null) {
			partsAuthorInfo = partsSpec.getAuthorInfo();
		} else {
			partsAuthorInfo = null;
		}
		
		PartsManageData.PartsVersionInfo versionInfo = new PartsManageData.PartsVersionInfo();
		versionInfo.setDownloadURL(downloadURL);
		versionInfo.setVersion(version);

		PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(partsIdentifier);
		partsManageData.putPartsInfo(partsKey, localizedName, partsAuthorInfo, versionInfo);
	}
	
}
