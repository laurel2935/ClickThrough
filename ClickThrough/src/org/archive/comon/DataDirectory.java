package org.archive.comon;

public class DataDirectory {
	//RawData
	//public static String RawDataRoot = "E:/Data_Log/DataSource_Raw/";
	public static String RawDataRoot = "C:/T/WorkBench/Corpus/DataSource_Raw/";
	//
	public static String [] RawData = {"AOLCorpus/", "SogouQ2008/", "SogouQ2012/"};
	
	//Unique element
	public static String UniqueElementRoot = "E:/Data_Log/DataSource_Analyzed/UniqueElement/";
	//
	public static String [] Unique_All = {"AOL/Unique_All/", "SogouQ2008/Unique_All/", "SogouQ2012/Unique_All/"};
	//
	public static String [] Unique_PerUnit = {"AOL/Unique_PerUnit/", "SogouQ2008/Unique_PerUnit/", "SogouQ2012/Unique_PerUnit/"};
	//
	public static String [] Unique_Training = {"AOL/Unique_Training/", "", ""};
	//
	public static String [] Unique_Testing = {"AOL/Unique_Testing/", "", ""};
	
	//Digital format
	public static String DigitalFormatRoot = "E:/Data_Log/DataSource_Analyzed/DigitalFormat/";
	//
	public static String [] DigitalFormat = {"AOL/", "SogouQ2008/", "SogouQ2012/"};
	
	//ordered sogouQ
	//public static String OrderedSogouQRoot = "E:/Data_Log/DataSource_Analyzed/OrderedSogouQ/";
	public static String OrderedSogouQRoot = "C:/T/WorkBench/Corpus/DataSource_Analyzed/OrderedSogouQ/";
	//
	//public static String [] OrderedVersion = {"", "SogouQ2008/", "SogouQ2012/"};
	
	//session segmentation
	public static String SessionSegmentationRoot = "E:/Data_Log/DataSource_Analyzed/SessionSegmentation/";
	
	//ClickThroughGraph
	public static String ClickThroughGraphRoot = "E:/Data_Log/DataSource_Analyzed/ClickThroughGraph/";
	//
	public static String [] GraphFile = {"AOL/GraphFile/", "SogouQ2008/GraphFile/", "SogouQ2012/GraphFile/"};
	//currently only for aol dataset
	public static String [] UnitGraphFile = {"AOL/UnitGraphFile/", "SogouQ2008/UnitGraphFile/", "SogouQ2012/UnitGraphFile/"};
	
	//index query
	public static String QueryIndexRoot = "E:/Data_Log/DataSource_Analyzed/QueryIndex/";
	//all the queries are indexed
	public static String [] QueryIndex_All = {"AOL/Query_All/", "", ""};
	
	
	//Public DataSet
	public static String PublicDataSetRoot = "E:/Data_Log/PublicDataSet/";
	//
	public static String [] PublicDataSet = {"corpus-webis-smc-12/", "webis-qsec-10-training-set/", ""};
	

}
