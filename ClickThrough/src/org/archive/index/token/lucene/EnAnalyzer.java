package org.archive.index.token.lucene;

import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;



public class EnAnalyzer {
	//
	private static Analyzer enSimpleAnalyzer = new SimpleAnalyzer_Var(Version.LUCENE_44);
	/**
	 * Analyze an input query text into a series of tokens
	 * (1) the irregular characters (characters excludes numbers, alphabets) are used as the dividing symbol;
	 * (2) convert all tokens into the lower case 
	 * **/
	public static Vector<String> simpleAnalyze(String qText){
		Vector<String> result = null;
		try {			
			TokenStream tStream = enSimpleAnalyzer.tokenStream("", qText);			
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
		Vector<String> result = simpleAnalyze(qText);
		System.out.println(result);
	}
	//
	public static void main(String []args){
		//1
		String enQTextString = "The 1988 the45 simplest world, yu-haitao@iss.tokushima-u.ac.jp";
		EnAnalyzer.test(enQTextString);

	}

}
