package charactermanaj.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * パーツセット.<br>
 * 各カテゴリの選択パーツと、そのパーツの色情報、および背景色をセットにしたもの.<br>
 * 保存する必要がなければIDおよび表示名は使用されないため、nullとなりえる.<br>
 * 
 * @author seraphy
 * 
 */
public final class PartsSet extends AbstractMap<PartsCategory, List<PartsIdentifier>> implements Serializable, Cloneable {

	/**
	 * シリアライズバージョンID.
	 */
	private static final long serialVersionUID = 5972528889825451761L;

	/**
	 * PartsSet用のデフォルトのコンパレータ.<br>
	 * 名前順、ID順にソートする.<br>
	 */
	public static final Comparator<PartsSet> DEFAULT_COMPARATOR = new Comparator<PartsSet>() {
		public int compare(PartsSet o1, PartsSet o2) {
			int ret = o1.getLocalizedName().compareTo(o2.getLocalizedName());
			if (ret == 0) {
				ret = o1.getPartsSetId().compareTo(o2.getPartsSetId());
			}
			if (ret == 0) {
				ret = o1.hashCode() - o2.hashCode();
			}
			return ret;
		}
	};

	/**
	 * パーツセットID.<br>
	 * 一時的な名前なしのパーツセットとして使われる場合はnull
	 */
	private String partsSetId;
	
	/**
	 * パーツセットの表示名.<br>
	 * 一時的な名前なしのパーツセットとして使われる場合はnull
	 */
	private String localizedName;
	
	/**
	 * プリセットパーツフラグ
	 */
	private boolean presetParts;
	
	/**
	 * パーツセットとともに使われる背景色.<br>
	 */
	private Color bgColor;
	
	/**
	 * アフィン変換用パラメータ.<br>
	 * 変換しない場合はnull.<br>
	 */
	private double[] affineTransformParameter;
	
	/**
	 * パーツリスト
	 */
	private HashMap<PartsCategory, List<PartsIdentifier>> parts = new HashMap<PartsCategory, List<PartsIdentifier>>();
	
	/**
	 * パーツに対するカラー情報.<br>
	 * かならずしも、パーツに対してカラー情報を設定する必要はない.<br>
	 * カラー情報がない場合は空.<br>
	 */
	private HashMap<PartsIdentifier, PartsColorInfo> partsColorInfoMap = new HashMap<PartsIdentifier, PartsColorInfo>();

	/**
	 * 無名、空のパーツセットを作成する.
	 */
	public PartsSet() {
		this(null, null, false);
	}

	/**
	 * 名前つきパーツセットを作成する.<br>
	 * 
	 * @param partsSetId
	 *            パーツセットID
	 * @param localizedName
	 *            表示名
	 * @param presetParts
	 *            プリセットフラグ
	 */
	public PartsSet(String partsSetId, String localizedName, boolean presetParts) {
		this.partsSetId = partsSetId;
		this.localizedName = localizedName;
		this.presetParts = presetParts;
	}
	
	/**
	 * パーツセットをディープコピーする.<br>
	 * 現在のキャラクターデータのカテゴリインスタンスに関連づけて再生させる場合は、
	 * resolverに現在のキャラクターデータのカテゴリリゾルバを指定します.<br>
	 * 
	 * @param org
	 *            元オブジェクト
	 * @param resolver
	 *            パーツセットのカテゴリを再生するためのリゾルバ、再生する必要がなければnull可
	 */
	protected PartsSet(PartsSet org, PartsCategoryResolver resolver) {
		if (org == null) {
			throw new IllegalArgumentException();
		}
		this.partsSetId = org.partsSetId;
		this.localizedName = org.localizedName;
		this.presetParts = org.presetParts;
		this.bgColor = org.bgColor;
		this.affineTransformParameter = org.affineTransformParameter == null ? null : org.affineTransformParameter.clone();
		
		// ColorInfoMapの正規化
		for (Map.Entry<PartsIdentifier, PartsColorInfo> partsColorInfoEntry : org.partsColorInfoMap.entrySet()) {
			PartsIdentifier partsIdentifier = partsColorInfoEntry.getKey();
			if (resolver != null) {
				PartsCategory orgPartsCategory = partsIdentifier.getPartsCategory(); 
				PartsCategory repPartsCategory = resolver.getPartsCategory(orgPartsCategory.getCategoryId());
				if (repPartsCategory == null) {
					// 同一IDのカテゴリがリゾルバになければ、このパーツは無かったことにする.
					continue;
				}
				if (orgPartsCategory != repPartsCategory) {
					// インスタンスが一致しなければリゾルバ側の結果を優先する.
					partsIdentifier = new PartsIdentifier(
							repPartsCategory,
							partsIdentifier.getPartsName(),
							partsIdentifier.getLocalizedPartsName());
				}
			}
			PartsCategory repPartsCategory = partsIdentifier.getPartsCategory();
			PartsColorInfo copiedPartsColorInfo = partsColorInfoEntry
					.getValue().createCompatible(repPartsCategory);
			partsColorInfoMap.put(partsIdentifier, copiedPartsColorInfo);
		}
		
		// PartsIdentifierの正規化
		for (Map.Entry<PartsCategory, List<PartsIdentifier>> partsEntry : org.parts.entrySet()) {
			PartsCategory orgPartsCategory = partsEntry.getKey();
			PartsCategory partsCategory = orgPartsCategory;
			if (resolver != null) {
				PartsCategory repPartsCategory = resolver.getPartsCategory(orgPartsCategory.getCategoryId());
				if (repPartsCategory == null) {
					// 同一IDのカテゴリがリゾルバになければ、このカテゴリはなかったことにする.
					continue;
				}
				if (repPartsCategory != orgPartsCategory) {
					// インスタンスが一致しなければリゾルバ側の結果を優先する.
					partsCategory = repPartsCategory;
				}
			}
			ArrayList<PartsIdentifier> partsIdentifiers = new ArrayList<PartsIdentifier>(partsEntry.getValue());
			parts.put(partsCategory, partsIdentifiers);
		}
	}
	
	/**
	 * リゾルバを使用してパーツカテゴリ(パーツ識別子のカテゴリも含む)のインスタンスを入れ替えます.<br>
	 * パーツセットはキャラクターデータとは独立してロード・セーブされることがあるため、同じ情報でも異なるインスタンスとなる場合があり、
	 * これを是正するために、このメソッドを使用します.<br>
	 * リゾルバから取得できないパーツカテゴリは除去されます.<br>
	 * 
	 * @param resolver
	 *            リゾルバ
	 * @return 互換性のあるパーツセット
	 */
	public PartsSet createCompatible(PartsCategoryResolver resolver) {
		if (resolver == null) {
			throw new IllegalArgumentException();
		}
		// XXX: 本当は、こんなことはしたくない。
		return new PartsSet(this, resolver);
	}

	/**
	 * パーツセットをディープコピーする.<br>
	 * 
	 * @return コピーされたパーツセット
	 */
	@Override
	public PartsSet clone() {
		return new PartsSet(this, null);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ (partsSetId == null ? 0 : partsSetId.hashCode());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (o != null && o instanceof PartsSet) {
			PartsSet obj = (PartsSet) o;
			// 双方のIDがnullもしくは、同一インスタンスであるか、ID文字列が等値である場合
			if (partsSetId == obj.partsSetId || (partsSetId != null && partsSetId.equals(obj.partsSetId))) {
				// AbstractMapのequalsでパーツの構成物を比較する.
				if (super.equals(obj)) {
					// カラー定義が等しいか比較する.
					if (partsColorInfoMap.equals(obj.partsColorInfoMap)) {
						// 背景色がともにnullもしくは同一インスタンスであるか、背景色が等値である場合
						if (bgColor == obj.bgColor || (bgColor != null && bgColor.equals(obj.bgColor))) {
							// アフィン変換パラメータがともにnullもしくは同一インスタンスであるか、同値である場合.
							if (affineTransformParameter == obj.affineTransformParameter
									|| (affineTransformParameter != null
											&& Arrays.equals(affineTransformParameter, obj.affineTransformParameter))) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public void setPresetParts(boolean presetParts) {
		this.presetParts = presetParts;
	}
	
	/**
	 * プリセット用パーツセットであるか? これがfalseの場合は一時的なパーツセットか、もしくはお気に入り用である.<br>
	 * 
	 * @return プリセット用パーツセットである場合
	 */
	public boolean isPresetParts() {
		return presetParts;
	}
	
	public void setBgColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	/**
	 * バックグラウンドカラーを取得する.<br>
	 * 設定されていなければnull.<br>
	 * 
	 * @return バックグラウンドカラー、もしくはnull
	 */
	public Color getBgColor() {
		return bgColor;
	}

	/**
	 * アフィン変換用パラメータを指定する.<br>
	 * 配列は4または6でなければならない.<br>
	 * アフィン変換しない場合はnull
	 * 
	 * @param affineTransformParameter
	 *            変換パラメータ(4または6個の要素)、もしくはnull
	 */
	public void setAffineTransformParameter(double[] affineTransformParameter) {
		if (affineTransformParameter != null && !(affineTransformParameter.length == 4 || affineTransformParameter.length == 6)) {
			throw new IllegalArgumentException("affineTransformParameter invalid length.");
		}
		this.affineTransformParameter = affineTransformParameter == null ? null : affineTransformParameter.clone();
	}
	
	/**
	 * アフィン変換用のパラメータを取得する.<br>
	 * 変換しない場合はnull.<br>
	 * 
	 * @return アフィン変換用のパラメータ、またはnull
	 */
	public double[] getAffineTransformParameter() {
		return affineTransformParameter == null ? null : affineTransformParameter.clone();
	}
	
	public void setPartsSetId(String partsSetId) {
		this.partsSetId = partsSetId;
	}
	
	public void setLocalizedName(String localizedName) {
		this.localizedName = localizedName;
	}
	
	/**
	 * プリセットIDを取得する.<br>
	 * 一時的なパーツセットである場合はnull
	 * 
	 * @return プリセットID、またはnull
	 */
	public String getPartsSetId() {
		return partsSetId;
	}

	/**
	 * プリセット名を取得する.<br>
	 * 一時的なパーツセットである場合はnull
	 * 
	 * @return プリセット名、またはnull
	 */
	public String getLocalizedName() {
		return localizedName;
	}
	
	/**
	 * パーツセットのエントリセットを取得する.
	 */
	@Override
	public Set<java.util.Map.Entry<PartsCategory, List<PartsIdentifier>>> entrySet() {
		return parts.entrySet();
	}
	
	/**
	 * パーツカラー情報を取得する.<br>
	 * カラー情報が関連づけられていない場合はnullが返される.<br>
	 * 
	 * @param partsIdentifier
	 *            パーツ識別子
	 * @return カラー情報、もしくはnull
	 */
	public PartsColorInfo getColorInfo(PartsIdentifier partsIdentifier) {
		PartsColorInfo partsColorInfo = partsColorInfoMap.get(partsIdentifier);
		return partsColorInfo == null ? null : partsColorInfo.clone();
	}
	
	/**
	 * カテゴリ別にパーツを登録する.<br>
	 * partsNameがnullまたは空文字の場合はカテゴリのみ登録する.<br>
	 * これにより、そのカテゴリに選択がないことを示す.<br>
	 * 複数選択可能なカテゴリの場合、複数回の呼び出しで登録する.(登録順)<br>
	 * 
	 * @param category
	 *            カテゴリ
	 * @param partsIdentifier
	 *            パーツ識別子、またはnull
	 * @param partsColorInfo
	 *            パーツの色情報、なければnull可
	 */
	public void appendParts(PartsCategory category, PartsIdentifier partsIdentifier, PartsColorInfo partsColorInfo) {
		if (category == null) {
			throw new IllegalArgumentException();
		}
		List<PartsIdentifier> partsIdentifiers = parts.get(category);
		if (partsIdentifiers == null) {
			partsIdentifiers = new ArrayList<PartsIdentifier>();
			parts.put(category, partsIdentifiers);
		}
		if (partsIdentifier != null) {
			partsIdentifiers.add(partsIdentifier);
			if (partsColorInfo != null) {
				partsColorInfoMap.put(partsIdentifier, partsColorInfo.clone());
			}
		}
	}
	
	/**
	 * すべてのパーツのカラー情報を除去する.<br>
	 */
	public void removeColorInfo() {
		partsColorInfoMap.clear();
	}
	
	/**
	 * パーツセットが構造的に一致するか検証します.<br>
	 * nullの場合は常にfalseとなります.<br>
	 * 
	 * @param other
	 *            比較対象、null可
	 * @return パーツ構成が一致すればtrue、そうでなければfalse
	 */
	public boolean isSameStructure(PartsSet other) {
		if (other != null && other.size() == this.size()) {
			// カテゴリが一致し、各カテゴリに登録されているパーツ識別子のリストも順序を含めて一致する場合、
			// 構造的に一致すると判定する.
			for (Map.Entry<PartsCategory, List<PartsIdentifier>> entry : entrySet()) {
				PartsCategory category = entry.getKey();
				List<PartsIdentifier> ownList = entry.getValue();
				List<PartsIdentifier> otherList = other.get(category);
				if (ownList == null || otherList == null || !ownList.equals(otherList)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 2つのパーツセットが構造的に一致するか検証します.<br>
	 * いずれか一方がnullであればfalseを返します.双方がnullであればtrueを返します.<br>
	 * 双方がnullでなければ{@link #isSameStructure(PartsSet)}で判定されます.<br>
	 * 
	 * @param a
	 *            比較対象1、null可
	 * @param b
	 *            比較対象2、null可
	 * @return パーツ構成が一致すればtrue、そうでなければfalse
	 */
	public static boolean isSameStructure(PartsSet a, PartsSet b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.isSameStructure(b);
	}

	/**
	 * 保持しているパーツ識別子のカラー情報と同一のカラー情報をもっているか判定します.<br>
	 * 相手側はカテゴリや順序を問わず、少なくとも自分と同じパーツ識別子をもっていれば足りるため、 パーツ構成が同一であるかの判定は行いません.<br>
	 * パーツ構造を含めて判定を行う場合は事前に{@link #isSameStructure(PartsSet)}を呼び出します.<br>
	 * nullの場合は常にfalseとなります.<br>
	 * 
	 * @param other
	 *            判定先、null可
	 * @return 同一であればtrue、そうでなければfalse
	 */
	public boolean isSameColor(PartsSet other) {
		if (other != null && other.size() == size()) {
			for (List<PartsIdentifier> partsIdentifiers : values()) {
				for (PartsIdentifier partsIdentifier : partsIdentifiers) {
					PartsColorInfo ownColorInfo = getColorInfo(partsIdentifier);
					PartsColorInfo otherColorInfo = other.getColorInfo(partsIdentifier);
					if ( !PartsColorInfo.equals(ownColorInfo, otherColorInfo)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
	
	/**
	 * 引数aが保持しているパーツ識別子のカラー情報と同一のカラー情報を引数bがもっているか判定します.<br>
	 * 引数b側はカテゴリや順序を問わず、少なくとも引数aと同じパーツ識別子をもっていれば足りるため、 パーツ構成が同一であるかの判定は行いません.<br>
	 * パーツ構造を含めて判定を行う場合は事前に{@link #isSameStructure(PartsSet, PartsSet)}を呼び出します.<br>
	 * 双方がnullであればtrueとなります.<br>
	 * いずれか一方がnullの場合はfalseとなります.<br>
	 * 
	 * @param a
	 *            対象1、null可
	 * @param b
	 *            対象2、null可
	 * @return 同一であればtrue、そうでなければfalse
	 */
	public static boolean isSameColor(PartsSet a, PartsSet b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.isSameColor(b);
	}
	
	/**
	 * このパーツセットが名前をもっているか?
	 * 
	 * @return 名前がある場合はtrue、設定されていないか空文字の場合はfalse
	 */
	public boolean hasName() {
		return localizedName != null && localizedName.length() > 0;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)));
		buf.append("(");
		buf.append("partsSetId: " + partsSetId + ", ");
		buf.append("localizedName: " + localizedName + ", ");
		buf.append("presetFlg: " + presetParts + ", ");
		buf.append("background-color: " + bgColor + ", ");
		buf.append("affin-trans-param: " + Arrays.toString(affineTransformParameter) + ", ");
		buf.append("parts: " + parts + ", ");
		buf.append("partsColorMap: " + partsColorInfoMap);
		buf.append(")");
		return buf.toString();
	}
	
}
