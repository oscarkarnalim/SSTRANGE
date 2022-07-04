package sstrange;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

import p3.feedbackgenerator.language.java.JavaFeedbackGenerator;
import p3.feedbackgenerator.language.python.PythonFeedbackGenerator;
import p3.feedbackgenerator.message.FeedbackMessageGenerator;
import p3.feedbackgenerator.token.FeedbackToken;
import sstrange.matchgenerator.MatchGenerator;
import support.stringmatching.GSTMatchTuple;

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

	public static int getSTRANGESim(ArrayList<FeedbackToken> tokenString1, ArrayList<FeedbackToken> tokenString2,
			int minimumMatchLength) {
		return getSTRANGESim(tokenString1, tokenString2, minimumMatchLength,false,false);
	}

	public static int getSTRANGESim(ArrayList<FeedbackToken> tokenString1, ArrayList<FeedbackToken> tokenString2,
			int minimumMatchLength, boolean isIDFWeighted, boolean isLengthWeighted) {

		// get matched tiles with RKRGST
		ArrayList<GSTMatchTuple> simTuples = FeedbackMessageGenerator.generateMatchedTuples(tokenString1, tokenString2,
				minimumMatchLength);

		// calculate similarity based on weight
		double sim = 0;
		if (isIDFWeighted && isLengthWeighted)
			sim = SimWeighter.calcIDFLengthWeightedSim(simTuples, tokenString1, tokenString2);
		else if (isIDFWeighted)
			sim = SimWeighter.calcIDFWeightedSim(simTuples, tokenString1, tokenString2);
		else if (isLengthWeighted)
			sim = SimWeighter.calcLengthWeightedSim(simTuples, tokenString1, tokenString2);
		else
			sim = MatchGenerator.calcAverageSimilarity(simTuples, tokenString1.size(), tokenString2.size());
		
		return (int) (sim * 100); // times 100 just to make the percentage comparable with that of JPlag

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
