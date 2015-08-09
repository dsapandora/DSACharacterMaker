package charactermanaj.util;

import java.lang.reflect.Method;

/**
 * ファイル名をノーマライズする.<br>
 * ただし、サポートされていない場合は何もしない.<br>
 * @author seraphy
 */
public class FileNameNormalizer {

	private static FileNameNormalizer DEFAULT = new FileNameNormalizer();

	public static void setDefault(FileNameNormalizer def) {
		if (def == null) {
			throw new IllegalArgumentException();
		}
		DEFAULT = def;
	}

	public static FileNameNormalizer getDefault() {
		return DEFAULT;
	}

	public String normalize(String name) {
		return name;
	}

	public static boolean setupNFCNormalizer() {
		final Method method;
		final Object nfd;
		try {
			Class<?> normalizerCls = Class.forName("java.text.Normalizer");
			Class<?> formCls = Class.forName("java.text.Normalizer$Form");

			method = normalizerCls.getMethod("normalize", CharSequence.class, formCls);
			nfd = formCls.getField("NFC").get(null);

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			return false;
		}

		FileNameNormalizer normalizer = new FileNameNormalizer() {
			@Override
			public String normalize(String name) {
				if (name != null) {
					try {
						return (String) method.invoke(null, name, nfd);

					} catch (Exception ex) {
						ex.printStackTrace(System.err);
					}
				}
				return name;
			}
		};

		setDefault(normalizer);
		return true;
	}
}
