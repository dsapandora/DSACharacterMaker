package charactermanaj.model.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;

public abstract class AbstractCharacterDataFileWriter implements CharacterDataWriter {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(AbstractCharacterDataFileWriter.class.getName());

	protected File outFile;
	
	protected File tmpFile;
	
	protected Exception occureException;
	
	protected AbstractCharacterDataFileWriter(File outFile) throws IOException {
		if (outFile == null) {
			throw new IllegalArgumentException();
		}
		
		if (outFile.exists()) {
			if (!outFile.canWrite()) {
				throw new IOException("not writable: " + outFile);
			}
		}
		File tmpFile = new File(outFile.getPath() + ".tmp");
		
		this.tmpFile = tmpFile;
		this.outFile = outFile;
	}
	
	public void writeExportProp(Properties prop) throws IOException {
		if (prop == null) {
			throw new IllegalArgumentException();
		}
		
		try {
			internalWriteExportProp(prop);
			
		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("write characterdata failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	protected abstract void internalWriteExportProp(Properties prop) throws IOException;
	

	public void writeCharacterData(CharacterData characterData) throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		
		try {
			internalWriteCharacterData(characterData);
			
		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("write characterdata failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	protected abstract void internalWriteCharacterData(CharacterData characterData) throws IOException;
	
	public void writeTextUTF16LE(String name, String contents) throws IOException {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		
		try {
			internalWriteTextUTF16LE(name, contents);
			
		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("internalWriteTextUTF16 failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	protected abstract void internalWriteTextUTF16LE(String name, String contents) throws IOException;
	
	public void writeSamplePicture(BufferedImage samplePicture) throws IOException {
		if (samplePicture == null) {
			throw new IllegalArgumentException();
		}

		try {
			internalWriteSamplePicture(samplePicture);

		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("write sample picture failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}

	protected abstract void internalWriteSamplePicture(BufferedImage samplePicture) throws IOException;

	public void writePartsImages(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException {
		if (partsImages == null) {
			throw new IllegalArgumentException();
		}

		try {
			internalWritePartsImages(partsImages);

		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("write parts images failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	protected abstract void internalWritePartsImages(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException;

	public void writePartsManageData(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException {
		if (partsImages == null) {
			throw new IllegalArgumentException();
		}

		try {
			internalWritePartsManageData(partsImages);

		} catch (IOException ex) {
			occureException = ex;
			throw ex;

		} catch (Exception ex) {
			occureException = ex;
			IOException ex2 = new IOException("write parts images failed.");
			ex2.initCause(ex);
			throw ex2;
		}
	}
	
	protected abstract void internalWritePartsManageData(Map<PartsIdentifier, PartsSpec> partsImages) throws IOException;

	
	public void close() throws IOException {
		try {
			internalClose();
			
			if (outFile.exists()) {
				if (!outFile.delete()) {
					throw new IOException("old file can't delete. " + outFile);
				}
			}

		} catch (Exception ex) {
			if (occureException == null) {
				occureException = ex;
			}
		}
		if (occureException != null) {
			if (!tmpFile.delete()) {
				logger.log(Level.WARNING, "temporary file can't delete. " + tmpFile);
			}
			return;
		}
		
		if (!tmpFile.renameTo(outFile)) {
			throw new IOException("rename failed. " + tmpFile);
		}
	}
	
	protected abstract void internalClose() throws IOException;
}
