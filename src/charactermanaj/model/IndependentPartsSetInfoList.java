package charactermanaj.model;

import java.util.ArrayList;

/**
 * 素のパーツセットのコレクション.<br>
 * レイヤーやカテゴリなどのリレーションシップがない、<br>
 * 特定のキャラクターデータモデルのツリーの一部には組み込まれていない状態のもの.<br>
 */
public class IndependentPartsSetInfoList
		extends
			ArrayList<IndependentPartsSetInfo> {

	/**
	 * シリアライズバージョンID
	 */
	private static final long serialVersionUID = -6121741586284912547L;

	/**
	 * デフォルトのパーツセットID.<br>
	 * ない場合はnull.<br>
	 */
	private String defaultPresetId;

	public String getDefaultPresetId() {
		return defaultPresetId;
	}

	/**
	 * デフォルトパーツセットIDを設定する.<br>
	 * nullはパーツセットIDがないことを示す.<br>
	 * 空文字はnullとみなされる.<br>
	 * 
	 * @param defaultPresetId
	 */
	public void setDefaultPresetId(String defaultPresetId) {
		if (defaultPresetId != null) {
			defaultPresetId = defaultPresetId.trim();
			if (defaultPresetId.length() == 0) {
				// デフォルトパーツセットがないことを示すためのnull
				defaultPresetId = null;
			}
		}
		this.defaultPresetId = defaultPresetId;
	}

	@Override
	public boolean add(IndependentPartsSetInfo o) {
		if (o == null) {
			throw new IllegalArgumentException();
		}
		return super.add(o);
	}

	@Override
	public IndependentPartsSetInfo set(int index,
			IndependentPartsSetInfo element) {
		if (element == null) {
			throw new IllegalArgumentException();
		}
		return super.set(index, element);
	}
}