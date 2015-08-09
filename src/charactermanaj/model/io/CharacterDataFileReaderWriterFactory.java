package charactermanaj.model.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;


public class CharacterDataFileReaderWriterFactory {

	private static final CharacterDataFileReaderWriterFactory singleton = new CharacterDataFileReaderWriterFactory();

	private CharacterDataFileReaderWriterFactory() {
		super();
	}
	
	public static CharacterDataFileReaderWriterFactory getInstance() {
		return singleton;
	}
	
	/**
	 * ファイルの拡張子に応じてzip/cmj形式でのライターを構築して帰します.<br>
	 * 拡張子がjarとcmjは同じ意味で、ともにjarファイル形式となります.<br>
	 * zip/cmj/jar以外の拡張子はIOExceptionとなります.<br>
	 * @param outfile 出力先ファイル名
	 * @return ライター
	 * @throws IOException 該当するライターがみつからない場合
	 */
	public CharacterDataWriter createWriter(File outfile) throws IOException {
		if (outfile == null) {
			throw new IllegalArgumentException();
		}
		
		String name = outfile.getName().toLowerCase();
		if (name.endsWith(".jar") || name.endsWith(".cmj")) {
			return new CharacterDataJarFileWriter(outfile);

		} else if (name.endsWith(".zip")) {
			return new CharacterDataZipFileWriter(outfile);
		}
		
		throw new IOException("unsupported file type: " + name);
	}
	
	public CharacterDataArchiveFile openArchive(URI archiveFile) throws IOException {
		if (archiveFile == null) {
			throw new IllegalArgumentException();
		}

		if ("file".equals(archiveFile.getScheme())) {
			// ファイルまたはディレクトリの場合
			File file = new File(archiveFile);
			return openArchive(file);
		}

		// file以外は現在のところサポートしない。
		throw new UnsupportedOperationException();
	}
	
	
	public CharacterDataArchiveFile openArchive(File archiveFile) throws IOException {
		if (archiveFile == null) {
			throw new IllegalArgumentException();
		}

		if (archiveFile.exists() && archiveFile.isDirectory()) {
			// ディレクトリの場合
			return new CharacterDataDirectoryFile(archiveFile);
		}
		
		// zipまたはcmjファイルの場合
		String name = archiveFile.getName().toLowerCase();
		if (name.endsWith(".jar") || name.endsWith(".cmj")) {
			return new CharacterDataJarArchiveFile(archiveFile);

		} else if (name.endsWith(".zip")) {
			return new CharacterDataZipArchiveFile(archiveFile);
		}
		
		throw new IOException("unsupported file type: " + name);
	}
	
}
