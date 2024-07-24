package sstrange;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import sstrange.language.java.JavaFeedbackGenerator;
import sstrange.language.python.PythonFeedbackGenerator;
import sstrange.token.FeedbackToken;

public class STRANGEPairGenerator {
	public static ArrayList<FeedbackToken> getTokenString(String filepath, String progLang) {
		if (progLang.equals("java")) {
			ArrayList<FeedbackToken> tokens = JavaFeedbackGenerator.generateFeedbackTokenString(filepath);
			// remove all white space and comments
			for (int i = 0; i < tokens.size(); i++) {
				String type = tokens.get(i).getType();
				if (type.equals("WS") || type.equals("COMMENT")) {
					tokens.remove(i);
					i--;
				}
			}
			return tokens;
		} else if (progLang.equals("py")) {
			return PythonFeedbackGenerator.generateSyntaxTokenString(filepath);
		} else
			return null;
	}

	public static void generateAdditionalDir(String targetDirPath) throws Exception {
		// create the dir
		File dir = new File(targetDirPath + File.separator + "strange_html_layout_additional_files");
		if (dir.exists() == false)
			dir.mkdir();

		// copy icon
		File source = new File(MainFrame.additional_dir_path + File.separator + "icon.png");
		File target = new File(dir.getAbsolutePath() + File.separator + "icon.png");
		if (target.exists() == false)
			Files.copy(source.toPath(), target.toPath());

		// copy logo
		source = new File(MainFrame.additional_dir_path + File.separator + "logo.png");
		target = new File(dir.getAbsolutePath() + File.separator + "logo.png");
		if (target.exists() == false)
			Files.copy(source.toPath(), target.toPath());

		// copy sort icon
		source = new File(MainFrame.additional_dir_path + File.separator + "sort icon.png");
		target = new File(dir.getAbsolutePath() + File.separator + "sort icon.png");
		if (target.exists() == false)
			Files.copy(source.toPath(), target.toPath());

		// copy jquery
		source = new File(MainFrame.additional_dir_path + File.separator + "run_prettify.js");
		target = new File(dir.getAbsolutePath() + File.separator + "run_prettify.js");
		if (target.exists() == false)
			Files.copy(source.toPath(), target.toPath());
	}
}
