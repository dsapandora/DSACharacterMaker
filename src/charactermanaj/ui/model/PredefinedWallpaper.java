package charactermanaj.ui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import charactermanaj.util.LocalizedMessageAware;
import charactermanaj.util.LocalizedResourcePropertyLoader;

/**
 * 定義済み壁紙
 * @author seraphy
 */
public class PredefinedWallpaper implements Comparable<PredefinedWallpaper>, LocalizedMessageAware {

	private static final String PREDEFINED_WALLPAPER_RESOURCE = "images/wallpaper";
	
	private final String key;
	
	private final String msgid;
	
	private final String resource;
	
	protected PredefinedWallpaper(String key, String msgid, String resource) {
		this.key = key;
		this.msgid = msgid;
		this.resource = resource;
	}
	
	public String getLocalizedResourceId() {
		return "predefinedWallpaper." + msgid;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getMsgid() {
		return msgid;
	}
	
	public String getResource() {
		return resource;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof PredefinedWallpaper) {
			PredefinedWallpaper o = (PredefinedWallpaper) obj;
			return key.equals(o.key);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	public int compareTo(PredefinedWallpaper o) {
		return key.compareTo(o.key);
	}
	
	@Override
	public String toString() {
		return msgid;
	}
	
	/**
	 * 定義済み壁紙リソースのリストを取得する
	 * @return 定義済み壁紙リソースリスト
	 */
	public static List<PredefinedWallpaper> getPredefinedWallpapers() {
		Properties predefinedWallpapers = LocalizedResourcePropertyLoader.getCachedInstance()
			.getLocalizedProperties(PREDEFINED_WALLPAPER_RESOURCE);

		ArrayList<PredefinedWallpaper> results = new ArrayList<PredefinedWallpaper>();
		
		for (Map.Entry<Object, Object> entry : predefinedWallpapers.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			String msgid;
			int pt = value.indexOf(';');
			if (pt >= 0) {
				msgid = value.substring(pt + 1);

			} else {
				msgid = value;
			}
			
			PredefinedWallpaper predefinedWallpaper = new PredefinedWallpaper(
					key, msgid, "images/" + key);
			
			results.add(predefinedWallpaper);
		}
		
		Collections.sort(results);
		
		return results;
	}
}