package charactermanaj.ui.model;

import java.util.EventObject;

import charactermanaj.model.CharacterData;

/**
 * お気に入り変更イベント.<br>
 * 
 * @author seraphy
 */
public class FavoritesChangeEvent extends EventObject {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = 3206827658882098336L;

	private CharacterData characterData;

	public FavoritesChangeEvent(Object src, CharacterData characterData) {
		super(src);
		this.characterData = characterData;
	}

	public CharacterData getCharacterData() {
		return characterData;
	}
}
