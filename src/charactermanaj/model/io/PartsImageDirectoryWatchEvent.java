package charactermanaj.model.io;

import java.util.EventObject;

import charactermanaj.model.CharacterData;

public class PartsImageDirectoryWatchEvent extends EventObject {

	private static final long serialVersionUID = 8090309437115158185L;

	public PartsImageDirectoryWatchEvent(CharacterData characterData) {
		super(characterData);
	}
	
	public CharacterData getCharacterData() {
		return (CharacterData) getSource();
	}
	
}
