package org.archive.index.token.lucene;

import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class ChAnalyzer {
	//
	private static Analyzer chStandardAnalyzer = new StandardAnalyzer(Version.LUCENE_44);
	/**
	 * Analyze an input query text into a series of tokens
	 * (1) the irregular characters (characters excludes numbers, Chinese character) are used as the dividing symbol;
	 * (2) output a series of single Chinese character or segments like 1922;
	 * **/
	public static Vector<String> standardAnalyze(String qText){
		Vector<String> result = null;
		try {			
			TokenStream tStream = chStandardAnalyzer.tokenStream("", qText);			
			CharTermAttribute charTermAttribute = tStream.addAttribute(CharTermAttribute.class);
			tStream.reset();
			//System.out.println(analyzer.getClass()+":\t");
			result = new Vector<String>();
			while(tStream.incrementToken()){
				result.add(charTermAttribute.toString());
                //System.out.print("[" + charTermAttribute.toString() + "]");
            }
            //System.out.println();	
            //System.out.println();
		} catch (Exception e) {
			e.printStackTrace();			
		}
		//
		if(null==result || result.size()==0){
			return null;
		}else{
			return result;
		}
	}
	//
	public static void test(String qText){
		Vector<String> result = standardAnalyze(qText);
		System.out.println(result);
	}
	//
	public static void main(String []args){
		//1
		String chQTextString = "分析1922方法：空格及各种符号分割,去掉停止词，停止词包括 is,are,in,on,the等无实际意义的词";
		ChAnalyzer.test(chQTextString);

	}

}
