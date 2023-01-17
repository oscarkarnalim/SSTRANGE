package sstrange.htmlgenerator;

import java.util.ArrayList;
import java.util.Collections;

import sstrange.message.FeedbackMessage;

public class HtmlTableStringGenerator {

	public static String getTableContentForMatchesForSSTRANGE(
			ArrayList<FeedbackMessage> messages, String tableId,
			String humanLanguageId) {
		String s = "";

		// put all feedback messages as a list
		ArrayList<HtmlTableTuple> list = new ArrayList<>();
		for (FeedbackMessage m : messages) {
			list.add(new HtmlTableTuple(m));
		}

		// sort the result
		Collections.sort(list);
		
		// start generating the resulted string
		for (int i = 0; i < list.size(); i++) {
			HtmlTableTuple cur = list.get(i);

			// set the first line
			s += "<tr id=\"" + cur.getEntity().getVisualId()
					+ "hr\" onclick=\"markSelectedWithoutChangingTableFocus('"
					+ cur.getEntity().getVisualId() + "','" + tableId + "')\">";

			/*
			 * Get table ID from visual ID and then aligns it for readability.
			 */
			String visualId = cur.getEntity().getVisualId();
			// search for the numeric ID part
			int curIdNumPos = 0;
			for (int k = 0; k < visualId.length(); k++) {
				if (Character.isLetter(visualId.charAt(k)) == false) {
					curIdNumPos = k;
					break;
				}
			}
			// merge them together
			String alignedTableID = visualId.toUpperCase().charAt(0) + "";
			int curIdNum = Integer.parseInt(visualId.substring(curIdNumPos));
			if (curIdNum < 10) {
				alignedTableID += "00" + curIdNum;
			} else if (curIdNum < 100) {
				alignedTableID += "0" + curIdNum;
			} else {
				alignedTableID += curIdNum;
			}

			// visualising the rest of the lines
			s += ("\n\t<td><a href=\"#" + cur.getEntity().getVisualId()
					+ "a\" id=\"" + cur.getEntity().getVisualId() + "hl\">"
					+ alignedTableID + "</a></td>");
			s += ("\n\t<td>" + cur.getEntity().getStartRowCode1() + "</td>");
			s += ("\n\t<td>" + cur.getEntity().getStartRowCode2() + "</td>");
			
			if (humanLanguageId.equalsIgnoreCase("en")) {
				if (cur.getMinCharacterLength() == 1)
					s += ("\n\t<td>" + cur.getMinCharacterLength() + " char</td>");
				else
					s += ("\n\t<td>" + cur.getMinCharacterLength() + " chars</td>");
				
				if (cur.getMatchedTokenLength() == 1)
					s += ("\n\t<td>" + cur.getMatchedTokenLength() + " token</td>");
				else
					s += ("\n\t<td>" + cur.getMatchedTokenLength() + " tokens</td>");
			} else {
				s += ("\n\t<td>" + cur.getMinCharacterLength() + " karakter</td>");
				
				s += ("\n\t<td>" + cur.getMatchedTokenLength() + " token</td>");
			}
			s += "\n</tr>\n";
		}

		return s;
	}
}
