package sstrange.htmlgenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import sstrange.token.FeedbackToken;

public class UniqueCodeHtmlGenerator {
	/*
	 * this method generates html for overly unique code
	 */
	public static void generateHtmlForSSTRANGE(String codepath1, String dirname1,
			String templateHTMLPath, String outputHTMLPath, String languageCode, double dissimDegree) throws Exception {

		File templateFile = new File(templateHTMLPath);
		File outputFile = new File(outputHTMLPath);
		BufferedReader fr = new BufferedReader(new FileReader(templateFile));
		BufferedWriter fw = new BufferedWriter(new FileWriter(outputFile));
		String line;
		while ((line = fr.readLine()) != null) {
			if (line.contains("@codepath1")) {
				line = line.replace("@codepath1", dirname1 + " ("+ dissimDegree+"% unique)");
			}

			// for default data
			if (line.contains("@code1")) {
				line = line.replace("@code1", readCode1(codepath1));
			}

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}
	

	/*
	 * read code and return that as a string
	 */
	private static String readCode1(String filepath) throws Exception {
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		while ((line = fr.readLine()) != null) {
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));
				code += c;
			}
			code += System.lineSeparator();
		}
		fr.close();
		return code;
	}

	public static String HTMLSafeStringFormat(char in) {
		String out = "";
		if (in == '<')
			out = "&lt;";
		else if (in == '>')
			out = "&gt;";
		else if (in == '\"')	
			out = "&quot;";
		else if (in == '&')
			out = "&amp;";
		else if (in == ' ')
			out = "&nbsp;";
		else
			out = in + "";
		return out;
	}
}
