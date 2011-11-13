/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-09
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

// public class stage2 implements compilerStage{
//     //Now start traversing the thing -- do the state space exploration
//     private static float bestCost=-1;
//     private static boolean add(Actor node, String processor,float ccost){
// 	boolean ret = false;
// 	if(bestCost == -1) ret= true;
// 	else{
// 	    //get the cost of processing this node
// 	    float cost = node.getCost(processor);
// 	    if(ccost+cost < bestCost) ret=true;
// 	}
// 	return ret;
//     }
//     //FIXME: The special flag alone will not work all the time, have to
//     //see the bad effects of the this special flag.
//     private static void collectNodesBF(Actor sNode, ArrayList<Actor> nodesBF,boolean special){
// 	collectNodes(sNode);
// 	//set all childNum to 0, because this might have been called
// 	//multiple times.
// 	while(!nodes.isEmpty())
// 	    nodes.remove(0).setCurrChildNum(0);
// 	LinkedList<Actor> queue = new LinkedList<Actor>();
// 	queue.offer(sNode);
// 	while(!queue.isEmpty()){
// 	    Actor node = queue.remove();
// 	    nodesBF.add(node);
// 	    for(int e=0;e<node.getConnectionCount();++e){
// 		if(node.getConnectionAt(e).getDirection().equals(GXL.IN)){
// 		    GXLEdge le = (GXLEdge)node.getConnectionAt(e).getLocalConnection();
// 		    if(le.getAttr("parallelEdge")==null){
// 			Actor node2 = (Actor)le.getTarget();
// 			//Now special case of mergeNode
// 			if(node2.getIsMergeNode() && !special){
// 			    node2.setCurrChildNum(node2.getCurrChildNum()+1);
// 			    if(node2.getCurrChildNum() == getMyNumPredecessors(node2)){
// 				queue.offer(node2);
// 			    }
// 			}
// 			else
// 			    queue.offer(node2);
// 		    }
// 		}
// 	    }
// 	}
//     }
//     private static int getMyNumPredecessors(Actor sNode){
// 	int counter=0;
// 	for(int e=0;e<sNode.getConnectionCount();++e){
// 	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
// 		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
// 		if(le.getAttr("parallelEdge")==null)
// 		    ++counter;
// 	    }
// 	}
// 	return counter;
//     }
//     private static ArrayList<Actor> nodes = new ArrayList<Actor>();
//     private static void collectNodes(Actor sNode){
// 	if(sNode.ifVisited()) return;
// 	sNode.setVisited();
// 	if(sNode.iseActor())
// 	    nodes.add(sNode);
// 	for(int e=0;e<sNode.getConnectionCount();++e){
// 	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
// 		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
// 		if(le.getAttr("parallelEdge")==null){
// 		    Actor node = (Actor)le.getTarget();
// 		    collectNodes(node);
// 		}
// 	    }
// 	}
//     }
//     private static Stack<Actor> completeStateDFS(ArrayList<Actor>nodes,state rootNode,stateGraph SG){
// 	//The stack to recurse in the opposite order
// 	//the currentTotalCost
// 	Stack<Actor> stack = new Stack<Actor>();
// 	float cCost = 0;
// 	String prevProcessor = null;
// 	boolean setBestCost = true;
// 	while(!nodes.isEmpty()){
// 	    Actor sNode = nodes.remove(0);
// 	    //set fAllocate
// 	    if(sNode.getID().equals("dummyStartNode")){
// 		if(sNode.getFAllocSize()==0)
// 		    sNode.setFAllocate(null);
// 	    }
// 	    else
// 		sNode.setFAllocate(prevProcessor);
// 	    if((prevProcessor=sNode.removeFAlloc())!=null){
// 		//Now we have the processor in hand let us build the
// 		//state graph now FIXME: This optimization of adding
// 		//only when cost is less the other costs gives a null
// 		//pointer execption 

// 		if(add(sNode,prevProcessor,cCost)){
// 		    //collect all the other nodes after this one if doit -->
// 		    //true
// 		    if(!doit.isEmpty() && doit.get(0).equals(sNode.getID())){
// 			ArrayList<Actor> tempNodes = new ArrayList<Actor>();
// 			// System.out.println("Calling collect nodes with sNode: "+sNode.getID());
// 			collectNodesBF(sNode,tempNodes,true);
// 			tempNodes.remove(0);
// 			//shift the ones already there and append these
// 			//ones to the start.
// 			for(int e=0;e<tempNodes.size();++e){
// 			    //DEBUG
// 			    // System.out.println("DEBUG: "+tempNodes.get(e).getID());
// 			    nodes.add(e,tempNodes.get(e));
// 			}
// 			doit.remove(0);
// 		    }
// 		    String id = gen.generateNodeID();
// 		    state s = new state((sNode.getID()+"-"+prevProcessor)+"-"+id,sNode.getCost(prevProcessor),cCost);
// 		    //change cAllocate as well
// 		    String cAllocate = ((GXLString)sNode.getAttr("cAllocate").getValue()).getValue();
// 		    cAllocate += "-"+id;
// 		    sNode.setAttr("cAllocate",new GXLString(cAllocate));
// 		    s.setAttr("myCost",new GXLString(s.getCost()+""));
// 		    //get the parent that I need to connect to
// 		    state sp = sNode.getParentStateNode(rootNode);
// 		    // System.out.println(s.getID()+": child");
// 		    // System.out.println(sp.getID()+": parent");
// 		    // System.out.println(sp.getID()+"--->"+s.getID());
// 		    //DEBUG
// 		    // System.out.println(s.getID());
// 		    SG.add(s);
// 		    s.updateCurrentCost(sp.getCurrentCost());
// 		    s.setAttr("totalCost",new GXLString(s.getCurrentCost()+""));
// 		    cCost = s.getCurrentCost();
// 		    //now add this node to the parent state node and
// 		    //make the edge, while you are at it!
// 		    //making the edge
// 		    // System.out.println("Cost is: "+cCost);
// 		    stateEdge edge = new stateEdge(sp,s);
// 		    SG.add(edge);
// 		}
// 		else{
// 		    //Clear the stack
// 		    nodes.clear(); //no point in moving on
// 		    //Also, have to reset the cost
// 		    setBestCost = false;
// 		}
// 	    }
// 	    //right at the end
// 	    //push only if there are more possible processor allocations left
// 	    if(!sNode.fAllocateEmpty())
// 		stack.push(sNode); //for later use
// 	}
// 	if(setBestCost){
// 	    if(bestCost==-1)
// 		bestCost = cCost;
// 	    else if(cCost < bestCost)
// 		bestCost = cCost;
// 	}
// 	return stack;
//     }
//     public String[] applyMethod(String args[],String fNames[]){
// 	String rets[] = new String[args.length];
//     	try{
//     	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
// 	    File dir = new File("./output");
// 	    if(!dir.exists())
// 		dir.mkdir();
// 	    for(int e=0;e<args.length;++e){
// 		File f = new File(fNames[e]);
//     		streamGraph sGraph = graphs.get(args[e]);
// 		//Make the state graph and its root node
// 		//we want to make a tree not a forest
// 		state root = new state("rootNode");
// 		stateGraph SG = new stateGraph("__stateGraph__"+f.getName(),root);
// 		SG.add(root);
// 		//The GXL document
// 		GXLDocument doc = new GXLDocument();
// 		doc.getDocumentElement().add(SG);
// 		gen = new GXLIDGenerator(doc);
// 		//carry out DFS state space search
// 		ArrayList<Actor> nodesBF = new ArrayList<Actor>();
// 		collectNodesBF(sGraph.getSourceNode(),nodesBF,false);
// 		ArrayList<Actor> list = new ArrayList<Actor>();
// 		//DEBUG
// 		// while(!nodesBF.isEmpty())
// 		//     System.out.println(nodesBF.remove(0).getID());

// 		//This is the very first time
// 		while(!nodesBF.isEmpty()){
// 		    Stack<Actor> stack = completeStateDFS(nodesBF,SG.getSourceNode(),SG);
// 		    //reverse the stack
// 		    if(!stack.empty()){
// 			nodesBF.add(stack.pop());
// 			doit.add(nodesBF.get(0).getID());
// 			// System.out.println("Adding to do it: "+doit.get(0));
// 			//Add the rest to the list
// 			while(!stack.empty()){
// 			    list.add(stack.pop());
// 			}
// 		    }
// 		    else if(!list.isEmpty()){
// 			nodesBF.add(list.remove(0));
// 			doit.add(nodesBF.get(0).getID());
// 			// System.out.println("Adding to do it: "+doit.get(0));
// 		    }
// 		}
// 		//Print the best obtained cost
// 		System.out.println("The best makespan is: "+bestCost);
// 		//write the state graph onto disk for debugging
// 		SG.getDocument().write(new File("./output","__state_graph"+f.getName()));
// 		//Write the file out onto the disk for stage3 processing
// 		sGraph.getDocument().write(new File("./output","__heuristics_stage2__"+f.getName()));
// 		rets[e] = "./output/__heuristics_stage2__"+f.getName();
// 	    }
// 	}
//     	catch(SAXException se){se.printStackTrace();}
//     	catch(IOException e){e.printStackTrace();}
//     	catch(Exception e){e.printStackTrace();}
// 	return rets;
//     }
//     private static ArrayList<String> doit = new ArrayList<String>();
//     private static GXLIDGenerator gen = null;
// }
