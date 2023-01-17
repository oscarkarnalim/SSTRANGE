package sstrange.message;

import java.util.ArrayList;
import java.util.Collections;

import sstrange.token.FeedbackToken;

public class SyntaxFeedbackMessage extends MultipleFeedbackMessage {
	// to store changes made between code files.
	protected ArrayList<MismatchSyntaxMessage> mismatchSyntaxMessages;
	// calculate the number of whitespace modif
	protected boolean areWhitespacesModified;
	protected String humanLanguageCode;

	public SyntaxFeedbackMessage(String action, String disguiseTarget, ArrayList<FeedbackToken> tokenList1,
			ArrayList<FeedbackToken> tokenList2, boolean areWhitespacesModified, String humanLanguageCode) {
		super(action, disguiseTarget, tokenList1, tokenList2);
		// TODO Auto-generated constructor stub
		this.updateActionAndGenerateAdditionalDetails();
		this.areWhitespacesModified = areWhitespacesModified;
		this.humanLanguageCode = humanLanguageCode;
	}

	private void updateActionAndGenerateAdditionalDetails() {
		mismatchSyntaxMessages = new ArrayList<>();

		// check whether the tokens are the same in nature
		for (int i = 0; i < tokenList1.size(); i++) {
			FeedbackToken cur1 = tokenList1.get(i);
			FeedbackToken cur2 = tokenList2.get(i);
			// if different
			if (!cur1.getContent().equals(cur2.getContent())) {
				// change the action to "modified"
				this.action = "modified";

				/*
				 * create additional message and add it. target disguise is described based on
				 * given token content for comparison.
				 */
				String disguiseTarget = cur1.getContentForComparison();
				int index = MismatchSyntaxMessage.indexOfAList(mismatchSyntaxMessages, disguiseTarget,
						cur1.getContent(), cur2.getContent());

				// check if such a message exists on the list
				MismatchSyntaxMessage msu;
				if (index == -1) {
					// if not found, add the new one
					msu = new MismatchSyntaxMessage();
					mismatchSyntaxMessages.add(msu);
				} else {
					// otherwise, get the existing one
					msu = mismatchSyntaxMessages.get(index);
				}

				// add the message
				msu.getMismatchTokenMessages().add(new StandardFeedbackMessage("modified", disguiseTarget, cur1, cur2));
			}
		}

		// sort in descending order
		Collections.sort(mismatchSyntaxMessages);
	}

	public String toString() {
		if (humanLanguageCode.equals("en"))
			return this.toStringEn("orig");
		else
			return this.toStringId("orig");
	}

	public String toString(String mode) {
		if (humanLanguageCode.equals("en"))
			return this.toStringEn(mode);
		else
			return this.toStringId(mode);
	}

	private String toStringEn(String mode) {
		String s = "";
		if (action.equals("copied")) {
			if (mode.equals("")) {
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
				// copied after the white space are formatted
				if (mode.equals("rena")) {
					s += "<b>The code fragments are identical after layout formatting and content generalisation.</b> ";
					s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
							+ "In these circumstances it is rare to see two or more honest students share the same solution implementation.";
				} else {
					s += "<b>The code fragments are identical after layout formatting.</b> ";
					s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
							+ "In these circumstances it is rare to see two or more honest students share the same solution implementation.";
				}
			}
		} else {
			if (mode.equals("")) {
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
			} else {
				// modified after the white space are formatted
				if (mode.equals("rena")) {
					s += "<b>The code fragments are identical after layout formatting and content generalisation.</b> ";
					s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
							+ "In these circumstances it is rare to see two or more honest students share the same solution implementation.";
				} else {
					s += "<b>The code fragments are similar despite some content differences after layout formatting.</b> ";
					s += "If the task has many possible solutions and students are not required to use this particular form of content, this is suspicious. "
							+ "In these circumstances it is rare to see two or more honest students share the same solution implementation.";
				}
			}
		}

		return s;
	}

	private String toStringId(String mode) {
		String s = "";
		if (action.equals("copied")) {
			if (mode.equals("")) {
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
				// copied after the white space are formatted
				if (mode.equals("rena")) {
					s += "<b>Kedua fragmen kode identik satu sama lain setelah tata letaknya dirapihkan dan sebagian kontennya digeneralisir.</b> ";
					s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
							+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				} else {
					s += "<b>Kedua fragmen kode identik satu sama lain setelah tata letaknya dirapihkan.</b> ";
					s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
							+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				}
			}
		} else {
			if (mode.equals("")) {
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
			} else {
				// modified after the white space are formatted
				if (mode.equals("rena")) {
					s += "<b>Kedua fragmen kode identik satu sama lain setelah tata letaknya dirapihkan dan sebagian kontennya digeneralisir.</b> ";
					s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
							+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				} else {
					s += "<b>Kedua fragmen kode sama dengan beberapa perbedaan pada konten setelah tata letaknya dirapihkan.</b> ";
					s += "Jika tugas yang diberikan memiliki banyak alternatif solusi dan para murid tidak wajib menggunakan konten fragmen ini, hal ini mencurigakan. "
							+ "Pada keadaan tersebut sangatlah langka dua atau lebih murid jujur menggunakan implementasi solusi yang sama. ";
				}
			}
		}

		return s;
	}
}
