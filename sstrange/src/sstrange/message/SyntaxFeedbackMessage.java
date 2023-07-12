package sstrange.message;

import java.util.ArrayList;
import java.util.Collections;

import sstrange.token.FeedbackToken;

public class SyntaxFeedbackMessage extends MultipleFeedbackMessage {
	// calculate the number of whitespace modif
	protected boolean areWhitespacesModified;
	protected String humanLanguageCode;

	public SyntaxFeedbackMessage(String action, String disguiseTarget, ArrayList<FeedbackToken> tokenList1,
			ArrayList<FeedbackToken> tokenList2, String humanLanguageCode) {
		super(action, disguiseTarget, tokenList1, tokenList2);
		// TODO Auto-generated constructor stub
		this.updateActionAndGenerateAdditionalDetails();
		this.humanLanguageCode = humanLanguageCode;
	}

	private void updateActionAndGenerateAdditionalDetails() {
		// for checking whether the whitespace is modified
		int startRowSyntax1 = tokenList1.get(0).getStartRow();
		int startRowSyntax2 = tokenList2.get(0).getStartRow();

		// check whether the tokens are the same in nature
		for (int i = 0; i < tokenList1.size(); i++) {
			FeedbackToken cur1 = tokenList1.get(i);
			FeedbackToken cur2 = tokenList2.get(i);
			// if different
			if (!cur1.getContent().equals(cur2.getContent())) {
				// change the action to "modified"
				this.action = "modified";
			}

			if (this.areWhitespacesModified == false) {
				// if different row and col
				if (cur1.getStartRow() - startRowSyntax1 != cur2.getStartRow() - startRowSyntax2) {
					this.areWhitespacesModified = true;
				}else if(cur1.getStartCol() != cur2.getStartCol()) {
					this.areWhitespacesModified = true;
				}
			}
		}
	}

	public String toString() {
		if (humanLanguageCode.equals("en"))
			return this.toStringEn();
		else
			return this.toStringId();
	}

	private String toStringEn() {
		String s = "";
		if (action.equals("copied")) {
			if (areWhitespacesModified) {
				// copied with different white space
				s = "<b>The code fragments have different layout but share the same content.</b> ";
				s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
						+ "In these circumstances it is rare to see two or more honest students share the same solution implementation. ";
				s += "This can also be suspicious when at least one of them has unusual layout formatting as it can be an attempt to disguise the similarity.";
			} else {
				// copied with the same white space
				s = "<b>The code fragments are identical.</b> ";
				s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
						+ "In these circumstances it is rare to see two or more honest students share the same solution implementation. ";
				s += "This can also be suspicious when the layout is unusually formatted, as unusual formatting is rarely shared by coincidence.";
			}
		} else {
			if (areWhitespacesModified) {
				// modified with different white space
				s = "<b>The code fragments are similar despite some differences in content and layout.</b> ";
				s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
						+ "In these circumstances it is rare to see two or more honest students share the same solution implementation. ";
				s += "This can also be suspicious when at least one of them has unusual layout formatting as it can be an attempt to disguise the similarity.";
			} else {
				// modified with the same white space
				s = "<b>The code fragments are similar despite some differences in content.</b> ";
				s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
						+ "In these circumstances it is rare to see two or more honest students share the same solution implementation. ";
				s += "This can also be suspicious when the layout is unusually formatted, as unusual formatting is rarely shared by coincidence.";
			}
		}

		return s;
	}

	private String toStringId() {
		String s = "";
		if (action.equals("copied")) {
			if (areWhitespacesModified) {
				// copied with different white space
				s = "<b>Kedua fragmen kode memiliki tata letak yang berbeda namun berisi konten yang sama.</b> ";
				s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
						+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				s += "Hal ini juga dapat mencurigakan jika paling tidak salah satu fragmen memiliki tata letak yang tidak umum; ketidakumuman tersebut bisa jadi "
						+ "salah satu percobaan untuk mengkaburkan kesamaan.";
			} else {
				// copied with the same white space
				s = "<b>Kedua fragmen kode identik satu sama lain.</b> ";
				s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
						+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				s += "Hal ini juga dapat mencurigakan jika tata letak fragmen tidak umum, karena ketidakumuman tersebut sangat jarang terjadi akibat ketidaksengajaan.";
			}
		} else {
			if (areWhitespacesModified) {
				// modified with different white space
				s = "<b>Kedua fragmen kode sama walaupun ada beberapa perbedaan pada konten dan tata letak.</b> ";
				s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
						+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				s += "Hal ini juga dapat mencurigakan jika paling tidak salah satu fragmen memiliki tata letak yang tidak umum; ketidakumuman tersebut bisa jadi "
						+ "salah satu percobaan untuk mengkaburkan kesamaan.";
			} else {
				// modified with the same white space
				s = "<b>Kedua fragmen kode sama walaupun ada beberapa perbedaan pada konten.</b> ";
				s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
						+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				s += "Hal ini juga dapat mencurigakan jika tata letak fragmen tidak umum, karena ketidakumuman tersebut sangat jarang terjadi akibat ketidaksengajaan.";
			}
		}

		return s;
	}
}
