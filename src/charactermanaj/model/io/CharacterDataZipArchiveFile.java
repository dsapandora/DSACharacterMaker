package charactermanaj.model.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;

import charactermanaj.model.AppConfig;


public class CharacterDataZipArchiveFile extends AbstractCharacterDataArchiveFile {

	protected ZipFile zipFile;
	
	protected class ZipFileContent implements FileContent {

		private ZipEntry entry;
		
		protected ZipFileContent(ZipEntry entry) {
			this.entry = entry;
		}
		
		public String getEntryName() {
			return entry.getName();
		}
		
		public long lastModified() {
			return entry.getTime();
		}
		
		public InputStream openStream() throws IOException {
			return zipFile.getInputStream(entry);
		}
		
	}
	
	public void close() throws IOException {
		zipFile.close();
	}
	
	public CharacterDataZipArchiveFile(File file) throws IOException {
		super(file);
		
		AppConfig appConfig = AppConfig.getInstance();
		String encoding = appConfig.getZipNameEncoding();
		
		zipFile = new ZipFile(file, encoding);
		load();
	}
	
	private void load() {
		@SuppressWarnings("unchecked")
		Enumeration<ZipEntry> enm = zipFile.getEntries();
		while (enm.hasMoreElements()) {
			ZipEntry entry = enm.nextElement();
			addEntry(new ZipFileContent(entry));
		}
		searchRootPrefix();
	}
}
