package charactermanaj.util;

import java.awt.Color;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Setter/Getterのペアをもつビーンのプロパティを文字列化してプロパティに設定するか、
 * プロパティからビーンに値を設定するためのユーテリティクラス.<br>
 * @author seraphy
 */
public final class BeanPropertiesUtilities {
	
	private static final Logger logger = Logger.getLogger(BeanPropertiesUtilities.class.getName());

	private BeanPropertiesUtilities() {
		throw new RuntimeException("utilities class.");
	}
	
	/**
	 * ビーンのSetter/Getterのペアをもつプロパティに対して、Propertiesより該当するプロパティの値を
	 * 読み取り、プロパティに設定します.<br>
	 * Propertiesに該当するプロパティ名が設定されていなければスキップされます.<br>
	 * Propertiesにビーンにないプロパティ名があった場合、それは単に無視されます.<br>
	 * Propertyの値が空文字の場合、Beanのプロパティの型が文字列以外であればnullが設定されます.<br>
	 * (文字列の場合、空文字のまま設定されます.書き込み時、nullは空文字にされるため、文字列についてはnullを表現することはできません。)<br>
	 * @param bean 設定されるビーン
	 * @param props プロパティソース
	 * @return 値の設定を拒否されたプロパティの名前、エラーがなければ空
	 */
	public static Set<String> loadFromProperties(Object bean, Properties props) {
		if (bean == null || props == null) {
			throw new IllegalArgumentException();
		}
		HashSet<String> rejectNames = new HashSet<String>();
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			for (PropertyDescriptor propDesc : beanInfo
					.getPropertyDescriptors()) {
				Class<?> typ = propDesc.getPropertyType();
				Method mtdReader = propDesc.getReadMethod();
				Method mtdWriter = propDesc.getWriteMethod();
				if (mtdReader != null && mtdWriter != null) {
					// 読み書き双方が可能なもののみ対象とする.

					String name = propDesc.getName();

					String strVal = props.getProperty(name);
					if (strVal == null) {
						// 設定がないのでスキップ
						continue;
					}
					Object val;
					Throwable reject = null;
					try {
						if (String.class.equals(typ)) {
							val = strVal;
						} else if (strVal.length() == 0) {
							val = null;
						} else {
							if (Boolean.class.equals(typ) || boolean.class.equals(typ)) {
								val = Boolean.valueOf(strVal);
							} else if (Integer.class.equals(typ) || int.class.equals(typ)) {
								val = Integer.valueOf(strVal);
							} else if (Long.class.equals(typ) || long.class.equals(typ)) {
								val = Long.valueOf(strVal);
							} else if (Float.class.equals(typ) || float.class.equals(typ)) {
								val = Float.valueOf(strVal);
							} else if (Double.class.equals(typ) || double.class.equals(typ)) {
								val = Double.valueOf(strVal);
							} else if (BigInteger.class.equals(typ)) {
								val = new BigInteger(strVal);
							} else if (BigDecimal.class.equals(typ)) {
								val = new BigDecimal(strVal);
							} else if (Color.class.equals(typ)) {
								val = Color.decode(strVal);
							} else {
								rejectNames.add(name);
								logger.log(Level.WARNING,
									"unsupported propery type: " + typ
									+ "/beanClass="	+ bean.getClass() + " #" + name);
								continue;
							}
						}
						mtdWriter.invoke(bean, val);
						reject = null;

					} catch (InvocationTargetException ex) {
						reject = ex;
					} catch (IllegalAccessException ex) {
						reject = ex;
					} catch (RuntimeException ex) {
						reject = ex;
					}

					if (reject != null) {
						rejectNames.add(name);
						logger.log(Level.WARNING, "invalid propery: "
								+ typ + "/beanClass="
								+ bean.getClass() + " #" + name + " /val=" + strVal
								, reject);
					}
				}
			}
		} catch (IntrospectionException ex) {
			throw new RuntimeException("bean intorospector failed. :" + bean.getClass(), ex);
		}
		return rejectNames;
	}
	
	/**
	 * ビーンのSetter/Getterのペアをもつプロパティの各値をPropertiesに文字列として登録します.<br>
	 * nullの場合は空文字が設定されます.<br>
	 * @param bean プロパティに転送する元情報となるビーン
	 * @param props 設定されるプロパティ
	 */
	public static void saveToProperties(Object bean, Properties props) {
		if (bean == null || props == null) {
			throw new IllegalArgumentException();
		}

		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
			for (PropertyDescriptor propDesc : beanInfo.getPropertyDescriptors()) {
				Method mtdReader = propDesc.getReadMethod();
				Method mtdWriter = propDesc.getWriteMethod();
				if (mtdReader != null && mtdWriter != null) {
					// 読み書き双方が可能なもののみ対象とする.
					
					String name = propDesc.getName();
					Object val = mtdReader.invoke(bean);
					
					String strVal;
					if (val == null) {
						strVal = "";
					} else if (val instanceof String) {
						strVal = (String) val;
					} else if (val instanceof Number) {
						strVal = ((Number) val).toString();
					} else if (val instanceof Boolean) {
						strVal = ((Boolean) val).booleanValue() ? "true" : "false";
					} else if (val instanceof Color) {
						strVal = "#" + Integer.toHexString(((Color) val).getRGB() & 0xffffff);
					} else {
						logger.log(Level.WARNING, "unsupported propery type: "
								+ val.getClass() + "/beanClass="
								+ bean.getClass() + " #" + name);
						continue;
					}
					
					props.setProperty(name, strVal);
				}
			}

		} catch (IllegalAccessException ex) {
			throw new RuntimeException("bean property read failed. :" + bean.getClass(), ex);
		} catch (InvocationTargetException ex) {
			throw new RuntimeException("bean property read failed. :" + bean.getClass(), ex);
		} catch (IntrospectionException ex) {
			throw new RuntimeException("bean intorospector failed. :" + bean.getClass(), ex);
		}
	}
	
}
