package sstrange.matchgenerator;

import java.util.ArrayList;

import sstrange.support.stringmatching.GSTMatchTuple;

public class ComparisonPairTuple implements Comparable<ComparisonPairTuple> {
	private int submissionID1, submissionID2;
	private double syntaxSimResult, surfaceSimResult;
	private int sameClusterOccurrences;
	private String resultedHTMLFilename;
	private ArrayList<GSTMatchTuple> matches;
	private String assignmentName1, assignmentName2;

	public ComparisonPairTuple(int submissionID1, int submissionID2, String assignmentName1, String assignmentName2,
			double syntaxSimResult, double surfaceSimResult, int sameClusterOccurrences,
			ArrayList<GSTMatchTuple> matches) {
		super();
		this.submissionID1 = submissionID1;
		this.submissionID2 = submissionID2;
		this.assignmentName1 = assignmentName1;
		this.assignmentName2 = assignmentName2;
		this.syntaxSimResult = syntaxSimResult;
		this.surfaceSimResult = surfaceSimResult;
		this.sameClusterOccurrences = sameClusterOccurrences;
		this.resultedHTMLFilename = "";
		this.matches = matches;
	}

	public String getResultedHTMLFilename() {
		return resultedHTMLFilename;
	}

	public void setResultedHTMLFilename(String resultedHTMLFilename) {
		this.resultedHTMLFilename = resultedHTMLFilename;
	}

	@Override
	public int compareTo(ComparisonPairTuple arg0) {
		// sort based on avg sim in descending order
		return (int) ((-getSimResult() + arg0.getSimResult()) * 100000);
	}

	public int getSameClusterOccurrences() {
		return sameClusterOccurrences;
	}

	public void setSameClusterOccurrences(int sameClusterOccurrences) {
		this.sameClusterOccurrences = sameClusterOccurrences;
	}

	public int getSubmissionID1() {
		return submissionID1;
	}

	public void setSubmissionID1(int submissionID1) {
		this.submissionID1 = submissionID1;
	}

	public int getSubmissionID2() {
		return submissionID2;
	}

	public void setSubmissionID2(int submissionID2) {
		this.submissionID2 = submissionID2;
	}

	public double getSimResult() {
		if (surfaceSimResult == -1)
			return syntaxSimResult;
		else
			return 0.5 * syntaxSimResult + 0.5 * surfaceSimResult;
	}

	public double getSyntaxSimResult() {
		return syntaxSimResult;
	}

	public void setSyntaxSimResult(double syntaxSimResult) {
		this.syntaxSimResult = syntaxSimResult;
	}

	public double getSurfaceSimResult() {
		return surfaceSimResult;
	}

	public void setSurfaceSimResult(double surfaceSimResult) {
		this.surfaceSimResult = surfaceSimResult;
	}

	public ArrayList<GSTMatchTuple> getMatches() {
		return matches;
	}

	public void setMatches(ArrayList<GSTMatchTuple> matches) {
		this.matches = matches;
	}

	public String getAssignmentName1() {
		return assignmentName1;
	}

	public void setAssignmentName1(String assignmentName1) {
		this.assignmentName1 = assignmentName1;
	}

	public String getAssignmentName2() {
		return assignmentName2;
	}

	public void setAssignmentName2(String assignmentName2) {
		this.assignmentName2 = assignmentName2;
	}

}
