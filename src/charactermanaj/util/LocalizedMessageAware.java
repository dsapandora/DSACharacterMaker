package charactermanaj.util;

/**
 * ローカライズリソースを持っていることを示すインターフェイス.<br>
 * @author seraphy
 */
public interface LocalizedMessageAware {

	/**
	 * ローカライズリソースのIDを取得する.<br>
	 * 設定されていない場合はnullが返される.<br>
	 * @return リソースID、もしくはnull
	 */
	String getLocalizedResourceId();
	
}
