package org.archive.clickgraph;



public class QueryEdge {
	//attributes of edge between words
	public static enum QCoType{CoSession, CoClick}
	//
	private int [] qCoArray = new int[QCoType.values().length];
	//
	public QueryEdge(){
		for(int i=0; i<qCoArray.length; i++){
			qCoArray[i] = 0;
		}
	}
	//
	public void upAttributeCount(QCoType dimension, int fre){
		this.qCoArray[dimension.ordinal()] += fre;
	}
	//
	public int[] getQCoArray(){
		return this.qCoArray;
	}
}
