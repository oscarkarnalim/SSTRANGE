package sstrange.htmlgenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class HTMLUniqueCodeHtmlGenerator {
	public static void generateHtmlForSSTRANGE(String codepath1, String phpCodepath1, String jsCodepath1,
			String cssCodepath1, String dirname1, String templateHTMLPath, String outputHTMLPath,
			String languageCode, double dissimDegree) throws Exception {

		File templateFile = new File(templateHTMLPath);
		File outputFile = new File(outputHTMLPath);
		BufferedReader fr = new BufferedReader(new FileReader(templateFile));
		BufferedWriter fw = new BufferedWriter(new FileWriter(outputFile));
		String line;
		while ((line = fr.readLine()) != null) {
			if (line.contains("@codepath1")) {
				line = line.replace("@codepath1", dirname1);
			}

			// for html
			if (line.contains("@code1")) {
				line = line.replace("@code1", readCode1(codepath1, languageCode));
			}

			// for php
			if (line.contains("@phpcode1")) {
				line = line.replace("@phpcode1", readCode1(phpCodepath1, languageCode));
			}

			// for js
			if (line.contains("@jscode1")) {
				line = line.replace("@jscode1",readCode1(jsCodepath1, languageCode));
			}

			// for css
			if (line.contains("@csscode1")) {
				line = line.replace("@csscode1", readCode1(cssCodepath1, languageCode));
			}

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}

	private static String readCode1(String filepath, String humanLanguageId)
			throws Exception {
		if(filepath ==  null) {
			String noteForEmptyFiletype = "No files in such format detected. \nKindly check other file types.";
			if (humanLanguageId.equals("id"))
				noteForEmptyFiletype = "Tidak ada berkas dengan format terkait terdeteksi. \nMohon mengecek tipe berkas lainnya.";
			return noteForEmptyFiletype;
		}
		
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
		else
			out = in + "";
		return out;
	}

	public static String HTMLSafeStringFormatWithSpace(char in) {
		String out = HTMLSafeStringFormat(in);
		if (in == ' ')
			out = "&nbsp;";
		return out;
	}

	public static String HTMLSafeStringFormatWithSpace(String in) {
		String out = "";
		for (int i = 0; i < in.length(); i++) {
			out += HTMLSafeStringFormatWithSpace(in.charAt(i));
		}
		return out;
	}
}
