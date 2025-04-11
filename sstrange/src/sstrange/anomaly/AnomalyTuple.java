package sstrange.anomaly;

import java.util.ArrayList;

public class AnomalyTuple implements Comparable<AnomalyTuple> {
	private int submissionID;
	private String assignmentName;
	private double syntaxDissim;
	private double aiSim;
	private String resultedHTMLFilename;
	private String resultedAIHTMLFilename;

	public AnomalyTuple(int submissionID, String assignmentName, double syntaxDissim, double aiSim) {
		super();
		this.submissionID = submissionID;
		this.assignmentName = assignmentName;
		this.syntaxDissim = syntaxDissim;
		this.aiSim = aiSim;
		this.resultedHTMLFilename = "";
		this.resultedAIHTMLFilename = "";
	}

	public String getResultedHTMLFilename() {
		return resultedHTMLFilename;
	}

	public void setResultedHTMLFilename(String resultedHTMLFilename) {
		this.resultedHTMLFilename = resultedHTMLFilename;
	}

	public String getResultedAIHTMLFilename() {
		return resultedAIHTMLFilename;
	}

	public void setResultedAIHTMLFilename(String resultedAIHTMLFilename) {
		this.resultedAIHTMLFilename = resultedAIHTMLFilename;
	}

	public int getSubmissionID() {
		return submissionID;
	}

	@Override
	public int compareTo(AnomalyTuple o) {
		// sort based on avg sim in descending order
		// uniqueness
		// return (int) ((-getDissimResult() + o.getDissimResult()) * 100000);
		// template
		// return (int) ((-getAiSim() + o.getAiSim()) * 100000);
		// template + uniqueness
		return (int) ((-(getDissimResult()+getAiSim())/2 + (o.getDissimResult()+o.getAiSim())/2) * 100000);
	}

	public double getDissimResult() {
		return syntaxDissim;
	}

	public double getAiSim() {
		return aiSim;
	}

	public String toString() {
		return submissionID + " " + getDissimResult();
	}

	public String getAssignmentName() {
		return assignmentName;
	}

	public void setSyntaxDissim(double syntaxDissim) {
		this.syntaxDissim = syntaxDissim;
	}

}
