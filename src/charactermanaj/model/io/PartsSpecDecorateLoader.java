package charactermanaj.model.io;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import charactermanaj.model.AppConfig;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.PartsCategory;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;

/**
 * パーツ名の末尾が、カラーグループの表記を括弧でくくったものと同じであれば、
 * そのパーツ固有のカラーグループとして設定するためのデコレータ.<br>
 * このクラス自身はパーツのロードは行わず、コンストラクタで指定したローダーによりロードを行い、
 * その結果に対してカラーグループの設定を行う.<br>
 * 
 * @author seraphy
 *
 */
public class PartsSpecDecorateLoader implements PartsDataLoader {

	private PartsDataLoader parent;
	
	private Collection<ColorGroup> colorGroups;

	/**
	 * パーツローダとカラーグループを指定して構築する.
	 * @param parent 元パーツローダー
	 * @param colorGroups カラーグループのコレクション、nullの場合は空とみなす.
	 */
	public PartsSpecDecorateLoader(PartsDataLoader parent, Collection<ColorGroup> colorGroups) {
		if (parent == null) {
			throw new IllegalArgumentException();
		}
		if (colorGroups == null) {
			colorGroups = Collections.emptyList();
		}
		this.parent = parent;
		this.colorGroups = colorGroups;
	}
	
	public Map<PartsIdentifier, PartsSpec> load(PartsCategory category) {
		Map<PartsIdentifier, PartsSpec> partsSpecs = parent.load(category);
		decolatePartsSpec(partsSpecs);
		return partsSpecs;
	}
	
	/**
	 * パーツ識別子の表示名に、カラーグループの表示名により判定されるパターンに合致する場合、
	 * パーツ設定のカラーグループを、そのカラーグループとして設定する.
	 * @param partsSpecs パーツマップ
	 */
	protected void decolatePartsSpec(Map<PartsIdentifier, PartsSpec> partsSpecs) {
		String templ = AppConfig.getInstance().getPartsColorGroupPattern();
		if (templ == null || templ.trim().length() == 0) {
			// パターンが設定されていない場合は無視する.
			return;
		}
		// パーツ名にカラーグループが含まれる場合、それを登録する.
		for (ColorGroup colorGroup : colorGroups) {
			String pattern = templ.replace("@", colorGroup.getLocalizedName());
			Pattern pat = Pattern.compile(pattern);
			for (PartsSpec partsSpec : partsSpecs.values()) {
				Matcher mat = pat.matcher(partsSpec.getPartsIdentifier()
						.getLocalizedPartsName());
				if (mat.matches()) {
					partsSpec.setColorGroup(colorGroup);
				}
			}
		}
	}
	
}
