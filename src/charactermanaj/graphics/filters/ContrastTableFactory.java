package charactermanaj.graphics.filters;

/**
 * コントラストの計算済みテーブルを用意する.<br>
 * 最大で255x3あれば足りる.<br>
 * 
 * @author seraphy
 * 
 */
public class ContrastTableFactory implements TableFactory {

	private float contrast = 1.f;
	
	public ContrastTableFactory() {
		this(1.f);
	}
	
	public ContrastTableFactory(float contrast) {
		this.contrast = contrast;
	}
	
	public int[][] createTable() {
		int[] table = new int[256];
		for (int level = 0; level <= 255; level++) {
			float f = level / 255.f;
			f = getContrast(f);
			int c = (int)(f * 256);
			if (c > 255) {
				c = 255;
			} else if (c < 0) {
				c = 0;
			}
			table[level] = c;
		}

		int[][] tables = new int[3][];
		for (int idx = 0; idx < 3; idx++) {
			tables[idx] = table;
		}
		return tables;
	}
	
	protected float getContrast(float f) {
		return (f - 0.5f) * contrast + 0.5f;
	}
	
}
