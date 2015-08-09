package charactermanaj.model.io;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;

import charactermanaj.graphics.io.FileImageResource;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsFiles;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;
import charactermanaj.util.FileNameNormalizer;

/**
 * ディレクトリを指定して、そこからキャラクターのパーツデータをロードするローダー.<br>
 * 
 * @author seraphy
 * 
 */
public class FilePartsDataLoader implements PartsDataLoader {

	/**
	 * ベースディレクトリ
	 */
	private File baseDir;
	
	public FilePartsDataLoader(File baseDir) {
		if (baseDir == null) {
			throw new IllegalArgumentException();
		}
		this.baseDir = baseDir;
	}
	
	public File getBaseDir() {
		return baseDir;
	}
	
	public Map<PartsIdentifier, PartsSpec> load(PartsCategory category) {
		if (category == null) {
			throw new IllegalArgumentException();
		}
		// ファイル名をノーマライズする
		FileNameNormalizer normalizer = FileNameNormalizer.getDefault();

		final Map<PartsIdentifier, PartsSpec> images = new HashMap<PartsIdentifier, PartsSpec>();
		for (Layer layer : category.getLayers()) {
			File searchDir = new File(baseDir, layer.getDir());
			if (!searchDir.exists() || !searchDir.isDirectory()) {
				continue;
			}
			File[] imgFiles = searchDir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					if (pathname.isFile()) {
						String lcfname = pathname.getName().toLowerCase();
						return lcfname.endsWith(".png");
					}
					return false;
				}
			});
			if (imgFiles == null) {
				imgFiles = new File[0];
			}
			for (File imgFile : imgFiles) {
				String partsName = normalizer.normalize(imgFile.getName());

				int extpos = partsName.lastIndexOf(".");
				if (extpos > 0) {
					partsName = partsName.substring(0, extpos); 
				}
				PartsIdentifier partsIdentifier = new PartsIdentifier(category, partsName, partsName);
				PartsSpec partsSpec = images.get(partsIdentifier);
				if (partsSpec == null) {
					partsSpec = createPartsSpec(partsIdentifier);
					images.put(partsIdentifier, partsSpec);
				}
				PartsFiles parts = partsSpec.getPartsFiles();
				parts.put(layer, new FileImageResource(imgFile));
			}
		}
		return images;
	}
	
	protected PartsSpec createPartsSpec(PartsIdentifier partsIdentifier) {
		return new PartsSpec(partsIdentifier);
	}

}
