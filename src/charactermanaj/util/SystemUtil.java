package charactermanaj.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * システムクラスのためのユーテリティ.<br>
 * Findbugsの警告がうっとおしいのでリフレクションで隠す.<br>
 * 
 * @author seraphy
 */
public final class SystemUtil {

	private static final Class<System> clsSystem;
	
	private static final Method garbageCollection;
	
	private static final Method exit;
	
	private static final int gcLoop = 3;
	
	private static final long sleepTime = 100;

	static {
		try {
			clsSystem = System.class;
			garbageCollection = clsSystem.getMethod("gc");
			exit = clsSystem.getMethod("exit", int.class);

		} catch (NoSuchMethodException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
	
	private SystemUtil() {
		super();
	}
	
	/**
	 * 数回ガベージコレクションをかける.<br>
	 */
	public static void gc() {
		try {
			for (int i = 0; i < gcLoop; i++) {
				if (i != 0) {
					Thread.sleep(sleepTime);
				}
				garbageCollection.invoke(null);
			}

		} catch (InterruptedException ex) {
			// 無視する.

		} catch (InvocationTargetException ex) {
			Throwable iex = ex.getCause();
			if (iex == null) {
				iex = ex;
			}
			throw new RuntimeException(iex.getMessage(), iex);

		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
	
	/**
	 * JVMを終了する.
	 * @param exitCode 終了コード
	 */
	public static void exit(int exitCode) {
		try {
			exit.invoke(null, exitCode);

		} catch (InvocationTargetException ex) {
			Throwable iex = ex.getCause();
			if (iex == null) {
				iex = ex;
			}
			throw new RuntimeException(iex.getMessage(), iex);

		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}
	
}
