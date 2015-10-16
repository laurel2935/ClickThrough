package org.archive.comon;

public class DataDirectory {
	private static final String _sysTag = "MAC";
	
	//RawData
	//public static String RawDataRoot = "E:/Data_Log/DataSource_Raw/";
	public static String RawDataRoot;
	//
	public static String [] RawData = {"AOLCorpus/", "SogouQ2008/", "SogouQ2012/"};
	
	//Unique element
	public static String UniqueElementRoot;
	//
	public static String [] Unique_All = {"AOL/Unique_All/", "SogouQ2008/Unique_All/", "SogouQ2012/Unique_All/"};
	//
	public static String [] Unique_PerUnit = {"AOL/Unique_PerUnit/", "SogouQ2008/Unique_PerUnit/", "SogouQ2012/Unique_PerUnit/"};
	//
	public static String [] Unique_Training = {"AOL/Unique_Training/", "", ""};
	//
	public static String [] Unique_Testing = {"AOL/Unique_Testing/", "", ""};
	
	//Digital format
	public static String DigitalFormatRoot;
	//
	public static String [] DigitalFormat = {"AOL/", "SogouQ2008/", "SogouQ2012/"};
	
	//ordered sogouQ
	//public static String OrderedSogouQRoot = "E:/Data_Log/DataSource_Analyzed/OrderedSogouQ/";
	public static String OrderedSogouQRoot;
	//
	//public static String [] OrderedVersion = {"", "SogouQ2008/", "SogouQ2012/"};
	
	//session segmentation
	public static String SessionSegmentationRoot;
	
	//ClickThroughGraph
	public static String ClickThroughGraphRoot;
	//
	public static String [] GraphFile = {"AOL/GraphFile/", "SogouQ2008/GraphFile/", "SogouQ2012/GraphFile/"};
	//currently only for aol dataset
	public static String [] UnitGraphFile = {"AOL/UnitGraphFile/", "SogouQ2008/UnitGraphFile/", "SogouQ2012/UnitGraphFile/"};
	
	//index query
	public static String QueryIndexRoot ;
	//all the queries are indexed
	public static String [] QueryIndex_All = {"AOL/Query_All/", "", ""};
	
	
	//Public DataSet
	public static String PublicDataSetRoot;
	//
	public static String [] PublicDataSet = {"corpus-webis-smc-12/", "webis-qsec-10-training-set/", ""};
	
	
	
	//
	public static String Bench_Output;
	
	
	static{
		if(_sysTag.equals("WIN")){
			
			RawDataRoot = "C:/T/WorkBench/Corpus/DataSource_Raw/";
			
			UniqueElementRoot = "E:/Data_Log/DataSource_Analyzed/UniqueElement/";
			
			DigitalFormatRoot = "E:/Data_Log/DataSource_Analyzed/DigitalFormat/";
			
			OrderedSogouQRoot = "C:/T/WorkBench/Corpus/DataSource_Analyzed/OrderedSogouQ/";
			
			SessionSegmentationRoot = "E:/Data_Log/DataSource_Analyzed/SessionSegmentation/";
			
			ClickThroughGraphRoot = "E:/Data_Log/DataSource_Analyzed/ClickThroughGraph/";
			
			QueryIndexRoot = "E:/Data_Log/DataSource_Analyzed/QueryIndex/";
			
			PublicDataSetRoot = "E:/Data_Log/PublicDataSet/";
			
		}else{
			
			UniqueElementRoot = "/Users/dryuhaitao/WorkBench/Corpus/DataSource_Analyzed/SogouQ/UniqueElement/";
			
			Bench_Output = "/Users/dryuhaitao/WorkBench/CodeBench/Bench_Output/";
			
		}
	}
	

}
