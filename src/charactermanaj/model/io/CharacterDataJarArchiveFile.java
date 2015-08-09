package charactermanaj.model.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class CharacterDataJarArchiveFile extends AbstractCharacterDataArchiveFile {

	protected JarFile jarFile;
	
	protected class JarFileContent implements FileContent {

		private JarEntry entry;
		
		protected JarFileContent(JarEntry entry) {
			this.entry = entry;
		}
		
		public String getEntryName() {
			return entry.getName();
		}
		
		public long lastModified() {
			return entry.getTime();
		}
		
		public InputStream openStream() throws IOException {
			return jarFile.getInputStream(entry);
		}
		
	}
	
	public void close() throws IOException {
		jarFile.close();
	}
	
	public CharacterDataJarArchiveFile(File file) throws IOException {
		super(file);
		jarFile = new JarFile(file);
		load();
	}
	
	private void load() {
		Enumeration<JarEntry> enm = jarFile.entries();
		while (enm.hasMoreElements()) {
			JarEntry entry = enm.nextElement();
			addEntry(new JarFileContent(entry));
		}
		searchRootPrefix();
	}
}
