package charactermanaj.model.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * パーツデータのローダーファクトリ.<br>
 * @author seraphy
 *
 */
public class PartsDataLoaderFactory {

	private static final PartsDataLoaderFactory singleton = new PartsDataLoaderFactory();
	
	private PartsDataLoaderFactory() {
		super();
	}
	
	public static PartsDataLoaderFactory getInstance() {
		return singleton;
	}

	/**
	 * 設定ファイル(character.xml)のDocBaseをもとに、キャラクターデータのローダーを作成する.<br>
	 * 現在のところ、fileプロトコルのみサポートする.<br>
	 * @param docBase 設定ファイルのURI
	 * @return ローダー
	 * @throws IOException URIに対応するローダーが存在しないか、構築できない場合
	 */
	public PartsDataLoader createPartsLoader(URI docBase) throws IOException {
		if (docBase == null) {
			throw new IllegalArgumentException();
		}
		
		if (!"file".equals(docBase.getScheme())) {
			throw new IOException("ファイル以外はサポートしていません。:" + docBase);
		}
		File docbaseFile = new File(docBase);
		File baseDir = docbaseFile.getParentFile();

		return new FilePartsDataLoader(baseDir);
	}
	
}
