package sstrange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

import sstrange.matchgenerator.ComparisonPairTuple;

public class IndexHTMLGenerator {

	public static void generateHtml(String assignmentRootPath, ArrayList<ComparisonPairTuple> codePairs,
			String coreTemplateHTMLPath, String outputDirPath, double threshold, String languageCode) throws Exception {

		String tableContent = getTableContent(codePairs, languageCode);

		String logoHeader = "<div class=\"embedimage\" style=\"width:15%;margin-left:0px;\">\r\n"
				+ "					<img class=\"embedimage\" src=\"strange_html_layout_additional_files/logo.png\" alt=\"logo STRANGE\">\r\n"
				+ "				</div>";

		// create directory
		File dirRoot = new File(outputDirPath);
		if (dirRoot.exists() == false)
			dirRoot.mkdir();

		// generate the html page
		File templateFile = new File(coreTemplateHTMLPath);
		File outputFile = new File(outputDirPath + File.separator + "index.html");
		BufferedReader fr = new BufferedReader(new FileReader(templateFile));
		BufferedWriter fw = new BufferedWriter(new FileWriter(outputFile));
		String line;
		while ((line = fr.readLine()) != null) {

			if (line.contains("@filepath")) {
				line = line.replace("@filepath", assignmentRootPath);
			}

			if (line.contains("@logo")) {
				line = line.replace("@logo", logoHeader);
			}

			if (line.contains("@about")) {
				line = line.replace("@about", (languageCode.equals("en")) ? getAboutEn() : getAboutID());
			}

			if (line.contains("@threshold")) {
				line = line.replace("@threshold", String.format("%.0f", threshold));
			}

			if (line.contains("@tablecontent")) {
				line = line.replace("@tablecontent", tableContent);
			}

			if (line.contains("@explanation"))
				line = line.replace("@explanation",
						(languageCode.equals("en")) ? getExplanationContentEn() : getExplanationContentID());

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}

	private static String getExplanationContentID() {
		String s = "";
		
		s += "<ul>";
		s += "<li>Kesamaan yang dilaporkan adalah kesamaan rerata, yang mempertimbangkan semua perbedaan dalam perhitungannya.</li>";
		s += "<li>Kesamaan mengabaikan perbedaan di komentar, spasi, dan nilai konstanta. "
				+ "Perbedaan di nama identifier (misal variabel atau fungsi) diabaikan di Java, Python, JavaScript dan CSS (terbatas pada nama variabel dan selectors). "
				+ "Perbedaan di konten teks, tag script, tag style, dan kapitalisasi di nama tag diabaikan di HTML. "
				+ "Perbedaan di tipe data primitif diabaikan di Java.</li>";
		s += "<li>MinHash</b> adalah sebuah algoritma locality sensitive hashing yang didasarkan pada kesamaan Jaccard dan cukup efisien untuk membandingkan program-program dalam sebuah data set besar.</li>";
		s += "<li>Super-Bit</b> adalah salah satu algoritma locality sensitive hashing lain dan ini didasarkan dari kesamaan Cosine. Algoritma ini juga efisien untuk membandingkan program-program dalam sebuah data set besar.</li>";
		s += "<li>Kesamaan Jaccard</b> menganggap dua program sama jika mereka berisi \"kata-kata\" unik yang sama. </li>";
		s += "<li><b>Kesamaan Cosine</b> mirip dengan Jaccard namun dia mempertimbangkan jumlah kemunculan \"kata-kata\". </li>";
		s += "<li><b>RKRGST</b> berarti algoritma running Karp-Rabin greedy string tiling. Ini adalah algoritma umum untuk menghitung kesamaan program walaupun kurang efisien. </li>";
		s += "<li>Clustering (pengelompokan) hanya ada untuk algoritma-algoritma locality sensitive hashing dan program-program yang berada pada cluster yang sama seharusnya cukup mirip satu sama lain.</li>";
		s = s + "</ul>\n";

		return s;
	}

	private static String getExplanationContentEn() {
		String s = "";

		s += "<ul>";
		s += "<li>The reported similarity is average similarity, which considers all differences in its calculation.</li>";
		s += "<li>Reported similarities ignore differences in comments, white space, and constants. "
				+ "Differences in identifier names (e.g., variables or functions) are ignored in Java, Python, JavaScript and CSS (limited to variable names and selectors). "
				+ "Differences in text content, script tag, style tag, and capitalisation in tag names are ignored in HTML. "
				+ "Differences in primitive data types are ignored in Java.</li>";
		s += "<li><b>MinHash</b> is a locality sensitive hashing algorithm that is based on Jaccard similarity and it is efficient for comparing submissions in a large data set.</li>";
		s += "<li><b>Super-Bit</b> is another locality sensitive hashing algorithm and it is based on Cosine similarity. The algorithm is also efficient for comparing submissions in a large data set.</li>";
		s += "<li><b>Jaccard similarity</b> considers two submissions as similar if they have the same set of distinct \"words\". </li>";
		s += "<li><b>Cosine similarity</b> is somewhat similar to Jaccard except that it considers \"word\" occurrences. </li>";
		s += "<li><b>RKRGST</b> stands for running Karp-Rabin greedy string tiling algorithm. It is a common algorithm to measure program similarity but it is quite inefficient. </li>";
		s += "<li>Clustering is only applicable for locality sensitive hashing algorithms and submissions in the same cluster should be relatively similar.</li>";
		s = s + "</ul>\n";

		return s;
	}

	public static String getAboutEn() {
		return "<ol>\r\n"
				+ "				<li>This HTML page is generated by SSTRANGE, a scalable and efficient tool to observe similarities among submissions with locality sensitive hashing.</li>\r\n"
				+ "				<li>SSTRANGE is an expanded version of <b>STRANGE</b> (<b>S</b>imilarity <b>TR</b>acker in <b>A</b>cademia with <b>N</b>atural lan<b>G</b>uage <b>E</b>xplanation), which details can be seen in <b><u><a href = \"https://github.com/oscarkarnalim/strange\">the Github page</a></u></b> or <b><u><a href=\"https://ieeexplore.ieee.org/document/9405994\">the corresponding publication</a></u></b></li>\r\n"
				+ "				<li>SSTRANGE can be downloaded from <b><u><a href=\"https://github.com/oscarkarnalim/sstrange\">the repository</a></u></b>.\r\n"
				+ "					Alternatively, you can email a request to <b>Oscar Karnalim</b> (<b><u><a href=\"mailto:oscar.karnalim@uon.edu.au\">this email</a></u></b> or <b><u><a href=\"mailto:oscar.karnalim@it.maranatha.edu\">that email</a></u></b>).</li>\r\n"
				+ "				<li>If you want to cite the program (or some parts of it), please cite it as <b><u><a href=\"#\">this paper</a></u></b>. </li>\r\n"
				+ "				</ol>";
	}

	public static String getAboutID() {
		return "<ol>\r\n"
				+ "				<li>Laman HTML ini dibuat dengan SSTRANGE, sebuah kakas skalabel dan efisien untuk mengobservasi kesamaan antar tugas dengan locality sensitive hashing.</li>\r\n"
				+ "				<li>SSTRANGE merupakan ekspansi dari <b>STRANGE</b> (<b>S</b>imilarity <b>TR</b>acker in <b>A</b>cademia with <b>N</b>atural lan<b>G</b>uage <b>E</b>xplanation), which details can be seen in <b><u><a href = \"https://github.com/oscarkarnalim/strange\">the Github page</a></u></b> or <b><u><a href=\"https://ieeexplore.ieee.org/document/9405994\">the corresponding publication</a></u></b></li>\r\n"
				+ "				<li>SSTRANGE dapat diunduh dari <b><u><a href=\"https://github.com/oscarkarnalim/sstrange\">repositori ini</a></u></b>.\r\n"
				+ "					Selain itu, kamu dapat mengirimkan email permintaan ke <b>Oscar Karnalim</b> (<b><u><a href=\"mailto:oscar.karnalim@uon.edu.au\">email ini</a></u></b> atau <b><u><a href=\"mailto:oscar.karnalim@it.maranatha.edu\">email itu</a></u></b>).</li>\r\n"
				+ "				<li>Jika kamu ingin mensitasi program ini (atau bagian-bagiannya), mohon sitasi <b><u><a href=\"#\">paper ini</a></u></b>. </li>\r\n"
				+ "				</ol>";
	}

	private static String getTableContent(ArrayList<ComparisonPairTuple> codePairs, String languageCode) {
		String textForObserve = "observe";
		if (languageCode.equals("id"))
			textForObserve = "amati";

		String s = "";
		int numID = 0;
		for (ComparisonPairTuple ct : codePairs) {
			// generate the ID
			String entryID = "r" + numID;

			// generate the string
			s += "<tr id=\"" + entryID + "\" onclick=\"selectRow('" + entryID + "','sumtablecontent')\">";
			s += ("\n\t<td><a>" + ct.getAssignmentName1() + "-" + ct.getAssignmentName2() + "</a></td>");
			s += ("\n\t<td>" + String.format("%.0f", ct.getSimResult()) + " %</td>");
			s += ("\n\t<td>" + ct.getSameClusterOccurrences() + "</td>");

			s += ("\n\t<td><button onclick=\"window.open('" + ct.getResultedHTMLFilename() + "', '_self');\">"
					+ textForObserve + "</button></td>");
			s += "\n</tr>\n";

			// increment number for ID
			numID++;
		}
		return s;
	}
}
