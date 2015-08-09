package charactermanaj.model;

import java.net.URI;

import charactermanaj.model.io.CharacterDataXMLReader;

public class CharacterDataPersistentTest {

	public static void main(String[] args) throws Exception {
		(new CharacterDataPersistentTest()).run();
	}

	public void run() {
		try {
			URI uri = getClass().getResource("character.xml").toURI();
			CharacterDataXMLReader persist = new CharacterDataXMLReader();
			CharacterData cd = persist.loadCharacterDataFromXML(uri);
			System.out.println("result=" + cd);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
