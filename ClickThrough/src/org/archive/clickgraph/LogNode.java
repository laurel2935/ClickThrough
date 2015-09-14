package org.archive.clickgraph;


public class LogNode {
	//types of scenario parameter
	public static enum NodeType{Query,Session,Doc,Word};
	//
	private String id = null;
	//
	private NodeType type = null;
	//
	private transient final int hash;
	//
	public LogNode(String id, NodeType type){
		if(Integer.parseInt(id) <= 0){
			new Exception("ID not starts from 1 error!").printStackTrace();
		}
		this.id = id;
		this.type = type;
		//
		this.hash = (id==null? 0 : id.hashCode()*31)+(type==null? 0 : type.hashCode());
	}
	//
	public NodeType getType(){
		return this.type;
	}
	//
	public String getID(){
		return this.id;
	}
	//
	@Override
    public int hashCode()
    {
		return hash;
    }
    @Override
    public boolean equals(Object cmp)
    {
    	if(this == cmp){
        	return true;
        }
        if (cmp == null || !(getClass().isInstance(cmp)))
        {
        	return false;
        }
        LogNode other = this.getClass().cast(cmp);
        return (id==null? other.id==null : id.equals(other.id))
        && (type==null? other.type==null : type.equals(other.type));
    }
}
