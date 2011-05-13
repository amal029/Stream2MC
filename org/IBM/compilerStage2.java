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
    private static boolean hasParallelEdge(Actor sNode, Actor n2){
	boolean ret = false;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")!=null){
		    Actor node = (Actor)le.getTarget();
		    if(node.getID().equals(n2.getID())) {ret = true; break;}
		}
	    }
	}
	return ret;
    }
    /**
       @author Avinash Malik
       @date Sat Mar 12 14:30:07 GMT 2011

       This is the second most important algorithm in the complete
       system. This algorithm identifies the nodes that can be possibly
       run in parallel, provided they are allocated to different
       processors.


       @bug This is a very very expensive algoritm
     */
    private static void makeParallelEdge(Actor sNode, Actor targetNode,streamGraph sGraph){
	if(sNode.ifVisited()) return;
	if(sNode.getStructureLabelAndIndex()!=null){
	    String [] sLabels = sNode.getStructureLabelAndIndex().split(":");
	    String [] tLabels = targetNode.getStructureLabelAndIndex().split(":");
	    boolean doit =true;
	    int count=0;
	    // if(tLabels.length != sLabels.length){
	    for(int e=tLabels.length-1;e>=0;--e){
		for(int t=sLabels.length-1;t>=0;--t){
		    if(tLabels[e].equals(sLabels[t]))
			{doit=false; break;}
		}
	    }
	    //Special case, all are within the same node
	    String sLab = sLabels[0].replace('[',':');
	    sLab = sLab.replace(']',':');
	    sLabels = sLab.split(":");
	    String tLab = tLabels[0].replace('[',':');
	    tLab = tLab.replace(']',':');
	    tLabels = tLab.split(":");
	    if(sLabels[0].equals(tLabels[0])){
		//now check the index
		if(sLabels[1].equals(tLabels[1]))
		    doit=false;
		else doit =true;
	    }
	    if(doit){
		//If you have n19-->n13, then n13-->n19 is not needed.
		//Don't make round about edges, because two systems
		//running in parallel have equivalent runtime
		/**@bug Need to prove this formally in the paper*/
		for(int w=0;w<targetNode.getConnectionCount();++w){
		    if(targetNode.getConnectionAt(w).getDirection().equals(GXL.OUT)){
			GXLEdge le = (GXLEdge)targetNode.getConnectionAt(w).getLocalConnection();
			if(le.getAttr("parallelEdge") != null){
			    if(le.getSource().equals(sNode)){
				doit=false;
				break;
			    }
			}
		    }
		}
		if(doit){
		    //Check again that a parallelEdge between the two
		    //does not alread exist.
		    if(!hasParallelEdge(targetNode,sNode)){
			pRelations edge = new pRelations(targetNode.getID(),sNode.getID());
			edge.setAttr("parallelEdge",new GXLString("true"));
			edge.setAttr("style",new GXLString("dashed"));
			sGraph.add(edge);
		    }
		}
	    }
	}
	sNode.setVisited();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		//Only take the edges, which are not parallel edges
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    makeParallelEdge(node,targetNode,sGraph);
		}
	    }
	}
    }
    private void makeParallelEdges(streamGraph sGraph, Actor sNode){
	if(sNode.getStructureLabelAndIndex() != null){
	    makeParallelEdge((Actor)sGraph.getSourceNode(),sNode,sGraph);
	    clearVisited(((Actor)sGraph.getSourceNode()));
	}
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		//Only take the edges, which are not parallel edges
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    makeParallelEdges(sGraph,node);
		}
	    }
	}
    }
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Make the parallel relation edges
		System.out.print(".......");
		makeParallelEdges(sGraph,sGraph.getSourceNode());
		System.out.print(".......");
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
