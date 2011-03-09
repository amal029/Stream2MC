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
	// System.out.println(e.getSourceID()+","+e.getTargetID());
    }
}
