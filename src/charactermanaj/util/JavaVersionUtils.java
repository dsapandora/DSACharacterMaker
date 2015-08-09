package charactermanaj.util;


public final class JavaVersionUtils {

	private JavaVersionUtils() {
		super();
	}

	/**
	 * Javaの簡易なバージョンを取得する.<br>
	 * 不明な場合は0を返す.<br>
	 * 
	 * @return バージョン
	 */
	public static double getJavaVersion() {
		try {
			String version = System.getProperty("java.version");
			String[] versions = version.split("\\.");
			if (versions.length > 2) {
				return Double.valueOf(versions[0] + "." + versions[1]);
			}
		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
		return 0d;
	}

	/**
	 * Javaの詳細なバージョンを取得する. メジャー・マイナー・メンテナンス・アップデートの4要素を返す.<br>
	 * 
	 * @return
	 */
	public static int[] getJavaVersions() {
		return getJavaVersions(System.getProperty("java.version"));
	}

	private static int[] getJavaVersions(String version) {
		int[] ret = new int[4];
		try {
			int posIdentifier = version.indexOf('-');
			if (posIdentifier >= 0) {
				version = version.substring(0, posIdentifier);
			}

			int posUpdate = version.indexOf("_");
			int update = 0;
			if (posUpdate >= 0) {
				update = Integer.parseInt(version.substring(posUpdate + 1));
				version = version.substring(0, posUpdate);
			}

			String[] versions = version.split("\\.");

			for (int idx = 0; idx < 3 && idx < versions.length; idx++) {
				ret[idx] = Integer.parseInt(versions[idx]);
			}
			ret[3] = update;

		} catch (RuntimeException ex) {
			ex.printStackTrace();
		}
		return ret;
	}
}
