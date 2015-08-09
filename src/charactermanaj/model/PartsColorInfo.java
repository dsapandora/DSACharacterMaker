package charactermanaj.model;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.graphics.filters.ColorConvertParameter;

/**
 * パーツ単位のカラーグループを含む色情報のコレクション.<br> 
 * パーツは複数のレイヤーから構成されるため、レイヤーごとのカラーグループを含む色情報の集合を意味する.<br>
 * キーのセットはカテゴリに属するレイヤーに固定されており、追加・削除することはできない.<br>
 * @author seraphy
 */
public final class PartsColorInfo extends AbstractMap<Layer, ColorInfo> implements Serializable, Cloneable {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(PartsColorInfo.class.getName());
	
	private static final long serialVersionUID = -8639109147043912257L;

	/**
	 * パーツが属するカテゴリのレイヤー構成に対する色情報のマップ.<br>
	 */
	private HashMap<Layer, ColorInfo> colorInfoMap = new HashMap<Layer, ColorInfo>();

	/**
	 * カテゴリ
	 */
	private final PartsCategory partsCategory;
	
	/**
	 * カテゴリを指定して色情報が未設定のインスタンスを構築する.<br>
	 * カテゴリに属するレイヤーが初期化されている.<br>
	 * @param partsCategory カテゴリ
	 */
	public PartsColorInfo(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		this.partsCategory = partsCategory;
		init();
	}
	
	@Override
	public PartsColorInfo clone() {
		try {
			PartsColorInfo inst = (PartsColorInfo) super.clone();
			inst.colorInfoMap = new HashMap<Layer, ColorInfo>();
			for (Map.Entry<Layer, ColorInfo> entry : colorInfoMap.entrySet()) {
				Layer layer = entry.getKey();
				ColorInfo colorInfo = entry.getValue();
				inst.colorInfoMap.put(layer, colorInfo.clone());
			}
			return inst;

		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * パーツカラー情報を指定したパーツカテゴリに存在するレイヤーに正規化して返す.<br>
	 * カテゴリに存在しないレイヤーの情報は破棄され、結果は有効なレイヤーのみの色情報となる.<br>
	 * @param partsCategory パーツカテゴリ
	 * @return 正規化されたパーツカラー情報
	 */
	public PartsColorInfo createCompatible(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		PartsColorInfo newInfo = new PartsColorInfo(partsCategory);
		newInfo.init();
		for (Map.Entry<Layer, ColorInfo> entry : colorInfoMap.entrySet()) {
			Layer layer = entry.getKey();
			ColorInfo colorInfo = entry.getValue();
			if (partsCategory.hasLayer(layer)) {
				newInfo.put(layer, colorInfo.clone());
			} else {
				logger.log(Level.INFO, "missing layer '" + layer + "' in " + partsCategory);
			}
		}
		return newInfo;
	}

	/**
	 * 2つのパーツカラー情報が同じであるか判定する.<br>
	 * 双方がnullである場合はtrueとなります.<br>
	 * いずれか一方がnullである場合はfalseとなります.<br>
	 * @param a 対象1、null可
	 * @param b 対象2, null可
	 * @return 同一であればtrue、そうでなければfalse
	 */
	public static boolean equals(PartsColorInfo a, PartsColorInfo b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}
	
	private void init() {
		for (Layer layer : partsCategory.getLayers()) {
			colorInfoMap.put(layer, createColorInfo(layer));
		}
	}
	
	protected ColorInfo createColorInfo(Layer layer) {
		ColorInfo colorInfo = new ColorInfo();
		colorInfo.setColorGroup(layer.getColorGroup());
		colorInfo.setSyncColorGroup(layer.getColorGroup().isEnabled());
		colorInfo.setColorParameter(new ColorConvertParameter());
		return colorInfo;
	}
	
	public PartsCategory getPartsCategory() {
		return partsCategory;
	}
	
	@Override
	public Set<java.util.Map.Entry<Layer, ColorInfo>> entrySet() {
		return Collections.unmodifiableSet(colorInfoMap.entrySet());
	}
	
	/**
	 * カテゴリに属するレイヤーの色情報を設定する.<br>
	 * カテゴリに該当しないレイヤーを指定した場合はIllegalArgumentException例外となる.<br>
	 * @param key レイヤー
	 * @param value 色情報
	 */
	@Override
	public ColorInfo put(Layer key, ColorInfo value) {
		if (key == null || value == null) {
			throw new IllegalArgumentException();
		}
		if (!colorInfoMap.containsKey(key)) {
			throw new IllegalArgumentException("invalid layer: " + key);
		}
		return colorInfoMap.put(key, value);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)));
		buf.append("(");
		buf.append(colorInfoMap.toString());
		buf.append(")");
		return buf.toString();
	}
	
}
