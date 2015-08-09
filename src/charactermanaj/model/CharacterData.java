package charactermanaj.model;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.model.io.PartsDataLoader;

/**
 * キャラクターデータ
 * @author seraphy
 */
public class CharacterData implements Serializable, PartsSpecResolver {

	/**
	 * シリアライズバージョン 
	 */
	private static final long serialVersionUID = -381763373314240953L;

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(CharacterData.class.getName());


	/**
	 * キャラクターデータを表示名順にソートするための比較器.<br>
	 */
	public static final Comparator<CharacterData> SORT_DISPLAYNAME = new Comparator<CharacterData>() {
		public int compare(CharacterData o1, CharacterData o2) {
			if (!o1.isValid() || !o2.isValid()) {
				return o1.isValid() == o2.isValid() ? 0 : o1.isValid() ? 1 : -1;
			}
			int ret = o1.getName().compareTo(o2.getName());
			if (ret == 0) {
				ret = o1.getId().compareTo(o2.getId());
			}
			if (ret == 0) {
				ret = o1.getDocBase().toString().compareTo(o2.getDocBase().toString());
			}
			return ret;
		}
	};

	
	/**
	 * キャラクターデータを定義しているXMLの位置.<br>
	 * docBase自身はxml定義には含まれず、xmlをロードした位置を記憶するためにPersistentクラスによって設定される.<br>
	 */
	private URI docBase;

	
	
	/**
	 * キャラクターデータの内部用ID.<br>
	 * キャラクターデータの構造を判定するために用いる.<br>
	 */
	private String id;
	
	/**
	 * キャラクターデータの更新番号.<br>
	 * キャラクターデータの構造が変更されたことを識別するために用いる.<br>
	 */
	private String rev;
	
	/**
	 * 表示用名
	 */
	private String localizedName;

	/**
	 * 作成者
	 */
	private String author;
	
	/**
	 * 説明
	 */
	private String description;

	/**
	 * イメージサイズ
	 */
	private Dimension imageSize;
	
	/**
	 * カテゴリ(定義順)
	 */
	private OrderedMap<String, PartsCategory> partsCategories = OrderedMap.emptyMap();

	/**
	 * 雑多なプロパティ.<br>
	 */
	private Properties properties = new Properties();
	
	
	/**
	 * プリセットのマップ.<br>
	 * キーはプリセット自身のID、値はプリセット自身.<br>
	 */
	private HashMap<String, PartsSet> presets = new HashMap<String, PartsSet>();
	
	/**
	 * デフォルトのプリセットのID
	 */
	private String defaultPartsSetId;

	/**
	 * カラーグループの定義.<br>
	 */
	private OrderedMap<String, ColorGroup> colorGroups = OrderedMap.emptyMap();
	
	
	/**
	 * お勧めリンクリスト.<br>
	 * Ver0.96以前には存在しないのでnullになり得る.
	 */
	private List<RecommendationURL> recommendationURLList;
	
	/**
	 * パーツカラーマネージャ.<br>
	 * (非シリアライズデータ、デシリアライズ時には新規インスタンスが作成される).<br>
	 */
	private transient PartsColorManager partsColorMrg = new PartsColorManager(this);
	
	/**
	 * パーツデータローダー.<br>
	 * パーツをロードしたときに設定され、リロードするときに使用する.<br>
	 * パーツを一度もロードしていない場合はnull.
	 * (非シリアライズデータ、デシリアライズ時はnullのまま).<br>
	 */
	private transient PartsDataLoader partsDataLoader;
	
	/**
	 * パーツイメージのセット.<br>
	 * (キャラクターセットはパーツイメージをもったままシリアライズされることは想定していないが、可能ではある。)
	 */
	private Map<PartsCategory, Map<PartsIdentifier, PartsSpec>> images
		= new HashMap<PartsCategory, Map<PartsIdentifier, PartsSpec>>();

	
	
	/**
	 * シリアライズする
	 * @param stream 出力先
	 * @throws IOException 失敗
	 */
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		 stream.defaultWriteObject();
	}

	/**
	 * 基本情報のみをコピーして返します.<br>
	 * DocBase, ID, REV, Name, Author, Description, ImageSize、および, PartsCategory, ColorGroup, PartSetのコレクションがコピーされます.<br>
	 * それ以外のものはコピーされません.<br>
	 * @return 基本情報をコピーした新しいインスタンス
	 */
	public CharacterData duplicateBasicInfo() {
		return duplicateBasicInfo(true);
	}

	/**
	 * 基本情報のみをコピーして返します.<br>
	 * DocBase, ID, REV, Name, Author, Description, ImageSize、および, PartsCategory, ColorGroupがコピーされます.<br>
	 * 引数のneedPartsSetがtrueの場合は,PartSetのコレクションもコピーされます.<br>
	 * ディレクトリの監視状態、お勧めリンクもコピーされます.<br>
	 * それ以外のものはコピーされません.<br>
	 * @param needPartsSets パーツセットもコピーする場合、falseの場合はパーツセットは空となります.
	 * @return 基本情報をコピーした新しいインスタンス
	 */
	public CharacterData duplicateBasicInfo(boolean needPartsSets) {
		CharacterData cd = new CharacterData();

		cd.setId(this.id);
		cd.setRev(this.rev);
		cd.setDocBase(this.docBase);

		cd.setName(this.localizedName);

		cd.setAuthor(this.author);
		cd.setDescription(this.description);
		
		cd.setImageSize((Dimension)(this.imageSize == null ? null : this.imageSize.clone()));
		
		cd.setWatchDirectory(this.isWatchDirectory());
		
		ArrayList<RecommendationURL> recommendationURLList = null;
		if (this.recommendationURLList != null) {
			recommendationURLList = new ArrayList<RecommendationURL>();
			for (RecommendationURL recommendationUrl : this.recommendationURLList) {
				recommendationURLList.add(recommendationUrl.clone());
			}
		}
		cd.setRecommendationURLList(recommendationURLList);
		
		ArrayList<PartsCategory> partsCategories = new ArrayList<PartsCategory>();
		partsCategories.addAll(this.getPartsCategories());
		cd.setPartsCategories(partsCategories.toArray(new PartsCategory[partsCategories.size()]));

		ArrayList<ColorGroup> colorGroups = new ArrayList<ColorGroup>();
		colorGroups.addAll(this.getColorGroups());
		cd.setColorGroups(colorGroups);

		if (needPartsSets) {
			for (PartsSet partsSet : this.getPartsSets().values()) {
				cd.addPartsSet(partsSet.clone());
			}
			cd.setDefaultPartsSetId(this.defaultPartsSetId);
		}
		
		return cd;
	}
	
	/**
	 * キャラクターデータが同じ構造であるか?<br>
	 * カラーグループ、カテゴリ、レイヤーの各情報が等しければtrue、それ以外はfalse.<br>
	 * 上記以外の項目(コメントや作者、プリセット等)については判定しない.<br>
	 * サイズ、カラーグループの表示名や順序、カテゴリの順序や表示名、
	 * 複数アイテム可などの違いは構造の変更とみなさない.<br>
	 * レイヤーはレイヤーID、重ね合わせ順、対象ディレクトリの3点が変更されている場合は構造の変更とみなす.<br>
	 * いずれも個数そのものが変わっている場合は変更とみなす.<br>
	 * 自分または相手がValidでなければ常にfalseを返す.<br>
	 * @param other 比較対象, null可
	 * @return 同じ構造であればtrue、そうでなければfalse
	 */
	public boolean isSameStructure(CharacterData other) {
		if (!this.isValid() || other == null || !other.isValid()) {
			// 自分または相手がinvalidであれば構造的には常に不一致と見なす.
			return false;
		}

		// カラーグループが等しいか? (順序は問わない)
		// IDのみによって判定する
		ArrayList<ColorGroup> colorGroup1 = new ArrayList<ColorGroup>(getColorGroups());
		ArrayList<ColorGroup> colorGroup2 = new ArrayList<ColorGroup>(other.getColorGroups());
		if (colorGroup1.size() != colorGroup2.size()) {
			return false;
		}
		if (!colorGroup1.containsAll(colorGroup2)) {
			return false;
		}
		
		// カテゴリが等しいか? (順序は問わない)
		// IDによってのみ判定する.
		ArrayList<PartsCategory> categories1 = new ArrayList<PartsCategory>(getPartsCategories());
		ArrayList<PartsCategory> categories2 = new ArrayList<PartsCategory>(other.getPartsCategories()); 
		Comparator<PartsCategory> sortCategoryId = new Comparator<PartsCategory>() {
			public int compare(PartsCategory o1, PartsCategory o2) {
				int ret = o1.getCategoryId().compareTo(o2.getCategoryId());
				if (ret == 0) {
					ret = o1.getOrder() - o2.getOrder();
				}
				return ret;
			}
		};
		// カテゴリID順に並び替えて, IDのみを比較する.
		Collections.sort(categories1, sortCategoryId);
		Collections.sort(categories2, sortCategoryId);
		int numOfCategories = categories1.size();
		if (numOfCategories != categories2.size()) {
			// カテゴリ数不一致
			return false;
		}
		for (int idx = 0; idx < numOfCategories; idx++) {
			PartsCategory category1 = categories1.get(idx);
			PartsCategory category2 = categories2.get(idx);
			String categoryId1 = category1.getCategoryId();
			String categoryId2 = category2.getCategoryId();
			if ( !categoryId1.equals(categoryId2)) {
				// カテゴリID不一致
				return false;
			}
		}
		
		// レイヤーが等しいか?
		// ID、重ね順序、dirによってのみ判定する.
		int mx = categories1.size();
		for (int idx = 0; idx < mx; idx++) {
			PartsCategory category1 = categories1.get(idx);
			PartsCategory category2 = categories2.get(idx);
			
			ArrayList<Layer> layers1 = new ArrayList<Layer>(category1.getLayers());
			ArrayList<Layer> layers2 = new ArrayList<Layer>(category2.getLayers());
			
			Comparator<Layer> sortLayerId = new Comparator<Layer>() {
				public int compare(Layer o1, Layer o2) {
					int ret = o1.getId().compareTo(o2.getId());
					if (ret == 0) {
						ret = o1.getOrder() - o2.getOrder();
					}
					return ret;
				}
			};
			
			Collections.sort(layers1, sortLayerId);
			Collections.sort(layers2, sortLayerId);

			// ID、順序、Dirで判断する.(それ以外のレイヤー情報はequalsでは比較されない)
			if ( !layers1.equals(layers2)) {
				// レイヤー不一致
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 引数で指定したキャラクター定義とアッパーコンパチブルであるか?<br>
	 * 構造が同一であるか、サイズ違い、もしくはレイヤーの順序、カテゴリの順序、
	 * もしくはレイヤーまたはカテゴリが増えている場合で、減っていない場合はtrueとなる.<br>
	 * 引数がnullの場合は常にfalseとなる.
	 * @param other 前の状態のキャラクター定義、null可
	 * @return アッパーコンパチブルであればtrue、そうでなければfalse
	 */
	public boolean isUpperCompatibleStructure(CharacterData other) {
		if (!this.isValid() || other == null || !other.isValid()) {
			// 自分または相手がinvalidであれば構造的には常に互換性なしと見なす.
			return false;
		}

		// カラーグループが等しいか? (順序は問わない)
		// IDのみによって判定する
		ArrayList<ColorGroup> colorGroupNew = new ArrayList<ColorGroup>(getColorGroups());
		ArrayList<ColorGroup> colorGroupOld = new ArrayList<ColorGroup>(other.getColorGroups());
		if (!colorGroupNew.containsAll(colorGroupOld)) {
			// 自分が相手分のすべてのものを持っていなければ互換性なし.
			return false;
		}
		
		// カテゴリをすべて含むか? (順序は問わない)
		// IDによってのみ判定する.
		Map<String, PartsCategory> categoriesNew = new HashMap<String, PartsCategory>();
		for (PartsCategory category : getPartsCategories()) {
			categoriesNew.put(category.getCategoryId(), category);
		}
		Map<String, PartsCategory> categoriesOld = new HashMap<String, PartsCategory>();
		for (PartsCategory category : other.getPartsCategories()) {
			categoriesOld.put(category.getCategoryId(), category);
		}
		if ( !categoriesNew.keySet().containsAll(categoriesOld.keySet())) {
			// 自分が相手のすべてのカテゴリを持っていなければ互換性なし.
			return false;
		}
		
		// レイヤーをすべて含むか?
		// ID、Dirによってのみ判定する.
		for (Map.Entry<String, PartsCategory> categoryOldEntry : categoriesOld.entrySet()) {
			String categoryId = categoryOldEntry.getKey();
			PartsCategory categoryOld = categoryOldEntry.getValue();
			PartsCategory categoryNew = categoriesNew.get(categoryId);
			if (categoryNew == null) {
				return false;
			}
			
			Map<String, Layer> layersNew = new HashMap<String, Layer>();
			for (Layer layer : categoryNew.getLayers()) {
				layersNew.put(layer.getId(), layer);
			}
			Map<String, Layer> layersOld = new HashMap<String, Layer>();
			for (Layer layer : categoryOld.getLayers()) {
				layersOld.put(layer.getId(), layer);
			}
			
			if ( !layersNew.keySet().containsAll(layersOld.keySet())) {
				// 自分が相手のすべてのレイヤー(ID)を持っていなければ互換性なし.
				return false;
			}
			for (Map.Entry<String, Layer> layerOldEntry : layersOld.entrySet()) {
				String layerId = layerOldEntry.getKey();
				Layer layerOld = layerOldEntry.getValue();
				Layer layerNew = layersNew.get(layerId);
				if (layerNew == null) {
					return false;
				}
				File dirOld = new File(layerOld.getDir());
				File dirNew = new File(layerNew.getDir());
				if ( !dirOld.equals(dirNew)) {
					// ディレクトリが一致しなければ互換性なし.
					return false;
				}
			}
		}
		
		return true;
	}
	
	/**
	 * キャラクターデータの構造を表す文字列を返す.<br>
	 * カテゴリ、レイヤー、色グループのみで構成される.<br>
	 * id, revなどは含まない.<br>
	 * @return キャラクターデータの構造を表す文字列
	 */
	public String toStructureString() {
		// カラーグループ
		StringBuilder buf = new StringBuilder();
		buf.append("{colorGroup:[");
		for (ColorGroup colorGroup : getColorGroups()) {
			buf.append(colorGroup.getId());
			buf.append(",");
		}
		buf.append("],");
		
		// カテゴリ
		buf.append("category:[");
		for (PartsCategory category : getPartsCategories()) {
			buf.append("{id:");
			buf.append(category.getCategoryId());

			buf.append(",layer:[");
			for (Layer layer : category.getLayers()) {
				buf.append("{id:");
				buf.append(layer.getId());
				buf.append(",dir:");
				buf.append(layer.getDir());
				buf.append("},");
			}
			buf.append("]},");
		}
		buf.append("]}");
		
		return buf.toString();
	}

	/**
	 * キャラクターデータのID, REVと構造を識別するシグネチャの文字列を返す.<br>
	 * (構造はカテゴリ、レイヤー、色グループのみ).<br>
	 * @return シグネチャの文字列
	 */
	public String toSignatureString() {
		StringBuilder buf = new StringBuilder();
		buf.append("{id:");
		buf.append(getId());
		buf.append(",rev:");
		buf.append(getRev());
		buf.append(",structure:");
		buf.append(toStructureString());
		buf.append("}");
		return buf.toString();
	}
	
	/**
	 * デシリアライズする.
	 * @param stream 入力もと
	 * @throws IOException 失敗
	 * @throws ClassNotFoundException 失敗
	 */
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		 stream.defaultReadObject();
		 partsColorMrg = new PartsColorManager(this);
	}
	
	/**
	 * お勧めリンクのリストを取得する.<br>
	 * 古いキャラクターデータで、お勧めリストノードが存在しない場合はnullとなる.<br>
	 * @return お気に入りリンクのリスト、もしくはnull
	 */
	public List<RecommendationURL> getRecommendationURLList() {
		return recommendationURLList;
	}
	
	/**
	 * お勧めリンクリストを設定する.<br>
	 * @param recommendationURLList、null可
	 */
	public void setRecommendationURLList(
			List<RecommendationURL> recommendationURLList) {
		this.recommendationURLList = recommendationURLList;
	}
	

	/**
	 * 作者を設定する.
	 * @param author 作者
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	
	/**
	 * 説明を設定する.<br>
	 * 説明の改行コードはプラットフォーム固有の改行コードに変換される.<br>
	 * @param description
	 */
	public void setDescription(String description) {
		if (description != null) {
			description = description.replace("\r\n", "\n");
			description = description.replace("\r", "\n");
			description = description.replace("\n", System.getProperty("line.separator"));
		}
		this.description = description;
	}
	
	public String getAuthor() {
		return author;
	}
	
	/**
	 * 説明を取得する.<br>
	 * 説明の改行コードはプラットフォーム固有の改行コードとなる.<br>
	 * @return 説明
	 */
	public String getDescription() {
		return description;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getRev() {
		return rev;
	}
	
	public void setRev(String rev) {
		this.rev = rev;
	}
	
	public void setDocBase(URI docBase) {
		this.docBase = docBase;
	}
	
	public URI getDocBase() {
		return docBase;
	}

	/**
	 * ディレクトリを監視するか? (デフォルトは監視する)
	 * @return ディレクトリを監視する場合はtrue
	 */
	public boolean isWatchDirectory() {
		try {
			String value = properties.getProperty("watch-dir");
			if (value != null) {
				return Boolean.parseBoolean(value);
			}
		} catch (RuntimeException ex) {
			logger.log(Level.WARNING, "watch-dir property is invalid.", ex);
		}
		// デフォルトは監視する.
		return true;
	}
	
	/**
	 * ディレクトリを監視するか指定する.
	 * @param watchDir 監視する場合はtrue、しない場合はfalse
	 */
	public void setWatchDirectory(boolean watchDir) {
		properties.setProperty("watch-dir", Boolean.toString(watchDir));
	}
	
	public String getProperty(String key) {
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		return properties.getProperty(key.trim());
	}
	
	public void setProperty(String key, String value) {
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException();
		}
		properties.setProperty(key.trim(), value);
	}
	
	public Collection<String> getPropertyNames() {
		ArrayList<String> names = new ArrayList<String>();
		for (Object key : properties.keySet()) {
			names.add(key.toString());
		}
		return names;
	}
	
	/**
	 * 有効なキャラクターデータであるか?
	 * ID, Name, DocBaseが存在するものが有効なキャラクターデータである.<br>
	 * @return 有効であればtrue
	 */
	public boolean isValid() {
		return id != null && id.length() > 0 && localizedName != null
				&& localizedName.length() > 0 && docBase != null;
	}
	
	/**
	 * 編集可能か?<br>
	 * まだdocbaseが指定されていない新しいインスタンスであるか、
	 * もしくはdocbaseが実在しファイルであり且つ読み込み可能であるか、
	 * もしくはdocbaseがまだ存在しない場合は、その親ディレクトリが読み書き可能であるか?
	 * @return 編集可能であればtrue
	 */
	public boolean canWrite() {
		try {
			checkWritable();
			return true;

		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 編集可能か?<br>
	 * まだdocbaseが指定されていない新しいインスタンスであるか、
	 * もしくはdocbaseが実在しファイルであり且つ読み込み可能であるか、
	 * もしくはdocbaseがまだ存在しない場合は、その親ディレクトリが読み書き可能であるか?
	 * @throws IOException 編集可能でなければIOException例外が発生する.
	 */
	public void checkWritable() throws IOException {
		if (docBase == null) {
			throw new IOException("invalid profile: " + this);
		}

		if ( !"file".equals(docBase.getScheme())) {
			throw new IOException("ファイルプロトコルではないため書き込みはできません。:" + docBase);
		}

		File xmlFile = new File(docBase);
		if (xmlFile.exists()) {
			// character.xmlファイルがある場合
			if ( !xmlFile.canWrite() || !xmlFile.canRead()) {
				throw new IOException("書き込み、もしくは読み込みが禁止されているプロファイルです。" + docBase);
			}
			
		} else {
			// character.xmlファイルが、まだ存在していない場合
			File parent = xmlFile.getParentFile();
			if ( !parent.exists()) {
				throw new IOException("親ディレクトリがありません。" + docBase);
			}
			if ( !parent.canWrite() || !parent.canRead()) {
				throw new IOException("親ディレクトリは書き込み、もしくは読み込みが禁止されています。" + docBase);
			}
		}
	}

	/**
	 * キャラクター名を設定する.
	 * @param name
	 */
	public void setName(String name) {
		this.localizedName = name;
	}
	
	/**
	 * キャラクター名を取得する.
	 * @return
	 */
	public String getName() {
		return localizedName;
	}
	
	public void setImageSize(Dimension imageSize) {
		if (imageSize != null) {
			imageSize = (Dimension) imageSize.clone();
		}
		this.imageSize = imageSize;
	}
	
	public Dimension getImageSize() {
		return imageSize != null ? (Dimension) imageSize.clone() : null;
	}
	
	public void setColorGroups(Collection<ColorGroup> colorGroups) {
		if (colorGroups == null) {
			throw new IllegalArgumentException();
		}
		
		ArrayList<ColorGroup> colorGroupWithNA = new ArrayList<ColorGroup>();

		colorGroupWithNA.add(ColorGroup.NA);
		for (ColorGroup colorGroup : colorGroups) {
			if (colorGroup.isEnabled()) {
				colorGroupWithNA.add(colorGroup);
			}
		}
		
		OrderedMap<String, ColorGroup> ret = new OrderedMap<String, ColorGroup>(
				colorGroupWithNA,
				new OrderedMap.KeyDetector<String, ColorGroup>() {
					public String getKey(ColorGroup data) {
						return data.getId();
					}
				});
		this.colorGroups = ret;
	}
	
	/**
	 * カラーグループIDからカラーグループを取得する.<br>
	 * 存在しない場合はN/Aを返す.<br>
	 * @param colorGroupId カラーグループID
	 * @return カラーグループ
	 */
	public ColorGroup getColorGroup(String colorGroupId) {
		ColorGroup cg = colorGroups.get(colorGroupId);
		if (cg != null) {
			return cg;
		}
		return ColorGroup.NA;
	}
	
	public Collection<ColorGroup> getColorGroups() {
		return colorGroups.values();
	}
	
	public PartsCategory getPartsCategory(String categoryId) {
		if (partsCategories == null) {
			return null;
		}
		return partsCategories.get(categoryId);
	}
	
	public void setPartsCategories(PartsCategory[] partsCategories) {
		if (partsCategories == null) {
			partsCategories = new PartsCategory[0];
		}
		this.partsCategories = new OrderedMap<String, PartsCategory>(
				Arrays.asList(partsCategories),
				new OrderedMap.KeyDetector<String, PartsCategory>() {
					public String getKey(PartsCategory data) {
						return data.getCategoryId();
					}
				});
	}
	
	public List<PartsCategory> getPartsCategories() {
		return partsCategories.asList();
	}
	
	/**
	 * パーツデータがロード済みであるか?<br>
	 * 少なくとも{@link #loadPartsData(PartsDataLoader)}が一度呼び出されていればtrueとなる.<br>
	 * falseの場合はパーツローダが設定されていないことを示す.<br>
	 * @return パーツデータがロード済みであればtrue、そうでなければfalse
	 */
	public boolean isPartsLoaded() {
		return partsDataLoader != null;
	}

	/**
	 * パーツデータをロードする.<br>
	 * パーツローダを指定し、このローダはパーツの再ロード時に使用するため保持される.<br>
	 * @param partsDataLoader ローダー
	 */
	public void loadPartsData(PartsDataLoader partsDataLoader) {
		if (partsDataLoader == null) {
			throw new IllegalArgumentException();
		}
		this.partsDataLoader = partsDataLoader;
		reloadPartsData();
	}
	
	/**
	 * パーツデータをリロードする.<br>
	 * ロード時に使用したローダーを使ってパーツを再ロードします.<br>
	 * まだ一度もロードしていない場合はIllegalStateException例外が発生します.<br>
	 * @return 変更があった場合はtrue、ない場合はfalse
	 */
	public boolean reloadPartsData() {
		if (partsDataLoader == null) {
			throw new IllegalStateException("partsDataLoader is not set.");
		}
		// パーツデータのロード
		images.clear();
		for (PartsCategory category : partsCategories.asList()) {
			images.put(category, partsDataLoader.load(category));
		}
		// NOTE: とりあえずパーツの変更を検査せず、常に変更ありにしておく。とりあえず実害ない。
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PartsSpec getPartsSpec(PartsIdentifier partsIdentifier) {
		if (partsIdentifier == null) {
			throw new IllegalArgumentException();
		}
		PartsCategory partsCategory = partsIdentifier.getPartsCategory();
		Map<PartsIdentifier, PartsSpec> partsSpecMap = images.get(partsCategory);
		if (partsSpecMap != null) {
			PartsSpec partsSpec = partsSpecMap.get(partsIdentifier);
			if (partsSpec != null) {
				return partsSpec;
			}
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Map<PartsIdentifier, PartsSpec> getPartsSpecMap(PartsCategory category) {
		Map<PartsIdentifier, PartsSpec> partsImageMap = images.get(category);
		if (partsImageMap == null) {
			return Collections.emptyMap();
		}
		return partsImageMap;
	}

	public PartsColorManager getPartsColorManager() {
		return this.partsColorMrg;
	}
	
	/**
	 * パーツセットを登録します.<br>
	 * お気に入りとプリセットの両方の共用です.<br>
	 * IDおよび名前がないものは登録されず、falseを返します.<br>
	 * パーツセットは、このキャラクター定義に定義されているカテゴリに正規化されます.<br>
	 * 正規化された結果カテゴリが一つもなくなった場合は何も登録されず、falseを返します.<br>
	 * 登録された場合はtrueを返します.<br>
	 * 同一のIDは上書きされます.<br>
	 * @param partsSet
	 * @return 登録された場合はtrue、登録できない場合はfalse
	 */
	public boolean addPartsSet(PartsSet partsSet) {
		if (partsSet == null) {
			throw new IllegalArgumentException();
		}
		if (partsSet.getPartsSetId() == null
				|| partsSet.getPartsSetId().length() == 0
				|| partsSet.getLocalizedName() == null
				|| partsSet.getLocalizedName().length() == 0) {
			return false;
		}
		PartsSet compatiblePartsSet = partsSet.createCompatible(this);
		if (compatiblePartsSet.isEmpty()) {
			return false;
		}
		presets.put(compatiblePartsSet.getPartsSetId(), compatiblePartsSet);
		return true;
	}
	
	/**
	 * プリセットパーツおよびパーツセット(Favorites)のコレクション.
	 * @return パーツセットのコレクション
	 */
	public Map<String, PartsSet> getPartsSets() {
		return presets;
	}

	/**
	 * プリセットパーツおよびパーツセットをリセットします.<br>
	 * @param noRemovePreset プリセットは削除せず残し、プリセット以外のパーツセットをクリアする場合はtrue、falseの場合は全て削除される.
	 */
	public void clearPartsSets(boolean noRemovePreset) {
		if (!noRemovePreset) {
			// 全部消す
			presets.clear();
			defaultPartsSetId = null;
		} else {
			// プリセット以外を消す
			Iterator<Map.Entry<String, PartsSet>> ite = presets.entrySet().iterator();
			while (ite.hasNext()) {
				Map.Entry<String, PartsSet> entry = ite.next();
				if (!entry.getValue().isPresetParts()) {
					// デフォルトパーツセットであれば、デフォルトパーツセットもnullにする.
					// (ただし、デフォルトパーツセットはプリセットであることを想定しているので、この処理は安全策用。)
					if (entry.getKey().equals(defaultPartsSetId)) {
						defaultPartsSetId = null;
					}
					ite.remove();
				}
			}
		}
	}
	
	/**
	 * デフォルトのパーツセットを取得する.<br>
	 * そのパーツセットIDが実在するか、あるいは、それがプリセットであるか、などは一切関知しない.<br>
	 * 呼び出しもとで必要に応じてチェックすること.<br>
	 * @return デフォルトとして指定されているパーツセットのID、なければnull
	 */
	public String getDefaultPartsSetId() {
		return defaultPartsSetId;
	}
	
	/**
	 * デフォルトのパーツセットIDを指定する.<br>
	 * nullの場合はデフォルトのパーツセットがないことを示す.<br>
	 * パーツセットはプリセットであることが想定されるが、<br>
	 * 実際に、その名前のパーツセットが存在するか、あるいは、そのパーツセットがプリセットであるか、などの判定は一切行わない.<br>
	 * @param defaultPartsSetId パーツセットID、もしくはnull
	 */
	public void setDefaultPartsSetId(String defaultPartsSetId) {
		this.defaultPartsSetId = defaultPartsSetId;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("character-id: " + id);
		buf.append("/rev:" + rev);
		buf.append("/name:" + localizedName);
		buf.append("/image-size:" + imageSize.width + "x" + imageSize.height);
		buf.append("/docBase:" + docBase);
		return buf.toString();
	}
	
}
