package charactermanaj.model.io;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

import charactermanaj.model.CharacterData;
import charactermanaj.model.PartsManageData;
import charactermanaj.model.io.AbstractCharacterDataArchiveFile.PartsImageContent;


/**
 * アーカイブ形式のファイルを読み取るためのインターフェイス.<br>
 * @author seraphy
 */
public interface CharacterDataArchiveFile {

	/**
	 * クローズする.
	 * @throws IOException
	 */
	void close() throws IOException;
	
	/**
	 * コンテンツルートへのプレフィックスを取得する.<br>
	 * アーカイブの実際のルートに単一のフォルダしかない場合、そのフォルダがルートプレフィックスとなる.<br>
	 * アーカイブの実際のルートに複数のフォルダがあるか、ファイルがある場合、ルートプレフィックスは空文字である.<br>
	 * @return コンテンツルートへのプレフィックス
	 */
	String getRootPrefix();
	
	/**
	 * 指定したコンテンツが存在するか?
	 * @param name コンテンツ名
	 * @return 存在すればtrue、存在しなければfalse
	 */
	boolean hasContent(String name);
	
	/**
	 * キャラクター定義を読み込む.<br>
	 * アーカイブに存在しなければnull
	 * @return キャラクター定義、もしくはnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	CharacterData readCharacterData() throws IOException;
	
	/**
	 * お気に入りを読み込みキャラクター定義に追加する.<br>
	 * アーカイブにお気に入りが存在しなければ何もしない.<br>
	 * @param characterData キャラクター定義(お気に入りが追加される)
	 * @throws IOException 読み取りに失敗した場合
	 */
	void readFavorites(CharacterData characterData) throws IOException;
	
	/**
	 * キャラクター定義をINIファイルより読み取る.<br>
	 * character.iniが存在しなければnull.<br>
	 * 「キャラクターなんとか機」の設定ファイルを想定している.<br>
	 * @return キャラクター定義、もしくはnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	CharacterData readCharacterINI() throws IOException;
	
	/**
	 * サンプルピクチャを読み込む.<br>
	 * アーカイブに存在しなければnull.
	 * @return サンプルピクチャ、もしくはnull
	 * @throws IOException 読み取りに失敗した場合
	 */
	BufferedImage readSamplePicture() throws IOException;
	
	/**
	 * アーカイブにある「readme.txt」、もしくは「readme」というファイルをテキストファイルとして読み込む.<br>
	 * readme.txtが優先される。ともに存在しない場合はnull. 
	 * @return テキスト、もしくはnull
	 * @throws 読み込みに失敗した場合
	 */
	String readReadMe() throws IOException;
	
	/**
	 * ファイルをテキストとして読み取り、返す.<br>
	 * UTF-16LE/BE/UTF-8についてはBOMにより判定する.<br>
	 * BOMがない場合はUTF-16/8ともに判定できない.<br>
	 * BOMがなければMS932もしくはEUC_JPであると仮定して読み込む.<br>
	 * @param name コンテンツ名 
	 * @return テキスト、コンテンツが存在しない場合はnull
	 * @throws IOException 読み込みに失敗した場合
	 */
	String readTextFile(String name) throws IOException;
	
	/**
	 * アーカイブに含まれる、キャラクター定義と同じフォルダをもつpngファイルからパーツイメージを取得する.<br>
	 * パーツの探索は引数で指定したキャラクター定義によって行われます.<br>
	 * @param characterData インポート先のキャラクターデータ、フォルダ名などを判別するため。nullの場合は空のマップを返す.<br>
	 * @param newly 新規インポート用であるか?(新規でない場合は引数で指定したキャラクターセットと同じパーツは読み込まれない)
	 * @return パーツイメージコンテンツのコレクション、なければ空
	 */
	Collection<PartsImageContent> getPartsImageContents(CharacterData characterData, boolean newly);
	
	/**
	 * アーカイブに含まれるparts-info.xmlを読み込み返す.<br>
	 * 存在しなければ空のインスタンスを返す.<br>
	 * @return パーツ管理データ
	 * @throws IOException
	 */
	PartsManageData getPartsManageData() throws IOException;
}
