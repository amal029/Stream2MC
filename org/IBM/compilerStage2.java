/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-08
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class compilerStage2 implements compilerStage{
    public compilerStage2 () {}
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Write the file out onto the disk for stage2 processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage2__"+f.getName()));
		rets[e] = "./output/__stage2__"+f.getName();
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
