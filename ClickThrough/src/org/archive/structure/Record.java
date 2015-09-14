package org.archive.structure;

public class Record {
	//common fields	
	protected String userID;
	protected String queryText;
	protected String clickUrl;
	protected String itemRank;
	//
	protected boolean valid;
	
	public String getUserID(){
		return this.userID;
	}
	//
	public String getQueryText(){
		return this.queryText;
	}	
	//
	public String getClickUrl(){
		return this.clickUrl;
	}
	//
	public String getItemRank(){
		return this.itemRank;
	}
	//
	public boolean validRecord(){
		return this.valid;
	}
}
