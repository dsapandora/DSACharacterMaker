package charactermanaj.graphics.io;

import java.awt.image.BufferedImage;

public abstract class OutputImageBuilder {

	private OutputOption outputOption;
	
	public OutputImageBuilder(OutputOption outputOption) {
		if (outputOption == null) {
			throw new IllegalArgumentException();
		}
		this.outputOption = outputOption;
	}
	
	public OutputOption getOutputOption() {
		return outputOption.clone();
	}

	public abstract BufferedImage buildImage(BufferedImage src);
	
}
