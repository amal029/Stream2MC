package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class Actor extends GXLNode{
    public Actor(String id){super(id);}
    private boolean visit = false;
    public boolean ifVisited(){return visit;}
    public void setVisited(boolean v){visit=v;}
    public void setVisited(){setVisited(true);}
    /**
      This adds the guard labels in the form L1,L2,L3..., which is
      basically a string, these should be converted to integers when
      generating code for MC. A new Attribute is added if one already
      does not exist.
      @return void
      @param String label to append to the GuradLabels
     */
    public void setGuardLabels(String guard){
	String currLabels = getGuardlabels();
	//Add a new attribute
	if(currLabels==null){
	    this.setAttr("guardLabels",new GXLString(guard));
	}
	else{
	    currLabels+=","+guard;
	    this.setAttr("guardLabels",new GXLString(currLabels));
	}
    }
    /**
       This method provides the labels that are to be guards for this actor.
       A null may be returned by this method.
       @author Avinash Malik
       @date Tue Mar  8 11:38:34 GMT 2011
       @return The guards for this actor in the form L1,L2,L3,... or null
     */
    public String getGuardlabels(){
	if(this.getAttr("guardLabels")==null)
	    return null;
	else return ((GXLString)this.getAttr("guardLabels").getValue()).getValue();
    }
    /**
       This method provides the labels that are to be updated by this actor.
       A null may be returned by this method.
       @return The update label string with labels as L1,L2,L3,... or null
       @param void
     */
    public String getUpdateLabels(){
	if(this.getAttr("updateLabels")==null) return null;
	else return ((GXLString)getAttr("updateLabels").getValue()).getValue();
    }
    public void setUpdateLabels(String label){
	String currLabels = getUpdateLabels();
	if(currLabels==null){
	    this.setAttr("updateLabels",new GXLString(label));
	}
	else{
	    currLabels+=","+label;
	    this.setAttr("updateLabels",new GXLString(currLabels));
	}
    }
    protected Stack<Actor> splitStack = new Stack<Actor>();
    protected Stack<Integer> splitIndex = new Stack<Integer>();
    public boolean getIsMergeNode(){
	if(this.getAttr("mergeNode") == null ) return false;
	else if(((GXLString)this.getAttr("mergeNode").getValue()).getValue().equals("true")) return true;
	else return false;
    }
    public boolean getIsSplitNode(){
	if(this.getAttr("splitNode") == null) return false;
	else if(((GXLString)this.getAttr("splitNode").getValue()).getValue().equals("true")) return true;
	else return false;
    }
    public void setIsMergeNode(boolean value){
	this.setAttr("mergeNode",new GXLString(value+""));
    }
    public void setIsSplitNode(boolean value){
	this.setAttr("splitNode",new GXLString(value+""));
    }
    protected void setSplitNode(String val){
	this.setAttr("splitNode",new GXLString(val));
    }
    protected void setMergeNode(String val){
	this.setAttr("mergeNode",new GXLString(val));
    }
    protected String getMergeNode(){
	if(this.getAttr("mergeNode") == null) return null;
	else return ((GXLString)this.getAttr("mergeNode").getValue()).getValue();
    }
    protected String getSplitNode(){
	if(this.getAttr("splitNode") == null) return null;
	else return ((GXLString)this.getAttr("splitNode").getValue()).getValue();
    }
    //If this is not called, this attribute will always be "null"
    protected void setStructureLabelAndIndex(String val){
	this.setAttr("strucutureLabelAndIndex",new GXLString(val));
    }
    protected String getStructureLabelAndIndex(){
	if(this.getAttr("strucutureLabelAndIndex") == null) return null;
	else return ((GXLString)this.getAttr("strucutureLabelAndIndex").getValue()).getValue();
    }
    public Actor(GXLNode n){
	super(n.getID());
	for(int e=0;e<n.getAttrCount();++e){
	    GXLAttr at = n.getAttrAt(e);
	    String s= at.getName();
	    GXLValue t = at.getValue();
	    /**
	      @bug This is a very bad hack, I know that all values are String Type
	      and hence this works. In the general case this will never work
	    */
	    this.setAttr(s,new GXLString(((GXLString)t).getValue()));
	    if(at.getKind()!=null)
	    	this.getAttr(s).setKind(at.getKind());
	}
    }
    public boolean iscActor(){if(this.getAttr("rate")==null) return false; else return true;}
    public boolean iseActor(){if(this.getAttr("rate")==null) return true; else return false;}
    public long getVals(String attr){
	long rate = 0;
	GXLAttr ret = null;
	if((ret=this.getAttr(attr))==null){
	    return rate;
	}
	else{
	    rate = new Long(((GXLString)ret.getValue()).getValue()).longValue();
	    return rate;
	}
    }
    //They both have repition vectors.
    public long getRep(){
	return getVals("rep");
    }
    public long getDelay(){
	return getVals("delay");
    }
    public String getLabel(){
	GXLAttr ret = null;
	if((ret=this.getAttr("label"))==null)
	    return "";
	else
	    return (((GXLString)ret.getValue()).getValue());
    }
}
