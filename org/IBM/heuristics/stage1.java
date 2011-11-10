/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-08
 */
package org.IBM.heuristics;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;

public class stage1 implements compilerStage{
    private static void clearVisited(Actor sNode){
	sNode.setVisited(false);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    clearVisited(node);
		}
	    }
	}
    }

    //Add the three allocation vectors
    private static void addAllocations(Actor sNode){
	if(sNode.ifVisited()) return;
	sNode.setVisited();
	sNode.initFAlloc();
	sNode.initCAlloc();
	sNode.initPAlloc();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    addAllocations(node);
		}
	    }
	}
    }
    
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    File dir = new File("./output");
	    if(!dir.exists())
		dir.mkdir();
	    for(int e=0;e<args.length;++e){
		File f = new File(fNames[e]);
    		streamGraph sGraph = graphs.get(args[e]);
		//apply allocations
		clearVisited(sGraph.getSourceNode());
		addAllocations(sGraph.getSourceNode());
		//Write the file out onto the disk for stage3 processing
		sGraph.getDocument().write(new File("./output","__heuristics_stage1__"+f.getName()));
		rets[e] = "./output/__heuristics_stage1__"+f.getName();
	    }
	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
