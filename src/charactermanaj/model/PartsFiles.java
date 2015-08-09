package charactermanaj.model;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import charactermanaj.graphics.io.ImageResource;

public class PartsFiles extends AbstractMap<Layer, ImageResource> implements Serializable {

	private static final long serialVersionUID = 5799830380308843243L;

	private HashMap<Layer, ImageResource> partsMap = new HashMap<Layer, ImageResource>(); 
	
	private final PartsIdentifier partsIdentifier;

	public PartsFiles(PartsIdentifier partsName) {
		if (partsName == null) {
			throw new IllegalArgumentException();
		}
		this.partsIdentifier = partsName;
	}
	
	public PartsIdentifier getPartsIdentifier() {
		return partsIdentifier;
	}
	
	@Override
	public Set<Map.Entry<Layer, ImageResource>> entrySet() {
		return Collections.unmodifiableSet(partsMap.entrySet());
	}
	
	@Override
	public ImageResource put(final Layer key, final ImageResource value) {
		if (key == null || value == null) {
			throw new IllegalArgumentException();
		}
		if (!partsIdentifier.hasLayer(key)) {
			throw new IllegalArgumentException(key.toString());
		}
		return partsMap.put(key, value);
	}
	
	@Override
	public ImageResource get(Object key) {
		return partsMap.get(key);
	}
	
	@Override
	public boolean containsKey(Object key) {
		return partsMap.containsKey(key);
	}
	
	public long lastModified() {
		long maxLastModified = 0;
		for (ImageResource imageResource : values()) {
			long lastModified = imageResource.lastModified();
			if (lastModified > maxLastModified) {
				maxLastModified = lastModified;
			}
		}
		return maxLastModified;
	}
}
