package charactermanaj.graphics;

import java.io.IOException;

import charactermanaj.graphics.ImageBuilder.ImageOutput;
import charactermanaj.graphics.ImageBuilder.ImageSourceCollector;
import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.graphics.io.ImageResource;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSet;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.model.io.PartsImageCollectionParser;

/**
 * 非同期に複合画像を生成するイメージビルダに引き渡すジョブを生成するためのアダプタクラス.<br>
 * パーツセットとパーツ設定リゾルバからイメージビルダに引き渡す情報を解決するジョブを生成する.<br>
 * @author seraphy
 *
 */
public abstract class ImageBuildJobAbstractAdaptor implements AsyncImageBuilder.AsyncImageBuildJob {

	protected PartsImageCollectionParser partsImageCollectorParser;
	
	public ImageBuildJobAbstractAdaptor(PartsSpecResolver partsSpecResolver) {
		if (partsSpecResolver == null) {
			throw new IllegalArgumentException();
		}
		this.partsImageCollectorParser = new PartsImageCollectionParser(partsSpecResolver);
	}
	
	public void loadParts(final ImageSourceCollector collector) throws IOException {
		if (collector == null) {
			throw new IllegalArgumentException("collector is null");
		}

		PartsSet partsSet = getPartsSet();
		if (partsSet == null) {
			throw new RuntimeException("PartsSet is null");
		}

		collector.setSize(partsImageCollectorParser.getPartsSpecResolver().getImageSize());
		collector.setImageBgColor(partsSet.getBgColor());
		collector.setAffineTramsform(partsSet.getAffineTransformParameter());
		partsImageCollectorParser.parse(partsSet, new PartsImageCollectionParser.PartsImageCollectionHandler() {
			public void detectImageSource(PartsIdentifier partsIdentifier,
					Layer layer, ImageResource imageResource,
					ColorConvertParameter param) {
				if (param == null) {
					param = new ColorConvertParameter();
				}
				collector.setImageSource(layer, imageResource, param);
			}
		});
		collector.setComplite();
	}
	
	protected abstract PartsSet getPartsSet() throws IOException;
	
	public abstract void buildImage(ImageOutput output);

	public abstract void handleException(Throwable ex);
	
	public void onAbandoned() {
		// なにもしない
	}
	
	public void onQueueing(long ticket) {
		// なにもしない
	}
	
}
