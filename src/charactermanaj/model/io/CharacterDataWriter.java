package charactermanaj.model.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;

public interface CharacterDataWriter {

	void writeExportProp(Properties prop) throws IOException;
	
	void writeCharacterData(CharacterData characterData) throws IOException;
	
	void writeTextUTF16LE(String name, String contents) throws IOException;
	
	void writeSamplePicture(BufferedImage samplePicture) throws IOException;
	
	void writePartsImages(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException;
	
	void writePartsManageData(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException;

	void close() throws IOException;
}
