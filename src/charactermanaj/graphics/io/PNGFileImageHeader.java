package charactermanaj.graphics.io;

/**
 * PNGヘッダ情報.<br>
 * http://en.wikipedia.org/wiki/Portable_Network_Graphics#cite_note-4 ,
 * http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html#C.IHDR あたりを参照.<br>
 * @author seraphy
 */
public class PNGFileImageHeader {

	private int width; // 4bytes
	
	private int height; // 4bytes
	
	private int bitDepth; // 1byte
	
	private int colorType; // 1byte

	private int compressionMethod; // 1byte
	
	private int filterMethod; // 1byte
	
	private int interlaceMethod; // 1byte
	
	private boolean transparencyInformation;

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getBitDepth() {
		return bitDepth;
	}

	public void setBitDepth(int bitDepth) {
		this.bitDepth = bitDepth;
	}

	/**
	 * カラータイプを取得する.<br>
	 * Bit0がインデックス or グレースケール、Bit1がカラー、Bit2がアルファ.<br>
	 * その組み合わせで8通りあるが、1、5、7はサポート外となる.<br>
	 * <ul>
	 * <li>0: greyscale</li>
	 * <li>2: Truecolor (Color)</li>
	 * <li>3: Indexed (Color | Palette)</li>
	 * <li>4: greyscale alpha (Alpha)</li>
	 * <li>6: Alpha Color (Color | Alpha) (CharacterManaJでは、これを想定する.)</li>
	 * </ul>
	 * @return
	 */
	public int getColorType() {
		return colorType;
	}

	public void setColorType(int colorType) {
		this.colorType = colorType;
	}

	public int getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public int getFilterMethod() {
		return filterMethod;
	}

	public void setFilterMethod(int filterMethod) {
		this.filterMethod = filterMethod;
	}

	public int getInterlaceMethod() {
		return interlaceMethod;
	}

	public void setInterlaceMethod(int interlaceMethod) {
		this.interlaceMethod = interlaceMethod;
	}
	
	public void setTransparencyInformation(boolean hasTransparencyInformation) {
		this.transparencyInformation = hasTransparencyInformation;
	}
	
	/**
	 * 透過情報があるか?<br>
	 * ColorTypeが3(Indexed Color), 2(TrueColor)で、透過情報がある場合は、透過情報つきカラーである.<br>
	 * そうでなければ透過なしカラーである.<br>
	 * @return
	 */
	public boolean hasTransparencyInformation() {
		return transparencyInformation;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("PNG(widht:" + width + ", height:" + height);
		buf.append(", bitDepth:" + bitDepth + ", colorType: " + colorType);
		buf.append(", hasTransparency: " + transparencyInformation);
		buf.append(", compressionMethod:" + compressionMethod);
		buf.append(", filterMethod:" + filterMethod);
		buf.append(", interlaceMethod:" + interlaceMethod);
		buf.append(")");
		return buf.toString();
	}
	
}
