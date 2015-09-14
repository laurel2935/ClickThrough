package org.archive.index;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.archive.comon.ClickThroughDataVersion.LogVersion;
import org.archive.comon.ClickThroughDataVersion.PartType;
import org.archive.comon.DataDirectory;
import org.archive.index.token.lucene.SimpleAnalyzer_Var;
import org.archive.util.Language;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrInt;
import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * For searching the queries that include the words within a candidate MWE,
 * what is more important is that: these words are not necessarily occur together as an n-gram! 
 * 
 * As for Lucene, one document corresponds to one query, the fields correspond to the "analyzed" terms, frequency.
 * some fields are analyzed, some are merely stored.
 * 
 * Usage: build index once and access with the static public search method
 * (1) Input query collection format: frequency \t queryString
 * (2) Convert all queries to their lower case
 * 
 * For Chinese: 
 * Analyze an input query text into a series of tokens
 * (1) the irregular characters (characters excludes numbers, Chinese character) are used as the dividing symbol;
 * (2) output a series of single Chinese character or segments like 1922;
 * 
 * For English:
 * Analyze an input query text into a series of tokens
 * (1) the irregular characters (characters excludes numbers, alphabets) are used as the dividing symbol;
 * (2) convert all tokens into the lower case 
 * **/

public class QueryIndex {
	public static enum BufferType {Buffer, NoBuffer};
	//index writer
	private IndexWriter indexWriter = null;
	//
	private static Directory indexDirectory = null;	
	private static IndexReader indexReader = null;
	//buffer for efficiency
	private static HashMap<Integer, StrInt> docToElementMap = null;	
	
	//used when building the index
	public QueryIndex(){}
	//used when directly search
	QueryIndex(String indexDir){		
		try {
			this.indexDirectory = FSDirectory.open(new File(indexDir));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}	
	//get inner document
	//StoredField : 整个域要存储的  
	//StringField : 是一个不需要分词，而直接用于索引的字符串  
	//TextField : 是一大块需要经过分词的文本 
	private Document getIndexDocument(StrInt rawQuery, String queryID){
	
		Document doc = new Document();
		//query string
		doc.add(new TextField("QStr", rawQuery.first, Store.YES));
		//query frequency
		doc.add(new StoredField("QFre", rawQuery.second));
		//for duplicate case
		doc.add(new StoredField("ID", queryID));
		//
		return doc;
	}
	//
	private void buildQueryIndex(List<StrInt> rawQList, Lang lanType){
		try {
			//WhitespaceAnalyzer : merely using white space
			//SimpleAnalyzer : using non-alphabets, also convert to lower case
			//StandardAnalyzer : merely a single character for Chinese
			if(Language.Lang.English == lanType){
				//English
				Analyzer enAnalyzer = new SimpleAnalyzer_Var (Version.LUCENE_44);
				indexWriter = new IndexWriter(indexDirectory, 
						new IndexWriterConfig(Version.LUCENE_44, enAnalyzer));
			}else if(Language.Lang.Chinese == lanType){
				//Chinese
				Analyzer chStandardAnalyzer = new StandardAnalyzer (Version.LUCENE_44);
				indexWriter = new IndexWriter(indexDirectory, 
						new IndexWriterConfig(Version.LUCENE_44, chStandardAnalyzer));				
			}else{
				System.out.println("Unaccepted language, please check!");
				return;
			}
			//
			Document document;
			for(int i=0; i<rawQList.size(); i++){
				document = this.getIndexDocument(rawQList.get(i), StandardFormat.serialFormat((i+1), "00000000"));
				indexWriter.addDocument(document);
			}
			/*
			for(StrInt rawQ: rawQList){
				document = this.getIndexDocument(rawQ);
				indexWriter.addDocument(document);
			}
			*/
		} catch (Exception e) {			
			e.printStackTrace();
		} finally{
			if(null != indexWriter){
				try {
					indexWriter.close();
				} catch (Exception e2) {					
					e2.printStackTrace();
				}
			}
		}		
	}
	//usage-1
	private void buildQueryIndex(String queryCollectionFile, String indexDirStr, Language.Lang lanType){
		try {
			//Vector<StrInt> rawQVector = IOBox.readStrInts("E:/CodeBench/QueryVotingExperts/test/QCorpus.txt", "GBK");
			Vector<StrInt> rawQVector = IOText.loadStrInts_LineFormat_Int_Str(queryCollectionFile, "GBK");			
			//String indexDir = "E:/Data_Log/QuerySegmentation/HoeffdingMethod/QIndex";
			//QueryIndex queryIndex = new QueryIndex(indexDir);
			try {
				File fileDir = new File(indexDirStr);
				if(fileDir.isDirectory()){
					if(fileDir.list().length > 0){
						System.out.println("Files exist in the index directory, please check!");
					}else{
						Directory indexDir = FSDirectory.open(new File(indexDirStr));
						indexDirectory = indexDir;
						//
						this.buildQueryIndex(rawQVector, lanType);
						//
						System.out.println("Succeeded in building query index!");
					}
				}else{
					System.out.println(indexDirStr);
					System.out.println("invalid index directory!");
				}				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//universal usage - 1
	private void buildQueryIndex(LogVersion logVersion, PartType partType){
		String queryCollectionFile = null, queryIndexDir=null;
		//
		if(PartType.All == partType){
			//AOL_UniqueQuery_All.txt
			queryCollectionFile = DataDirectory.UniqueElementRoot
				+DataDirectory.Unique_All[logVersion.ordinal()]
			    +logVersion.toString()+"_UniqueQuery_"+partType.toString()+".txt";
			//
			queryIndexDir = DataDirectory.QueryIndexRoot+DataDirectory.QueryIndex_All[logVersion.ordinal()];
		}		
		//
		System.out.println(queryIndexDir);
		File queryIndexDirFile = new File(queryIndexDir);
		if(!queryIndexDirFile.isDirectory()){
			System.out.println(queryIndexDirFile.mkdir());
		}
		//
		Lang lanType = null;
		if(LogVersion.AOL == logVersion){
			lanType = Lang.English;			
		}else if(LogVersion.SogouQ2008 == logVersion){
			lanType = Lang.Chinese;
		}
		//
		buildQueryIndex(queryCollectionFile, queryIndexDir, lanType);
	}
	//
	private static void iniIndexReader(String dirString){
		try {
			//String dirString = "E:/Data_Log/QuerySegmentation/HoeffdingMethod/QIndex";
			indexDirectory = FSDirectory.open(new File(dirString));
			indexReader = DirectoryReader.open(indexDirectory);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//
	private static void iniDocToQTextMap(String dirString){
		if(null == indexReader){
			iniIndexReader(dirString);
		}
		//
		try {
			docToElementMap = new HashMap<Integer, StrInt>();
			//
			for(int i=0; i<indexReader.maxDoc(); i++){
				Document document = indexReader.document(i);
				docToElementMap.put(i, new StrInt(document.get("QStr"), Integer.parseInt(document.get("QFre"))));				
			}
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}		
	}
	//given pre-segmented units of M, this interface is used for both English and Chinese 
	private static StrInt [] searchParentQSet_Buffer(String [] unitsOfMWE, String dirString){
		if(null == indexReader){
			iniIndexReader(dirString);
		}
		if(null == docToElementMap){
			iniDocToQTextMap(dirString);
		}
		//result buffer
		StrInt [] qArray = null;
		//search query
		BooleanQuery booleanQuery = new BooleanQuery();
		for(String unit: unitsOfMWE){
			TermQuery termQuery = new TermQuery(new Term("QStr", unit));
			booleanQuery.add(termQuery, Occur.MUST);
		}
		//
		try {			
			IndexSearcher searcher = new IndexSearcher(indexReader);
			QCollector qCollector = new QCollector();
			searcher.search(booleanQuery, qCollector);
			//
			Vector<Integer> hitDocuments = qCollector.getHitDocuments();
			if(null!=hitDocuments && hitDocuments.size()>0){
				Vector<StrInt> qVector = new Vector<StrInt>();
				for(Integer docID: hitDocuments){
					qVector.add(docToElementMap.get(docID));
				}
				return qVector.toArray(new StrInt[0]);
			}else{
				return null;
			}		
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return qArray;
	}
	private static StrInt [] searchParentQSet_NoBuffer(String [] unitsOfMWE, String dirString){
		if(null == indexReader){
			iniIndexReader(dirString);
		}		
		//result buffer
		StrInt [] qArray = null;
		//search query
		BooleanQuery booleanQuery = new BooleanQuery();
		for(String unit: unitsOfMWE){
			TermQuery termQuery = new TermQuery(new Term("QStr", unit));
			booleanQuery.add(termQuery, Occur.MUST);
		}
		//
		try {			
			IndexSearcher searcher = new IndexSearcher(indexReader);
			QCollector qCollector = new QCollector();
			searcher.search(booleanQuery, qCollector);
			//
			Vector<Integer> hitDocuments = qCollector.getHitDocuments();
			if(null!=hitDocuments && hitDocuments.size()>0){
				Vector<StrInt> qVector = new Vector<StrInt>();
				Document document;
				//
				for(Integer docID: hitDocuments){
					document = indexReader.document(docID);
					qVector.add(new StrInt(document.get("QStr"), Integer.parseInt(document.get("QFre"))));
				}
				return qVector.toArray(new StrInt[0]);
			}else{
				return null;
			}		
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return qArray;
	}
	//
	public static StrInt [] searchParentQSet(List<String> unitsOfMWE, LogVersion logVersion, PartType partType, BufferType bufferType){
		String queryIndexDir = null;
		if(PartType.All == partType){			
			queryIndexDir = DataDirectory.QueryIndexRoot+DataDirectory.QueryIndex_All[logVersion.ordinal()];
		}
		//
		Lang lanType = null;
		if(LogVersion.AOL == logVersion){
			lanType = Lang.English;			
		}else if(LogVersion.SogouQ2008 == logVersion){
			lanType = Lang.Chinese;
		}
		//
		if(BufferType.Buffer == bufferType){
			return searchParentQSet_Buffer(unitsOfMWE.toArray(new String[0]), queryIndexDir);
		}else{
			return searchParentQSet_NoBuffer(unitsOfMWE.toArray(new String[0]), queryIndexDir);
		}		
	}
	//
	public static StrInt [] searchParentQSet(String [] unitsOfMWE, LogVersion logVersion, PartType partType, BufferType bufferType){
		String queryIndexDir = null;
		if(PartType.All == partType){			
			queryIndexDir = DataDirectory.QueryIndexRoot+DataDirectory.QueryIndex_All[logVersion.ordinal()];
		}
		//
		Lang lanType = null;
		if(LogVersion.AOL == logVersion){
			lanType = Lang.English;			
		}else if(LogVersion.SogouQ2008 == logVersion){
			lanType = Lang.Chinese;
		}
		//
		//
		if(BufferType.Buffer == bufferType){
			return searchParentQSet_Buffer(unitsOfMWE, queryIndexDir);
		}else{
			return searchParentQSet_NoBuffer(unitsOfMWE, queryIndexDir);
		}	
	}
	
	//-1 represents abnormal case
	//given one unit of M, this interface is used for both English and Chinese 
	private static int getTotalHitCount_NoBuffer(String oneUnitOfMWE, String dirString){
		if(null == indexReader){
			iniIndexReader(dirString);
		}
		//search query
		BooleanQuery booleanQuery = new BooleanQuery();
		TermQuery termQuery = new TermQuery(new Term("QStr", oneUnitOfMWE));
		booleanQuery.add(termQuery, Occur.MUST);
		//
		try {
			IndexReader indexReader = DirectoryReader.open(indexDirectory);
			IndexSearcher searcher = new IndexSearcher(indexReader);
			TotalHitCountCollector totalHitCount = new TotalHitCountCollector();
			searcher.search(booleanQuery, totalHitCount);
			//
			return totalHitCount.getTotalHits();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return -1;
	}
	//-1 represents abnormal case
	//given one unit of M, this interface is used for both English and Chinese 
	private static int getExactParentQCount_NoBuffer(String oneUnitOfMWE, String dirString){
		if(null == indexReader){
			iniIndexReader(dirString);
		}		
		//search query
		BooleanQuery booleanQuery = new BooleanQuery();
		TermQuery termQuery = new TermQuery(new Term("QStr", oneUnitOfMWE));
		booleanQuery.add(termQuery, Occur.MUST);
		//
		try {
			IndexReader indexReader = DirectoryReader.open(indexDirectory);
			IndexSearcher searcher = new IndexSearcher(indexReader);
			QCollector qCollector = new QCollector();
			searcher.search(booleanQuery, qCollector);
			//--
			StrInt [] qArray = null;
			Vector<Integer> hitDocuments = qCollector.getHitDocuments();
			if(null!=hitDocuments && hitDocuments.size()>0){
				Vector<StrInt> qVector = new Vector<StrInt>();
				Document document;
				//
				for(Integer docID: hitDocuments){
					document = indexReader.document(docID);
					qVector.add(new StrInt(document.get("QStr"), Integer.parseInt(document.get("QFre"))));
				}
				qArray = qVector.toArray(new StrInt[0]);
			}
			//--
			int pqCount = 0;			
			if(null!=qArray && qArray.length>0){
				for(int i=0; i<qArray.length; i++){
					pqCount += qArray[i].second;
				}				
			}
			return pqCount;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return -1;
	}
	//usage-2
	private static int getExactParentQcount_NoBuffer(String segment, String dirString, Language.Lang lanType){
		if(null == indexReader){
			iniIndexReader(dirString);
		}	
		//		
		QueryParser parser;
		if(Language.Lang.English == lanType){
			Analyzer enSimpleAnalyzer = new SimpleAnalyzer_Var (Version.LUCENE_44);
			//
			parser = new QueryParser(Version.LUCENE_44, "QStr", enSimpleAnalyzer); 
		}else if(Language.Lang.Chinese == lanType){
			Analyzer chStandardAnalyzer = new StandardAnalyzer (Version.LUCENE_44);
			//
			parser = new QueryParser(Version.LUCENE_44, "QStr", chStandardAnalyzer);
		}else{
			System.out.println("Unaccepted language, please check!");
			return -1;
		}		
		//
		try {
			Query searchQuery = parser.parse(segment); 				
			//
			IndexReader indexReader = DirectoryReader.open(indexDirectory);
			IndexSearcher searcher = new IndexSearcher(indexReader);
			QCollector qCollector = new QCollector();
			searcher.search(searchQuery, qCollector);
			//--
			StrInt [] qArray = null;
			Vector<Integer> hitDocuments = qCollector.getHitDocuments();
			if(null!=hitDocuments && hitDocuments.size()>0){
				Vector<StrInt> qVector = new Vector<StrInt>();
				Document document;
				//
				for(Integer docID: hitDocuments){
					document = indexReader.document(docID);
					qVector.add(new StrInt(document.get("QStr"), Integer.parseInt(document.get("QFre"))));
				}
				qArray = qVector.toArray(new StrInt[0]);
			}
			//--
			int pqCount = 0;			
			//
			System.out.println();
			//
			if(null!=qArray && qArray.length>0){
				for(int i=0; i<qArray.length; i++){
					System.out.println(qArray[i].first);
					pqCount += qArray[i].second;
				}				
			}
			return pqCount;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return -1;		
	}
	//universal usage - 2 //the big index should be previously constructed
	public static int getExactParentQcount(String segment, LogVersion logVersion, PartType partType){
		String queryIndexDir = null;
		if(PartType.All == partType){			
			queryIndexDir = DataDirectory.QueryIndexRoot+DataDirectory.QueryIndex_All[logVersion.ordinal()];
		}
		//
		Lang lanType = null;
		if(LogVersion.AOL == logVersion){
			lanType = Lang.English;			
		}else if(LogVersion.SogouQ2008 == logVersion){
			lanType = Lang.Chinese;
		}
		//
		return getExactParentQcount_NoBuffer(segment, queryIndexDir, lanType);
	}
	//
	public static void searchTest(){		
		//1
		Vector<String> mStrings = new Vector<String>();
		mStrings.add("harry");
		mStrings.add("potter");			
		//
		StrInt [] qArray = QueryIndex.searchParentQSet(mStrings, LogVersion.AOL, PartType.All, BufferType.Buffer);
		//output test
		int count = 0;
		if(null!=qArray && qArray.length>0){
			//System.out.println(qArray.length);
			for(int i=0; i<qArray.length; i++){
				System.out.println(qArray[i].first+"\t"+qArray[i].second);
				count += qArray[i].second;
			}
		}else{
			System.out.println("Zero Case!!!");
		}
		System.out.println("Count:\t"+count);
		//2
		//String segmentString = "antiquesandthearts";		
		//System.out.println("Count:\t"+getExactParentQcount(segmentString, LogVersion.AOL, PartType.All));
	}
	//
	public static void main(String []args){
		//1: build query index just once
		//1
		/*
		String qFile = "E:/Data_Log/QuerySegmentation/QueryData/En/QCorpus_En.txt"; 
		String indexDir = "E:/Data_Log/QuerySegmentation/QueryIndex/En";		
		QueryIndex indexer = new QueryIndex();
		indexer.buildQueryIndex(qFile,indexDir, Language.LanType.English);
		*/
		//2
		/*
		String qFile = "E:/Data_Log/QuerySegmentation/QueryData/Ch/QCorpus_Ch.txt"; 
		String indexDir = "E:/Data_Log/QuerySegmentation/QueryIndex/Ch";		
		QueryIndex indexer = new QueryIndex();
		indexer.buildQueryIndex(qFile,indexDir, Language.LanType.Chinese);
		*/
		//3 universal usage
		/*
		QueryIndex indexer = new QueryIndex();
		indexer.buildQueryIndex(LogVersion.AOL, PartType.All);
		*/
		//2: search test
		//1
		///*				
		//QueryIndex.searchTest();		
		//*/
		//2
		/*
		String segmentString = "书籍";		
		String indexDir = "E:/Data_Log/QuerySegmentation/QueryIndex/Ch";
		System.out.println("Count:\t"+getExactParentQcount(segmentString, indexDir, LanType.Chinese));
		*/
	}
}
