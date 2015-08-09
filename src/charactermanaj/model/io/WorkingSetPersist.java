package charactermanaj.model.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.CharacterData;
import charactermanaj.model.WorkingSet;
import charactermanaj.model.WorkingSet2;
import charactermanaj.util.UserData;
import charactermanaj.util.UserDataFactory;

/**
 * ワーキングセットの保存と復元.<br>
 * 
 * @author seraphy
 */
public class WorkingSetPersist {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger
			.getLogger(WorkingSetPersist.class.getName());

	/**
	 * ワーキングセットのサフィックス.
	 */
	public static final String WORKINGSET_FILE_SUFFIX = "workingset.xml";

	private static final WorkingSetPersist singletion = new WorkingSetPersist();

	public static WorkingSetPersist getInstance() {
		return singletion;
	}

	/**
	 * すべてのワーキングセットをクリアする.<br>
	 */
	public void removeAllWorkingSet() {
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		File dir = userDataFactory.getSpecialDataDir("foo-"
				+ WORKINGSET_FILE_SUFFIX);
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isFile()
							&& pathname.getName().endsWith(
									WORKINGSET_FILE_SUFFIX);
				}
			});
			if (files == null) {
				logger.log(Level.WARNING, "dir access failed. " + dir);
				return;
			}
			for (File file : files) {
				boolean success = file.delete();
				logger.log(Level.INFO, "remove file: " + file + " /success="
						+ success);
			}
		}
	}

	/**
	 * ワーキングセットを削除する.
	 * 
	 * @param cd
	 *            対象のキャラクターデータ
	 */
	public void removeWorkingSet(CharacterData cd) {
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		UserData workingSetXmlData = userDataFactory.getMangledNamedUserData(
				cd.getDocBase(), WORKINGSET_FILE_SUFFIX);
		if (workingSetXmlData != null && workingSetXmlData.exists()) {
			logger.log(Level.INFO, "remove file: " + workingSetXmlData);
			workingSetXmlData.delete();
		}
	}

	/**
	 * ワーキングセットを保存する.<br>
	 * ワーキングセットインスタンスには、あらかじめ全て設定しておく必要がある.<br>
	 * 
	 * @param workingSet
	 *            ワーキングセット
	 * @throws IOException
	 *             失敗
	 */
	public void saveWorkingSet(WorkingSet workingSet) throws IOException {
		if (workingSet == null) {
			throw new IllegalArgumentException();
		}
		CharacterData characterData = workingSet.getCharacterData();
		if (characterData == null) {
			throw new IllegalArgumentException("character-data must be set.");
		}

		// XML形式でのワーキングセットの保存
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		UserData workingSetXmlData = userDataFactory.getMangledNamedUserData(
				characterData.getDocBase(), WORKINGSET_FILE_SUFFIX);
		OutputStream outstm = workingSetXmlData.getOutputStream();
		try {
			WorkingSetXMLWriter workingSetXmlWriter = new WorkingSetXMLWriter();
			workingSetXmlWriter.writeWorkingSet(workingSet, outstm);
		} finally {
			outstm.close();
		}
	}

	/**
	 * ワーキングセットを取得する.<br>
	 * 
	 * @param characterData
	 *            対象のキャラクターデータ
	 * @return ワーキングセット、なければnull
	 * @throws IOException
	 *             読み込みに失敗した場合
	 */
	public WorkingSet2 loadWorkingSet(CharacterData characterData)
			throws IOException {
		if (characterData == null) {
			throw new IllegalArgumentException();
		}
		// XML形式でのワーキングセットの復元
		UserDataFactory userDataFactory = UserDataFactory.getInstance();
		UserData workingSetXmlData = userDataFactory.getMangledNamedUserData(
				characterData.getDocBase(), WORKINGSET_FILE_SUFFIX);
		if (workingSetXmlData == null || !workingSetXmlData.exists()) {
			// 保存されていない場合
			return null;
		}
		WorkingSet2 workingSet2;

		InputStream is = workingSetXmlData.openStream();
		try {
			WorkingSetXMLReader WorkingSetXMLReader = new WorkingSetXMLReader();
			workingSet2 = WorkingSetXMLReader.loadWorkingSet(is);

		} finally {
			is.close();
		}

		return workingSet2;
	}
}
