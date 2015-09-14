package org.archive.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.lucene.analysis.tokenattributes.FlagsAttributeImpl;
import org.apache.lucene.util.Version;
import org.archive.clickgraph.LogEdge;
import org.archive.clickgraph.LogNode;
import org.archive.clickgraph.QueryEdge;
import org.archive.clickgraph.WordEdge;
import org.archive.comon.ClickThroughDataVersion;
import org.archive.comon.ClickThroughDataVersion.ElementType;
import org.archive.comon.ClickThroughDataVersion.LogVersion;
import org.archive.comon.DataDirectory;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.structure.BingQSession1;
import org.archive.structure.ClickTime;
import org.archive.structure.Record;
import org.archive.structure.AOLRecord;
import org.archive.structure.SogouQRecord2008;
import org.archive.structure.SogouQRecord2012;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrStrEdge;
import org.archive.util.tuple.StrStrInt;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;


public class ClickThroughAnalyzer {
	private static boolean DEBUG = true;
	//
	public static final int STARTID = 1;
	public static final String TabSeparator = "\t";
	//for session segmentation, i.e., 30 minutes
	private static final int SessionSegmentationThreshold = 30;
	//unique files: AOL, SogouQ2008, SogouQ2012
	//for id access, and id starts from "1"
	private static Hashtable<String, Integer> UniqueQTextTable = null;
	private static Hashtable<String, Integer> UniqueUserIDTable = null;
	private static Hashtable<String, Integer> UniqueClickUrlTable = null;
	private static Hashtable<String, Integer> UniqueWordTable = null;
	//for accessing multiple fields	
	private static ArrayList<IntStrInt> UniqueQTextList = null;
	private static ArrayList<StrInt> UniqueUserIDList = null;
	private static ArrayList<IntStrInt> UniqueClickUrlList = null;
	private static ArrayList<IntStrInt> UniqueWordList = null;
	
	
	
	
	//
	//query-document bipartite graph, records click information between clicked documents and queries
	//thus the recorded file: the first column is query node!
	private static Graph<LogNode, LogEdge> Q_D_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//query-query, records co-session information
	private static Graph<LogNode, LogEdge> Q_Q_CoSession_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//query-query graph, record co-session, co-click information
	public static Graph<LogNode, QueryEdge> Q_Q_Graph = new UndirectedSparseGraph<LogNode, QueryEdge>();
	//query-word bi-graph, records query and its consisting words information
	private static Graph<LogNode, LogEdge> Q_W_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	//
	private static Graph<LogNode, LogEdge> CoClick_Q_Q_Graph = new UndirectedSparseGraph<LogNode, LogEdge>();
	
	//word-word graph, record co-parent, co-session, co-click information
	private static Graph<LogNode, WordEdge> W_W_Graph = new UndirectedSparseGraph<LogNode, WordEdge>();
	
	
	//////////////////////////
	//part-1 generate unique files: attributes corresponding to :session-id(unit file), query, document(url)
	//////////////////////////	
	/**
	 * recordMap of one unit file
	 * **/
	private static HashMap<String, Vector<Record>> getRecordMapPerUnit(int unitSerial, LogVersion version){
		//
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = null;
		String unitFile = null;
		if(LogVersion.AOL == version){			
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";			
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";	
		}else if(LogVersion.SogouQ2008 == version){
			dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
			unitFile = dir + "SogouQ2008_Ordered_UTF8_"+unit+".txt";
		}else if(LogVersion.SogouQ2012 == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";	
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";
		}		
		//recordMap of one unit file
		HashMap<String, Vector<Record>> unitRecordMap = new HashMap<String, Vector<Record>>();
		//
		try{				
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader_UTF8(unitFile);
				//
				String recordLine = null;
				//int count = 0;
				Record record = null;				
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						if(LogVersion.AOL == version){
							record = new AOLRecord(recordLine, true);	
						}else if(LogVersion.SogouQ2008 == version){
							record = new SogouQRecord2008(recordLine);
						}else if(LogVersion.SogouQ2012 == version){
							record = new SogouQRecord2012(recordLine, true);
						}
					}catch(Exception ee){
						System.out.println("invalid record-line exist in "+unit);
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!= record && record.validRecord()){
						if(unitRecordMap.containsKey(record.getUserID())){
							unitRecordMap.get(record.getUserID()).add(record);
						}else{
							Vector<Record> recordVec = new Vector<Record>();
							recordVec.add(record);
							unitRecordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		//
		return unitRecordMap;
	}
	/**
	 * get unique elements per unit folder
	 * **/
	private static void getUniqueElementsPerUnit(int unitSerial, LogVersion version){		
		//output
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_PerUnit[version.ordinal()];	
		//
		String qDir = dir +"Query/";		
		File queryDirFile = new File(qDir);
		if(!queryDirFile.exists()){
			//System.out.println(queryDirFile.mkdir());
			queryDirFile.mkdirs();
		}
		String userIDDir = dir + "UserID/";
		File userIDDirFile = new File(userIDDir);
		if(!userIDDirFile.exists()){
			userIDDirFile.mkdirs();
		}
		String urlDir = dir + "Url/";
		File urlDirFile = new File(urlDir);
		if(!urlDirFile.exists()){
			urlDirFile.mkdirs();
		}		
		//
		String qFile = null, userIDFile = null, urlFile = null;
		qFile = qDir+version.toString()+"_UniqueQuery_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
		userIDFile = userIDDir+version.toString()+"_UniqueUserID_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";		
		urlFile = urlDir+version.toString()+"_UniqueUrl_"+StandardFormat.serialFormat(unitSerial, "00")+".txt";
		//
		//
		try{
			BufferedWriter queryWriter = IOText.getBufferedWriter_UTF8(qFile);
			BufferedWriter userIDWriter = IOText.getBufferedWriter_UTF8(userIDFile);
			BufferedWriter urlWriter = IOText.getBufferedWriter_UTF8(urlFile);
			//
			Hashtable<String, StrInt> queryTable = new Hashtable<String, StrInt>();
			Hashtable<String, StrInt> urlTable = new Hashtable<String, StrInt>();
			//
			HashMap<String, Vector<Record>> unitRecordMap = getRecordMapPerUnit(unitSerial, version);
			//
			for(Entry<String, Vector<Record>> entry: unitRecordMap.entrySet()){
				//for userID
				userIDWriter.write(entry.getKey().toString());
				userIDWriter.newLine();
				//
				Vector<Record> recordVec = entry.getValue();
				//no specific session segmentation, just for the same user
				HashSet<String> distinctQPerSession = new HashSet<String>();
				//
				for(Record record: recordVec){					
					//for query
					String queryText = record.getQueryText();
					if(!distinctQPerSession.contains(queryText)){
						distinctQPerSession.add(queryText);
					}
					//for clickUrl
					String clickUrl = record.getClickUrl();
					if(null != clickUrl){
						if(urlTable.containsKey(clickUrl)){
							urlTable.get(clickUrl).intPlus1();						
						}else{
							urlTable.put(clickUrl, new StrInt(clickUrl));
						}
					}					
				}				
				//up only once
				for(Iterator<String> itr = distinctQPerSession.iterator(); itr.hasNext(); ){
					String queryText = itr.next();
					if(queryTable.containsKey(queryText)){
						queryTable.get(queryText).intPlus1();						
					}else{
						queryTable.put(queryText, new StrInt(queryText));						
					}
				}				
			}
			//for userID
			userIDWriter.flush();
			userIDWriter.close();
			//for query			
			for(StrInt queryInstance: queryTable.values()){
				queryWriter.write(queryInstance.second+TabSeparator
						+queryInstance.first);
				queryWriter.newLine();
			}			
			queryWriter.flush();
			queryWriter.close();
			//for clicked url
			for(StrInt urlInstance: urlTable.values()){
				urlWriter.write(urlInstance.second+TabSeparator
						+urlInstance.first);
				urlWriter.newLine();
			}
			urlWriter.flush();
			urlWriter.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * get the distinct element per unit file from the whole query log
	 * **/
	public static void getUniqueElementsPerUnit(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				getUniqueElementsPerUnit(i, LogVersion.AOL);
			}
		}else if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				getUniqueElementsPerUnit(i, LogVersion.SogouQ2008);
			}
		}else if(LogVersion.SogouQ2012 == version){
			for(int i=1; i<=10; i++){
				getUniqueElementsPerUnit(i, LogVersion.SogouQ2012);
			}			
		}		
	}
	//
	private static Vector<StrInt> loadUniqueElementsPerUnit(String targetFile, ElementType elementType){
		Vector<StrInt> elementVector = new Vector<StrInt>();
		//
		BufferedReader unitReader;
		String elementLine;
		try {
			System.out.println("loading "+targetFile);
			unitReader = IOText.getBufferedReader_UTF8(targetFile);
			//
			while(null != (elementLine=unitReader.readLine())){
				try{
					if(ElementType.UserID == elementType){
						elementVector.add(new StrInt(elementLine));
					}else{
						int elementFre = Integer.parseInt(elementLine.substring(0, elementLine.indexOf(TabSeparator)));
						String elementText = elementLine.substring(elementLine.indexOf(TabSeparator)+1);
						//
						elementVector.add(new StrInt(elementText, elementFre));
					}										
				}catch(Exception ee){						
					System.out.println("Bad Line "+elementLine);						
					continue;
				}				
			}
			unitReader.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//
		return elementVector;
	}
	//
	private static Vector<StrInt> getUniqueElementsPerSpan(int unitStart, int unitEnd, 
			LogVersion version, ElementType elementType){
		//input
		String inputDir = null, inputFilePrefix = null;
		String rootDir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_PerUnit[version.ordinal()];	
		//
		if(ElementType.UserID == elementType){
			inputDir = rootDir + "UserID/";
			inputFilePrefix = version.toString()+"_UniqueUserID_";	
		}else if(ElementType.Query == elementType){
			inputDir = rootDir + "Query/";
			inputFilePrefix = version.toString()+"_UniqueQuery_";
		}else{
			inputDir = rootDir + "Url/";
			inputFilePrefix = version.toString()+"_UniqueUrl_";
		}
		//collector
		int maxStrID = STARTID;
		HashMap<String, Integer> elementMapPerSpan = new HashMap<String, Integer>();
		Vector<StrInt> elementVectorPerSpan = new Vector<StrInt>();
		//
		inputFilePrefix = inputDir + inputFilePrefix;
		for(int i=unitStart; i<=unitEnd; i++){
			String inputFile = inputFilePrefix + StandardFormat.serialFormat(i, "00")+".txt";
			//System.out.println("Loading ... "+inputFile);
			//
			Vector<StrInt> elementVectorPerUnit = loadUniqueElementsPerUnit(inputFile, elementType);
			//
			for(StrInt element : elementVectorPerUnit){
				if(elementMapPerSpan.containsKey(element.first)){
					int index = elementMapPerSpan.get(element.first);
					elementVectorPerSpan.get(index).intPlusk(element.second);						
				}else{
					elementMapPerSpan.put(element.first, maxStrID++);
					//
					elementVectorPerSpan.add(new StrInt(element.first, element.second));						
				}
			}
		}
		//
		return elementVectorPerSpan;
	}
	//get the distinct elements at the level of the whole query log
	public static void getUniqueElementsForAll(LogVersion version){
		String outputDir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueUserID_AllFile = version.toString()+"_UniqueUserID_All.txt";
		String uniqueQuery_AllFile = version.toString()+"_UniqueQuery_All.txt";
		String uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt";  
		//
		BufferedWriter userIDWriter, queryWriter, urlWriter;
		Vector<StrInt> uniqueUserID_All=null, uniqueQuery_All=null, uniqueClickUrl_All=null;
		if(LogVersion.AOL == version){
			uniqueUserID_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.UserID);			
			uniqueQuery_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.Query);			
			uniqueClickUrl_All = getUniqueElementsPerSpan(1, 10, LogVersion.AOL, ElementType.ClickUrl);						
		}else if(LogVersion.SogouQ2008 == version){
			uniqueUserID_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.UserID);
			uniqueQuery_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.Query);
			uniqueClickUrl_All = getUniqueElementsPerSpan(1, 31, LogVersion.SogouQ2008, ElementType.ClickUrl);			
		}else if(LogVersion.SogouQ2012 == version){
			uniqueUserID_All = getUniqueElementsPerSpan(1, 10, LogVersion.SogouQ2012, ElementType.UserID);			
			uniqueQuery_All = getUniqueElementsPerSpan(1, 10, LogVersion.SogouQ2012, ElementType.Query);			
			uniqueClickUrl_All = getUniqueElementsPerSpan(1, 10, LogVersion.SogouQ2012, ElementType.ClickUrl);	
		}
		//output
		try {
			//for userID
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){				
				outputDirFile.mkdirs();
			}
			userIDWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueUserID_AllFile);
			for(StrInt element: uniqueUserID_All){
				userIDWriter.write(element.getFirst());
				userIDWriter.newLine();
			}
			userIDWriter.flush();
			userIDWriter.close();
			//for query
			queryWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueQuery_AllFile);
			for(StrInt element: uniqueQuery_All){
				queryWriter.write(element.getSecond()+TabSeparator
						+element.getFirst());
				queryWriter.newLine();
			}
			queryWriter.flush();
			queryWriter.close();
			//for clickUrl
			urlWriter = IOText.getBufferedWriter_UTF8(outputDir+uniqueClickUrl_AllFile);
			for(StrInt element: uniqueClickUrl_All){
				urlWriter.write(element.getSecond()+TabSeparator
						+element.getFirst());
				urlWriter.newLine();
			}
			urlWriter.flush();
			urlWriter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//////////////////////////
	//access unique files
	//////////////////////////
	private static void loadUniqueQText(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueQuery_AllFile = version.toString()+"_UniqueQuery_All.txt";		
		//
		UniqueQTextList = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueQuery_AllFile, encoding);
		//
		if(null == UniqueQTextList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueUserID(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueUserID_AllFile = version.toString()+"_UniqueUserID_All.txt";
		//
		UniqueUserIDList = IOText.loadStrInts_LineFormat_Str(dir+uniqueUserID_AllFile, encoding);
		//
		if(null == UniqueUserIDList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static void loadUniqueClickUrl(LogVersion version, String encoding){
		String dir = DataDirectory.UniqueElementRoot+DataDirectory.Unique_All[version.ordinal()];		
		String uniqueClickUrl_AllFile = version.toString()+"_UniqueClickUrl_All.txt"; 		
		//
		UniqueClickUrlList = IOText.loadUniqueElements_LineFormat_IntTabStr(dir+uniqueClickUrl_AllFile, encoding);
		//
		if(null == UniqueClickUrlList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	//
	private static Integer getSessionID(LogVersion version, String userIDStr){		
		if(null == UniqueUserIDTable){
			if(null == UniqueUserIDList){
				loadUniqueUserID(version, "UTF-8");
			}
			//
			UniqueUserIDTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueUserIDList.size(); id++){
				UniqueUserIDTable.put(UniqueUserIDList.get(id-1).getFirst(), id);
			}			
		}
		return UniqueUserIDTable.get(userIDStr);						
	}
	private static Integer getQTextID(LogVersion version, String qText){	
		if(null == UniqueQTextTable){
			if(null==UniqueQTextList){
				loadUniqueQText(version, "UTF-8");
			}
			//
			UniqueQTextTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueQTextList.size(); id++){
				UniqueQTextTable.put(UniqueQTextList.get(id-1).getSecond(), id);
			}
		}		
		//
		return UniqueQTextTable.get(qText);		
	}
	private static Integer getClickUrlID(LogVersion version, String urlStr){
		if(null == UniqueClickUrlTable){
			if(null == UniqueClickUrlList){
				loadUniqueClickUrl(version, "UTF-8");;
			}			
			UniqueClickUrlTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueClickUrlList.size(); id++){
				UniqueClickUrlTable.put(UniqueClickUrlList.get(id-1).getSecond(), id);
			}
		}
		//
		return UniqueClickUrlTable.get(urlStr);							
	}
	private static Integer getWordID(LogVersion version, String wordStr){
		if(null == UniqueWordTable){
			if(null == UniqueWordList){
				loadUniqueWord(version, "UTF-8");
			}
			//
			UniqueWordTable = new Hashtable<String, Integer>();
			for(int id=STARTID; id<=UniqueWordList.size(); id++){
				UniqueWordTable.put(UniqueWordList.get(id-1).getSecond(), id);
			}
		}
		return UniqueWordTable.get(wordStr);
	}
	//
	private static int getUniqueNumberOfQuery(LogVersion version){
		if(null==UniqueQTextList){
			loadUniqueQText(version, "UTF-8");
		}
		return UniqueQTextList.size();		
	}
	private static int getUniqueNumberOfUserID(LogVersion version){
		if(null == UniqueUserIDList){
			loadUniqueUserID(version, "UTF-8");			
		}
		return UniqueUserIDList.size();
	}
	private static int getUniqueNumberOfClickUrl(LogVersion version){
		if(null == UniqueClickUrlList){
			loadUniqueClickUrl(version, "UTF-8");
		}
		return UniqueClickUrlList.size();
	}
	
	
	//////////////////////////
	//part-2 convert to digital unit file, essentially a compression
	//////////////////////////	
	public static void convertToDigitalUnitClickThrough(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				convertToDigitalUnitClickThrough(i, version);
			}
		}else if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				convertToDigitalUnitClickThrough(i, version);
			}
		}else if(LogVersion.SogouQ2012 == version){
			for(int i=1; i<=10; i++){
				convertToDigitalUnitClickThrough(i, version);
			}			
		}		
	}
	//
	private static void convertToDigitalUnitClickThrough(int unitSerial, LogVersion version){
		//
		//target file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = null;
		String unitFile = null;
		if(LogVersion.AOL == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";			
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";				
		}else if(LogVersion.SogouQ2008 == version){
			dir = DataDirectory.RawDataRoot + DataDirectory.RawData[version.ordinal()];
			unitFile = dir + "SogouQ2008_Ordered_UTF8_"+unit+".txt";
		}else if(LogVersion.SogouQ2012 == version){
			dir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";	
			unitFile = dir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";	
		}
		//output
		String outputDir = DataDirectory.DigitalFormatRoot+DataDirectory.DigitalFormat[version.ordinal()];	
		//
		try{
			//output			
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			//
			String digitalUnitFileName = outputDir+version.toString()+"_DigitalLog_"+unit+".txt";
			//
			BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(digitalUnitFileName);
			//
    		File fileExist = new File(unitFile);
			if(fileExist.exists()){	
				System.out.println("reading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader_UTF8(unitFile);
				//
				String recordLine = null;					
				while(null != (recordLine=reader.readLine())){
					if(LogVersion.AOL == version){
						AOLRecord aolRecord = null;
						try{
							aolRecord = new AOLRecord(recordLine, false);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);																
							continue;
						}		
						if(null!=aolRecord && aolRecord.validRecord()){
							Integer sessionID = getSessionID(version, aolRecord.getUserID().toString());						
							Integer qID = getQTextID(version, aolRecord.getQueryText());							
							//
							if(null!=sessionID && null!=qID){
								if(null != aolRecord.getItemRank()){
									Integer clickUrlID = getClickUrlID(version, aolRecord.getClickUrl());
									dWriter.write(sessionID+TabSeparator
											+qID+TabSeparator
											+aolRecord.getQueryTime()+TabSeparator
											+aolRecord.getItemRank()+TabSeparator
											+clickUrlID);
									dWriter.newLine();
								}else{
									dWriter.write(sessionID+TabSeparator
											+qID+TabSeparator
											+aolRecord.getQueryTime());
									dWriter.newLine();
								}								
							}else{
								if(null==sessionID){
									System.out.println("Null sessionid:\t"+recordLine);
								}else if(null == qID){
									System.out.println("Null qid:\t"+recordLine);
								}							
							}
						}													
					}else if(LogVersion.SogouQ2008 == version){
						SogouQRecord2008 record2008 = null;
						try {
							record2008 = new SogouQRecord2008(recordLine);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);								
							continue;
						}
						//				
						if(null!=record2008 && record2008.validRecord()){
							Integer sessionID = getSessionID(version, record2008.getUserID());						
							Integer qID = getQTextID(version, record2008.getQueryText());
							Integer docID = getClickUrlID(version, record2008.getClickUrl());
							//
							if(null!=sessionID && null!=qID && null!=docID){
								dWriter.write(sessionID+TabSeparator
										+qID+TabSeparator
										+record2008.getItemRank()+TabSeparator
										+record2008.getClickOrder()+TabSeparator
										+docID);
								dWriter.newLine();
							}else{
								if(null==sessionID){
									System.out.println("Null sessionid:\t"+recordLine);
								}else if(null == qID){
									System.out.println("Null qid:\t"+recordLine);
								}else{
									System.out.println("Null docid:\t"+recordLine);
								}							
							}
						}						
					}else if(LogVersion.SogouQ2012 == version){	
						SogouQRecord2012 record2012 = null;
						try {
							record2012 = new SogouQRecord2012(recordLine, false);
						}catch(Exception ee){
							System.out.println("invalid record-line exist in "+unit);
							System.out.println(recordLine);								
							continue;
						}
						//				
						if(null!=record2012 && record2012.validRecord()){
							Integer sessionID = getSessionID(version, record2012.getUserID());						
							Integer qID = getQTextID(version, record2012.getQueryText());
							Integer docID = getClickUrlID(version, record2012.getClickUrl());
							//
							if(null!=sessionID && null!=qID && null!=docID){
								dWriter.write(record2012.getQueryTime()+TabSeparator
										+sessionID+TabSeparator
										+qID+TabSeparator
										+record2012.getItemRank()+TabSeparator
										+record2012.getClickOrder()+TabSeparator
										+docID);
								dWriter.newLine();
							}else{
								if(null==sessionID){
									System.out.println("Null sessionid:\t"+recordLine);
								}else if(null == qID){
									System.out.println("Null qid:\t"+recordLine);
								}else{
									System.out.println("Null docid:\t"+recordLine);
								}							
							}
						}						
					}else{
						new Exception("Version Error!").printStackTrace();
					}
				}
				reader.close();
				reader=null;				
			}
			dWriter.flush();
			dWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//session-id -> set of records as a unit
	private static Vector<Vector<Record>> loadDigitalUnitClickThroughSessions(int unitSerial, LogVersion version){
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String inputDir = DataDirectory.DigitalFormatRoot+DataDirectory.DigitalFormat[version.ordinal()];
		//
		String digitalUnitFileName  = inputDir+version.toString()+"_DigitalLog_"+unit+".txt";				
		//buffering distinct sessions
		Vector<Vector<Record>> digitalUnitClickThroughSessions = new Vector<Vector<Record>>();
		Hashtable<String, Integer> sessionTable = new Hashtable<String, Integer>();		
		//		
		File file = new File(digitalUnitFileName);
		if(file.exists()){
			try{
				System.out.println("reading...\t"+file.getAbsolutePath());
				BufferedReader reader = IOText.getBufferedReader_UTF8(file.getAbsolutePath());
				//
				String line = null;				
				Record record = null;
				//
				while(null != (line=reader.readLine())){
					if(LogVersion.AOL == version){
						//digital construct
						record = new AOLRecord(line, true);	
					}else if(LogVersion.SogouQ2008 == version){
						//digital construct
						record = new SogouQRecord2008(line);
					}else if(LogVersion.SogouQ2012 == version){
						//using digital construct
						record = new SogouQRecord2012(line, true);
					}else{
						new Exception("Version Error!").printStackTrace();
					}				
					//
					if(null != record){
						String userID = record.getUserID();
						if(sessionTable.containsKey(userID)){
							digitalUnitClickThroughSessions.get(sessionTable.get(userID)).add(record);
						}else{
							//new id
							sessionTable.put(userID, digitalUnitClickThroughSessions.size());
							//
							Vector<Record> drVec = new Vector<Record>();
							drVec.add(record);
							digitalUnitClickThroughSessions.add(drVec);
						}						
					}else{
						System.out.println("Null DigitalRecord!");
					}
				}				
				reader.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		//
		return digitalUnitClickThroughSessions;				
	}
	///////////////////////////
	//query-level 
	///////////////////////////
	private static void ini_Q_D_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){			
			Q_D_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
		//
		int docNodeNumber = getUniqueNumberOfClickUrl(version);
		for(int id=STARTID; id<=docNodeNumber; id++){
			Q_D_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Doc));
		}
	}
	//for aol due to day by day operation
	private static void refresh_D_Q_GraphNodes(LogVersion version){
		Q_D_Graph = null;
		Q_D_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		ini_Q_D_GraphNodes(version);
	}
	private static void ini_Q_Q_CoSessionGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_Q_CoSession_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
	//for aol due to day by day operation
	private static void refresh_Q_Q_CoSessionGraphNodes(LogVersion version){
		Q_Q_CoSession_Graph = null;
		Q_Q_CoSession_Graph = new UndirectedSparseGraph<LogNode, LogEdge>(); 
		//
		ini_Q_Q_CoSessionGraphNodes(version);
	}
	//
	private static void ini_QQAttributeGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_Q_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
		
	
	//-0
	private static void generateGraphFile_QQCoSession(LogVersion version){
		ini_Q_Q_CoSessionGraphNodes(version);
		//
		int from, to;
		if(LogVersion.SogouQ2008 == version){		
			from=1;
			to=31;					
		}else{
			from=1;
			to=10;
		}
		//
		Vector<Vector<Record>> digitalUnitClickThroughSessions = null;
		for(int unitSerial=from; unitSerial<=to; unitSerial++){
			//load unit digital clickthrough file
			digitalUnitClickThroughSessions = loadDigitalUnitClickThroughSessions(unitSerial, version);
			int count = 1;				
			for(Vector<Record> drVec: digitalUnitClickThroughSessions){
				if(count%10000 == 0){
					System.out.println(count);
				}
				count++;
				//session-range queries
				HashSet<String> sessionQSet = new HashSet<String>();					
				for(Record dr: drVec){				
					if(!sessionQSet.contains(dr.getQueryText())){
						sessionQSet.add(dr.getQueryText());
					}				
				}
				//
				String [] coSessionQArray = sessionQSet.toArray(new String[1]);
				for(int i=0; i<coSessionQArray.length-1; i++){
					LogNode fNode = new LogNode(coSessionQArray[i], LogNode.NodeType.Query);
					for(int j=i+1; j<coSessionQArray.length; j++){
						LogNode lNode = new LogNode(coSessionQArray[j], LogNode.NodeType.Query);
						//
						LogEdge q_qEdge = Q_Q_CoSession_Graph.findEdge(fNode, lNode);
						if(null == q_qEdge){
							q_qEdge = new LogEdge(LogEdge.EdgeType.QQ);
							Q_Q_CoSession_Graph.addEdge(q_qEdge, fNode, lNode);
						}else{
							q_qEdge.upCount();
						}				
					}
				}
			}
		}	
		//output
		String outputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];
		try{	
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			//
			String qqCoSessionFile = outputDir+"Query_Query_CoSession.txt";
			BufferedWriter qqCoSessionWriter = IOText.getBufferedWriter_UTF8(qqCoSessionFile);			
			//++
			for(LogEdge edge: Q_Q_CoSession_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//
				qqCoSessionWriter.write(firstNode.getID()+TabSeparator
						+secondNode.getID()+TabSeparator
						+Integer.toString(edge.getCount()));
				qqCoSessionWriter.newLine();				
			}
			//++			
			qqCoSessionWriter.flush();
			qqCoSessionWriter.close();					
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//-1
	private static void generateGraphFile_QQCoClick(LogVersion version){
		ini_Q_D_GraphNodes(version);
		//
		int from, to;
		if(LogVersion.SogouQ2008 == version){		
			from=1;
			to=31;					
		}else{
			from=1;
			to=10;
		}
		//
		for(int unitSerial=from; unitSerial<=to; unitSerial++){
			//load unit digital clickthrough file
			Vector<Vector<Record>> digitalUnitClickThroughSessions = loadDigitalUnitClickThroughSessions(unitSerial, version);
			//
			int count = 1;		
			LogNode qNode=null, docNode=null;
			for(Vector<Record> drVec: digitalUnitClickThroughSessions){
				if(count%10000 == 0){
					System.out.println(count);
				}
				count++;
				for(Record dr: drVec){
					//1 query - clicked document
					if(null != dr.getClickUrl()){
						qNode = new LogNode(dr.getQueryText(), LogNode.NodeType.Query);
						docNode = new LogNode(dr.getClickUrl(), LogNode.NodeType.Doc);
						//
						LogEdge d_qEdge = Q_D_Graph.findEdge(qNode, docNode);
						if(null==d_qEdge){
							d_qEdge = new LogEdge(LogEdge.EdgeType.QDoc);
							Q_D_Graph.addEdge(d_qEdge, qNode, docNode);
						}else{
							d_qEdge.upCount();
						}
					}
				}			
			}	
		}		
		//output
		try{			
			String outputDir = DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()];
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			String qqCoClickFile = outputDir+"Query_Query_CoClick.txt";				
			BufferedWriter qqCoClickWriter = IOText.getBufferedWriter_UTF8(qqCoClickFile);
			for(LogNode node: Q_D_Graph.getVertices()){
				//doc node
				if(node.getType() == LogNode.NodeType.Doc){
					LogNode [] coClickQArray = Q_D_Graph.getNeighbors(node).toArray(new LogNode[1]);
					//
					for(int i=0; i<coClickQArray.length-1; i++){
						//
						LogEdge formerEdge = Q_D_Graph.findEdge(coClickQArray[i], node);
						//
						for(int j=i+1; j<coClickQArray.length; j++){
							//
							LogEdge latterEdge = Q_D_Graph.findEdge(coClickQArray[j], node);
							//
							int coFre = Math.min(formerEdge.getCount(), latterEdge.getCount());
							//
							qqCoClickWriter.write(coClickQArray[i].getID()+TabSeparator
									+coClickQArray[j].getID()+TabSeparator
									+Integer.toString(coFre));
							qqCoClickWriter.newLine();
						}				
					}
				}
			}
			//
			qqCoClickWriter.flush();
			qqCoClickWriter.close();				
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//-2
	private static void generateGraphFile_QQAttribute(LogVersion version){
		ini_QQAttributeGraphNodes(version);
		//
		String line;
		String[] array;
		try{
			//co-session attribute
			String coSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoSession.txt";
			BufferedReader coSessionReader = IOText.getBufferedReader_UTF8(coSessionFile);
			//			
			while(null != (line=coSessionReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);						
						int fre = Integer.parseInt(array[2]);
						//
						QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);						
						if(null != edge){
							edge.upAttributeCount(QueryEdge.QCoType.CoSession, fre);
						}else{
							edge = new QueryEdge();
							edge.upAttributeCount(QueryEdge.QCoType.CoSession, fre);
							Q_Q_Graph.addEdge(edge, firstNode, secondNode);
						}
					}
				}
			}
			//
			coSessionReader.close();
			//co-click attribute
			String coClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoClick.txt";	
			BufferedReader coClickReader = IOText.getBufferedReader_UTF8(coClickFile);
			//	
			while(null != (line=coClickReader.readLine())){
				if(line.length() > 0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);
						int fre = Integer.parseInt(array[2]);
						//
						QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);
						if(null != edge){
							edge.upAttributeCount(QueryEdge.QCoType.CoClick, fre);
						}else{
							edge = new QueryEdge();
							edge.upAttributeCount(QueryEdge.QCoType.CoClick, fre);
							Q_Q_Graph.addEdge(edge, firstNode, secondNode);
						}
					}
				}
			}
			coClickReader.close();
			//
			String qqAttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_Attribute.txt";
			BufferedWriter qqWriter = IOText.getBufferedWriter_UTF8(qqAttributeFile);
			//
			for(QueryEdge edge: Q_Q_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				int [] qCoArray = edge.getQCoArray();
				//
				qqWriter.write(firstNode.getID()+TabSeparator
						+secondNode.getID());
				//
				for(int i=0; i<qCoArray.length; i++){
					qqWriter.write(":"+Integer.toString(qCoArray[i]));
				}
				qqWriter.newLine();
			}
			qqWriter.flush();
			qqWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	//
	
	
	public static void load_QQAttributeGraph(LogVersion version){
		ini_QQAttributeGraphNodes(version);
		//
		try{
			String qqAttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_Attribute.txt";
			BufferedReader qqAttributeReader = IOText.getBufferedReader_UTF8(qqAttributeFile);
			//
			String line;
			String[] array;
			int count = 0;
			while(null != (line=qqAttributeReader.readLine())){
				if(line.length() > 0){
					//System.out.println(count++);
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
					//
					String [] parts = array[1].split(":");
					LogNode secondNode = new LogNode(parts[0], LogNode.NodeType.Query);
					//
					QueryEdge edge = new QueryEdge();
					edge.upAttributeCount(QueryEdge.QCoType.CoSession, Integer.parseInt(parts[1]));
					edge.upAttributeCount(QueryEdge.QCoType.CoClick, Integer.parseInt(parts[2]));
					//
					Q_Q_Graph.addEdge(edge, firstNode, secondNode);
					//
					line = null;
				}				
			}
			qqAttributeReader.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Interface for q-q attributes
	 * **/
	public static int [] getCoInfoOfQToQ(LogVersion version, String qStr_1, String qStr_2){
		Integer qID_1 = getQTextID(version, qStr_1);
		Integer qID_2 = getQTextID(version, qStr_2);
		if(null!=qID_1 && null!=qID_2 && qID_1!=qID_2){
			//
			if(null == Q_Q_Graph){
				ini_QQAttributeGraphNodes(version);
			}
			//
			LogNode firstNode = new LogNode(qID_1.toString(), LogNode.NodeType.Query);
			LogNode secondNode = new LogNode(qID_2.toString(), LogNode.NodeType.Query);
			QueryEdge edge = Q_Q_Graph.findEdge(firstNode, secondNode);
			if(null != edge){
				return edge.getQCoArray();
			}
		}
		//
		return null;
	}
	
	
	////////////////////////////////
	//word-level
	///////////////////////////////	
	private static void loadUniqueWord(LogVersion version, String encoding){		
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
		//
		UniqueWordList = IOText.loadUniqueElements_LineFormat_IntTabStr(uniqueWordFile, encoding);
		//
		if(null == UniqueWordList){
			new Exception("Loading Error!").printStackTrace();
		}		
	}
	private static int getUniqueNumberOfWord(LogVersion version){
		if(null == UniqueWordList){
			loadUniqueWord(version, "UTF-8");			
		}
		//
		return UniqueWordList.size();
	}
	private static void ini_W_W_GraphNodes(LogVersion version){
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int id=STARTID; id<=wordNodeNumber; id++){
			W_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Word));
		}
	}
	private static void ini_Q_W_GraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
		//
		int wordNodeNumber = getUniqueNumberOfWord(version);
		for(int id=STARTID; id<=wordNodeNumber; id++){
			Q_W_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Word));
		}
	}
	private static void ini_Q_Q_CoClickGraphNodes(LogVersion version){
		int queryNodeNumber = getUniqueNumberOfQuery(version);
		for(int id=STARTID; id<=queryNodeNumber; id++){
			CoClick_Q_Q_Graph.addVertex(new LogNode(Integer.toString(id), LogNode.NodeType.Query));
		}
	}
	//not quantified edge
	private static void loadQ_W_Graph(LogVersion version){
		ini_Q_W_GraphNodes(version);
		//
		String queryToMemberWordsFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_MemberWords.txt";
		//
		try{
			BufferedReader reader = IOText.getBufferedReader_UTF8(queryToMemberWordsFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length>1){
						LogNode qNode = new LogNode(array[0], LogNode.NodeType.Query);
						for(int i=1; i<array.length; i++){
							LogNode wNode = new LogNode(array[i], LogNode.NodeType.Word);
							//
							LogEdge qwEdge = new LogEdge(LogEdge.EdgeType.WQuery);
							Q_W_Graph.addEdge(qwEdge, qNode, wNode);
						}
					}
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		//test
		/*
		LogNode qNode = new LogNode("236", LogNode.NodeType.Query);
		for(LogNode wNode: this.Q_W_Graph.getNeighbors(qNode)){
			System.out.print(TextDataBase.getWordStr(Integer.parseInt(wNode.getID()))+"\t");
		}
		*/
	}
	public static void loadQ_Q_CoSessionGraph(LogVersion version){
		ini_Q_Q_CoSessionGraphNodes(version);
		//
		String q_q_CoSessionFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoSession.txt";
		try{
			BufferedReader reader = IOText.getBufferedReader_UTF8(q_q_CoSessionFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length() > 0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode sNode = new LogNode(array[0], LogNode.NodeType.Query);
						LogNode tNode = new LogNode(array[1], LogNode.NodeType.Query);
						int fre = Integer.parseInt(array[2]);
						//						
						LogEdge edge = Q_Q_CoSession_Graph.findEdge(sNode, tNode);
						if(edge == null){
							edge = new LogEdge(LogEdge.EdgeType.QQ);
							edge.setCount(fre);
							Q_Q_CoSession_Graph.addEdge(edge, sNode, tNode);
						}else{
							edge.upCount(fre);
						}						
					}
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private static void loadCoClick_Q_Q_Graph(LogVersion version){
		ini_Q_Q_CoClickGraphNodes(version);
		try{
			String q_q_CoClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_Query_CoClick.txt";
			BufferedReader reader = IOText.getBufferedReader_UTF8(q_q_CoClickFile);
			//
			String line;
			String[] array;
			while(null != (line=reader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Query);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Query);
					LogEdge edge = new LogEdge(LogEdge.EdgeType.QQ);
					edge.setCount(Integer.parseInt(array[2]));
					CoClick_Q_Q_Graph.addEdge(edge, firstNode, secondNode);
				}
			}
			reader.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//0
	public static void parsingQueriesToWords(LogVersion version){		
		loadUniqueQText(version, "UTF-8");		
		//
		String uniqueWordFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"UniqueWord.txt";
    	String wordToSourceQFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_SourceQueries.txt";
    	String queryToMemberWordsFile = 
    		DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Query_MemberWords.txt";
		//
    	Hashtable<String, Integer> wordTable = new Hashtable<String, Integer>();
    	Vector<UserWord> wordNodeVec = new Vector<UserWord>();
    	//    	
    	ArrayList<String> words;
		//
    	try {
    		//query to member words
    		BufferedWriter queryToMemberWordsWriter = IOText.getBufferedWriter_UTF8(queryToMemberWordsFile);
    		//
    		for(IntStrInt ununiqueQuery: UniqueQTextList){    			
    			if(LogVersion.AOL == version){
    				//words = Tokenizer.qSegment(ununiqueQuery.getSecond(), Lang.English);
    				words = Tokenizer.adaptiveQuerySegment(Lang.English, ununiqueQuery.getSecond(), "", true, true);
    			}else{
    				//words = Tokenizer.qSegment(ununiqueQuery.getSecond(), Lang.Chinese);
    				words = Tokenizer.adaptiveQuerySegment(Lang.Chinese, ununiqueQuery.getSecond(), "", true, true);
    			}
    			//
    			if(null!=words){
    				//distinct composing words
    				HashSet<String> wordSet = new HashSet<String>();
    				for(String word: words){
    					if(!wordSet.contains(word)){
    						wordSet.add(word);
    					}
    				}
    				//record source query and log frequency
    				for(Iterator<String> itr = wordSet.iterator(); itr.hasNext();){
    					String w = itr.next();
    					if(wordTable.containsKey(w)){
    						int id = wordTable.get(w);    						
    						UserWord wNode = wordNodeVec.get(id);    						
    						wNode.addFre(ununiqueQuery.getThird());
    						wNode.addSourceQ(ununiqueQuery.getFirst());
    					}else{    						
    						wordTable.put(w, wordNodeVec.size());
    						//
    						UserWord wNode = new UserWord(w, ununiqueQuery.getThird(), ununiqueQuery.getFirst());
    						wordNodeVec.add(wNode);
    					}
    				}
    				//record q to words
    				queryToMemberWordsWriter.write(Integer.toString(ununiqueQuery.getFirst()));
    				for(Iterator<String> itr=wordSet.iterator(); itr.hasNext();){
    					String w = itr.next();
    					int wid = wordTable.get(w);
    					queryToMemberWordsWriter.write(TabSeparator+Integer.toString(wid+1));        					
    				}
    				queryToMemberWordsWriter.newLine();
    			} 
    		}
    		queryToMemberWordsWriter.flush();
    		queryToMemberWordsWriter.close();
    		//unique words and word to source queries    		
    		BufferedWriter uniqueWordsWriter = IOText.getBufferedWriter_UTF8(uniqueWordFile);
    		BufferedWriter wordToSourceQWriter = IOText.getBufferedWriter_UTF8(wordToSourceQFile);
			//
    		UserWord wNode;
    		Vector<Integer> sourceQList;
    		for(int id=1; id<=wordNodeVec.size(); id++){
    			wNode = wordNodeVec.get(id-1);
    			sourceQList = wNode.sourceQList;
    			//to word small text
    			uniqueWordsWriter.write(wNode.logFre+TabSeparator+wNode.word);
    			uniqueWordsWriter.newLine();
    			//to word-query small text
    			//wordToQuerySmallTextWriter.write(Integer.toString(i));
    			for(Integer sQID: sourceQList){
    				wordToSourceQWriter.write(Integer.toString(id)+TabSeparator+sQID.toString());
    				wordToSourceQWriter.newLine();
    			}
    			//wordToQuerySmallTextWriter.newLine();
    		}    		
    		//
    		uniqueWordsWriter.flush();
    		uniqueWordsWriter.close();
    		//
    		wordToSourceQWriter.flush();
    		wordToSourceQWriter.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//
	//1	
	private static void generateUnmergedFile_WordCoParent(LogVersion version){
		if(null == UniqueQTextList){
			loadUniqueQText(version, "UTF-8");
		}
		//
		loadQ_W_Graph(version);
		//co-parent
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent_Unmerged.txt";
			BufferedWriter wwCoParentWriter = IOText.getBufferedWriter_UTF8(wwCoParentFile);
			//			
			for(IntStrInt uniqueQ: UniqueQTextList){				
				LogNode qNode = new LogNode(Integer.toString(uniqueQ.getFirst()), LogNode.NodeType.Query);
				//
				LogNode [] wNodeArray = Q_W_Graph.getNeighbors(qNode).toArray(new LogNode[1]);
				if(null == wNodeArray){
					System.out.println("Null consisting words");
					continue;
				}				
				//
				for(int j=0; j<wNodeArray.length-1; j++){
					for(int k=j+1; k<wNodeArray.length; k++){
						//
						wwCoParentWriter.write(wNodeArray[j].getID()+TabSeparator
								+wNodeArray[k].getID()+TabSeparator
								+Integer.toString(uniqueQ.getThird()));
						//
						wwCoParentWriter.newLine();												
					}
				}
			}			
			//
			wwCoParentWriter.flush();
			wwCoParentWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//2
	private static void generateUnmergedFile_WordCoSession(LogVersion version){
		//query co-session
		loadQ_Q_CoSessionGraph(version);
		//query with its consisting words
		loadQ_W_Graph(version);		
		//
		try{
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession_Unmerged.txt";
			//			
			BufferedWriter wwCoSessionWriter = IOText.getBufferedWriter_UTF8(w_w_CoSessionFile);
			//
			for(LogEdge qqEdge: Q_Q_CoSession_Graph.getEdges()){
				Pair<LogNode> pair = Q_Q_CoSession_Graph.getEndpoints(qqEdge);
				LogNode firstQNode = pair.getFirst();
				LogNode secondQNode = pair.getSecond();
				//
				for(LogNode firstWordNode: Q_W_Graph.getNeighbors(firstQNode)){
					for(LogNode secondWordNode: Q_W_Graph.getNeighbors(secondQNode)){
						if(!firstWordNode.equals(secondWordNode)){
							//
							wwCoSessionWriter.write(firstWordNode.getID()+TabSeparator
									+secondWordNode.getID()+TabSeparator
									+Integer.toString(qqEdge.getCount()));
							wwCoSessionWriter.newLine();
						}
					}
				}			
			}
			//
			wwCoSessionWriter.flush();
			wwCoSessionWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}				
	}
	//3
	private static void generateUnmergedFile_WordCoClick(LogVersion version){		
		//
		loadQ_W_Graph(version);		
		loadCoClick_Q_Q_Graph(version);
		//
		String w_w_CoClickFile = 
			DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick_Unmerged.txt";
		try{
			BufferedWriter w_w_Writer = IOText.getBufferedWriter_UTF8(w_w_CoClickFile);
			//
			for(LogEdge edge: CoClick_Q_Q_Graph.getEdges()){
				Pair<LogNode> pair = CoClick_Q_Q_Graph.getEndpoints(edge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				//
				for(LogNode fWNode: Q_W_Graph.getNeighbors(firstNode)){
					for(LogNode sWNode: Q_W_Graph.getNeighbors(secondNode)){
						if(!fWNode.equals(sWNode)){
							w_w_Writer.write(fWNode.getID()+TabSeparator
									+sWNode.getID()+TabSeparator
									+Integer.toString(edge.getCount()));
							//
							w_w_Writer.newLine();
						}
					}
				}
			}
			//
			w_w_Writer.flush();
			w_w_Writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//4	
	public static void generateFile_WWAttributeGraph(LogVersion version){
		ini_W_W_GraphNodes(version);
		//
		String line;
		String[] array;		
		try{
			String wwCoParentFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoParent_Unmerged.txt";
			BufferedReader coParentReader = IOText.getBufferedReader_UTF8(wwCoParentFile);
			//			
			while(null != (line=coParentReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					if(array.length > 1){
						LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
						LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
						int fre = Integer.parseInt(array[2]);
						//
						WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
						if(null != edge){
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
						}else{
							edge = new WordEdge();
							edge.upAttributeCount(WordEdge.WCoType.CoParent, fre);
							W_W_Graph.addEdge(edge, firstNode, secondNode);
						}						
					}
				}
			}
			coParentReader.close();
			//co-session
			String w_w_CoSessionFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoSession_Unmerged.txt";
			BufferedReader coSessionReader = IOText.getBufferedReader_UTF8(w_w_CoSessionFile);
			//			
			while(null != (line=coSessionReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					//
					WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
					if(null != edge){
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoSession, fre);
						W_W_Graph.addEdge(edge, firstNode, secondNode);
					}
				}
			}
			coSessionReader.close();
			//co-click
			String w_w_CoClickFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_CoClick_Unmerged.txt";
			BufferedReader coClickReader = IOText.getBufferedReader_UTF8(w_w_CoClickFile);
			//
			while(null != (line=coClickReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					LogNode secondNode = new LogNode(array[1], LogNode.NodeType.Word);
					int fre = Integer.parseInt(array[2]);
					WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
					if(null!=edge){
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
					}else{
						edge = new WordEdge();
						edge.upAttributeCount(WordEdge.WCoType.CoClick, fre);
						W_W_Graph.addEdge(edge, firstNode, secondNode);
					}
				}
			}
			coClickReader.close();
			//
			//
			String w_w_AttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_Attribute.txt";
			BufferedWriter wwWriter = IOText.getBufferedWriter_UTF8(w_w_AttributeFile);
			//
			for(WordEdge wEdge: W_W_Graph.getEdges()){
				Pair<LogNode> pair = W_W_Graph.getEndpoints(wEdge);
				LogNode firstNode = pair.getFirst();
				LogNode secondNode = pair.getSecond();
				int [] attributes = wEdge.getCoArray();
				//
				wwWriter.write(firstNode.getID()+TabSeparator+secondNode.getID());
				//
				for(int i=0; i<attributes.length; i++){
					wwWriter.write(":"+Integer.toString(attributes[i]));
				}
				wwWriter.newLine();
			}
			wwWriter.flush();
			wwWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//
	private static void load_WWAttributeGraph(LogVersion version){
		ini_W_W_GraphNodes(version);
		//
		try{
			String w_w_AttributeFile = 
				DataDirectory.ClickThroughGraphRoot+DataDirectory.GraphFile[version.ordinal()]+"Word_Word_Attribute.txt";
			BufferedReader wwReader = IOText.getBufferedReader_UTF8(w_w_AttributeFile);
			//
			String line;
			String[] array;
			//
			while(null != (line=wwReader.readLine())){
				if(line.length()>0){
					array = line.split(TabSeparator);
					LogNode firstNode = new LogNode(array[0], LogNode.NodeType.Word);
					//
					String [] parts = array[1].split(":");
					LogNode secondNode = new LogNode(parts[0], LogNode.NodeType.Word);
					//
					WordEdge edge = new WordEdge();
					edge.upAttributeCount(WordEdge.WCoType.CoParent, Integer.parseInt(parts[1]));
					edge.upAttributeCount(WordEdge.WCoType.CoSession, Integer.parseInt(parts[2]));
					edge.upAttributeCount(WordEdge.WCoType.CoClick, Integer.parseInt(parts[3]));
					//
					W_W_Graph.addEdge(edge, firstNode, secondNode);
				}
			}
			wwReader.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Interface for w-w attributes
	 * **/
	public static int [] getCoInfoOfWToW(LogVersion version, String word_1, String word_2){
		Integer wID_1 = getWordID(version, word_1);
		Integer wID_2 = getWordID(version, word_2);
		if(null!=wID_1 && null!=wID_2 && wID_1!=wID_2){
			if(null == W_W_Graph){
				load_WWAttributeGraph(version);
			}
			//
			LogNode firstNode = new LogNode(wID_1.toString(), LogNode.NodeType.Word);
			LogNode secondNode = new LogNode(wID_2.toString(), LogNode.NodeType.Word);
			WordEdge edge = W_W_Graph.findEdge(firstNode, secondNode);
			if(null != edge){
				return edge.getCoArray();
			}
		}
		return null;
	}
	
	
	
	//////////////////////////////////
	//PreProcess: session segmentation
	//(1): AOL clickthrough: time threshold 30 min;
	//(2): SogouQ2008: the same cookie id in a day;
	//(3): SogouQ2012: the same cookie id in a day;
	//////////////////////////////////
	/** analysis for SogouQ2012 session identification
	//�û�ID�Ǹ����û�ʹ�������������������ʱ��Cookie��Ϣ�Զ���ֵ����ͬһ��ʹ�����������Ĳ�ͬ��ѯ��Ӧͬһ���û�ID
	20111230000009	96994a0480e7e1edcaef67b20d8816b7	ΰ����	1	1	http://movie.douban.com/review/1128960/
	20111230000135	96994a0480e7e1edcaef67b20d8816b7	ΰ����	2	2	http://www.mtime.com/news/2009/02/20/1404845.html
	20111230000149	96994a0480e7e1edcaef67b20d8816b7	ΰ����	5	3	http://i.mtime.com/1449171/blog/4297703/
	20111230000439	96994a0480e7e1edcaef67b20d8816b7	ΰ����	9	4	http://news.xinhuanet.com/newmedia/2007-08/14/content_6527307.htm
	//
	 * **/
	
	////////////////////////////////////////////
	//Get ordered clickthrough SogouQ
	////////////////////////////////////////////
	//sogouQ2008 in descending order by click order due to no query time included
	//sogouQ2012 in descending order by query time
	private static void getOrderedSogouQ2012(){
		//input file		
		String inputDir = DataDirectory.RawDataRoot;
		String unitFile = inputDir + "SogouQ2012_Original/querylog";			
		//recordMap of one unit file
		HashMap<String, Vector<SogouQRecord2012>> recordMap = new HashMap<String, Vector<SogouQRecord2012>>();
		//
		try{		
			//input
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;				
				SogouQRecord2012 record = null;				
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						record = new SogouQRecord2012(recordLine, false);
					}catch(Exception ee){
						System.out.println("invalid record-line exist!");
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!=record && record.validRecord()){
						if(recordMap.containsKey(record.getUserID())){
							recordMap.get(record.getUserID()).add(record);
						}else{
							Vector<SogouQRecord2012> recordVec = new Vector<SogouQRecord2012>();
							recordVec.add(record);
							recordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
			//sort and output
			String outputDir = DataDirectory.OrderedSogouQRoot+"SogouQ2012/";
			File dirFile = new File(outputDir);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			String targetFile = outputDir + "SogouQ2012_Ordered_UTF8.txt";
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			//
			for(Entry<String, Vector<SogouQRecord2012>> entry: recordMap.entrySet()){				
				//
				Vector<SogouQRecord2012> recordVec = entry.getValue();
				java.util.Collections.sort(recordVec);
				//no specific session segmentation, just for the same user
				for(SogouQRecord2012 r: recordVec){
					//
					writer.write(r.getQueryTime()+TabSeparator
							+r.getUserID()+TabSeparator
							+r.getQueryText()+TabSeparator
							+r.getItemRank()+TabSeparator
							+r.getClickOrder()+TabSeparator
							+r.getClickUrl());
					//
					writer.newLine();
				}			
			}
			//
			writer.flush();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	private static void getOrderedSogouQ2008(int unitSerial){
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		//sort and output
		String outputDir = DataDirectory.OrderedSogouQRoot+LogVersion.SogouQ2008.toString()+"/";
		File dirFile = new File(outputDir);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		//
		String targetFile = outputDir + LogVersion.SogouQ2008.toString() +"_Ordered_UTF8_"+unit+".txt";
			
		//input				
		String inputDir = DataDirectory.RawDataRoot+DataDirectory.RawData[LogVersion.SogouQ2008.ordinal()];
		String unitFile = inputDir + "access_log.200608"+unit+".decode.filter";									
		//recordMap of one unit file
		HashMap<String, Vector<SogouQRecord2008>> recordMap = new HashMap<String, Vector<SogouQRecord2008>>();
		//
		try{		
			//input
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;				
				SogouQRecord2008 record = null;				
				while(null!=(recordLine=reader.readLine())){
					//System.out.println(count++);					
					try{							
						record = new SogouQRecord2008(unit, recordLine);
					}catch(Exception ee){
						System.out.println("invalid record-line exist!");
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					//
					if(null!=record && record.validRecord()){
						if(recordMap.containsKey(record.getUserID())){
							recordMap.get(record.getUserID()).add(record);
						}else{
							Vector<SogouQRecord2008> recordVec = new Vector<SogouQRecord2008>();
							recordVec.add(record);
							recordMap.put(record.getUserID(), recordVec);
						}
					}																
				}
				reader.close();
				reader=null;				
			}
			//
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			//
			for(Entry<String, Vector<SogouQRecord2008>> entry: recordMap.entrySet()){				
				//
				Vector<SogouQRecord2008> recordVec = entry.getValue();
				java.util.Collections.sort(recordVec);
				//no specific session segmentation, just for the same user
				for(SogouQRecord2008 r: recordVec){
					//
					writer.write(r.getUserID()+TabSeparator
							+r.getQueryText()+TabSeparator
							+r.getItemRank()+TabSeparator
							+r.getClickOrder()+TabSeparator
							+r.getClickUrl());
					//
					writer.newLine();
				}			
			}
			//
			writer.flush();
			writer.close();
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	public static void getOrderedSogouQ(LogVersion version){
		if(LogVersion.SogouQ2008 == version){
			for(int i=1; i<=31; i++){
				getOrderedSogouQ2008(i);
			}
		}else if(LogVersion.SogouQ2012 == version){
			getOrderedSogouQ2012();
		}		
	}
	
	/////////////////////////////////////
	//segment SogouQ2012 into 10 units
	/////////////////////////////////////
	public static String getUserID(String record){
		String [] fields = record.split(ClickThroughAnalyzer.TabSeparator);
		if(6 == fields.length){
			return fields[1];
		}else{
			System.out.println("bad line!");
			return null;
		}		
	}
	public static void splitSogouQ2012(){
		String orderedSogouQ2012 = "E:/Data_Log/DataSource_Analyzed/OrderedSogouQ/SogouQ2012/SogouQ2012_Ordered_UTF8.txt";
		String outputDir = "E:/Data_Log/DataSource_Raw/SogouQ2012/";
		
		int unitSerial = 1;
		String outputFile = "SogouQ2012-Collection-UTF8-"+StandardFormat.serialFormat(unitSerial, "00")+".txt";
		try {
			BufferedReader reader = IOText.getBufferedReader_UTF8(orderedSogouQ2012);
			String record = null;
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(outputDir+outputFile);
			Vector<String> vec = new Vector<String>();
			//
			int count = 1;
			int unitSize = 4500000;
			while(null != (record=reader.readLine())){
				if(count%unitSize==0 || vec.size()>0){
					if(vec.size() > 0){
						if(getUserID(record).equals(getUserID(vec.get(0)))){
							writer.write(vec.get(0));
							writer.newLine();
							//
							vec.clear();
							vec.add(record);							
						}else{
							writer.write(vec.get(0));
							writer.newLine();
							writer.flush();
							writer.close();
							writer = null;
							vec.clear();
							//
							unitSerial++;
							outputFile = "SogouQ2012-Collection-UTF8-"+StandardFormat.serialFormat(unitSerial, "00")+".txt";
							writer = IOText.getBufferedWriter_UTF8(outputDir+outputFile);
							writer.write(record);
							writer.newLine();
						}
					}else if(count%unitSize == 0){
						vec.add(record);
					}
				}else{
					writer.write(record);
					writer.newLine();
				}
				//
				count++;
			}
			//
			reader.close();
			//
			writer.flush();
			writer.close();					
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//////////////////////////////////////
	//perform simple session segmentation
	//////////////////////////////////////
	public static void performSessionSegmentation(LogVersion version){
		if(LogVersion.AOL == version){
			for(int i=1; i<=10; i++){
				segmentSessions(i, version);				
			}
		}else if(LogVersion.SogouQ2012 == version){
			for(int i=1; i<=10; i++){
				segmentSessions(i, version);				
			}			
		}else{
			new Exception("Version error!").printStackTrace();
		}
	}
	//
	private static void segmentSessions(int unitSerial, LogVersion version){		
		//input file
		String unit = StandardFormat.serialFormat(unitSerial, "00");
		String dir = DataDirectory.RawDataRoot+DataDirectory.RawData[version.ordinal()];
		String inputFile = null;
		if(LogVersion.AOL == version){			
			inputFile = dir + "user-ct-test-collection-"+unit+".txt";
		}else if (LogVersion.SogouQ2012 == version) {					
			inputFile = dir + "SogouQ2012-Collection-UTF8-"+unit+".txt";
		}
		//
		try{	
			//output
			String outputDir = DataDirectory.SessionSegmentationRoot+version.toString()+"/";
			File outputDirFile = new File(outputDir);
			if(!outputDirFile.exists()){
				outputDirFile.mkdirs();
			}
			String outputFile = outputDir + version.toString()+"_Sessioned_"+SessionSegmentationThreshold+"_"+unit+".txt";		
			//
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(outputFile);
			//
    		File file = new File(inputFile);
			if(file.exists()){	
				System.out.println("loading...\t"+inputFile);
				BufferedReader reader = null;	
				if(LogVersion.AOL == version){			
					reader = IOText.getBufferedReader(inputFile, "GBK");	
				}else if (LogVersion.SogouQ2012 == version) {					
					reader = IOText.getBufferedReader_UTF8(inputFile);	
				}
				String recordLine = null;
				
				//
				if(LogVersion.AOL == version){
					//
					AOLRecord formerRecord = null, newRecord = null;
					Date referenceDate = null;
					//overlook the first line, which is attribute names
					reader.readLine();
					//first record
					int sessionID = STARTID;
					recordLine = reader.readLine();
					formerRecord = new AOLRecord(recordLine, false);
					referenceDate = formerRecord.getDateQueryTime();
					if(!formerRecord.hasClickEvent()){
						//
						writer.write(unit+"-"+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getQueryTime());
						//
						writer.newLine();
					}else{
						//
						writer.write(unit+"-"+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getQueryTime()+TabSeparator
								+formerRecord.getItemRank()+TabSeparator
								+formerRecord.getClickUrl());
						//
						writer.newLine();
					}					
					//
					while(null!=(recordLine=reader.readLine())){
						newRecord = new AOLRecord(recordLine, false);
						try {
							if(!newRecord.getUserID().equals(formerRecord.getUserID())){
								sessionID = STARTID;
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());
									//
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());
									//
									writer.newLine();
								}
								//
								formerRecord = newRecord;
								referenceDate = newRecord.getDateQueryTime();
							}else if(ClickTime.getTimeSpan_MM(referenceDate, newRecord.getDateQueryTime()) 
									<= SessionSegmentationThreshold){
								//same session
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());								
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());								
									writer.newLine();
								}							
							}else{
								//same user id, but another session
								sessionID++;
								//
								if(newRecord.hasClickEvent()){
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime()+TabSeparator
											+newRecord.getItemRank()+TabSeparator
											+newRecord.getClickUrl());								
									writer.newLine();
								}else{								
									writer.write(unit+"-"+newRecord.getUserID()+"-"+sessionID+TabSeparator
											+newRecord.getQueryText()+TabSeparator
											+newRecord.getQueryTime());								
									writer.newLine();
								}
								//
								referenceDate = newRecord.getDateQueryTime();							
							}
						} catch (Exception e) {
							// TODO: handle exception
							System.out.println(recordLine);
						}																									
					}
					//over
				}else if(LogVersion.SogouQ2012 == version){
					//
					SogouQRecord2012 formerRecord = null, newRecord = null;
					Date referenceDate = null;					
					//first record
					int sessionID = STARTID;
					recordLine = reader.readLine();
					formerRecord = new SogouQRecord2012(recordLine, false);
					referenceDate = formerRecord.getDateQueryTime();
					if(formerRecord.validRecord()){						
						writer.write(formerRecord.getQueryTime()+TabSeparator
								+formerRecord.getUserID()+"-"+sessionID+TabSeparator
								+formerRecord.getQueryText()+TabSeparator
								+formerRecord.getItemRank()+TabSeparator
								+formerRecord.getClickOrder()+TabSeparator
								+formerRecord.getClickUrl());						
						writer.newLine();
					}				
					//
					while(null!=(recordLine=reader.readLine())){
						newRecord = new SogouQRecord2012(recordLine, false);
						//						
						if(!newRecord.getUserID().equals(formerRecord.getUserID())){
							sessionID = STARTID;
							//
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}
							//
							formerRecord = newRecord;
							referenceDate = newRecord.getDateQueryTime();
						}else if(ClickTime.getTimeSpan_MM(referenceDate, newRecord.getDateQueryTime()) 
								<= SessionSegmentationThreshold){
							if(ClickTime.getTimeSpan_MM(referenceDate, newRecord.getDateQueryTime()) 
									< 0){
								System.out.println(recordLine);
							}
							//same session
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}						
						}else{
							//same user id, but another session
							sessionID++;
							//
							if(newRecord.validRecord()){						
								writer.write(newRecord.getQueryTime()+TabSeparator
										+newRecord.getUserID()+"-"+sessionID+TabSeparator
										+newRecord.getQueryText()+TabSeparator
										+newRecord.getItemRank()+TabSeparator
										+newRecord.getClickOrder()+TabSeparator
										+newRecord.getClickUrl());						
								writer.newLine();
							}
							//
							referenceDate = newRecord.getDateQueryTime();							
						}																											
					}
				}
				//
				reader.close();
				reader=null;	
				//
				writer.flush();
				writer.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	//--test for fajie
	private static void getNumberOfRecordsSogouQ2012(){
		//input file		
		String inputDir = DataDirectory.RawDataRoot;
		String unitFile = inputDir + "SogouQ2012_Original/querylog";			
		//recordMap of one unit file
		HashMap<String, Vector<SogouQRecord2012>> recordMap = new HashMap<String, Vector<SogouQRecord2012>>();
		//
		int count = 0;
		int acceptedC = 0;
		try{		
			//input
    		File file = new File(unitFile);
			if(file.exists()){	
				System.out.println("loading...\t"+unitFile);
				BufferedReader reader = IOText.getBufferedReader(unitFile, "GBK");
				//
				String recordLine = null;				
				SogouQRecord2012 record = null;				
				while(null!=(recordLine=reader.readLine())){
										
					try{							
						record = new SogouQRecord2012(recordLine, false);
						if(null != record){
							
						}
					}catch(Exception ee){
						System.out.println("invalid record-line exist!");
						System.out.println(recordLine);
						System.out.println();
						recordLine=null;
						record=null;
						continue;
					}
					count++;
					//
					if(null!=record && record.validRecord()){
						acceptedC++;
					}																
				}
				reader.close();
				reader=null;				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}	
		
		System.out.println("count:\t"+count);
		System.out.println("accepted:\t"+acceptedC);
	}
	//
	
	//Bing organic search log
	//distribution of session-click (i.e., the number of clicks in a session)
	//unique urls within sessions that include clicks
	//unique queries within sessions that include clicks
	private static void getStatistics(int atLeastClickNum){
		try {
			String rawDataFile = "E:/Tao_CICEE/MSRA/DataPackage/D2/OrganicSearchPart_Raw_Week/OrganicSearchPart_Raw_Week.txt";
			//FilteredBingOrganicSearchLog_AtLeast_2Click
			String outputPath = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_"+Integer.toString(atLeastClickNum)+"Click/";
			
			String acceptedSessionFile = outputPath+"AcceptedSessionData.txt";
			BufferedWriter acceptedSessionWriter = IOText.getBufferedWriter_UTF8(acceptedSessionFile);
			
			String uniqueQFile = outputPath+"UniqueQueryInSessionsWithClicks.txt";
			BufferedWriter uniqueQWriter = IOText.getBufferedWriter_UTF8(uniqueQFile);
			
			String uniqueUrlFile = outputPath+"UniqueUrlInSessionsWithClicks.txt";
			BufferedWriter uniqueUrlWriter = IOText.getBufferedWriter_UTF8(uniqueUrlFile);

			//corresponding to click numbers of 1,2,3,4,5
			int [] clickNumDistribution = new int [5];
			
			//distribution of query frequency (from sessions with clicks)
			ArrayList<org.archive.util.tuple.Pair<String, Integer>> uniqueQList = 
					new ArrayList<org.archive.util.tuple.Pair<String,Integer>>();
			//q-> index (w.r.t. uniqueQList)
			HashMap<String, Integer> uniqueQMap = new HashMap<String, Integer>();
			
			
			//unique queries within sessions that include clicks
			ArrayList<String> uniqueUrlList = new ArrayList<String>();
			HashSet<String> uniqueUrlSet = new HashSet<String>();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rawDataFile)));
			String line = null;
			//10: userid, requestTime, sessionid, query, rguid, rank, url, isClick, clickTime, time   
			String [] parts;
			BingQSession1 qSession1 = null;
			
			int count = 0;
			int total = 85;
			
			int totalSessions = 0;
			int acceptedSessions = 0;
						
			while(null !=(line=reader.readLine())){
				parts = line.split("\t");				
				
				/*
				if((++count) > total){
					break;
				}
				*/
				
				String userID_Rguid = parts[0]+parts[4];
				//String userid= parts[0];
				String query = parts[3];
				//String rguid = parts[4];
				String url = parts[6];
				int isClicked = Integer.parseInt(parts[7]);
				
				//System.out.println((count)+"\t"+parts.length+"\t"+query+"\t"+isClicked);
				
				if(null!=qSession1 && qSession1.getKey().equals(userID_Rguid)){
					qSession1.addDisplayedUrl(url, isClicked>0);
					qSession1.addRecord(line);
				}else{
					if(null != qSession1){
					//new session
						if(qSession1.isClicked() && qSession1.getClickNum()>=atLeastClickNum){
							//System.out.println(qSession1.toString());							
							acceptedSessions ++;
							
							//1
							int clickNum = qSession1.getClickNum();
							if(clickNum>5){
								clickNumDistribution[4]++;
							}else{
								clickNumDistribution[clickNum-1]++;
							}							
							
							//2
							String clickedQ = qSession1.getQuery();
							if(uniqueQMap.containsKey(clickedQ)){
								uniqueQList.get(uniqueQMap.get(clickedQ)).second++;
							}else{
								uniqueQList.add(new org.archive.util.tuple.Pair<String, Integer>(clickedQ, 1));
								uniqueQMap.put(clickedQ, uniqueQList.size()-1);
							}
							
							//3
							ArrayList<String> displayedUrlList = qSession1.getDisplayedUrlList();
							for(String d: displayedUrlList){
								if(!uniqueUrlSet.contains(d)){
									uniqueUrlList.add(d);
									uniqueUrlSet.add(d);
								}
							}
							
							//4
							ArrayList<String> recordList = qSession1.getRecords();
							for(String r: recordList){
								acceptedSessionWriter.write(StandardFormat.serialFormat(acceptedSessions, "000000")+"\t"+r);
								acceptedSessionWriter.newLine();
							}
							
							//new
							qSession1 = new BingQSession1(userID_Rguid, query);
							qSession1.addDisplayedUrl(url, isClicked>0);
							qSession1.addRecord(line);
							
						}else{
							//System.out.println(qSession1.toString());
							
							//new
							qSession1 = new BingQSession1(userID_Rguid, query);
							qSession1.addDisplayedUrl(url, isClicked>0);
							qSession1.addRecord(line);								
						}
						
						totalSessions++;
					}else{
						qSession1 = new BingQSession1(userID_Rguid, query);
						qSession1.addDisplayedUrl(url, isClicked>0);
						qSession1.addRecord(line);
						
					}					
				}				
			}
			
			//the last session
			totalSessions++;
			
			if(qSession1.isClicked() && qSession1.getClickNum()>=atLeastClickNum){
				//System.out.println(qSession1.toString());
				
				acceptedSessions ++;
				
				//1
				int clickNum = qSession1.getClickNum();
				if(clickNum>5){
					clickNumDistribution[4]++;
				}else{
					clickNumDistribution[clickNum-1]++;
				}
				
				//2
				String clickedQ = qSession1.getQuery();
				if(uniqueQMap.containsKey(clickedQ)){
					uniqueQList.get(uniqueQMap.get(clickedQ)).second++;
				}else{
					uniqueQList.add(new org.archive.util.tuple.Pair<String, Integer>(clickedQ, 1));
					uniqueQMap.put(clickedQ, uniqueQList.size()-1);
				}
				
				//3
				ArrayList<String> displayedUrlList = qSession1.getDisplayedUrlList();
				for(String d: displayedUrlList){
					if(!uniqueUrlSet.contains(d)){
						uniqueUrlList.add(d);
						uniqueUrlSet.add(d);
					}
				}
				
				//4
				ArrayList<String> recordList = qSession1.getRecords();
				for(String r: recordList){
					acceptedSessionWriter.write(StandardFormat.serialFormat(acceptedSessions, "000000")+"\t"+r);
					acceptedSessionWriter.newLine();
				}
			}
			
			Collections.sort(uniqueQList, new PairComparatorBySecond_Desc<String, Integer>());
			
			for(org.archive.util.tuple.Pair<String,Integer> staQ: uniqueQList){
				uniqueQWriter.write(staQ.getSecond()+"\t"+staQ.getFirst());
				uniqueQWriter.newLine();
			}
			
			for(String uniqueUrl: uniqueUrlList){
				uniqueUrlWriter.write(uniqueUrl);
				uniqueUrlWriter.newLine();
			}
			
			//totally
			acceptedSessionWriter.flush();
			acceptedSessionWriter.close();
			
			uniqueQWriter.flush();
			uniqueQWriter.close();
			
			uniqueUrlWriter.flush();
			uniqueUrlWriter.close();	
			
			for(int i=0; i<clickNumDistribution.length; i++){
				System.out.println("ClickNum-"+(i+1)+"\t"+clickNumDistribution[i]);
			}
			
			System.out.println("Total number of sessions:\t"+totalSessions);
			System.out.println("Accepted number of sessions:\t"+acceptedSessions);		
			
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
		
	}
	//split url list
	private static void splitUrlList(){
		try {			
			String inputFile = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/UniqueUrlInSessions_RemainingWithoutTop100.txt";
			ArrayList<String> urlList = IOText.getLinesAsAList_UTF8(inputFile);
			
			String dir = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/SplittedUrlFiles/";
			
			ArrayList<BufferedWriter> writerList = new ArrayList<BufferedWriter>();			
			for(int i=0; i<10; i++){
				String outputFile = dir+StandardFormat.serialFormat((i+1), "00");
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(outputFile);
				writerList.add(writer);				
			}
			
			for(int i=0; i<urlList.size(); i++){
				String url = urlList.get(i);
				
				BufferedWriter writer = writerList.get(i%10);
				writer.write(url);
				writer.newLine();
			}
			
			for(BufferedWriter writer: writerList){
				writer.flush();
				writer.close();
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//
	private static void filterUrls(String dir){
		try {
			File dirFile = new File(dir);
			File [] files = dirFile.listFiles();
			
			for(File uFile: files){
				ArrayList<String> urlList = IOText.getLinesAsAList_UTF8(uFile.getAbsolutePath());
				
				String name = uFile.getName();
				String tFile = uFile.getAbsolutePath().substring(0, uFile.getAbsolutePath().lastIndexOf("\\"))+"/"
				+StandardFormat.serialFormat(Integer.parseInt(name), "0000");
				
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(tFile);
				
				for(String url: urlList){
					if(url.endsWith(".doc") || url.endsWith(".DOC") 
							|| url.endsWith(".pdf") || url.endsWith(".PDF")
							|| url.endsWith(".docx") || url.endsWith(".DOCX")
							|| url.endsWith(".ppt") || url.endsWith(".PPT")){
						continue;
					}else {
						writer.write(url);
						writer.newLine();
					}					
				}
				writer.flush();
				writer.close();				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//check tail of urls
	private static void checkSuffix(){
		String file = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/UniqueUrlInSessions_RemainingWithoutTop100.txt";
		HashMap<String, org.archive.util.tuple.Pair<String, Integer>> suffixMap 
		= new HashMap<String, org.archive.util.tuple.Pair<String,Integer>>();
		
		try {
			
			ArrayList<String> urlList = IOText.getLinesAsAList_UTF8(file);
			for(String url: urlList){
				String suffix = url.substring(url.lastIndexOf("."), url.length());
				if(suffixMap.containsKey(suffix)){
					suffixMap.get(suffix).second++;					
				}else{
					suffixMap.put(suffix, new org.archive.util.tuple.Pair<String, Integer>(suffix, 1));
				}
			}
			
			ArrayList<org.archive.util.tuple.Pair<String, Integer>> suffixList 
			= new ArrayList<org.archive.util.tuple.Pair<String,Integer>>();
			
			for(org.archive.util.tuple.Pair<String, Integer> element: suffixMap.values()){
				suffixList.add(element);				
			}
			
			Collections.sort(suffixList, new PairComparatorBySecond_Desc<String, Integer>());
			
			for(org.archive.util.tuple.Pair<String, Integer> element: suffixList){
				if(element.getSecond() > 1 
						&& !(element.getFirst().indexOf("/")>=0)
						&& !(element.getFirst().indexOf("?")>=0)){
					System.out.println(element.getSecond()+"\t"+element.getFirst());

				}
			}
			
			
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	//
	private static void getUrlListOfTop100Sessions(){
		try {
			String file = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/AcceptedSessionData_AtLeast_2Click.txt";
			ArrayList<BingQSession1> sessionList = ClickThroughAnalyzer.loadSearchLog(file);
			
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(
					"C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/UrlList_Top100Sessions.txt");
			HashSet<String> urlSet = new HashSet<String>();
			
			for(int i=0; i<100; i++){
				BingQSession1 session = sessionList.get(i);
				ArrayList<String> recordList = session.getRecords();
				for(String record: recordList){
					String [] parts = record.split("\t");
					String url = parts[7];
					if(!urlSet.contains(url)){
						urlSet.add(url);
						writer.write(url);
						writer.newLine();
					}
				}
			}
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	private static ArrayList<BingQSession1> loadSearchLog(String file){
		
		ArrayList<BingQSession1> sessionList = new ArrayList<BingQSession1>();
		int count = 0;
				
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

			String line = null;
			//10: userid, requestTime, sessionid, query, rguid, rank, url, isClick, clickTime, time  
			//11: pseudo-id, userid, requestTime, sessionid, query, rguid, rank, url, isClick, clickTime, time 
			//!!now the number of parts is 11, since we added the pseudo accepted session serial
			String [] parts;
			BingQSession1 qSession1 = null;		
			
			while(null !=(line=reader.readLine())){
				parts = line.split("\t");
				
				String userID_Rguid = parts[0]+parts[1]+parts[5];
				//String userid= parts[0];
				String query = parts[4];
				//String rguid = parts[4];
				String url = parts[7];
				int isClicked = Integer.parseInt(parts[8]);
				
				//System.out.println((count)+"\t"+parts.length+"\t"+query+"\t"+isClicked);
				
				if(null!=qSession1 && qSession1.getKey().equals(userID_Rguid)){
					qSession1.addDisplayedUrl(url, isClicked>0);
					qSession1.addRecord(line);
				}else{
					if(null != qSession1){
						count ++;
						sessionList.add(qSession1);	
						
						//new
						qSession1 = new BingQSession1(userID_Rguid, query);
						qSession1.addDisplayedUrl(url, isClicked>0);
						qSession1.addRecord(line);
						
					}else{
						
						qSession1 = new BingQSession1(userID_Rguid, query);
						qSession1.addDisplayedUrl(url, isClicked>0);
						qSession1.addRecord(line);						
					}					
				}				
			}
			
			//last session			
			sessionList.add(qSession1);
			count ++;
			
			reader.close();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		System.out.println("Loaded Session Count:\t"+count);
		
		return sessionList;
		
		//for check only
		/*
		try {
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(
					"C:/T/WorkBench/Corpus/DataSource_Analyzed/text.txt");
			
			BingQSession1 qSession1 = null;
			for(int i=0; i<sessionList.size(); i++){
				qSession1 = sessionList.get(i);
				ArrayList<String> recordList = qSession1.getRecords();
				for(String r: recordList){
					writer.write(StandardFormat.serialFormat((i+1), "000000")+r);
					writer.newLine();
				}				
			}
			
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/		
	}
	
	public static void main(String []args){
		//ClickThroughAnalyzer.getNumberOfRecordsSogouQ2012();
		
		/** get ordered files **/
		//ClickThroughAnalyzer.getOrderedSogouQ(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.getOrderedSogouQ(LogVersion.SogouQ2012);
		
		/** split SogouQ2012 **/
		//ClickThroughAnalyzer.splitSogouQ2012();
		
		/** session segmentation **/
		//ClickThroughAnalyzer.performSessionSegmentation(LogVersion.AOL);
		//ClickThroughAnalyzer.performSessionSegmentation(LogVersion.SogouQ2012);
		
			
		//1 get the distinct element per unit file from the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.getUniqueElementsPerUnit(LogVersion.SogouQ2012);
		//*/
		
		//2 get the distinct elements at the level of the whole query log
		///*
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.AOL);
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.getUniqueElementsForAll(LogVersion.SogouQ2012);
		//*/
		
		//3 convert to digital format
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.AOL);
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.convertToDigitalUnitClickThrough(LogVersion.SogouQ2012);
		
		//4 QQCoSession
		//ClickThroughAnalyzer.generateGraphFile_QQCoSession(LogVersion.AOL);
		//ClickThroughAnalyzer.generateGraphFile_QQCoSession(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateGraphFile_QQCoSession(LogVersion.SogouQ2012);
		
		//5	QQCoClick	
		//ClickThroughAnalyzer.generateGraphFile_QQCoClick(LogVersion.AOL);
		//ClickThroughAnalyzer.generateGraphFile_QQCoClick(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateGraphFile_QQCoClick(LogVersion.SogouQ2012);
		
		//6 
		//ClickThroughAnalyzer.generateGraphFile_QQAttribute(LogVersion.AOL);
		//ClickThroughAnalyzer.generateGraphFile_QQAttribute(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateGraphFile_QQAttribute(LogVersion.SogouQ2012);
		
		//4 generate query-level co-session, co-click files
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.AOL);
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.SogouQ2008);
		//ClickThroughAnalyzer.generateFiles_QQCoSessioin_QQCoClick_QDGraph(LogVersion.SogouQ2012);
		
		//4.5 only for aol
		//ClickThroughAnalyzer.mergeUnitGraph_QQCoSession_QQCoClick_DQ(LogVersion.AOL);
		
		//5 generate query-level attribute file
		//ClickThroughAnalyzer.generateFiles_QQAttributeGraph(LogVersion.SogouQ2008);
		
		//6 parsing queries into fine-grained granularity: words
		//ClickThroughAnalyzer.parsingQueriesToWords(LogVersion.SogouQ2008);
		
		//System.out.println("test!");
		
		////////////
		//Bing log//
		////////////
		//1
		ClickThroughAnalyzer.getStatistics(2);
		
		//2
		//String file = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/AcceptedSessionData_AtLeast_2Click.txt";
		//ClickThroughAnalyzer.loadSearchLog(file);
		
		//3
		ClickThroughAnalyzer.getUrlListOfTop100Sessions();
		
		//4
		//ClickThroughAnalyzer.splitUrlList();
		
		//5
		//String dir = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/Fetch_Targets_2nd";
		//ClickThroughAnalyzer.filterUrls(dir);
		
		//6
		//ClickThroughAnalyzer.checkSuffix();
		
	}
}
