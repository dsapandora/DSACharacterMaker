package charactermanaj.model;

import java.util.EventListener;

public interface CharacterDataChangeListener extends EventListener {

	void notifyChangeCharacterData(CharacterDataChangeEvent e);

}
