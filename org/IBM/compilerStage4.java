/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-14 

 * This the compiler stage that produces the XML file that is then fed into
 * the Uppaal model checker.
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class compilerStage4 implements compilerStage{
    public compilerStage4 () {}
    private static StringBuilder globalDeclarations = new StringBuilder();
    private static StringBuilder systemDeclaration = new StringBuilder();
    private static StringBuilder templateDeclaration = new StringBuilder();
    /**
       @author Avinash Malik
       @date Fri Mar 18 13:31:15 GMT 2011


       This is the tail recursive method that build the final xml file from the
       stream graph for verification.
     */
    private static void buildBackEndForUppaal(Actor sNode)throws Exception{
	if(sNode.ifVisited()) return;
	//Make the single sequential state machine
	//Put the required 
	sNode.buildGlobals(globalDeclarations);
	//Build the template
	sNode.buildTemplate(templateDeclaration);
	//Build the system declaration
	names = sNode.buildSystem(systemDeclaration);
	//Now do the same for parallelism extraction from the system
	//Write the output into a text file
	BufferedWriter outG = new  BufferedWriter(new FileWriter(".tempG",true));
	BufferedWriter outS = new BufferedWriter(new FileWriter(".tempS",true));
	BufferedWriter outT = new BufferedWriter(new FileWriter(".tempT",true));
	outG.write(globalDeclarations.toString());
	outS.write(systemDeclaration.toString());
	outT.write(templateDeclaration.toString());
	outG.flush(); outS.flush(); outT.flush();
	outG.close(); outS.close(); outT.close();
	globalDeclarations = new StringBuilder(1);
	templateDeclaration = new StringBuilder(1);
	systemDeclaration = new StringBuilder(1);
	System.gc();
	if(sNode.isBuildParallel()){
	    outG = new  BufferedWriter(new FileWriter(".tempG",true));
	    outS = new BufferedWriter(new FileWriter(".tempS",true));
	    outT = new BufferedWriter(new FileWriter(".tempT",true));
	    sNode.buildParallel(globalDeclarations,templateDeclaration,systemDeclaration);
	    outG.write(globalDeclarations.toString());
	    outS.write(systemDeclaration.toString());
	    outT.write(templateDeclaration.toString());
	    outG.flush(); outS.flush(); outT.flush();
	    outG.close(); outT.close(); outS.close();
	    globalDeclarations = new StringBuilder(1);
	    templateDeclaration = new StringBuilder(1);
	    systemDeclaration = new StringBuilder(1);
	    System.gc();// have to call the gc, because it's very memory consuming
	}
	sNode.setVisited();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    buildBackEndForUppaal(node);
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
    private static void getSplitNodes(Actor sNode, ArrayList<eActor> list){
	if(sNode.ifVisited()) return;
	if(sNode.getIsSplitNode() && (!sNode.getID().equals("dummyStartNode"))) 
	    list.add((eActor)sNode);
	sNode.setVisited();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    getSplitNodes(node,list);
		}
	    }
	}
    }
    //In this method I never split for ",", because there can be no
    //merge nodes possible. Any merge nodes occuring in this algorithm
    //means something has gone wrong previously.
    private static Long[] getSingleProcessorCommCosts(cActor node, String guards){
	Long costs[] = new Long[guards.split(";").length];
	//communication actor guards and updates
	String cGuards[] = ((GXLString)node.getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String cUpdates[] = ((GXLString)node.getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
	String pGuards [] = guards.split(";");
	String sCosts[] = ((GXLString)node.getAttr("work_x86").getValue()).getValue().split(";");
	for(int r=0;r<pGuards.length;++r)
	    pGuards[r] = pGuards[r].split("\\$")[1];
	for(int r=0;r<cGuards.length;++r)
	    cGuards[r] = cGuards[r].split("\\$")[1];
	for(int r=0;r<cUpdates.length;++r)
	    cUpdates[r] = cUpdates[r].split("\\$")[1];
	int counter=0;
	for(int r=0;r<cGuards.length;++r){
	    if(cGuards[r].equals(cUpdates[r])){
		boolean add = false;
		for(String g : pGuards){
		    if(g.equals(cGuards[r])){
			costs[counter]=new Long(sCosts[r]);
			++counter;
		    }
		}
	    }
	}
	return costs;
    }
    private static ArrayList<Object> isCommNodeChildSplitOrJoin(eActor node,String mergeNode,Long[] costs,String guards){
	ArrayList<Object> ret = new ArrayList<Object>(3);
	int counter=0,counter1=0;
	for(int e=0;e<node.getConnectionCount();++e){
	    if(node.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)node.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    if(counter > 0) throw new RuntimeException();
		    ++counter;
		    cActor node1 = (cActor)le.getTarget();
		    for(int e1=0;e1<node1.getConnectionCount();++e1){
			if(node1.getConnectionAt(e1).getDirection().equals(GXL.IN)){
			    GXLEdge le1 = (GXLEdge)node1.getConnectionAt(e1).getLocalConnection();
			    ret.add(false);
			    if(le1.getAttr("parallelEdge")==null){
				if(counter1 > 0) throw new RuntimeException();
				++counter1;
				eActor node2 = (eActor)le1.getTarget();
				if(node2.getIsSplitNode()){ret.set(0,true);}
				else if(node2.getIsMergeNode() && node2.getID().equals(mergeNode)){
				    ret.set(0,true);
				}
				else if(node2.getIsMergeNode() && (!node2.getID().equals(mergeNode)))
				    throw new RuntimeException();
				if(!((Boolean)ret.get(0)).booleanValue()){
				    //Get the cost of communication on a single node and
				    //add it to the costs already defined
				    Long commCosts[] = getSingleProcessorCommCosts(node1,guards);
				    //add costs to the already defined ones
				    if(commCosts.length != costs.length) throw new RuntimeException();
				    for(int r=0;r<costs.length;++r)
					costs[r]+=commCosts[r];
				    //Now get the cost of running node2
				    //and add it to the costs array.
				    String costs2[] = ((GXLString)node2.getAttr("total_time_x86").getValue()).getValue().split(";");
				    if(costs2.length != costs.length) throw new RuntimeException();
				    int counter2=0;
				    for(String c : costs2){
					costs[counter2] += new Long(c).longValue();
					++counter2;
				    }
				}
				ret.add(node2);
				ret.add(costs);
				String names [] = {node1.getID(),node2.getID()};
				ret.add(names);
			    }
			}
		    }
		}
	    }
	}
	return ret;
    }
    @SuppressWarnings("unchecked")
    private static void addFusedNode(eActor sNode, String mergeNode, streamGraph sGraph,String guards,
				     Long[] costs,String names){
	/*
	  TODO
	  1.) this should be a eActor
	  2.) the communication node following this node should be 
	  a.) SplitNode
	  b.) Join node
	  3.) The guards of this eActor should not be = guards arg
	*/
	ArrayList<Object> list = isCommNodeChildSplitOrJoin(sNode,mergeNode,costs,guards);
	if(((Boolean)list.get(0)).booleanValue()){
	    if(((GXLString)sNode.getAttr("__guard_labels_with_processors").getValue()).getValue().equals(guards));
	    else{
		//Now make a new node with the required edges and
		//connections.
		GXLIDGenerator genID = new GXLIDGenerator(sGraph.getDocument());
		eActor fusedActor = new eActor(genID.generateNodeID());
		fusedActor.setAttr("__guard_labels_with_processors",new GXLString(guards));
		fusedActor.setAttr("__update_labels_with_processors",
				   new GXLString(((GXLString)sNode.getAttr("__update_labels_with_processors").getValue()).getValue()));
		String ncosts = "";
		for(Long val : (Long [])list.get(2))
		    ncosts+=val+";";
		fusedActor.setAttr("total_time_x86",new GXLString(ncosts));
		fusedActor.setAttr("isFusedActor",new GXLString("true"));
		fusedActor.setFusedActor();
		sGraph.add(fusedActor); //added to the graph
		//Now attach the parallel edges so that this node can be
		//traversed
		//Case 1 --> sNode only has outgoing edges
		//Case 2 --> sNode has both outgoing and incoming edges

		ArrayList<ArrayList> list1 = getParallelEdges(sNode);
		ArrayList<pRelations> incoming = (ArrayList<pRelations>)list1.get(0);
		ArrayList<pRelations> outgoing = (ArrayList<pRelations>)list1.get(1);

		//Case where there are no out going or incoming edges at all..
		if(incoming.isEmpty() && outgoing.isEmpty()) 
		    throw new RuntimeException ("node "+sNode.getID()+" has no parallel edges even though it is in parallel");
		for(pRelations p : incoming){
		    pRelations pNew = new pRelations(p.getSourceID(),fusedActor.getID());
		    pNew.setAttr("parallelEdge",new GXLString("true"));
		    pNew.setAttr("style",new GXLString("dashed"));
		    pNew.setDirected(true);
		    sGraph.add(pNew);
		}
		for(pRelations p: outgoing){
		    pRelations pNew = null;
		    if(incoming.isEmpty()){
			pNew = new pRelations(p.getTargetID(),fusedActor.getID());
			fusedActor.setFusedActorNames(names);
			fusedActor.setAttr("fusedActorNames",new GXLString(names));
		    }
		    else{
			pNew = new pRelations(fusedActor.getID(),p.getTargetID());
		    }
		    pNew.setDirected(true);
		    pNew.setAttr("parallelEdge",new GXLString("true"));
		    pNew.setAttr("style",new GXLString("dashed"));
		    sGraph.add(pNew);
		}
	    }
	}
	else{
	    for(String n : (String [])list.get(3))
		names +=n+",";
	    addFusedNode(((eActor)list.get(1)),mergeNode,sGraph,guards,(Long [])list.get(2),names);
	}
    }
    private static ArrayList<ArrayList> getParallelEdges(eActor sNode){
	ArrayList<ArrayList> list = new ArrayList<ArrayList>(2);
	ArrayList<pRelations> incoming = new ArrayList<pRelations>();
	ArrayList<pRelations> outgoing = new ArrayList<pRelations>();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")!=null)
		    incoming.add((pRelations)le);
	    }
	    else if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")!=null){
		    outgoing.add((pRelations)le);
		}
	    }
	}
	list.add(incoming); list.add(outgoing);
	return list;
    }
    private static void addFusedActors(Actor sNode,streamGraph sGraph){
	ArrayList<eActor> splitNodes = new ArrayList<eActor>();
	getSplitNodes(sNode,splitNodes);
	clearVisited(sNode);
	//Do for each splitNode
	for(eActor splitNode : splitNodes){
	    String mergeNode = ((GXLString)splitNode.getAttr("myMergeNode").getValue()).getValue();
	    for(int e=0;e<splitNode.getConnectionCount();++e){
		if(splitNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		    GXLEdge le = (GXLEdge)splitNode.getConnectionAt(e).getLocalConnection();
		    if(le.getAttr("parallelEdge")==null){
			Actor node = (Actor)le.getTarget();
			if(node.getIsSplitNode()) continue;
			else if(node instanceof cActor){
			    //Get the guards from this node
			    ArrayList<Object> obs = getGuards((cActor)node);
			    addFusedNode((eActor)obs.get(0),mergeNode,sGraph,((String)obs.get(1)),(Long[])obs.get(2),
					 ((eActor)obs.get(0)).getID()+",");
			}
		    }
		}
	    }
	}
    }
    private static ArrayList<Object> getGuards(cActor node){
	ArrayList<Object> ret= new ArrayList<Object>(3);
	int counter=0;
	for(int e=0;e<node.getConnectionCount();++e){
	    if(node.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)node.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    if(counter > 0) throw new RuntimeException();
		    ++counter;
		    eActor node1 = (eActor)le.getTarget();
		    ret.add(node1);
		    ret.add(((GXLString)node1.getAttr("__guard_labels_with_processors").getValue()).getValue());
		    String costs[] = ((GXLString)node1.getAttr("total_time_x86").getValue()).getValue().split(";");
		    Long cots[] = new Long[costs.length];
		    short counter1 = 0;
		    for(String c : costs){
			cots[counter1] = new Long(c);
			++counter1;
		    }
		    ret.add(cots);
		}
	    }
	}
	return ret;
    }
    private static String names = "";
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Make the backend
		//This is the final String Buffer being written
		//Delete the .temp files
		File fifi = new File(".tempG");
		if(fifi.exists())
		    fifi.delete();
		fifi = new File(".tempS");
		if(fifi.exists())
		    fifi.delete();
		fifi = new File(".tempT");
		if(fifi.exists())
		    fifi.delete();
		//Add the fused actors here
		addFusedActors(sGraph.getSourceNode(),sGraph);
		//Write the file out onto the disk for stage3 processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage4__"+f.getName()));
		rets[e] = "./output/__stage4__"+f.getName();
		StringBuilder finalBuffer = new StringBuilder();
		finalBuffer.append("<nta>\n");
		buildBackEndForUppaal(sGraph.getSourceNode());
		//First append the globalDeclaration
		BufferedReader read = new BufferedReader(new FileReader(".tempG"));
		String sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		finalBuffer.append("</declaration>\n");
		// finalBuffer.append(globalDeclarations.toString());
		//Second append the template declarations
		read = new BufferedReader(new FileReader(".tempT"));
		sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		// finalBuffer.append(templateDeclaration.toString());
		//Finally add the system Declarations
		read = new BufferedReader(new FileReader(".tempS"));
		sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		finalBuffer.append("system "+names+";</system>\n");
		// finalBuffer.append(systemDeclaration.toString());
		//Close the netwroked timed automata
		finalBuffer.append("</nta>");
		System.out.print(".......");
		//Write the file out onto the disk after stage4 processing
		// File f = new File(fNames[e]);
		// File dir = new File("./output");
		// if(!dir.exists())
		//     dir.mkdir();
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__final__"+f.getName()+".xml")));
		out.write(finalBuffer.toString());
		out.close();
		rets[e] = "./output/__final__"+f.getName()+".xml";
		System.out.print("Done Compiling");
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
