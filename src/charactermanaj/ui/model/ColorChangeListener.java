package charactermanaj.ui.model;

import java.util.EventListener;

public interface ColorChangeListener extends EventListener {

	void onColorChange(ColorChangeEvent event);
	
	void onColorGroupChange(ColorChangeEvent event);
	
}
