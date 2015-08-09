package charactermanaj.graphics.filters;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.Serializable;

/**
 * カラー情報.<br>
 * RGB置換、GrayLevel、RGBオフセット各種、HSB調整値を持つ.<br>
 * @author seraphy
 */
public final class ColorConvertParameter implements Serializable, Cloneable {

	private static final long serialVersionUID = 3092895708547846162L;

	/**
	 * 色置換パターン
	 */
	private ColorConv rgbChanelMixierPattern;

	/**
	 * グレーレベル(0でモノトーン、1でノーマル、0.5で半分ほどモノトーン化)
	 */
	private float grayLevel = 1.f;
	
	private float hue = 0.f;
	
	private float saturation = 0.f;

	private float brightness = 0.f;
	
	private float contrast = 0.f;

	private int offsetR = 0;

	private int offsetG = 0;

	private int offsetB = 0;

	private int offsetA = 0;
	
	private float factorR = 1.f;

	private float factorG = 1.f;

	private float factorB = 1.f;

	private float factorA = 1.f;
	
	private float gammaR = 1.f;

	private float gammaG = 1.f;

	private float gammaB = 1.f;

	private float gammaA = 1.f;
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		GetField fields = stream.readFields();
		this.rgbChanelMixierPattern = (ColorConv) fields.get("rgbChanelMixierPattern", ColorConv.NONE);
		this.grayLevel = fields.get("grayLevel", 1.f);
		this.hue = fields.get("hue", 0.f);
		this.saturation = fields.get("saturation", 0.f);
		this.brightness = fields.get("brightness", 0.f);
		this.contrast = fields.get("contrast", 0.f);
		this.offsetR = fields.get("offsetR", 0);
		this.offsetG = fields.get("offsetG", 0);
		this.offsetB = fields.get("offsetB", 0);
		this.offsetA = fields.get("offsetA", 0);
		this.factorR = fields.get("factorR", 1.f);
		this.factorG = fields.get("factorG", 1.f);
		this.factorB = fields.get("factorB", 1.f);
		this.factorA = fields.get("factorA", 1.f);
		this.gammaR = fields.get("gammaR", 1.f);
		this.gammaG = fields.get("gammaG", 1.f);
		this.gammaB = fields.get("gammaB", 1.f);
		this.gammaA = fields.get("gammaA", 1.f);
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if (rgbChanelMixierPattern != null) {
			ret = rgbChanelMixierPattern.ordinal();
		}
		ret ^= (int)(grayLevel * 100);
		ret ^= (int)(hue * 100);
		ret ^= (int)(saturation * 100);
		ret ^= (int)(brightness * 100);
		ret ^= (int)(contrast * 100);
		ret ^= offsetR;
		ret ^= offsetG;
		ret ^= offsetB;
		ret ^= offsetA;
		ret ^= (int)(factorR * 100);
		ret ^= (int)(factorG * 100);
		ret ^= (int)(factorB * 100);
		ret ^= (int)(factorA * 100);
		ret ^= (int)(gammaR * 100);
		ret ^= (int)(gammaG * 100);
		ret ^= (int)(gammaB * 100);
		ret ^= (int)(gammaA * 100);
		return ret;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof ColorConvertParameter) {
			ColorConvertParameter o = (ColorConvertParameter) obj;
			return rgbChanelMixierPattern == o.rgbChanelMixierPattern && grayLevel == o.grayLevel
					&& hue == o.hue && saturation == o.saturation && contrast == o.contrast
					&& brightness == o.brightness && offsetR == o.offsetR
					&& offsetG == o.offsetG && offsetB == o.offsetB
					&& offsetA == o.offsetA && factorR == o.factorR
					&& factorG == o.factorG && factorB == o.factorB
					&& factorA == o.factorA && gammaR == o.gammaR
					&& gammaG == o.gammaG && gammaB == o.gammaB
					&& gammaA == o.gammaA;
		}
		return false;
	}
	
	public static boolean equals(ColorConvertParameter a, ColorConvertParameter b) {
		if (a == b) {
			return true;
		}
		if (a != null && b != null) {
			return a.equals(b);
		}
		return false;
	}
	
	@Override
	public ColorConvertParameter clone() {
		try {
			// シャローコピー. すべてimmutableな単純型だけのメンバなので問題なし.
			return (ColorConvertParameter) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new Error("internal error.");
		}
	}
	
	public ColorConv getColorReplace() {
		return rgbChanelMixierPattern;
	}

	public void setColorReplace(ColorConv colorReplace) {
		this.rgbChanelMixierPattern = colorReplace;
	}

	public float getGrayLevel() {
		return grayLevel;
	}

	public void setGrayLevel(float grayLevel) {
		this.grayLevel = grayLevel;
	}

	public float getHue() {
		return hue;
	}

	public void setHue(float hue) {
		this.hue = hue;
	}

	public float getSaturation() {
		return saturation;
	}

	public void setSaturation(float saturation) {
		this.saturation = saturation;
	}

	public float getBrightness() {
		return brightness;
	}

	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}
	
	public float getContrast() {
		return contrast;
	}
	
	public void setContrast(float contrast) {
		this.contrast = contrast;
	}

	public int getOffsetR() {
		return offsetR;
	}

	public void setOffsetR(int offsetR) {
		this.offsetR = offsetR;
	}

	public int getOffsetG() {
		return offsetG;
	}

	public void setOffsetG(int offsetG) {
		this.offsetG = offsetG;
	}

	public int getOffsetB() {
		return offsetB;
	}

	public void setOffsetB(int offsetB) {
		this.offsetB = offsetB;
	}

	public int getOffsetA() {
		return offsetA;
	}

	public void setOffsetA(int offsetA) {
		this.offsetA = offsetA;
	}

	public float getFactorR() {
		return factorR;
	}

	public void setFactorR(float factorR) {
		this.factorR = factorR;
	}

	public float getFactorG() {
		return factorG;
	}

	public void setFactorG(float factorG) {
		this.factorG = factorG;
	}

	public float getFactorB() {
		return factorB;
	}

	public void setFactorB(float factorB) {
		this.factorB = factorB;
	}

	public float getFactorA() {
		return factorA;
	}

	public void setFactorA(float factorA) {
		this.factorA = factorA;
	}

	public float getGammaR() {
		return gammaR;
	}

	public void setGammaR(float gammaR) {
		this.gammaR = gammaR;
	}

	public float getGammaG() {
		return gammaG;
	}

	public void setGammaG(float gammaG) {
		this.gammaG = gammaG;
	}

	public float getGammaB() {
		return gammaB;
	}

	public void setGammaB(float gammaB) {
		this.gammaB = gammaB;
	}

	public float getGammaA() {
		return gammaA;
	}

	public void setGammaA(float gammaA) {
		this.gammaA = gammaA;
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(getClass().getSimpleName() + "@"	+ Integer.toHexString(System.identityHashCode(this)) + "(");
		buf.append("(replace:" + rgbChanelMixierPattern + ", grayLevel:" + grayLevel + ")");
		buf.append("(H:" + hue + ", S:" + saturation + ", B:" + brightness + ", C:" + contrast +")");
		buf.append("(Red:" + offsetR + "/" + factorR + "/" + gammaR +
				", Green:" + offsetG + "/" + factorG + "/" + gammaG +
				", Blue:" + offsetB + "/" + factorB + "/" + gammaB +
				", Alpha:" + offsetA + "/" + factorA + "/" + gammaA + ")");
		buf.append(")");
		return buf.toString();
	}
	
}
