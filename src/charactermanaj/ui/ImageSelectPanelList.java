package charactermanaj.ui;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;

import charactermanaj.model.PartsCategory;

public class ImageSelectPanelList extends AbstractList<ImageSelectPanel> {

	protected ArrayList<ImageSelectPanel> imageSelectPanels = new ArrayList<ImageSelectPanel>();
	
	protected HashMap<PartsCategory, ImageSelectPanel> imageSelectPanelMap = new HashMap<PartsCategory, ImageSelectPanel>();

	public ImageSelectPanelList() {
	}
	
	@Override
	public boolean add(ImageSelectPanel o) {
		if (o == null) {
			throw new IllegalArgumentException();
		}
		PartsCategory partsCategory = o.getPartsCategory();
		if (imageSelectPanelMap.containsKey(partsCategory)) {
			throw new IllegalArgumentException("duplicate category: " + partsCategory);
		}
		imageSelectPanelMap.put(partsCategory, o);
		return imageSelectPanels.add(o);
	}
	
	@Override
	public ImageSelectPanel get(int index) {
		return imageSelectPanels.get(index);
	}
	
	@Override
	public int size() {
		return imageSelectPanels.size();
	}
	
	public ImageSelectPanel findByPartsCategory(PartsCategory partsCategory) {
		if (partsCategory == null) {
			throw new IllegalArgumentException();
		}
		ImageSelectPanel panel = imageSelectPanelMap.get(partsCategory);
		if (panel == null) {
			throw new IllegalArgumentException("not registered: " + partsCategory);
		}
		return panel;
	}
	
}
