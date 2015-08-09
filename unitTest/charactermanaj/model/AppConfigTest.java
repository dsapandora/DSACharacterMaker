package charactermanaj.model;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import charactermanaj.util.BeanPropertiesUtilities;

import junit.framework.TestCase;

public class AppConfigTest extends TestCase {

	public static class Bean1 {
		
		private int val1;
		
		private String val2;
		
		private Color val3;
		
		private boolean val4;

		public int getVal1() {
			return val1;
		}

		public void setVal1(int val1) {
			this.val1 = val1;
		}

		public String getVal2() {
			return val2;
		}

		public void setVal2(String val2) {
			this.val2 = val2;
		}

		public Color getVal3() {
			return val3;
		}

		public void setVal3(Color val3) {
			this.val3 = val3;
		}
		
		public boolean isVal4() {
			return val4;
		}
		
		public void setVal4(boolean val4) {
			this.val4 = val4;
		}
		
		@Override
		public String toString() {
			return val1 + ":" + val2 + ":" + val3;
		}
		
		public String getX() {
			throw new UnsupportedOperationException();
		}
		
		public void setY() {
			throw new UnsupportedOperationException();
		}
	}

	public void test1() throws Exception {
		Properties prop = new Properties();
		assertTrue(prop.isEmpty());

		URL[] urls = new URL[] {
				getClass().getResource("prop1.xml"),
				getClass().getResource("prop2.xml"),
		};
		for (URL url : urls) {
			assertTrue(url != null);
			InputStream is = url.openStream();
			try {
				prop.loadFromXML(is);
			} finally {
				is.close();
			}
		}
		assertTrue(prop.size() == 2);
		System.out.println(prop);
	}
	
	public void test2() throws Exception {

		Bean1 o = new Bean1();
		o.setVal1(123);
		o.setVal2("abc");
		o.setVal3(Color.blue);
		o.setVal4(true);
		
		Properties props = new Properties();
		BeanPropertiesUtilities.saveToProperties(o, props);
		
		System.out.println(props);
		assertTrue(props.size() == 4);
		assertTrue(props.getProperty("val1").equals("123"));
		assertTrue(props.getProperty("val2").equals("abc"));
		assertTrue(props.getProperty("val3").equals("#ff"));
		assertTrue(props.getProperty("val4").equals("true"));
		
		Bean1 o2 = new Bean1();
		BeanPropertiesUtilities.loadFromProperties(o2, props);
		
		System.out.println(o2);
		
		assertTrue(o2.getVal1() == 123);
		assertTrue(o2.getVal2().equals("abc"));
		assertTrue(o2.getVal3().equals(Color.blue));
		assertTrue(o2.isVal4());
	}
	
	public void test3() throws Exception {
		Properties props1 = new Properties();
		AppConfig appConfig = AppConfig.getInstance();
		BeanPropertiesUtilities.saveToProperties(appConfig, props1);
		String val1 = props1.toString();
		System.out.println(val1);
		BeanPropertiesUtilities.loadFromProperties(appConfig, props1);
		Properties props2 = new Properties();
		BeanPropertiesUtilities.saveToProperties(appConfig, props2);
		String val2 = props1.toString();
		System.out.println(val2);
		assertTrue(val1.equals(val2));
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		props2.storeToXML(bos, "appConfig.xml");
		bos.close();
		
		Reader rd = new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()), "UTF-8");
		int ch;
		while ((ch = rd.read()) != -1) {
			System.out.print((char) ch);
		}
		rd.close();
		
		appConfig.saveConfig();
	}
	
}

