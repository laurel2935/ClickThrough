package org.archive.analysis;

import java.util.Vector;

public class UserWord{
	String word = null;
	int logFre = 0;
	Vector<Integer> sourceQList = new Vector<Integer>();
	//
	UserWord(String w, int fre, int qID){
		this.word = w;
		this.logFre = fre;
		this.sourceQList.add(qID);
	}
	public void addFre(int n){
		this.logFre += n;
	}
	//
	public void addSourceQ(int qID){
		this.sourceQList.add(qID);
	}
}
