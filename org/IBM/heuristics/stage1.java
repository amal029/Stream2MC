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
    private static ArrayList<Actor> nodesBF = new ArrayList<Actor>();
    private static ArrayList<Actor> nodes = new ArrayList<Actor>();
    private static void collectNodes(Actor sNode){
	if(sNode.ifVisited()) return;
	sNode.setVisited();
	if(sNode.iseActor())
	    nodes.add(sNode);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    collectNodes(node);
		}
	    }
	}
    }
    private static void collectNodesBF(Actor sNode){
	collectNodes(sNode);
	//set all childNum to 0, because this might have been called
	//multiple times.
	while(!nodes.isEmpty())
	    nodes.remove(0).setCurrChildNum(0);
	LinkedList<Actor> queue = new LinkedList<Actor>();
	queue.offer(sNode);
	while(!queue.isEmpty()){
	    Actor node = queue.remove();
	    nodesBF.add(node);
	    //DEBUG
	    // System.out.println(node.getID());
	    for(int e=0;e<node.getConnectionCount();++e){
		if(node.getConnectionAt(e).getDirection().equals(GXL.IN)){
		    GXLEdge le = (GXLEdge)node.getConnectionAt(e).getLocalConnection();
		    if(le.getAttr("parallelEdge")==null){
			Actor node2 = (Actor)le.getTarget();
			//Now special case of mergeNode
			if(node2.getIsMergeNode()){
			    node2.setCurrChildNum(node2.getCurrChildNum()+1);
			    if(node2.getCurrChildNum() == getMyNumPredecessors(node2)){
				queue.offer(node2);
			    }
			}
			else
			    queue.offer(node2);
		    }
		}
	    }
	}
    }
    private static int getMyNumPredecessors(Actor sNode){
	int counter=0;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null)
		    ++counter;
	    }
	}
	return counter;
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
    //Add the three allocation vectors
    // @SuppressWarnings("unchecked")
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
    
    private static boolean add(Actor node, String processor,float ccost){
	boolean ret = false;
	if(bestCost == -1) ret= true;
	else{
	    //get the cost of processing this node
	    float cost = node.getCost(processor);
	    if(ccost+cost < bestCost) ret=true;
	}
	return ret;
    }

    //Now start traversing the thing -- do the state space exploration
    private static float bestCost=-1;
    private static Stack<Actor> completeStateDFS(ArrayList<Actor>nodesBF,state rootNode){
	//The stack to recurse in the opposite order
	//the currentTotalCost
	Stack<Actor> stack = new Stack<Actor>();
	float cCost = 0;
	String prevProcessor = null;
	while(!nodesBF.isEmpty()){
	    Actor sNode = nodesBF.remove(0);
	    //set fAllocate
	    if(sNode.getID().equals("dummyStartNode"))
		sNode.setFAllocate(null);
	    else
		sNode.setFAllocate(prevProcessor);
	    if((prevProcessor=sNode.removeFAlloc())!=null){
		//Now we have the processor in hand
		//let us build the state graph now
		if(add(sNode,prevProcessor,cCost)){
		    state s = new state(sNode.getID()+"-"+prevProcessor,sNode.getCost(prevProcessor),cCost);
		    //get the parent that I need to connect to
		    state sp = sNode.getParentStateNode(rootNode,prevProcessor);
		    //now add this node to the parent state node and
		    //make the edge, while you are at it!
		    //making the edge
		    stateEdge edge = new stateEdge(sp,s);
		}
	    }
	    //right at the end
	    //push only if there are more possible processor allocations left
	    if(!sNode.fAllocateEmpty())
		stack.push(sNode); //for later use
	}
	return stack;
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
		//Make the state graph and its root node
		//we want to make a tree not a forest
		state root = new state("rootNode");
		stateGraph SG = new stateGraph("__stateGraph__"+f.getName(),root);
		SG.add(root);
		//carry out DFS state space search
		collectNodesBF(sGraph.getSourceNode());
		while(!nodesBF.isEmpty()){
		    Stack<Actor> stack = completeStateDFS(nodesBF,SG.getSourceNode());
		    //reverse the stack
		    while(!stack.empty())
			nodesBF.add(stack.pop());
		}
		//Write the file out onto the disk for stage3 processing
		sGraph.getDocument().write(new File("./output","__heuristics_stage1__"+f.getName()));
		rets[e] = "./output/__heusristics_stage1__"+f.getName();
	    }
	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
