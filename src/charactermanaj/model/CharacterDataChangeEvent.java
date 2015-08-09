package charactermanaj.model;

import java.util.EventObject;

public class CharacterDataChangeEvent extends EventObject {

	private static final long serialVersionUID = -99746684880598436L;

	private CharacterData characterData;

	private boolean changeStructure;

	private boolean reloadPartsAndFavorites;

	public CharacterDataChangeEvent(Object src, CharacterData characterData,
			boolean changeStructure, boolean reloadPartsAndFavorites) {
		super(src);
		this.characterData = characterData;
		this.changeStructure = changeStructure;
		this.reloadPartsAndFavorites = reloadPartsAndFavorites;
	}

	public CharacterData getCharacterData() {
		return characterData;
	}

	public boolean isChangeStructure() {
		return changeStructure;
	}

	public boolean isReloadPartsAndFavorites() {
		return reloadPartsAndFavorites;
	}
}
