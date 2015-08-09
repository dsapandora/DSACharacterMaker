package charactermanaj.model.io;

import java.util.HashMap;
import java.util.Map;

import charactermanaj.model.PartsAuthorInfo;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.PartsSpec;


/**
 * パーツ管理情報をパーツデータに補填するデコレータ・ローダー.<br>
 * ローダから読み込まれたパーツデータに対して、それに該当する管理情報があれば、その管理情報で更新し、
 * 更新した結果を返す.<br>
 * @author seraphy
 */
public class PartsManageDataDecorateLoader implements PartsDataLoader {

	/**
	 * パーツ管理情報を構築するファクトリ.<br>
	 * ローダーが呼び出された段階で、最新のパーツ管理情報をロードするためのもの.<br>
	 * @author seraphy
	 *
	 */
	public interface PartsManageDataFactory {
		
		/**
		 * パーツ管理情報をロードする.<br>
		 * パーツ管理情報が存在しない場合、もしくは読み取りできなった場合は管理情報はないものとして空のインスタンスを返す.<br>
		 * @return バーツ管理情報、存在しない場合は空を返す.<br>
		 */
		PartsManageData createPartsManageData();
		
	}
	
	private PartsDataLoader parent;
	
	private PartsManageDataFactory partsManageDataFactory;

	public PartsManageDataDecorateLoader(PartsDataLoader parent, PartsManageDataFactory partsManageDataFactory) {
		if (parent == null || partsManageDataFactory == null) {
			throw new IllegalArgumentException();
		}
		this.parent = parent;
		this.partsManageDataFactory = partsManageDataFactory;
	}
	
	public Map<PartsIdentifier, PartsSpec> load(PartsCategory category) {
		
		// 親よりパーツデータをロードする.
		Map<PartsIdentifier, PartsSpec> partsSpecs = parent.load(category);

		// 最新のパーツ管理情報をロードする.
		PartsManageData partsManageData = partsManageDataFactory.createPartsManageData();
		if (partsManageData == null || partsManageData.isEmpty()) {
			// パーツ管理情報がnullであるか空であれば、元のまま何もせず返す.
			return partsSpecs;
		}
		
		// 返却用
		HashMap<PartsIdentifier, PartsSpec> newPartsSpecs = new HashMap<PartsIdentifier, PartsSpec>();

		// パーツ管理データからのデータを補填する.
		for (Map.Entry<PartsIdentifier, PartsSpec> entry : partsSpecs.entrySet()) {
			PartsIdentifier orgPartsId = entry.getKey();
			PartsIdentifier newPartsId;

			PartsSpec partsSpec = entry.getValue();
			
			PartsManageData.PartsKey partsKey = new PartsManageData.PartsKey(orgPartsId);
			
			// ローカライズ名が指定されているか?
			String localizedName = partsManageData.getLocalizedName(partsKey);
			if (localizedName != null
					&& localizedName.length() > 0
					&& !localizedName.equals(orgPartsId.getLocalizedPartsName())) {
				// ローカライズ名が指定されており、且つ、元のローカライズ名と異なる場合
				newPartsId = orgPartsId.setLocalizedPartsName(localizedName);
			} else {
				// ローカライズ名の変更なし
				newPartsId = orgPartsId;
			}

			// パーツ作者情報があれば設定する.
			PartsAuthorInfo partsAuthorInfo = partsManageData.getPartsAuthorInfo(partsKey);
			if (partsAuthorInfo != null) {
				partsSpec.setAuthorInfo(partsAuthorInfo);
			}
			
			// パーツのバージョンとダウンロード情報があれば設定する.
			PartsManageData.PartsVersionInfo versionInfo = partsManageData.getVersion(partsKey);
			if (versionInfo != null) {
				double version = versionInfo.getVersion();
				String downloadURL = versionInfo.getDownloadURL();
				partsSpec.setVersion(version);
				partsSpec.setDownloadURL(downloadURL);
			}
			
			newPartsSpecs.put(newPartsId, partsSpec);
		}
		
		return newPartsSpecs;
	}
	
}
