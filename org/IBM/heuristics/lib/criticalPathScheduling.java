/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2012-02-10
 */
package org.IBM.heuristics.lib;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;
import org.IBM.heuristics.lib.*;
import org.IBM.declustering.listSchedule;

public class criticalPathScheduling implements compilerStage{
    
    private static long getVal(String [] temp){
	long ret = 0;
	long val = 0;
	for(String s : temp)
	    val += Long.valueOf(s);
	ret = val/temp.length;
	return ret;
    }
    private static long getAverageTime(Actor node){
	long ret = 0;
	if(node.iscActor()){
	    //use work_x86
	    String temp[] = ((GXLString)(((cActor)node).getAttr("work_x86")).getValue()).getValue().split(";");
	    ret = getVal(temp);
	}
	else if(node.iseActor()){
	    if(node.getID().startsWith("dummy")){
		ret = 0;
	    }
	    else{
		//use total_time_x86
		String temp[] = ((GXLString)(((eActor)node).getAttr("total_time_x86")).getValue()).getValue().split(";");
		ret = getVal(temp);
	    }
	}
	return ret;
    }
    private static void backFlow(Actor sNode, long currCT){
	long ret = currCT;
	
	//get the average execution time
	long avgTime = getAverageTime(sNode);
	
	//max critical time for this path
	long criticalTime = currCT+avgTime;
	
	//if this sNode already has an SL, check that SL is less than
	//criticalTime
	
	if(sNode.getAttr("SL") != null){
	    long SL = Long.valueOf(((GXLString)sNode.getAttr("SL").getValue()).getValue());
	    if(SL < criticalTime){
		//then set this as SL
		sNode.setAttr("SL",new GXLString(criticalTime+""));
		ret = criticalTime;
	    }
	}
	else {
	    //then set this as SL
	    sNode.setAttr("SL",new GXLString(criticalTime+""));
	    ret = criticalTime;
	}
    
	//Now find the execution time of this damn thing
    	for(int e=0;e<sNode.getConnectionCount();++e){
    	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
    		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
    		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getSource();
		    backFlow(node,ret);
		}
    	    }
    	}
    }
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    StringBuilder buf = new StringBuilder();
	    ArrayList<ArrayList> list = pArchParser(new File("pArch.gxl"));
	    ArrayList<GXLNode> processors = (ArrayList<GXLNode>)list.get(0);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		
		long time = System.nanoTime();
		//Get the nodes in the SDF graph put into the decreasing
		//order using variance
		backFlow(sGraph.getTerminalNode(),0);
		
		//do a demain list schedule
		long makespan = new listSchedule(sGraph,processors).schedule();
		long totTime = (System.nanoTime()-time)/1000000;
		System.out.println("Makespan from critical path algorithm is: "+makespan);
		System.out.println("Time taken (ms): "+totTime);
		System.out.print(".......Done\n");
		//Write the file out onto the disk for next stage
		//processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();

		//This is writing the GXL file after processing
		sGraph.getDocument().write(new File("./output","__critical_path__"+f.getName()));
		rets[e] = "./output/__critical_path__"+f.getName();
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
    //FIXME: Need to put this into a library (copied from compilerStage3)
    //A big space over head in the compiler
    private static ArrayList<ArrayList> pArchParser(File pArch) throws Exception{
	ArrayList<GXLNode> processors = new ArrayList<GXLNode>();
	ArrayList<GXLEdge> connections = new ArrayList<GXLEdge>();
	ArrayList<ArrayList> ret = new ArrayList<ArrayList>(2);
	GXLDocument pArchDoc = new GXLDocument(pArch);
	if(pArchDoc.getDocumentElement().getGraphCount() > 1)
	    throw new RuntimeException("More than one architecture is described in the pArch.gxl file");
	GXLGraph topGraph = pArchDoc.getDocumentElement().getGraphAt(0);

	//Get the cluster
	GXLNode cluster = PArchParser.getClusterAt(topGraph,0);
	//Get the cluster architecture
	GXLGraph cArch = PArchParser.getClusterArch(cluster);

	//How many machines are there in this architecture
	//I know there is just one cluster that's why I am not 
	//getting the cluster information
	for(int r=0;r<PArchParser.getMachinesCount(cArch);++r){
	    GXLNode mnode = PArchParser.getMachineAt(cArch,r);
	    GXLGraph mGraph = PArchParser.getMachineArch(mnode);

	    //mGraph holds the architecture of the machine
	    //This includes the nodes (logicalProcessors) and edges 
	    //(connections between these processors)

	    //Collect all the logicalProcessors in the mGraph
	    for(int e=0;e<PArchParser.getLogicalProcessorCount(mGraph);++e)
		processors.add(PArchParser.getLogicalProcessorAt(mGraph,e));
	    for(int e=0;e<PArchParser.getLogicalProcessorConnectionCount(mGraph);++e)
		connections.add(PArchParser.getLogicalProcessorConnectionAt(mGraph,e));
	}
	//Now add the network connections to connection arraylist
	for(int e=0;e<PArchParser.getNetworkConnectionCount(cArch);++e)
	    connections.add(PArchParser.getNetworkConnectionAt(cArch,e));
	ret.add(processors); ret.add(connections);
	return ret;
    }
}
