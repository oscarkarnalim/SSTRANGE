package sstrange.matchgenerator;

import java.util.ArrayList;

import sstrange.support.stringmatching.GSTMatchTuple;

public class ComparisonPairTupleWeb extends ComparisonPairTuple {
	private ArrayList<GSTMatchTuple> phpMatches, jsMatches, cssMatches;

	public ComparisonPairTupleWeb(int submissionID1, int submissionID2, String assignmentName1, String assignmentName2,
			double simResult, double surfaceSimResult, int sameClusterOccurrences, ArrayList<GSTMatchTuple> matches, ArrayList<GSTMatchTuple> phpMatches,
			ArrayList<GSTMatchTuple> jsMatches, ArrayList<GSTMatchTuple> cssMatches) {
		super(submissionID1, submissionID2, assignmentName1, assignmentName2, simResult, surfaceSimResult,
				sameClusterOccurrences, matches);
		// TODO Auto-generated constructor stub
		this.phpMatches = phpMatches;
		this.jsMatches = jsMatches;
		this.cssMatches = cssMatches;
	}

	public ArrayList<GSTMatchTuple> getJsMatches() {
		return jsMatches;
	}

	public ArrayList<GSTMatchTuple> getCssMatches() {
		return cssMatches;
	}

	public ArrayList<GSTMatchTuple> getPhpMatches() {
		return phpMatches;
	}

}
