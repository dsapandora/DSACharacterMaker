package charactermanaj.graphics.io;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

/**
 * ロードされたイメージ情報
 * @author seraphy
 */
public final class LoadedImage {

	private final BufferedImage image;

	private final long lastModified;

	private final int imageSize;

	public LoadedImage(BufferedImage image, long lastModified) {
		this.image = image;
		this.lastModified = lastModified;
		this.imageSize = getBufferSize(image);
	}

	public BufferedImage getImage() {
		return image;
	}

	public long getLastModified() {
		return lastModified;
	}

	public int getImageSize() {
        return imageSize;
    }

	/**
	 * 画像バッファのバイト数を求める.<br>
	 * @param image イメージ
	 * @return バイト数
	 */
	private static int getBufferSize(BufferedImage image) {
	    if (image == null) {
	        return 0;
	    }
	    DataBuffer buff = image.getRaster().getDataBuffer();
	    int bytes = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
	    return bytes;
	}
}
