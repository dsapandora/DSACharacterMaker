package charactermanaj.util;

import java.awt.Component;
import java.util.Properties;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * ローカライズリソースをサポートするオブジェクトをコンボボックスに表示するコンボボックスレンダー
 */
public class LocalizedMessageComboBoxRender extends BasicComboBoxRenderer {

	/**
	 * シリアライズバージョンID 
	 */
	private static final long serialVersionUID = 2148264299941543651L;

	/**
	 * リソースホルダ
	 */
	private Properties strings;
	
	
	public LocalizedMessageComboBoxRender(Properties strings) {
		if (strings == null) {
			throw new IllegalArgumentException();
		}
		this.strings = strings;
	}
	

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		
		Object localizedString = getLocalizedString(value);
		
		return super.getListCellRendererComponent(list, localizedString, index, isSelected,
				cellHasFocus);
	}
	
	/**
	 * ローカライズリソースIDを取得する.<br>
	 * サポートしていないか、該当がなければ、toString()の結果を返す.<br>
	 * @param value オブジェクト
	 * @return ローカライズされた文字列、もしくは通常の文字列
	 */
	protected String getLocalizedString(Object value) {
		if (value == null) {
			return "(null)";
		}
		if (value instanceof LocalizedMessageAware) {
			LocalizedMessageAware o = (LocalizedMessageAware) value;
			String id = o.getLocalizedResourceId();
			String localizedString = null;
			if (id != null) {
				localizedString = strings.getProperty(id);
				if (localizedString == null) {
					return id;
				}
			}
			if (localizedString != null) {
				return localizedString;
			}
		}
		return value.toString();
	}
}
