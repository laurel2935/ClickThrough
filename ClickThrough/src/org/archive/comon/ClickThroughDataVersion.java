package org.archive.comon;

public class ClickThroughDataVersion {
	//Query Log
	public static enum LogVersion {AOL, SogouQ2008, SogouQ2012}	
	//
	public static enum ElementType {UserID, Query, ClickUrl}
	//Testing part: the last day, or the last unit file
	public static enum PartType {All, Training, Testing};
	
	
	//Public DataSet
	public static enum PublicDataSet {Corpus_Webis_SMC_12, Webis_Qsec_10_Training_Set};

}
