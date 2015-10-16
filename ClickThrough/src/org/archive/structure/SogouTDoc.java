package org.archive.structure;


public class SogouTDoc {
	
	String _docno;
	String _url;
	String _htmlStr;

	public SogouTDoc(String docno, String url, String htmlStr) {
		this._docno = docno;
		this._url = url;
		this._htmlStr = htmlStr;
	}
	
	public void sysOutput(){
		System.out.println("docno: "+this._docno);
		System.out.println("url: "+this._url);
		System.out.println(this._htmlStr);
	}
	
	public String getHtmlStr(){
		return this._htmlStr;
	}
	
	public String getUrl(){
		return this._url;
	}
	
	public String getDocNo(){
		return this._docno;
	}
}
