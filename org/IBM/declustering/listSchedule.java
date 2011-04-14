/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-04-14
 */
package org.IBM.declustering;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

public final class listSchedule {
    //The graph that needs to be scheduled
    private static streamGraph graph = null;
    //The list of all available processors
    private static ArrayList<GXLNode> processors = null;
    //The list of currently running processors
    private static ArrayList<GXLNode> runningProcessors = new ArrayList<GXLNode>(3);
    //The list of currently running nodes
    private static Stack<Actor> runningNodes = new Stack<Actor>();
    //The list of currently available processors
    private static ArrayList<GXLNode> availableProcessors = new ArrayList<GXLNode>(4);
    //The list of ready nodes
    private static Stack<Actor> readyNodes = new Stack<Actor>();
    //The labels that are set to one
    private static ArrayList<String> updatedLabels = new ArrayList<String>(10);

    public listSchedule (streamGraph graph, ArrayList<GXLNode> processors) {
	this.graph = graph;
	this.processors = processors;
	//Set the dummy startNode guard label to 1
	updatedLabels.add(graph.getSourceNode().getGuardLabels());
    }

    public static long schedule() throws Exception{
	long makeSpan = 0;

	//Get the timings for all actors in the graph
	ArrayList<Actor> alist = getNodes(graph);
	ArrayList<Long> timings = new ArrayList<Long>(alist.size());
	ArrayList<String> updates = new ArrayList<String>(alist.size());
	for(Actor a : alist){
	    timings.add(getTime(a));
	    if(!(a.getID().equals("dummyTerminalNode")))
		a.setAttr("correctupdateLabels",new GXLString(a.getUpdateLabels()));
	}
	do{
	    //1.) Get the list of available processors
	    getAvailableProcessors();

	    //2.) Get the list of nodes that are ready to run
	    getReadyNodes(alist);
	    //3.) Put the readyNodes in HLFET order (Highest Level First with Estimated Times) --> list scheduling
	    putInHLFETOrder();
	    ArrayList<Actor> toRemove = new ArrayList<Actor>();
	    for(Actor a : readyNodes){
		//check if a is allocated to some processor already??
		boolean yes = false;
		if(a.getAttr("ProcessorAlloc")!=null){
		    //It already has a processor allocation!! OH MY..!!
		    //check if this processor is present in the availableProcessors list
		    GXLNode processorAlloc = null;
		    for(GXLNode n : availableProcessors){
			if(n.getID().equals(((GXLString)a.getAttr("ProcessorAlloc").getValue()).getValue())){
			    processorAlloc = n; break;
			}
		    }
		    //Remove n from the availableProcessors list
		    if(processorAlloc != null){
			availableProcessors.remove(processorAlloc);
			//put it into the runningProcessors list
			runningProcessors.add(processorAlloc);
			yes = true;
		    }
		}
		else{
		    //It is not allocated to any processor
		    //Allocate it to the first free processor
		    if(!availableProcessors.isEmpty()){
			GXLNode p = availableProcessors.remove(0);
			a.setAttr("ProcessorAlloc",new GXLString(p.getID()));
			//add p to the runningProcessors list
			runningProcessors.add(p);
			yes= true;
		    }
		}
		if(yes){
		    //Add actor a to the runningNodes list
		    runningNodes.add(a);
		    //remove a from the readyNodes list
		    toRemove.add(a);
		    //adjust children if this is a split node
		    if((!a.getID().equals("dummyStartNode")) && 
		       a.getIsSplitNode())
			adjustSplitNodeChildren((eActor)a);
		}
	    }
	    //actually remove a from the readyNodes list
	    for(Actor a : toRemove)
		readyNodes.remove(a);
	    toRemove.clear();
	    
	    //Now run the actors...(emulate list scheduling)
	    ArrayList<Actor> doneNodes = new ArrayList<Actor>(processors.size());
	    long minCost = getMinCost(doneNodes);

	    //Now add the minCost to the makeSpan
	    makeSpan += minCost;

	    //update the updatedLabels list
	    updateUpdatedLables(doneNodes);

	    //Remove the doneNodes from the runningNodes list.
	    for(Actor a : doneNodes)
		runningNodes.remove(a);

	    //Now decrement the cost of the still runningNodes by minCost
	    for(Actor a : runningNodes){
		long aTime = getTime(a);
		aTime -= minCost;
		//Change the aTime in the nodes
		setTime(a,aTime);
	    }

	    //remove the assigned processors from the runningProcessors
	    //list and add them to the availableProcessors list
	    shiftProcessors(doneNodes);

	}while(!updatedLabels.isEmpty());

	//Remove the ProcessorAlloc attributes from this graph.
	//reset the timings for all actors in the graph.
	int counter=0;
	for(Actor a : alist){
	    GXLAttr attr = a.getAttr("ProcessorAlloc");
	    GXLString attrlabels = null;
	    if(!(a.getID().equals("dummyTerminalNode")))
		attrlabels = (GXLString)a.getAttr("correctupdateLabels").getValue();
	    a.remove(attr);
	    setTime(a,timings.get(counter));
	    if(!(a.getID().equals("dummyTerminalNode")))
		a.setAttr("updateLabels",new GXLString(attrlabels.getValue()));
	    ++counter;
	}

	// System.out.println("List schedule calculated makeSpan: "+makeSpan);

	//return the calculated makespan
	return makeSpan;
    }

    //This method will remove all the labels from the updatedLabels list
    //for the actors that are running and add new labels for the actors
    //that have completed processing.
    private static void updateUpdatedLables(ArrayList<Actor> dNodes){
	for(Actor a : runningNodes){
	    String uLabels[] = a.getGuardLabels().split(",");
	    for(String u : uLabels)
		updatedLabels.remove(u);
	}
	//Get the updateLabels for the nodes in dNodes
	for(Actor a : dNodes){
	    if(!(a.getID().equals("dummyTerminalNode"))){
		String labs[] = a.getUpdateLabels().split(",");
		for(String l : labs)
		    updatedLabels.add(l);
	    }
	    else{
		//DEBUG
		// for(String s : updatedLabels)
		//     System.out.println(s);
	    }
	}
    }

    private static void shiftProcessors(ArrayList<Actor> dNodes){
	ArrayList<GXLNode> toRemove = new ArrayList<GXLNode>(4);
	for(Actor a : dNodes){
	    String pName = ((GXLString)a.getAttr("ProcessorAlloc").getValue()).getValue();
	    //get the processor from the runningProcessors list
	    for(GXLNode p : runningProcessors){
		String pName2 = p.getID();
		if(pName2.equals(pName)){toRemove.add(p); break;}
	    }
	}
	for(GXLNode p : toRemove)
	    runningProcessors.remove(p);
    }

    private static void setTime(Actor a, long time){
	if(a.iseActor()){
	    if(a.getID().equals("dummyTerminalNode") || a.getID().equals("dummyStartNode")) ;
	    else a.setAttr("total_time_x86",new GXLString(""+time));
	}
	else if(a.iscActor()){
	    a.setAttr("work_x86",new GXLString(""+time));
	}
    }
    private static long getTime(Actor a){
	long time1 = 0;
	if(a.iseActor()){
	    if(a.getID().equals("dummyTerminalNode") || a.getID().equals("dummyStartNode")) ;
	    else
		time1 = new Long(((GXLString)((eActor)a).getAttr("total_time_x86").getValue()).getValue()).longValue();
	}
	else if(a.iscActor())
	    time1 = ((cActor)a).getMultiProcessorTime();
	else throw new RuntimeException("Node "+a.getID()+" is of unknown type");
	return time1;
    }

    private static long getMinCost(ArrayList<Actor> dNodes){
	long makeSpan = 0;
	ArrayList<Actor> rNodes = new ArrayList<Actor>(4);
	for(Actor node : runningNodes){
	    long SL1 = getTime(node);
	    int counter=0;
	    for(Actor a : rNodes){
		long SL2 = getTime(a);
		if(SL1<=SL2)
		    break;
		++counter;
	    }
	    rNodes.add(counter,node);
	}
	//rNodes contains all the nodes in ascending order of work.
	makeSpan = getTime(rNodes.get(0));

	//Now go through the rNodes list and put the equal work nodes in
	//dNodes list
	while(!rNodes.isEmpty()){
	    Actor node = rNodes.remove(0);
	    if(getTime(node) == makeSpan)
		dNodes.add(node);
	}

	//Return the minimum cost
	return makeSpan;
    }

    private static void adjustSplitNodeChildren(eActor splitNode){
	String updateLabels[] = splitNode.getUpdateLabels().split(",");
	//get the children nodes
	ArrayList<cActor> children = new ArrayList<cActor>(4);
	int counter=0;
	for(int r=0;r<splitNode.getConnectionCount();++r){
	    if(splitNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)splitNode.getConnectionAt(r).getLocalConnection();
		cActor node1 = (cActor)le1.getTarget();
		if(node1.getGuardLabels().equals(updateLabels[counter]) &&
		   counter < updateLabels.length-1)
		    children.add(node1);
		++counter;
	    }
	}
	//DEBUG
	// for(Actor a : children)
	//     System.out.println(a.getID());
	//Now remove all except for the first update label from splitNode
	splitNode.setAttr("updateLabels",new GXLString(updateLabels[0]));
	//Set the update labels for the children
	counter=1;
	for(cActor c : children){
	    c.setUpdateLabels(updateLabels[counter]);
	    ++counter;
	    //DEBUG
	    // System.out.println(c.getUpdateLabels());
	}
    }

    //This method rearranges the nodes in the readyNodes list, so that
    //the highest static levels are first in the stack.  This method can
    //possibly throw a NullPointerException -- but in that case it's
    //just wrong
    private static void putInHLFETOrder() throws Exception{
	Stack<Actor>rNodes = new Stack<Actor>();
	while(!readyNodes.empty()){
	    Actor node = readyNodes.pop();
	    long SL1 = new Long(((GXLString)node.getAttr("SL").getValue()).getValue()).longValue();
	    int counter=0;
	    for(Actor a : rNodes){
		long SL2 = new Long(((GXLString)a.getAttr("SL").getValue()).getValue()).longValue();
		if(SL1<=SL2)
		    break;
		++counter;
	    }
	    rNodes.add(counter,node);
	}
	readyNodes = rNodes;
	//DEBUG
	// System.out.println("*******************************");
	// for(Actor a : readyNodes)
	//     System.out.println(a.getID());

    }

    private static ArrayList<Actor> getNodes(streamGraph g){
	ArrayList<Actor> ret = new ArrayList<Actor>(10);
	for(int e=0;e<g.getGraphElementCount();++e)
	    if(g.getGraphElementAt(e) instanceof Actor) ret.add((Actor)g.getGraphElementAt(e));
	return ret;
    }
    /**
       @bug This method can be made much more speedier, by just getting
       the nodes that are attached to the currently running Node and not
       looking at the complete graph nodes each and every time.
     */
    private static void getReadyNodes(ArrayList<Actor> nodes){
	readyNodes.clear();
	for(Actor a : nodes){
	    //Get the guard labels for this node
	    String guards[] = a.getGuardLabels().split(",");
	    //Now check if all the guard labels are there in the
	    //updatedLabels list
	    int counter=0;
	    for(String g : guards){
		for(String n : updatedLabels){
		    if(g.equals(n)){++counter; break;}
		}
	    }
	    if(counter == guards.length) readyNodes.add(a);
	}
    }

    private static void getAvailableProcessors(){
	availableProcessors.clear();
	for(GXLNode n : processors){
	    boolean add = true;
	    for(GXLNode g : runningProcessors){
		if(n.equals(g)) {add=false; break;}
	    }
	    if(add)
		availableProcessors.add(n);
	}
    }
}
