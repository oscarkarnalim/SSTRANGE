package sstrange.matchgenerator;

import java.util.ArrayList;

import support.stringmatching.GSTMatchTuple;

public class ComparisonPairTupleWeb extends ComparisonPairTuple {
	private ArrayList<GSTMatchTuple> jsMatches, cssMatches;

	public ComparisonPairTupleWeb(int submissionID1, int submissionID2, String assignmentName1, String assignmentName2,
			double simResult, int sameClusterOccurrences, ArrayList<GSTMatchTuple> matches, ArrayList<GSTMatchTuple> jsMatches,ArrayList<GSTMatchTuple> cssMatches) {
		super(submissionID1, submissionID2, assignmentName1, assignmentName2, simResult, sameClusterOccurrences, matches);
		// TODO Auto-generated constructor stub
		this.jsMatches = jsMatches;
		this.cssMatches = cssMatches;
	}

	public ArrayList<GSTMatchTuple> getJsMatches() {
		return jsMatches;
	}

	public ArrayList<GSTMatchTuple> getCssMatches() {
		return cssMatches;
	}

}
