package charactermanaj.model.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;


public class CharacterDataJarFileWriter extends AbstractCharacterDataArchivedFileWriter {

	protected JarOutputStream jarOutStm;
	
	public CharacterDataJarFileWriter(File outFile) throws IOException {
		super(outFile);
		this.jarOutStm = new JarOutputStream(
				new BufferedOutputStream(new FileOutputStream(tmpFile)));
	}

	@Override
	protected void closeEntry() throws IOException {
		jarOutStm.closeEntry();
	}
	
	@Override
	protected OutputStream getOutputStream() throws IOException {
		return jarOutStm;
	}
	
	@Override
	protected void putNextEntry(String name, long lastModified)
			throws IOException {
		JarEntry entry = new JarEntry(name);
		if (lastModified > 0) {
			entry.setTime(lastModified);
		}
		jarOutStm.putNextEntry(entry);
	}
	
	protected void internalClose() throws IOException {
		jarOutStm.close();
	}
}
