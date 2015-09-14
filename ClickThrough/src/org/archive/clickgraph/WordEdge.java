package org.archive.clickgraph;

public class WordEdge {
	//attributes of edge between words
	public static enum WCoType{CoParent, CoSession, CoClick}
	//
	public double weight = 0.0;
	//
	private int [] wCoArray = new int[WCoType.values().length];
	//
	public WordEdge(){
		for(int i=0; i<this.wCoArray.length; i++){
			this.wCoArray[i] = 0;
		}
	}
	//
	public void upAttributeCount(WCoType dimension, int fre){
		this.wCoArray[dimension.ordinal()] += fre;
	}
	//
	public int[] getCoArray(){
		return this.wCoArray;
	}
}
