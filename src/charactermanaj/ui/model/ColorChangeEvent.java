package charactermanaj.ui.model;

import java.util.EventObject;

import charactermanaj.model.Layer;
import charactermanaj.ui.ColorDialog;

public class ColorChangeEvent extends EventObject {

	private static final long serialVersionUID = -4185234778107466586L;

	private Layer layer;
	
	private boolean cascaded;
	
	public ColorChangeEvent(ColorDialog colorDialog, Layer layer) {
		this(colorDialog, layer, false);
	}
	
	public ColorChangeEvent(ColorChangeEvent src, boolean cascaded) {
		this((ColorDialog) src.getSource(), src.getLayer(), cascaded);
	}

	protected ColorChangeEvent(ColorDialog colorDialog, Layer layer, boolean cascaded) {
		super(colorDialog);
		if (layer == null) {
			throw new IllegalArgumentException("null layer");
		}
		this.layer = layer;
		this.cascaded = cascaded;
	}
	
	public Layer getLayer() {
		return layer;
	}
	
	public boolean isCascaded() {
		return cascaded;
	}
	
}
