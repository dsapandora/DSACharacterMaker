package charactermanaj.graphics.io;

import charactermanaj.model.AppConfig;

public class OutputImageBuilderFactory {

	public OutputOption createDefaultOutputOption() {
		AppConfig appConfig = AppConfig.getInstance();

		OutputOption outputOption = new OutputOption();
		outputOption.setJpegQuality(appConfig.getCompressionQuality());

		return outputOption;
	}
	
	public OutputImageBuilder createOutputImageBuilder(OutputOption outputOption) {
		if (outputOption == null) {
			outputOption = createDefaultOutputOption();
		}

		return null;
	}
}
