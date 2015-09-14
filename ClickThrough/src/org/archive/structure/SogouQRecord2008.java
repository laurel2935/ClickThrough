package org.archive.structure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SogouQRecord2008 extends Record implements Comparable{
	//specific fields of SogouQ2008 version		
	protected String clickOrder;	
	
	//rank and session order string pattern
	protected static final Pattern RankAndSessionOrderPattern = Pattern.compile("\\t\\d+\\s{1}\\d+\\t");
	
	SogouQRecord2008(){};
	
	//construct using digital record, and all fields are id instead of raw values
	public SogouQRecord2008(String digitalRecord){
		String [] array = digitalRecord.split("\t");
		this.userID = array[0];
		this.queryText = array[1];		
		this.itemRank = array[2];
		this.clickOrder = array[3];
		this.clickUrl = array[4];
		//
		this.valid = true;
	}
	
	//merely used for the original record
	public SogouQRecord2008(String daySerial, String recordText){
		String userID, queryText, itemRank, clickOrder, clickUrl;
		String rankAndPagePart = null;		
		//
    	Matcher matcher = RankAndSessionOrderPattern.matcher(recordText);
		if(matcher.find()){
			rankAndPagePart = matcher.group().trim();  	
			//
			String fieldArray[] = recordText.split("\\t\\d+\\s{1}\\d+\\t");
			//
			userID = fieldArray[0].substring(0, fieldArray[0].indexOf("\t"));    	
	    	//
			queryText = fieldArray[0].substring(fieldArray[0].indexOf("\t")+1).trim();
			queryText = queryText.substring(1, queryText.length()-1);			
			//
			clickUrl = fieldArray[1].trim();
			clickUrl = clickUrl.toLowerCase();
	    	//
	    	String [] A =  rankAndPagePart.split(" ");
	    	itemRank = A[0];
	    	clickOrder = A[1];
	    	//    	
	    	if(null!=userID && null!=queryText){	    		
	    		//
	    		this.userID = daySerial+"-"+userID;	    		
	    		this.queryText = queryText;
	    		this.clickUrl = clickUrl;
	    		this.itemRank = itemRank;
	    		this.clickOrder = clickOrder;
	    		//
	    		this.valid = true;
	    	}else{
	    		this.valid = false;	    		
	    	}	    	
		}else{
			this.valid = false;
		}		
	}
	//
	public String getClickOrder(){
		return this.clickOrder;
	}
	//
	//descending order by query time
	public int compareTo(Object o){		
		SogouQRecord2008 cmp = (SogouQRecord2008)o;
		Integer cOrder = Integer.parseInt(this.clickOrder);
		return cOrder.compareTo(Integer.parseInt(cmp.clickOrder));		
	}
	
}
