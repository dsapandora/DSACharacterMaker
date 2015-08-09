package charactermanaj.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import charactermanaj.graphics.filters.ColorConvertParameter;
import charactermanaj.model.AppConfig;
import charactermanaj.model.ColorGroup;
import charactermanaj.model.ColorInfo;
import charactermanaj.model.Layer;
import charactermanaj.model.PartsColorInfo;
import charactermanaj.model.PartsColorManager;
import charactermanaj.model.PartsIdentifier;
import charactermanaj.model.PartsSpec;
import charactermanaj.model.PartsSpecResolver;
import charactermanaj.ui.ColorDialog;
import charactermanaj.ui.ImageSelectPanel;
import charactermanaj.ui.ImageSelectPanel.ImageSelectPanelEvent;
import charactermanaj.ui.ImageSelectPanel.ImageSelectPanelListener;

/**
 * パーツの選択パネルとカラーダイアログを関連づけて調整するコーディネータオブジェクト.<br>
 * @author seraphy
 *
 */
public class PartsColorCoordinator {

	/**
	 * ロガー
	 */
	private static final Logger logger = Logger.getLogger(PartsColorCoordinator.class.getName());
	
	/**
	 * パーツ選択パネルとカラーダイアログの関係を示すマップ.<br>
	 */
	private IdentityHashMap<ImageSelectPanel, ColorDialog> colorDialogMap
			= new IdentityHashMap<ImageSelectPanel, ColorDialog>();
	
	/**
	 * パーツ識別子ごとのカラー情報を保存するパーツカラーマネージャ.<br>
	 */
	private PartsColorManager partsColorMrg;
	
	/**
	 * パーツ設定
	 */
	private PartsSpecResolver partsSpecResolver;
	
	/**
	 * 同一の色グループに設定値を同期させるためのコーディネータ.<br>
	 */
	private ColorGroupCoordinator colorGroupCoordinator;

	/**
	 * コンストラクタ.<br>
	 * @param partsSpecResolver パーツ
	 * @param partsColorMrg パーツ識別子ごとの色情報を管理するオブジェクト
	 * @param colorGroupCoordinator 同一の色グループに設定値を同期させるためのコーディネータ.
	 */
	public PartsColorCoordinator(PartsSpecResolver partsSpecResolver,
			PartsColorManager partsColorMrg,
			ColorGroupCoordinator colorGroupCoordinator) {
		if (partsSpecResolver == null || partsColorMrg == null || colorGroupCoordinator == null) {
			throw new IllegalArgumentException();
		}
		this.partsSpecResolver = partsSpecResolver;
		this.partsColorMrg = partsColorMrg;
		this.colorGroupCoordinator = colorGroupCoordinator;
	}
	
	/**
	 * パーツ選択パネルと色ダイアログの関係を登録する.<br>
	 * @param imageSelectPanel パーツ選択パネル
	 * @param colorDialog 色ダイアログ
	 */
	public void register(final ImageSelectPanel imageSelectPanel, final ColorDialog colorDialog) {
		if (imageSelectPanel == null || colorDialog == null) {
			throw new IllegalArgumentException();
		}
		if (colorDialogMap.containsKey(imageSelectPanel)) {
			throw new IllegalArgumentException("already registered: " + imageSelectPanel);
		}
		colorDialogMap.put(imageSelectPanel, colorDialog);
		
		imageSelectPanel.addImageSelectListener(new ImageSelectPanelListener() {
			public void onChangeColor(ImageSelectPanelEvent event) {
				// なにもしない
			}
			public void onPreferences(ImageSelectPanelEvent event) {
				// なにもしない
			}
			public void onChange(ImageSelectPanelEvent event) {
				PartsColorCoordinator.this.loadColorSettingToColorDialog(imageSelectPanel, colorDialog);
			}
			public void onSelectChange(ImageSelectPanelEvent event) {
				PartsColorCoordinator.this.loadColorSettingToColorDialog(imageSelectPanel, colorDialog);
			}
			public void onTitleClick(ImageSelectPanelEvent event) {
				// なにもしない
			}
			public void onTitleDblClick(ImageSelectPanelEvent event) {
				// なにもしない
			}
		});
		
		colorDialog.addColorChangeListener(new ColorChangeListener() {
			public void onColorChange(ColorChangeEvent event) {
				saveColorSettingAll();
			}
			public void onColorGroupChange(ColorChangeEvent event) {
				saveColorSettingAll();
			}
		});
	}
	
	/**
	 * パーツ選択パネルの現在の選択に対する保存されているカラー情報を色ダイアログに設定する.<br>
	 * @param imageSelectPanel パーツ選択パネル
	 * @param colorDialog 色ダイアログ
	 */
	protected void loadColorSettingToColorDialog(ImageSelectPanel imageSelectPanel, ColorDialog colorDialog) {
		PartsIdentifier selectedParts = imageSelectPanel.getSelectedPartsIdentifier();
		
		// 選択されているパーツのパーツ名と有効なレイヤーをカラーダイアログに設定する.
		// 選択されているパーツがない場合はデフォルトに戻す.
		colorDialog.setPartsIdentifier(selectedParts);
		colorDialog.setEnableLayers(getEnabledLayers(selectedParts));
		
		if (selectedParts == null) {
			// 選択されているパーツがない場合は、ここまで。
			return;
		}

		PartsColorInfo partsColorInfo = partsColorMrg.getPartsColorInfo(selectedParts, false);
		
		for (Map.Entry<Layer, ColorInfo> entry : partsColorInfo.entrySet()) {
			Layer layer = entry.getKey();
			ColorInfo colorInfo = entry.getValue();
			
			ColorGroup colorGroup = colorInfo.getColorGroup();
			if (colorGroup == null) {
				colorGroup = ColorGroup.NA;
			}
			colorDialog.setColorGroup(layer, colorGroup);
			boolean syncColorGroup = colorInfo.isSyncColorGroup();
			colorDialog.setSyncColorGroup(layer, syncColorGroup);

			ColorConvertParameter param = colorInfo.getColorParameter();
			colorDialog.setColorConvertParameter(layer, param);
			
			if (syncColorGroup) {
				colorGroupCoordinator.syncColorGroup(colorDialog.getPartsCategory(), layer, colorDialog);
			}
		}
	}

	/**
	 * 現在選択中のパーツの組み合わせに対応するカラーダイアログの設定情報をPartsColorManagerに保存する.<br>
	 * (カラーダイアログの値が変更されるたびに呼び出される.)<br>
	 */
	protected void saveColorSettingAll() {
		for (Map.Entry<ImageSelectPanel, ColorDialog> entry : colorDialogMap.entrySet()) {
			ImageSelectPanel imageSelectPanel = entry.getKey();
			ColorDialog colorDialog = entry.getValue();
			saveColorSettingFromColorDialog(imageSelectPanel, colorDialog);
		}
	}
	
	/**
	 * カテゴリべつの現在選択中のパーツ識別子と、それに対応するカラーダイアログの設定値をPartsColorManagerに保存する.<br>
	 * @param imageSelectPanel カテゴリ別のパーツ選択
	 * @param colorDialog 対応するカラーダイアログ
	 */
	protected void saveColorSettingFromColorDialog(ImageSelectPanel imageSelectPanel, ColorDialog colorDialog) {
		PartsIdentifier selectedParts = imageSelectPanel.getSelectedPartsIdentifier();
		if (selectedParts == null) {
			return;
		}

		Map<Layer, ColorConvertParameter> paramMap = colorDialog.getColorConvertParameters();
		
		PartsColorInfo partsColorInfo = partsColorMrg.getPartsColorInfo(selectedParts, true);
		for (Map.Entry<Layer, ColorConvertParameter> entry : paramMap.entrySet()) {
			Layer layer = entry.getKey();
			ColorConvertParameter param = entry.getValue();

			ColorInfo colorInfo = new ColorInfo();
			ColorGroup colorGroup = colorDialog.getColorGroup(layer);
			colorInfo.setColorGroup(colorGroup);
			boolean syncColorGroup = colorDialog.isSyncColorGroup(layer);
			colorInfo.setSyncColorGroup(syncColorGroup);
			colorInfo.setColorParameter(param);
			partsColorInfo.put(layer, colorInfo);
		}
		boolean applyAll = colorDialog.isApplyAll();
		partsColorMrg.setPartsColorInfo(selectedParts, partsColorInfo, applyAll);
	}
	
	/**
	 * 全カラーダイアログで設定されている各レイヤーごとの色パラメータを全て取得する.<br>
	 * @return 各レイヤーごとの色パラメータ
	 */
	public Map<Layer, ColorConvertParameter> getColorConvertParameterMap() {
		final HashMap<Layer, ColorConvertParameter> colorConvertParameterMap = new HashMap<Layer, ColorConvertParameter>();
		for (ColorDialog colorDlg : colorDialogMap.values()) {
			for (Map.Entry<Layer, ColorConvertParameter> entry : colorDlg.getColorConvertParameters().entrySet()) {
				Layer layer = entry.getKey();
				ColorConvertParameter colorConvertParameter = entry.getValue();
				colorConvertParameterMap.put(layer, colorConvertParameter);
			}
		}
		return colorConvertParameterMap;
	}
	
	/**
	 * 現在選択されている各カテゴリのパーツの組み合わせに対するカラー情報に各カテゴリの色ダイアログを設定する.<br>
	 * (選択中のパーツ名も設定される.)<br>
	 */
	public void initColorDialog() {
		for (Map.Entry<ImageSelectPanel, ColorDialog> entry : colorDialogMap.entrySet()) {
			ImageSelectPanel imageSelectPanel = entry.getKey();
			ColorDialog colorDialog = entry.getValue();

			PartsIdentifier partsIdentifier = imageSelectPanel.getSelectedPartsIdentifier();

			// 選択されているパーツのパーツ名と有効なレイヤーをカラーダイアログに設定する.
			// 選択されているパーツがない場合はデフォルトに戻す.
			colorDialog.setPartsIdentifier(partsIdentifier);
			colorDialog.setEnableLayers(getEnabledLayers(partsIdentifier));
			
			if (partsIdentifier != null) {
				PartsColorInfo partsColorInfo = partsColorMrg.getPartsColorInfo(partsIdentifier, false);
				for (Map.Entry<Layer, ColorInfo> colorInfoEntry : partsColorInfo.entrySet()) {
					Layer layer = colorInfoEntry.getKey();
					ColorInfo colorInfo = colorInfoEntry.getValue();
					if (logger.isLoggable(Level.FINE)) {
						logger.log(Level.FINE, layer + "=" + colorInfo);
					}
					colorDialog.setColorGroup(layer, colorInfo.getColorGroup());
					colorDialog.setSyncColorGroup(layer, colorInfo.isSyncColorGroup());
					colorDialog.setColorConvertParameter(layer, colorInfo.getColorParameter());
				}
			}
		}
	}

	/**
	 * 指定したパーツ識別子を構成する実際に存在するレイヤーを返します.<br>
	 * (カテゴリが2つ以上のレイヤーをもっている場合でもパーツが、カテゴリのレイヤー数を全て使い切っていない場合は、使っているレイヤーのみが返されます.)<br>
	 * 指定したパーツ識別子がnullの場合はnullを返します.<br>
	 * 指定したパーツ識別子がnullではなく、且つ、パーツリゾルバから取得できない場合は空が返されます.<br>
	 * ただし、アプリケーション設定で「存在しないレイヤーをディセーブルにしない」がtrueであれば、常にnullを返します.<br>
	 * @param partsIdentifier パーツ識別子、もしくはnull
	 * @return レイヤーのコレクション、もしくはnull
	 */
	protected Collection<Layer> getEnabledLayers(PartsIdentifier partsIdentifier) {
		AppConfig appConfig = AppConfig.getInstance();
		if (appConfig.isNotDisableLayerTab()) {
			return null;
		}

		if (partsIdentifier != null) {
			PartsSpec partsSpec = partsSpecResolver.getPartsSpec(partsIdentifier);
			ArrayList<Layer> layers = new ArrayList<Layer>();
			if (partsSpec != null) {
				for (Layer layer : partsSpec.getPartsFiles().keySet()) {
					layers.add(layer);
				}
			}
			return layers;
		}
		return null;
	}
	
}
