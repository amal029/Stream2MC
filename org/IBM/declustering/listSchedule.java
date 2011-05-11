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
	//DEBUG
	// System.out.println("!!!!Start scheduling!!!");
	long makeSpan = 0;

	//Get the timings for all actors in the graph
	ArrayList<Actor> alist = getNodes(graph);
	ArrayList<Long> timings = new ArrayList<Long>(alist.size());
	ArrayList<String> updates = new ArrayList<String>(alist.size());
	for(Actor a : alist){
	    timings.add(getTime(a));
	    if(!(a.getID().equals("dummyTerminalNode")))
		a.setAttr("correctupdateLabels",new GXLString(a.getUpdateLabels()));
	    a.setAttr("correctguardLabels",new GXLString(a.getGuardLabels()));
	}
	do{
	    //1.) Get the list of available processors
	    getAvailableProcessors();

	    //2.) Get the list of nodes that are ready to run
	    getReadyNodes(alist);
	    //3.) Put the readyNodes in HLFET order (Highest Level First with Estimated Times) --> list scheduling
	    putInHLFETOrder();
	    ArrayList<Actor> toRemove = new ArrayList<Actor>();
	    for(int f=readyNodes.size()-1;f>=0;--f){
		Actor a = readyNodes.get(f);
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
			adjustSplitNodeChildren((eActor)a,alist);
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
	    //DEBUG
	    // System.out.println("Make span: "+makeSpan);
	    // for(Actor a : runningNodes)
	    // 	System.out.println("Running nodes: "+a.getID());
	    // for(Actor a : doneNodes)
	    // 	System.out.println("Done nodes: "+a.getID());

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
	    attrlabels = (GXLString)a.getAttr("correctguardLabels").getValue();
	    a.setAttr("guardLabels",new GXLString(attrlabels.getValue()));

	    //Remove the doodle attributes from these guys
	    if(a.getIsMergeNode() && !a.getID().equals("dummyTerminalNode")){
		GXLAttr attr2 = a.getAttr("doodle");
		a.remove(attr2);
	    }
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
		time1 = ((eActor)a).getMultiProcessorTime();
		// time1 = new Long(((GXLString)((eActor)a).getAttr("total_time_x86").getValue()).getValue()).longValue();
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

    /**
       TODO

       1.) Go to the correspondng mergeNode

       2.) Get the predecessors of the corresponding merge node (Actors)

       3.) Get the predecessors of these cActors (eActors).

       4.) Change the update labels for the eActors to point to the guard label
       of the first cActor.

       5.) So if there are 3 cActors, the first cActor's guard label would be
       changed to be updated by the 3 eActors updateLabels.

       5.) Finally, update the updateLabels of the first and second cActor to
       update the second and third cActors guardLabels, respectively.
    */
    private static void adjustSplitNodeChildren(eActor splitNode,ArrayList<Actor> nodes){
	String updateLabels[] = ((GXLString)splitNode.getAttr("correctupdateLabels").getValue()).getValue().split(",");
	//get the children nodes
	ArrayList<Actor> children = new ArrayList<Actor>(4);
	Actor lastChild = null;
	int counter=0;
	for(int r=0;r<splitNode.getConnectionCount();++r){
	    if(splitNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)splitNode.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		if(node1.getGuardLabels().equals(updateLabels[counter]) &&
		   counter < updateLabels.length-1)
		    children.add(node1);
		else
		    lastChild = node1;
		++counter;
	    }
	}
	//Now remove all except for the first update label from splitNode
	/*
	  Special stuff for multiple splitNodes attached to each other.
	*/
	String newUpdateLabels[] = splitNode.getUpdateLabels().split(",");
	String diffUpdateLabels [] = new String[newUpdateLabels.length-updateLabels.length];
	splitNode.setAttr("updateLabels",new GXLString(updateLabels[0]));

	//Set the update labels for the children
	counter=1;
	String toUpdate="";
	for(Actor c : children){
	    //update only the next communication Node
	    toUpdate += c.getUpdateLabels();
	    if(counter <= children.size()-1)
		toUpdate +=",";
	    c.setAttr("updateLabels",new GXLString(updateLabels[counter]));
	    // c.setUpdateLabels(updateLabels[counter]);
	    ++counter;
	    //DEBUG
	    // System.out.println(c.getUpdateLabels());
	}
	//DEBUG
	// System.out.println(splitNode.getID()+":::"+splitNode.getUpdateLabels());
	// for(Actor a : children)
	//     System.out.println(a.getID()+":::"+a.getUpdateLabels());
	lastChild.setUpdateLabels(toUpdate); //add it to the lastChild
	//Get the difference between the new and old updateLabels
	for(int y=updateLabels.length;y<newUpdateLabels.length;++y)
	    lastChild.setUpdateLabels(newUpdateLabels[y]);
	//DEBUG
	// System.out.println(lastChild.getID()+"::::"+lastChild.getUpdateLabels());

	//Now do the changes for the corresponding mergeNode
	//Get the pointer to the mergeNode
	Actor mNode = null;
	for(Actor node : nodes){
	    if(node.getID().equals(splitNode.getMergeNode())){
		mNode = node;
		break;
	    }
	}
	if(mNode.getAttr("doodle")!=null)
	    return;
	mNode.setAttr("doodle",new GXLString("true"));

	//Only continue if this mergenode has not been doodles yet!!
	ArrayList<cActor> sourcecActors = new ArrayList<cActor>(5);
	for(int e=0;e<mNode.getConnectionCount();++e){
	    if(mNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)mNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getSource();
		//TODO
		/*
		  1.) Do a recursive DFT upwards until one gets the required cActors.
		*/
		if(le.getSource() instanceof cActor)
		    sourcecActors.add((cActor)le.getSource());
		else if(le.getSource() instanceof eActor && ((eActor)le.getSource()).getIsMergeNode()){
		    getcActors((eActor)le.getSource(),sourcecActors);
		}
		else if(le.getSource() instanceof eActor && !((eActor)le.getSource()).getIsMergeNode())
		    throw new RuntimeException();
	    }
	}

	//DEBUG
	// System.out.println(mNode.getID());

	//Now get the corresponding eActors
	ArrayList<eActor> sourceeActors = new ArrayList<eActor>(sourcecActors.size());
	for(cActor c : sourcecActors){
	    int ccounter=0;
	    for(int e=0;e<c.getConnectionCount();++e){
		if(c.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		    if(ccounter > 0) throw new RuntimeException("Communication node: "+c.getID()+" has more than one source actor");
		    GXLEdge le = (GXLEdge)c.getConnectionAt(e).getLocalConnection();
		    sourceeActors.add((eActor)le.getSource()); //FIXME: This line can possibly give a classCastException.
		    ++ccounter;
		}
	    }
	}

	//DEBUG
	// System.out.println(sourceeActors.size());

	//The updateLabels
	String uLabels[] = new String[sourceeActors.size()-1];
	String firstcActorGuardLabels[] = new String[sourceeActors.size()-1];
	int mcounter=0;
	for(int e=1;e<sourceeActors.size();++e){
	    //DEBUG
	    // System.out.println("orig :"+sourceeActors.get(e).getID()+":::"+sourceeActors.get(e).getUpdateLabels());

	    uLabels[mcounter] = sourceeActors.get(e).getUpdateLabels();
	    //update the updatelabels for these actors
	    sourceeActors.get(e).setAttr("updateLabels",new GXLString(sourceeActors.get(0).getUpdateLabels()+e));
	    firstcActorGuardLabels[mcounter] = sourceeActors.get(0).getUpdateLabels()+e;

	    //DEBUG
	    // System.out.println("new :"+sourceeActors.get(e).getID()+":::"+sourceeActors.get(e).getUpdateLabels());
	    ++mcounter;
	}
	//DEBUG
	// System.out.println("orig :"+sourcecActors.get(0).getID()+":::"+sourcecActors.get(0).getGuardLabels());
	for(String s : firstcActorGuardLabels)
	    sourcecActors.get(0).setGuardLabels(s);
	//DEBUG
	// System.out.println("new :"+sourcecActors.get(0).getID()+":::"+sourcecActors.get(0).getGuardLabels());

	//Now finally set the updates for all cActors in order
	for(int e=0;e<sourcecActors.size()-1;++e){
	    //DEBUG
	    // System.out.println("orig :"+sourcecActors.get(e).getID()+":::"+sourcecActors.get(e).getUpdateLabels());

	    sourcecActors.get(e).setUpdateLabels(uLabels[e]);

	    //DEBUG
	    // System.out.println("new :"+sourcecActors.get(e).getID()+":::"+sourcecActors.get(e).getUpdateLabels());
	}
    }

    private static void getcActors(eActor c, ArrayList<cActor> list){
	c.setAttr("doodle",new GXLString("true"));
	for(int e=0;e<c.getConnectionCount();++e){
	    if(c.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)c.getConnectionAt(e).getLocalConnection();
		if(le.getSource() instanceof cActor)
		    list.add((cActor)le.getSource());
		else if(((Actor)le.getSource()).getIsMergeNode())
		    getcActors((eActor)le.getSource(),list);
	    }
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
	//     System.out.println("Ready nodes "+a.getID()+" static level "+((GXLString)a.getAttr("SL").getValue()).getValue());
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
