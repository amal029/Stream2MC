/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-09
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
public class compilerStage3 implements compilerStage{
    public compilerStage3 () {}
    //The cluster architecture parser being invoked
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
    /**
       This method adds processor allocation labels to the guards and
       updates in the stream Graph G.
       @author Avinash Malik
       @date Mon Mar 14 10:20:46 GMT 2011
     */
    /*
      TODO
      1.) Open the architecture pArch.gxl file

      2.) For every vertex v_i \in G, p_j \in P allocate guards and
      updates for v_i:p_j
     */
    @SuppressWarnings("unchecked")
	private static void allocProcessorGuardsAndUpdates(Actor sNode)throws Exception{
	ArrayList<ArrayList> list = pArchParser(new File("pArch.gxl"));
	ArrayList<GXLNode> p = list.get(0);
	ArrayList<GXLEdge> e = list.get(1);
	alloc(sNode,p,e);
	clearVisited(sNode);
    }
    private static void alloc(Actor sNode, ArrayList<GXLNode> processors, ArrayList<GXLEdge> connections)throws Exception{
	sNode.updateLabels(processors,connections);
	sNode.setVisited();
	//Start updating the guards and update labels as: label$processor_num;label$processor_num
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    alloc(node,processors,connections);
		}
	    }
	}
    }
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
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Stage-3(A)
		System.out.print(".......");
		allocProcessorGuardsAndUpdates(sGraph.getSourceNode());
		System.out.print(".......");
		//Write the file out onto the disk for stage3 processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage3__"+f.getName()));
		rets[e] = "./output/__stage3__"+f.getName();
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
