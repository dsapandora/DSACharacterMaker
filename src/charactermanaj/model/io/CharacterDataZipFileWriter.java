package charactermanaj.model.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;

import charactermanaj.model.AppConfig;


public class CharacterDataZipFileWriter extends AbstractCharacterDataArchivedFileWriter {

	/**
	 * Zipストリーム
	 */
	protected ZipOutputStream zipOutStm;
	
	/**
	 * ファイル名のエンコーディング
	 */
	protected CharsetEncoder enc;
	
	/**
	 * ルートコンテンツへのプレフィックス
	 */
	protected String rootPrefix = "";


	public CharacterDataZipFileWriter(File outFile) throws IOException {
		super(outFile);

		AppConfig appConfig = AppConfig.getInstance();
		String zipNameEncoding = appConfig.getZipNameEncoding();
		
		// コンストラクタにストリームではなくファイル名を指定することで、
		// 内部でランダムアクセスファイルを使うようになるためヘッダのCRCチェックの書き込み等で有利
		this.zipOutStm = new ZipOutputStream(tmpFile);
		
		// ファイル名の文字コードを設定する.
		// (JDKの標準のZipOutputStreamはUTF-8になるが、一般的にはMS932が多いため、Apache Antのものを借用し指定する.)
		this.enc = Charset.forName(zipNameEncoding).newEncoder();
		zipOutStm.setEncoding(zipNameEncoding);
		enc.onUnmappableCharacter(CodingErrorAction.REPORT);

		// zipの場合、根本に1つフォルダをたてておく.
		// 一般的なフォルダ圧縮したものと体裁をそろえるため.
		String fname = outFile.getName();
		int extpos = fname.lastIndexOf('.');
		if (extpos > 0) { // ドットで始まる名前の場合は無視
			fname = fname.substring(0, extpos);
		}
		setRootPrefix(fname);
	}

	public void setRootPrefix(String rootPrefix) {
		if (rootPrefix == null || rootPrefix.trim().equals("/")) {
			rootPrefix = "";
		}
		if (rootPrefix.length() > 0 && !rootPrefix.endsWith("/")) {
			rootPrefix += "/";
		}
		this.rootPrefix = rootPrefix.trim();
	}
	
	public String getRootPrefix() {
		return rootPrefix;
	}

	@Override
	protected void closeEntry() throws IOException {
		zipOutStm.closeEntry();
	}
	
	@Override
	protected OutputStream getOutputStream() throws IOException {
		return zipOutStm;
	}
	
	@Override
	protected void putNextEntry(String name, long lastModified)
			throws IOException {

		 // ルートプレフィックスをすべてのエントリの登録時に付与する.
		String fname = rootPrefix + name;
		
		// ファイル名がキャラクターセットに合致するか?
		checkName(fname);

		// Zipエントリの登録
		ZipEntry entry = new ZipEntry(fname);
		if (lastModified > 0) {
			entry.setTime(lastModified);
		}
		zipOutStm.putNextEntry(entry);
	}
	
	protected void internalClose() throws IOException {
		zipOutStm.close();
	}
	
	/**
	 * ファイル名がエンコーディング可能であるかチェックする.<br>
	 * @param name チェックする名前
	 * @throws IOException ファイル名が不正である場合
	 * @throws UnsupportedEncodingException ファイル名がエンコーディングできない場合
	 */
	protected void checkName(String name) throws UnsupportedEncodingException, IOException {
		if (name == null || name.length() == 0) {
			throw new IOException("missing entry name");
		}
		if (!enc.canEncode(name)) {
			throw new UnsupportedEncodingException("file name encoding error.: " + name);
		}
	}
}
