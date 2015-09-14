package org.archive.structure;

import java.util.Date;

public class AOLRecord extends Record{
	//specific fields of AOL	
	protected Date queryTime;	
	private boolean hasClickEvent = false;
	
	//construct using original records
	public AOLRecord(String recordText, boolean ignoreQueryTime){
		String [] fieldArray = recordText.split("\t");
		//
		if(3 == fieldArray.length && null!=fieldArray[0] && null!=fieldArray[1] && null!=fieldArray[2]){			
			this.userID = fieldArray[0];
			this.queryText = fieldArray[1].toLowerCase();
			this.queryTime = ignoreQueryTime? null:ClickTime.getAOLTime(fieldArray[2]);
			//
			this.hasClickEvent = false;
			this.valid = true;
		}else if(5 == fieldArray.length && null!=fieldArray[0] && null!=fieldArray[1] && null!=fieldArray[2]){			
			this.userID = fieldArray[0];
			this.queryText = fieldArray[1].toLowerCase();
			this.queryTime = ignoreQueryTime? null:ClickTime.getAOLTime(fieldArray[2]);
			this.itemRank = fieldArray[3];
			this.clickUrl = fieldArray[4].toLowerCase();
			//
			this.hasClickEvent = true;
			this.valid = true;
		}else{
			this.valid = false;
		}		
	}
	//
	public String getQueryTime(){
		return ClickTime.getAOLTime(this.queryTime);
	}
	//
	public Date getDateQueryTime(){
		return this.queryTime;
	}
	//
	public boolean hasClickEvent(){
		return this.hasClickEvent;
	}	
	//
	public static void main(String []args){
		//1
		 	 
	}
}
