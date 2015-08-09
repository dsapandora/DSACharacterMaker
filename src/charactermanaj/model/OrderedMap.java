package charactermanaj.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定義順でのリストアクセスと、キーによる一意アクセスの双方を格納にするマップ兼リスト.<br>
 * 元となるコレクションからキーを分離し、キーでアクセス可能にする.<br>
 * マップへのvalues()等のコレクションアクセスでは元のコレクションと同じ定義順で返される.<br>
 * より明確には{{@link #asList()}を使うとリストとしてアクセスすることができる.<br>
 * 読み込み専用で、書き込みはできない.<br>
 * 
 * @author seraphy
 *
 * @param <K> データに含まれるキーの型
 * @param <V> データの型
 */
public class OrderedMap<K, V> extends AbstractMap<K, V> implements Serializable {

	/**
	 * シリアライズバージョンID. 
	 */
	private static final long serialVersionUID = 5049488493598426918L;

	/**
	 * 空を示す定数インスタンス
	 */
	public static final OrderedMap<?, ?> EMPTY_MAP = new OrderedMap<Object, Object>();
	
	@SuppressWarnings("unchecked")
	public static final <K, V> OrderedMap<K, V> emptyMap() {
		return (OrderedMap<K, V>) EMPTY_MAP;
	}
	
	public interface KeyDetector<K, V> {
		
		K getKey(V data);
		
	}
	
	protected static class OrderedMapEntry<K, V> implements Map.Entry<K, V>, Serializable {
		
		private static final long serialVersionUID = 5111249402034089224L;

		private final K key;
		
		private final V data;
		
		protected OrderedMapEntry(K key, V data) {
			this.key = key;
			this.data = data;
		}
		
		public K getKey() {
			return key;
		}

		public V getValue() {
			return data;
		}
		
		public V setValue(V arg0) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * リストとしてのエントリ
	 */
	private ArrayList<Map.Entry<K, V>> entries = new ArrayList<Map.Entry<K, V>>();
	
	/**
	 * マップアクセス用.<br>
	 * シリアライズ時はスキップされ、デシリアライズ時にリストから復元される.<br>
	 */
	private transient HashMap<K, V> entryMap = new HashMap<K, V>();

	/**
	 * デシリアライズ.<br>
	 * @param stream
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();

		entryMap = new HashMap<K, V>();
		for (Map.Entry<K, V> entry : entries) {
			entryMap.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * シリアライズ
	 * @param stream
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

     /**
	 * 空のコレクションを作成する.
	 */
	protected OrderedMap() {
		super();
	}
	
	/**
	 * 元となるコレクションをコピーし、そのコレクションの各データのキーを分離しマップアクセス可能にする.<br>
	 * @param datas 元となるコレクション
	 * @param keyDetector コレクションの各要素からキーを分離するためのインターフェイス
	 */
	public OrderedMap(Collection<V> datas, KeyDetector<K, V> keyDetector) {
		if (datas == null || keyDetector == null) {
			throw new IllegalArgumentException();
		}
		for (V data : datas) {
			K key = keyDetector.getKey(data);
			if (key == null) {
				throw new IllegalArgumentException("null key: " + data);
			}
			entries.add(new OrderedMapEntry<K, V>(key, data));
			entryMap.put(key, data);
		}
		
		if (entries.size() != entryMap.size()) {
			throw new IllegalArgumentException("duplicate-key");
		}
	}
	
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return new AbstractSet<java.util.Map.Entry<K, V>>() {
			@Override
			public int size() {
				return entries.size();
			}
			@Override
			public Iterator<java.util.Map.Entry<K, V>> iterator() {
				final Iterator<java.util.Map.Entry<K, V>> ite = entries.iterator();;
				return new Iterator<java.util.Map.Entry<K, V>>() {
					public boolean hasNext() {
						return ite.hasNext();
					}
					public java.util.Map.Entry<K, V> next() {
						return ite.next();
					}
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}
	
	/**
	 * マップの値を定義順のリストとして扱えるようにリストインターフェイスで返す.<br>
	 * 読み込み専用です.<br>
	 * @return マップの値のリスト
	 */
	public List<V> asList() {
		return new AbstractList<V>() {
			@Override
			public int size() {
				return entries.size();
			}
			@Override
			public V get(int index) {
				return entries.get(index).getValue();
			}
		};
	}
	
	@Override
	public V get(Object key) {
		return entryMap.get(key);
	}
	
	@Override
	public int size() {
		return entries.size();
	}
}
