package org.archive.index;

import java.util.Vector;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Customized collector:
 * 
 * **/

public class QCollector extends Collector {
	//for getting the final document id
	private int docBase;
	//buffer
	private Vector<Integer> hitDocumentVector = new Vector<Integer>();
	
	//
	public QCollector() {}
	//ignore scorer
	@Override
	public void setScorer(Scorer scorer){}
	//accept docs out of order
	@Override
	public boolean acceptsDocsOutOfOrder(){
		return true;
	}	
	@Override
	public void setNextReader(AtomicReaderContext context) {
		this.docBase = context.docBase;
	}
	//
	public void collect(int segmentID){
		try {			
			int docID = this.docBase+segmentID;
			this.hitDocumentVector.add(docID);
			//expensive
			//Document document = searcher.doc(docID);
			//qVector.add(new StrInt(document.get("QStr"), Integer.parseInt(document.get("QFre"))));
			//
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//
	public Vector<Integer> getHitDocuments(){
		return this.hitDocumentVector;
	}
	/*
	public StrInt[] getQuery(){
		return this.qVector.toArray(new StrInt[0]);
	}
	*/
}
