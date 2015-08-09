package charactermanaj.model.io;

import java.util.List;
import java.util.Map;

import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.model.ColorInfo;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsFiles;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.PartsSpecResolver;

/**
 * パーツセットから複合画像イメージを生成するために必要なイメージリソースを抽出する.
 * @author seraphy
 *
 */
public class PartsImageCollectionParser {

	/**
	 * 抽出された複合画像イメージの個々のイメージソースと、カラー情報を受け取るハンドラ.<br>
	 * @author seraphy
	 */
	public interface PartsImageCollectionHandler {
		
		/**
		 * 個々のイメージリソースとカラー情報を受け取るハンドラ.<br>
		 *  
		 * @param partsIdentifier 対象のパーツ識別子
		 * @param layer パーツのレイヤー
		 * @param imageResource パーツのレイヤーの画像リソース
		 * @param param カラー情報、設定されていない場合はnull
		 */
		void detectImageSource(PartsIdentifier partsIdentifier,
				Layer layer, ImageResource imageResource,
				ColorConvertParameter param);
	}
	
	/**
	 * パーツ設定を解決するためのインターフェイス
	 */
	protected PartsSpecResolver partsSpecResolver;
	
	/**
	 * パーツ設定のリゾルバを指定して構築する
	 * @param partsSpecResolver パーツ設定のリゾルバ
	 */
	public PartsImageCollectionParser(PartsSpecResolver partsSpecResolver) {
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException("resolver is null");
		}
		this.partsSpecResolver = partsSpecResolver;
	}
	
	public PartsSpecResolver getPartsSpecResolver() {
		return this.partsSpecResolver;
	}

	/**
	 * パーツセットを指定して複合画像を生成するために必要なイメージソースおよびカラー設定を解決する.<br>
	 * protectedなので派生クラスで呼び出すか、publicに昇格させる.<br>
	 * @param partsSet パーツセット
	 */
	public void parse(PartsSet partsSet, PartsImageCollectionHandler listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener is null");
		}
		if (partsSet == null) {
			throw new IllegalArgumentException("PartsSet is null");
		}

		for (List<PartsIdentifier> partsIdentifiers : partsSet.values()) {
			for (PartsIdentifier partsIdentifier : partsIdentifiers) {
				PartsColorInfo partsColorInfo = partsSet.getColorInfo(partsIdentifier);
				PartsSpec partsSpec = partsSpecResolver.getPartsSpec(partsIdentifier);
				if (partsSpec != null) {
					PartsFiles partsFiles = partsSpec.getPartsFiles();
					for (Map.Entry<Layer, ImageResource> entry : partsFiles.entrySet()) {
						Layer layer = entry.getKey();
						ImageResource file = entry.getValue();
						ColorConvertParameter param = null;
						if (partsColorInfo != null) {
							ColorInfo colorInfo = partsColorInfo.get(layer);
							if (colorInfo != null) {
								param = colorInfo.getColorParameter();
							}
						}
						listener.detectImageSource(partsIdentifier, layer, file, param);
					}
				}
			}
		}
	}
	
}
