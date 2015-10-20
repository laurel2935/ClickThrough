package org.archive.structure;

public class TemporaliaDoc {
	public String _docno;
	public String _host;
	public String _dateStr;
	public String _url;
	public String _title;
	static final String _encoding = "UTF-8";
	public String _text;
	
	public TemporaliaDoc(String docno, String host, String dateStr, String url, String title, String text){
		this._docno = docno;
		this._host = host;
		this._dateStr = dateStr;
		this._url = url;
		this._title = title;
		this._text = text;
	}
	
	public void sysOutput(){
		System.out.println("docno: "+this._docno);
		System.out.println("host: "+this._host);
		System.out.println("date: "+this._dateStr);
		System.out.println("url: "+this._url);
		System.out.println("title: "+this._title);
		System.out.println("text: "+this._text);
		System.out.println();
	}
}
