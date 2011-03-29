package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class streamGraphParser{
    /**
       This is the streamGraphParser library start class
       Always call the parse function with .gxl file names
       This thing can take an infinite number of files
       @author Avinash Malik
       @date Tue Mar  8 11:50:42 GMT 2011
       @param String args[]
       @return void
    */
    private HashMap<String,streamGraph> allGraphs = new HashMap<String,streamGraph>(1);
    public streamGraphParser(){}
    public streamGraphParser(boolean insert){dummyNodesInsertion=insert;}
    private boolean dummyNodesInsertion = false;
    public HashMap<String,streamGraph> parse(String args[]) throws IOException,streamGraphException,SAXException,Exception{
	for(int e=0;e<args.length;++e){
	    GXLGXL gxl = new GXLDocument(new File(args[e])).getDocumentElement();
	    if(gxl.getGraphCount() > 1){throw new streamGraphException("File "+args[e]+" has more than one stream graph defined in it");}
	    else
		allGraphs.put(args[e],new streamGraph(gxl.getGraphAt(0),dummyNodesInsertion,args[e]));
	}
	return allGraphs;
    }
    private class streamGraphException extends RuntimeException{
	private static final long serialVersionUID = 0;
	public streamGraphException(String error){super(error);}
    }
}
