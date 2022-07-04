package sstrange.matchgenerator;

import java.util.ArrayList;

import support.stringmatching.GSTMatchTuple;

public class ComparisonPairTuple implements Comparable<ComparisonPairTuple> {
	private String codePath1, codePath2, assignmentName1, assignmentName2;
	private double simResult;
	private int clusterID;
	private String resultedHTMLFilename;
	private ArrayList<GSTMatchTuple> matches;

	public ComparisonPairTuple(String codePath1, String codePath2, String assignmentName1, String assignmentName2,
			double simResult, int clusterID, ArrayList<GSTMatchTuple> matches) {
		super();
		this.codePath1 = codePath1;
		this.codePath2 = codePath2;
		this.assignmentName1 = assignmentName1;
		this.assignmentName2 = assignmentName2;
		this.simResult = simResult;
		this.clusterID = clusterID;
		this.resultedHTMLFilename = "";
		this.matches = matches;
	}

	public String getResultedHTMLFilename() {
		return resultedHTMLFilename;
	}

	public void setResultedHTMLFilename(String resultedHTMLFilename) {
		this.resultedHTMLFilename = resultedHTMLFilename;
	}

	public String getCodePath1() {
		return codePath1;
	}

	public void setCodePath1(String codePath1) {
		this.codePath1 = codePath1;
	}

	public String getCodePath2() {
		return codePath2;
	}

	public void setCodePath2(String codePath2) {
		this.codePath2 = codePath2;
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

	public double getSimResult() {
		return simResult;
	}

	public void setSimResult(double simResult) {
		this.simResult = simResult;
	}

	public int getClusterID() {
		return clusterID;
	}

	public void setClusterID(int clusterID) {
		this.clusterID = clusterID;
	}

	public ArrayList<GSTMatchTuple> getMatches() {
		return matches;
	}

	public void setMatches(ArrayList<GSTMatchTuple> matches) {
		this.matches = matches;
	}

	@Override
	public int compareTo(ComparisonPairTuple arg0) {
		// sort based on avg sim in descending order
		return (int) ((-getSimResult() + arg0.getSimResult()) * 100000);
	}

	public String toString() {
		return assignmentName1 + " " + assignmentName2 + " " + getSimResult();
	}
}
