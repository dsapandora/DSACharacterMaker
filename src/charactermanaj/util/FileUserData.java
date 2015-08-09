package charactermanaj.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルベースのユーザーデータの保存先の実装
 * @author seraphy
 */
public class FileUserData implements UserData {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(FileUserData.class.getName());

	/**
	 * 保存先ファイル
	 */
	private File file;
	
	public FileUserData(File file) {
		if (file == null) {
			throw new IllegalArgumentException();
		}
		this.file = file;
	}
	
	public boolean exists() {
		return file.exists() && file.isFile();
	}
	
	public long lastModified() {
		return file.lastModified();
	}
	
	public InputStream openStream() throws IOException {
		return new BufferedInputStream(new FileInputStream(file));
	}
	
	public OutputStream getOutputStream() throws IOException {
		return new BufferedOutputStream(new FileOutputStream(file));
	}
	
	public boolean delete() {
		try {
			return file.delete();

		} catch (Exception ex) {
			// セキュリティ例外ぐらい.
			logger.log(Level.WARNING, "file removing failed." + file, ex);
			return false;
		}
	}
	
	public void save(Object userData) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(getOutputStream());
		try {
			oos.writeObject(userData);
			oos.close();
		} finally {
			oos.close();
		}
	}

	public Object load() throws IOException {
		ObjectInputStream ois = new ObjectInputStream(openStream());
		try {
			try {
				return ois.readObject();

			} catch (ClassNotFoundException ex) {
				// 復元先クラスがみつからないということは、このアプリケーションの保存した形式としておかしい
				IOException ex2 = new IOException("invalid format.");
				ex2.initCause(ex2);
				throw ex2;
			}
		} finally {
			ois.close();
		}
	}
	
	@Override
	public String toString() {
		return "FileUserData{file:" + file + "}";
	}
}