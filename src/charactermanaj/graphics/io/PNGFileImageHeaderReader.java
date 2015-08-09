package charactermanaj.graphics.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;

/**
 * PNGファイルのヘッダ情報を読み取る
 * @author seraphy
 */
public class PNGFileImageHeaderReader {

	/**
	 * PNGファイルのファイルヘッダ
	 */
	public static final byte[] FILE_HEADER = { (byte) 0x89, (byte) 0x50,
		(byte) 0x4e, (byte) 0x47, (byte) 0x0d, (byte) 0x0a, (byte) 0x1a,
		(byte) 0x0a };

	
	private static final PNGFileImageHeaderReader singletion = new PNGFileImageHeaderReader();
	
	private PNGFileImageHeaderReader() {
		super();
	}
	
	public static PNGFileImageHeaderReader getInstance() {
		return singletion;
	}
	
	/**
	 * URLを指定してヘッダ情報を取得する.<br>
	 * @param url
	 * @return ヘッダ、PNGでない場合はnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	public PNGFileImageHeader readHeader(URI uri) throws IOException {
		if (uri == null) {
			throw new IllegalArgumentException();
		}
		URL url = uri.toURL();
		InputStream is = url.openStream();
		try {
			return readHeader(is);
		} finally {
			is.close();
		}
	}
	
	/**
	 * ファイルを指定してヘッダ情報を取得する.
	 * @param file
	 * @return ヘッダ、PNGでない場合はnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	public PNGFileImageHeader readHeader(File file) throws IOException {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			return readHeader(is);
		} finally {
			is.close();
		}
	}
	
	/**
	 * ストリームを指定してヘッダ情報を読み取る.<br>
	 * ストリームは読み取った分だけ消費された状態で返される.<br>
	 * @param is ストリーム
	 * @return ヘッダ、PNGでない場足はnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	public PNGFileImageHeader readHeader(InputStream is) throws IOException {
		if (is == null) {
			throw new IllegalArgumentException();
		}
		
		DataInputStream dis = new DataInputStream(is);
		try {
			// ファイルヘッダの読み取り
			byte[] fileHeader = new byte[FILE_HEADER.length];
			dis.readFully(fileHeader);
			if (!Arrays.equals(fileHeader, FILE_HEADER)) {
				// ヘッダが一致しない
				return null;
			}
			
			PNGFileImageHeader imageHeader = null;
			boolean hasTransparencyInfomation = false;
			
			for (;;) {
				int chunkLen = dis.readInt(); // チャンクの長さ

				byte[] chunkType = new byte[4];
				dis.readFully(chunkType);

				if (Arrays.equals(chunkType, "IHDR".getBytes())) {
					imageHeader = new PNGFileImageHeader();

					imageHeader.setWidth(dis.readInt()); // 4bytes
					imageHeader.setHeight(dis.readInt()); // 4bytes
					imageHeader.setBitDepth(((int) dis.readByte()) & 0xff); // 1byte
					imageHeader.setColorType(((int) dis.readByte()) & 0xff); // 1byte
					imageHeader.setCompressionMethod(((int) dis.readByte()) & 0xff); // 1byte
					imageHeader.setFilterMethod(((int) dis.readByte()) & 0xff); // 1byte
					imageHeader.setInterlaceMethod(((int) dis.readByte()) & 0xff); // 1byte

					int zan = chunkLen - 13;
					if (zan < 0) {
						throw new EOFException("IHDR too short");
					}
					if (dis.skipBytes(zan) != zan) {
						throw new IOException("チャンクのサイズが不正です.");
					}

				} else if (Arrays.equals(chunkType, "tRNS".getBytes())) {
					// カラータイプによりチャンクの中身の形式は異なる.
					// インデックス(ColorType=3)の場合は透過色のインデックス
					// グレースケールの場合は、透過色とするスケール値など.
					hasTransparencyInfomation = chunkLen > 0;
					if (dis.skipBytes(chunkLen) != chunkLen) {
						throw new IOException("チャンクのサイズが不正です.");
					}
					
				} else if (Arrays.equals(chunkType, "IEND".getBytes())) {
					// 終了チャンク
					break;
					
				} else {
					// IHDR以外のチャンクは読み飛ばす
					if (dis.skipBytes(chunkLen) != chunkLen) {
						throw new IOException("チャンクのサイズが不正です.");
					}
				}
				dis.readInt(); // CRC32を読み飛ばす
			}
			
			if (imageHeader != null) {
				imageHeader.setTransparencyInformation(hasTransparencyInfomation);
			}
			
			return imageHeader;
			
		} catch (EOFException e) {
			// 何もしない
		}
		
		return null;
	}
	
}
