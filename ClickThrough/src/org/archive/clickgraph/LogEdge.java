package org.archive.clickgraph;

public class LogEdge {
	public static enum EdgeType{QQ, QDoc, WSession, WDoc, WQuery};
	//
	private int count = 1;
	private EdgeType type = null;
	//
	public void upCount(){
		this.count += 1;
	}
	//
	public void upCount(int fre){
		this.count += fre;
	}
	//
	public LogEdge(EdgeType type){
		this.type = type;
	}
	//
	public void setCount(int count){
		this.count = count;
	}
	//
	public int getCount(){
		return this.count;
	}
}
