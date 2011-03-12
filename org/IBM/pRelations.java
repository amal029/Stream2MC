package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class pRelations extends GXLEdge{
    public pRelations(String s, String t){super(s,t);}
    public pRelations(GXLEdge e){
	super(e.getSourceID(),e.getTargetID());
	this.setDirected(true);
	//Set any attributes that this edge might have
	for(int r=0;r<e.getAttrCount();++r){

	    GXLAttr at = e.getAttrAt(r);
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
}
