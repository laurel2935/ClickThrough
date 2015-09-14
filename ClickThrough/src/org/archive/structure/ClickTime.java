package org.archive.structure;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

public class ClickTime {
	//for parsing time string in SogouQ2012
	private static final DateFormat TimeFormat_SogouQ = new SimpleDateFormat("yyyyMMddHHmmss");
	//for parsing time string in AOL, e.g., 2006-04-08 08:38:31
	private static final DateFormat TimeFormat_AOL = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//
	public static Date getSogouQTime(String tString){
		try {
			return TimeFormat_SogouQ.parse(tString);	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	//
	public static Date getAOLTime(String tString){
		try {
			return TimeFormat_AOL.parse(tString);	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	//
	public static String getSogouQTime(Date date){
		try {
			return TimeFormat_SogouQ.format(date);	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	//
	public static String getAOLTime(Date date){
		try {
			return TimeFormat_AOL.format(date);	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	//
	public static int getTimeSpan_MM(Date fromDate, Date toDate){		
		if(fromDate.after(toDate)){			
			System.out.println("Minus time span error!");
			System.out.println(getSogouQTime(fromDate));
			System.out.println(getSogouQTime(toDate));
			System.out.println();
			return -1;
		}else{
			long fLong = fromDate.getTime();
			long tLong = toDate.getTime();
			//
			long delta = tLong - fLong;
			return (int)(delta/(1000*60));
		}	
	}
	
	//
	public static void test(){
		String s1 = "20111230000435";
		String s2 = "2006-03-18 08:03:09";
		Vector<Date> dateVector = new Vector<Date>();
		
		dateVector.add(getAOLTime(s2));
		
		dateVector.add(getSogouQTime(s1));
		
		Collections.sort(dateVector);
		for(Date date: dateVector){
			System.out.println(date.toString());
		}
		//
		//reference time:	2006-03-20 11:57:42
		//current time:	2006-03-20 12:01:45
		String s3 = "2006-03-20 11:57:42";
		String s4 = "2006-03-20 12:01:45";
		System.out.println(getTimeSpan_MM(getAOLTime(s3), getAOLTime(s4)));
	}
	//
	public static void main(String []args){
		//1
		ClickTime.test();
	}
			
			 

}
