/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-04-08
 */

package org.IBM.declustering;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

public class declusterStage1 implements declusterStage{
    public declusterStage1() {}

    /**@bug This can be converted into a binary search*/
    private static void in(int index,Stack<eActor> list, eActor a){
	long aVal = new Long(((GXLString)a.getAttr("SL").getValue()).getValue()).longValue();
	//Check the corner case
	if((new Long(((GXLString)list.get(list.size()-1).getAttr("SL").getValue()).getValue()).longValue())<=aVal){
	    list.push(a);
	    return;
	}
	for(int r=0;r<list.size();++r){
	    if((new Long(((GXLString)list.get(r).getAttr("SL").getValue()).getValue()).longValue())>=aVal){
		list.add(r,a);
		break;
	    }
	}
    }
    private static void insert(Stack<eActor> list, eActor a){
	if(!list.isEmpty()){
	    if(list.contains(a)) {
		int lIndex = list.indexOf(a);
		long SL1 = new Long(((GXLString)list.get(lIndex).getAttr("SL").getValue()).getValue()).longValue();
		long SL2 = new Long(((GXLString)a.getAttr("SL").getValue()).getValue()).longValue();
		if(SL1==SL2) return;
		else if(SL1>SL2) throw new RuntimeException(a.getID());
		else list.remove(a);
		in(lIndex,list,a);
	    }
	    else in(0,list,a);
	    //Now check with elements and insert it in the right place.
	}
	else list.push(a);
    }
    private static void calculateStaticLevel(Actor lNode, long x, Stack <eActor> sortedList, boolean cAct) throws RuntimeException{
	long SL = -1;
	long temp=0;
	if(lNode.iseActor()){
	    if(lNode.getAttr("SL")!=null){
		SL = new Long(((GXLString)lNode.getAttr("SL").getValue()).getValue()).longValue();
		SL = SL>=x?SL:x;
		lNode.setAttr("SL",new GXLString(""+SL));
	    }
	    else
		lNode.setAttr("SL",new GXLString(""+x));
	    //Putting the node on the sortedList, if this is a splitNode
	    //Stack is a bit expensive for anything with java version <
	    //1.5
	    if(lNode.getIsSplitNode()){
		//Always added the freshest version in the stack
		insert(sortedList,(eActor)lNode);
	    }
	    //Adding to time
	    if(lNode.getAttr("total_time_x86")!=null)
		temp = ((eActor)lNode).getMultiProcessorTime();
		// temp = new Long(((GXLString)lNode.getAttr("total_time_x86").getValue()).getValue()).longValue();
	    else if((lNode.getID().equals("dummyTerminalNode")) || (lNode.getID().equals("dummyStartNode"))) ;
	    else throw new RuntimeException(lNode.getID());
	    x+=temp;
	}
	else if(lNode.iscActor() && cAct){
	    if(lNode.getAttr("SL")!=null){
		SL = new Long(((GXLString)lNode.getAttr("SL").getValue()).getValue()).longValue();
		SL = SL>=x?SL:x;
		lNode.setAttr("SL",new GXLString(""+SL));
	    }
	    else
		lNode.setAttr("SL",new GXLString(""+x));
	    x += ((cActor)lNode).getMultiProcessorTime();
	}
	//Now do a tail recursive call upwards in the graph
	int counter=0;
	for(int e=0;e<lNode.getConnectionCount();++e){
	    if(lNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		if(counter>0)
		    x=(new Long(((GXLString)lNode.getAttr("SL").getValue()).getValue()).longValue()+temp);
		GXLEdge le = (GXLEdge)lNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getSource();
		calculateStaticLevel(node,x,sortedList,cAct);
		++counter;
	    }
	}
    }
    private static Actor getLastNode(Actor sNode){
	Actor ret = null;
	if(sNode.getID().equals("dummyTerminalNode")) return sNode;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		if((ret=getLastNode(node))!=null) break;
	    }
	}
	return ret;
    }

    private static Stack<eActor> getSortedImmediateSuccessors(eActor splitNode){
	Stack<eActor> ret = new Stack<eActor>();

	Stack<eActor> temp = new Stack<eActor>();
	//DEBUG
	// System.out.println(splitNode.getID());
	for(int e=0;e<splitNode.getConnectionCount();++e){
	    if(splitNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)splitNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		if(node instanceof cActor){
		    //Now get the eActor from node
		    for(int r=0;r<node.getConnectionCount();++r){
			if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
			    GXLEdge le1 = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
			    eActor node1 = (eActor)le1.getTarget(); //This has got to be the communication node
			    insert(ret,node1);
			    break;
			}
		    }
		}
		else if(node instanceof eActor && ((eActor)node).getIsSplitNode());
		else if(node instanceof eActor && !((eActor)node).getIsSplitNode())
		    throw new RuntimeException("Node "+node.getID()
					  +" is not a split node but still connected directly to the parent split node "+splitNode);
	    }
	}
	return ret;
    }

    private static void removeParallelEdges(streamGraph sGraph){
	Stack<GXLEdge> toRemove = new Stack<GXLEdge>();
	for(int e=0;e<sGraph.getGraphElementCount();++e){
	    if(sGraph.getGraphElementAt(e) instanceof GXLEdge){
		if(((GXLEdge)sGraph.getGraphElementAt(e)).getAttr("parallelEdge") != null){
		    toRemove.push((GXLEdge)sGraph.getGraphElementAt(e));
		}
	    }
	}
	while(!toRemove.empty()){
	    GXLEdge temp = toRemove.pop();
	    sGraph.remove(temp);
	    temp.remove(sGraph);
	}
    }

    private static eActor MmergeNode = null;
    private static long timeOnMyBranch(Actor node, String untilHere, eActor mergeNode){
	long time = 0;
	if(node instanceof cActor)
	    time = ((cActor)node).getSingleProcessorTime();
	else if(node instanceof eActor)
	    time = ((eActor)node).getSingleProcessorTime();
	    // time = new Long(((GXLString)((eActor)node).getAttr("total_time_x86").getValue()).getValue()).longValue();
	if(node.getID().equals(untilHere)){
	    if(mergeNode == null){
		mergeNode = (eActor)node;
		MmergeNode = (eActor)node;
		return time;
	    }
	    else return time;
	}
	//Tail recursive call to calculate the SL in an upward going
	//manner in the graph.
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		time += timeOnMyBranch(node1,untilHere,mergeNode);
	    }
	}
	return time;
    }
    private static long getTotalTimeOnSingleProcessor(cActor child1, cActor child2, eActor node, eActor mergeNode){
	long totalTime = 0;
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		if(node1.equals(child1) || node1.equals(child2)){
		    totalTime += timeOnMyBranch(node1,node.getMergeNode(),mergeNode);
		}
	    }
	}
	return totalTime;
    }
    private static void getCorrectcReturns(Stack<cActor> cReturns, Stack<cActor>returns,Actor node){
	if(cReturns.size()==2) return;
	if(incList(node,returns))
	    cReturns.push((cActor)node);
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le.getTarget();
		getCorrectcReturns(cReturns,returns,node1);
	    }
	}
    }
    private static Stack<Object> makeBasicClusters(Stack<eActor> sortedList, Actor sourceNode) throws Exception{
	ArrayList<cActor> cList = new ArrayList<cActor>(4);
	while(!sortedList.empty()){
	    eActor splitNode = sortedList.pop();
	    splitNode.setAttr("declustered",new GXLString("true"));
	    Stack<eActor> sortedImmediateSuccessors = getSortedImmediateSuccessors(splitNode);
	    //DEBUG
	    // for(int e=0;e<sortedImmediateSuccessors.size();++e){
	    // 	System.out.println(sortedImmediateSuccessors.get(e).getID()+":"+
	    // 			   ((GXLString)sortedImmediateSuccessors.get(e).getAttr("SL").getValue()).getValue());
	    // }
	    while(sortedImmediateSuccessors.size()>=2){
	    	//Put the cut arcs at the correct place
		//Get the real-children (the cActors connected to immediate successors)
		//FIXME: The loops below seem to be incorrect
		eActor eChild1 = null, eChild2=null;
		do{
		    eChild1 = sortedImmediateSuccessors.pop();
		}while(eChild1.getAttr("declustered")!=null && sortedImmediateSuccessors.size()>=2);
		do{
		    eChild2 = sortedImmediateSuccessors.pop();
		}while(eChild2.getAttr("declustered")!=null && sortedImmediateSuccessors.size()>=2);
		//FIXME: It is possible that there is only one child, because the second is a 
		//splitNode
		//Make a check here
		if(eChild1.getIsSplitNode() || eChild2.getIsSplitNode())
		    throw new RuntimeException("Danger Will Robinson: splitNodes "+eChild1.getID()+" or "+eChild2.getID()+
					       "have not been considered for declustering, before their parent");
		//First get the time to run these two branches on a single processor
		eActor mergeNode = null;
		long totalTimeOnSingleProcessor = 
		    getTotalTimeOnSingleProcessor(getSourcecActor(eChild1),getSourcecActor(eChild2),splitNode,mergeNode);
		mergeNode = MmergeNode;
		//Make a new cyclic graph for calculating the optimal cutarcs
		streamGraph newGraph = new streamGraph("temp");
		eActor sNode = makeNewGraph(newGraph,getSourcecActor(eChild1),getSourcecActor(eChild2),splitNode,mergeNode);
		Stack<cActor> returns = new Stack<cActor>();
		long totalTimeOnMultiProcessor = getBestCutArcsTime(sNode,sNode,returns);
		// System.out.println("multi: "+totalTimeOnMultiProcessor+", single: "+totalTimeOnSingleProcessor);
		//Now if we can run the system faster on two processors
		//compared to one processor..then do it. Make clusters
		//by actually removing the arcs from the graph.
		if(totalTimeOnMultiProcessor <= totalTimeOnSingleProcessor){
		    // System.out.println(returns.get(0).getID()+","+returns.get(1).getID());
		    //Put a special attr on the nodes that need to be cut
		    returns.get(0).setAttr("toCut",new GXLString("true"));
		    returns.get(1).setAttr("toCut",new GXLString("true"));
		    //First get the correct nodes from the realGraph, not the modified one
		    Stack<cActor> cReturns = new Stack<cActor>();
		    getCorrectcReturns(cReturns,returns,sourceNode);
		    cReturns.get(0).setAttr("toCut",new GXLString("true"));
		    cReturns.get(1).setAttr("toCut",new GXLString("true"));
		    insertIncList(cReturns.get(0),cReturns.get(1),cList);
		    //Put the child back onto the stack
		    switch(findClosest(getSourcecActor(eChild1),getSourcecActor(eChild2),cReturns.pop(),cReturns.pop(),mergeNode)){
		    case 0:
			sortedImmediateSuccessors.push(eChild1);
			break;
		    case 1:
			sortedImmediateSuccessors.push(eChild2);
			break;
		    default:
			throw new RuntimeException();
		    }
		}
		else{
		    //This means there are no arcs to be removed so,
		    //just push child1 back onto the stack.
		    sortedImmediateSuccessors.push(eChild1);
		}
	    }
	}
	//Build the cluster graph
	ArrayList<streamGraph> clusters = buildClusterGraph(cList,sourceNode);
	//Sort the clusters in ascending order of execTime
	streamGraph mainGraph = clusters.remove(clusters.size()-1);
	Stack<streamGraph> sortedClusterList = sortClusters(clusters);
	System.gc();
	Stack<Object> rets = new Stack<Object>(); rets.push(mainGraph);rets.push(cList); rets.push(sortedClusterList);
	return rets;
    }
    private static Stack<streamGraph> sortClusters(ArrayList<streamGraph> clusters){
	Stack<streamGraph> sortedClusterList = new Stack<streamGraph>();
	for(streamGraph sg : clusters){
	    long execTimesg = new Long(((GXLString)sg.getAttr("clusterExecTime").getValue()).getValue()).longValue();
	    int counter=0;
	    boolean insert=true;
	    for(streamGraph g : sortedClusterList){
		long execTimeg = new Long(((GXLString)g.getAttr("clusterExecTime").getValue()).getValue()).longValue();
		if(execTimesg <= execTimeg){
		    sortedClusterList.insertElementAt(sg,counter);
		    insert= false;
		    break;
		}
		++counter;
	    }
	    if(insert) sortedClusterList.add(sg);
	}
	return sortedClusterList;
    }
    /**@bug: This method and the one following it can be made into a
     * single one with template*/
    private static boolean incList(Actor a, Stack<cActor> list){
	boolean ret = false;
	for(Actor p : list){
	    if(p.getID().equals(a.getID())) {ret= true; list.remove(p);break;}
	}
	return ret;
    }
    private static boolean incList(Actor a, ArrayList<cActor> list){
	boolean ret = false;
	for(Actor p : list){
	    if(p.getID().equals(a.getID())) {ret= true; list.remove(p);break;}
	}
	return ret;
    }
    @SuppressWarnings("unchecked")
    private static long makeCluster(Actor node, streamGraph graph, Stack<cActor> list, ArrayList<cActor> cList){
	long execTime = 0;
	if(incList(node,cList)){
	    list.push((cActor)node);
	    return execTime;
	}
	else if(node.ifVisited())
	    return execTime;
	node.setVisited();
	if(node instanceof eActor){
	    if(node.getID().equals("dummyTerminalNode") || node.getID().equals("dummyStartNode"));
	    else{
		execTime = ((eActor)node).getMultiProcessorTime();
		// execTime = new Long(((GXLString)((eActor)node).getAttr("total_time_x86").getValue()).getValue()).longValue();
	    }
	    graph.add(new eActor(node));
	}
	else if(node instanceof cActor){
	    graph.add(new cActor(node));
	}
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		if(!incList(node1,(ArrayList<cActor>)cList.clone()) && !node1.ifVisited()){
		    pRelations edge = new pRelations(le1);
		    edge.setDirected(true);
		    edge.setID((le1.getSource().getID()+"---->"+le1.getTarget().getID()));
		    graph.add(edge);
		    // System.out.println(le1.getSource().getID()+"---->"+le1.getTarget().getID());
		}
		execTime += makeCluster(node1,graph,list,cList);
	    }
	}
	return execTime;
    }
    @SuppressWarnings("unchecked")
    private static ArrayList<streamGraph> buildClusterGraph(ArrayList<cActor> cList,Actor node) throws IOException{
	//First do a normal DFT from the startNode
	//The Stack with the cutArcs that I find
	GXLDocument doc = new GXLDocument();
	streamGraph mainGraph = new streamGraph("clusteredGraph");
	Stack<cActor> cutArcs = new Stack<cActor>();
	ArrayList<cActor> ccList = (ArrayList<cActor>)cList.clone();
	ArrayList<streamGraph> clusters = new ArrayList<streamGraph>(5);
	int count = 1;
	streamGraph cluster1 = new streamGraph("cluster"+count);
	long time = makeCluster(node,cluster1,cutArcs,ccList);
	cluster1.setAttr("clusterExecTime",new GXLString(""+time));
	clusters.add(cluster1);
	while(!cutArcs.empty()){
	    ++count;
	    streamGraph cluster = new streamGraph("cluster"+count);
	    time = makeCluster(cutArcs.pop(),cluster,cutArcs,ccList);
	    cluster.setAttr("clusterExecTime",new GXLString(""+time));
	    clusters.add(cluster);
	}
	int cc=1;
	for(streamGraph g : clusters){
	    eActor cNode = new eActor("clusterNode"+cc);
	    //DEBUGGIN
	    // isDongling(g);
	    cNode.add(g);
	    mainGraph.add(cNode);
	    // doc.getDocumentElement().add(g);
	    ++cc;
	}
	mainGraph.setEdgeMode("directed");
	//Add the cutNode edges
	for(cActor a : cList){
	    //Getting the source and target edges
	    for(int r=0;r<a.getConnectionCount();++r){
		GXLEdge le1 = (GXLEdge)a.getConnectionAt(r).getLocalConnection();
		// System.out.println(("Adding: "+le1.getSource().getID()+"---->"+le1.getTarget().getID()));
		mainGraph.add(new pRelations(le1));
	    }
	}
	doc.getDocumentElement().add(mainGraph);
	clusters.add(mainGraph);
	doc.write(new File("output/__temp_clusteredGraph.gxl"));
	return clusters;
    }
    private static void insertIncList(cActor c1, cActor c2, ArrayList<cActor> list){
	boolean in1 = true, in2=true;
	for(cActor act : list){
	    if(act.getID().equals(c1.getID()))
		in1 = false;
	    else if(act.getID().equals(c2.getID()))
		in2= false;
	    if(in1 && in2) break;
	}
	if(in1) list.add(c1);
	if(in2) list.add(c2);
    }
    private static int length(Actor node, Actor untilHere, Actor mergeNode){
	int ret = 0;
	if(node.getID().equals(untilHere.getID()) || node.getID().equals(mergeNode.getID()))
	    return ret;
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le.getTarget();
		++ret;
		ret += length(node1,untilHere,mergeNode);
	    }
	}
	return ret;
    }
    private static short findClosest(cActor child1, cActor child2, cActor uh1, cActor uh2, Actor mergeNode){
	//It is possible that the length method never finds uh1/2 in
	//that case it will return after reaching the mergeNode
	int ret = length(child1,uh1,mergeNode);
	int temp = length(child1,uh2,mergeNode);
	ret = ret <= temp ? ret : temp;
	int ret2 = length(child2,uh1,mergeNode);
	int temp2 = length(child2,uh2,mergeNode);
	ret2 = ret2 <= temp2 ? ret2 : temp2;
	short r = ret>=ret2? (short)0: (short)1;
	return r;
    }
    private static int counter=0;
    private static Actor getSecondArc(Actor sNode){
	Actor ret= sNode;
	if(counter<=0)
	    return ret;
	for(int r=0;r<sNode.getConnectionCount();++r){
	    if(sNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(r).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		--counter;
		ret = getSecondArc(node);
	    }
	}
	return ret;
    }
    private static long getBestCutArcsTime(eActor graph, eActor sNode, Stack<cActor> ret){
	long bestTime = 0;
	//Now you have to consider all the possible arcs that can be cut in
	//this circular graph.
	//Do a multiple DFT
	for(int r=0;r<sNode.getConnectionCount();++r){
	    if(sNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(r).getLocalConnection();
		Actor node = (Actor)le.getTarget(); //This is always a cActor
		Actor node2= null;
		int c=2;
		do{
		    // if(c == 2){
		    // }
		    counter +=c;
		    node2 = getSecondArc(node);
		    if(node2==node) {break;}
		    c+=2;
		    /**@bug this can be made more speedy, by calculating
		     * the total single processor time once and then
		     * just finding the time required for a single
		     * branch (the shorter one) and then subtracting it
		     * from total to get the second branches runtime
		     * But, this algorithm is clearer and more flexible,
		     * because it considers heterogeneous execution
		     * times*/
		    //Start calculating times
		    long temp1=timeOnMyBranch(node,node2.getID(),graph);
		    // System.out.println("temp1: "+temp1);
		    temp1-= (((cActor)node).getSingleProcessorTime()+((cActor)node2).getSingleProcessorTime());
		    long temp2 = timeOnMyBranch(node2,node.getID(),graph);
		    // System.out.println("temp2: "+temp2);
		    temp2-= (((cActor)node).getSingleProcessorTime()+((cActor)node2).getSingleProcessorTime());
		    long temp = temp1>=temp2?temp1:temp2;
		    temp+= ((cActor)node).getMultiProcessorTime()+((cActor)node2).getMultiProcessorTime(); 
		    if(bestTime != 0){
			if(bestTime > temp){
			    bestTime = temp;
			    ret.clear();
			    ret.push((cActor)node);
			    ret.push((cActor)node2);
			}
			// bestTime = bestTime<=temp?bestTime:temp;
		    }
		    else{
			bestTime = temp;
			ret.clear();
			ret.push((cActor)node);
			ret.push((cActor)node2);
		    }
		    // System.out.println("temp1: "+temp1+", temp2: "+temp2+","+node.getID()+","+node2.getID()+","+bestTime);
		}while(true);
		//Now call myself tail recursively
		//Get the execution node connected to node
		for(int p=0;p<node.getConnectionCount();++p){
		    if(node.getConnectionAt(p).getDirection().equals(GXL.IN)){
			GXLEdge le1 = (GXLEdge)node.getConnectionAt(p).getLocalConnection();
			eActor node1 = (eActor)le1.getTarget(); //This is always a eActor
			if(node1.equals(graph))
			    return bestTime;
			else{
			    Stack<cActor> stepe = new Stack<cActor>();
			    // System.out.println(ret.get(0).getID()+","+ret.get(1).getID()+","+bestTime);
			    long tepe = getBestCutArcsTime(graph,node1,stepe);
			    if(bestTime>tepe){
				ret.clear();
				ret.push(stepe.pop());
				ret.push(stepe.pop());
				bestTime = tepe;
				// System.out.println(ret.get(0).getID()+","+ret.get(1).getID()+","+bestTime);
			    }
			}
		    }
		}
	    }
	}
	return bestTime;
    }
    private static Actor lastNode = null;
    private static Actor makeGraph(streamGraph graph, Actor node, eActor mergeNode, boolean rev){
	Actor nNode = null;
	if(node instanceof cActor)
	    nNode = new cActor(node);
	else if(node instanceof eActor)
	    nNode = new eActor(node);
	graph.add(nNode);
	for(int r=0;r<node.getConnectionCount();++r){
	    if(node.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)node.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		if(node1.equals(mergeNode)) {lastNode = nNode; return nNode;}
		if(!rev)
		    graph.add(new pRelations(le1));
		else{
		    pRelations revP = new pRelations(node1.getID(),node.getID());
		    graph.add(revP);
		}
		makeGraph(graph,node1,mergeNode,rev);
	    }
	}
	return nNode;
    }
    private static eActor makeNewGraph(streamGraph graph, cActor child1, cActor child2, eActor splitNode, eActor mergeNode) 
	throws Exception{
	eActor sNode = new eActor(splitNode);
	eActor mNode = new eActor(mergeNode);
	//Add the splitNode to the temp graph
	graph.add(sNode);
	//Add the mergeNode to this temp graph
	graph.add(mNode);
	for(int r=0;r<splitNode.getConnectionCount();++r){
	    if(splitNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)splitNode.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		if(node1.equals(child1)){
		    graph.add(new pRelations(le1));
		    makeGraph(graph,node1,mergeNode,false);
		    graph.add(new pRelations(lastNode.getID(),mergeNode.getID()));
		}
		else if(node1.equals(child2)){
		    Actor firstNode = makeGraph(graph,node1,mergeNode,true);
		    graph.add(new pRelations(mNode.getID(),lastNode.getID()));
		    graph.add(new pRelations(firstNode.getID(),sNode.getID()));
		}
	    }
	}
	graph.setEdgeMode("directed");
	graph.setEdgeIDs(true);
	GXLDocument doc = new GXLDocument();
	doc.getDocumentElement().add(graph);
	doc.write(new File("output/__temp_graphDeclusterStage1.gxl"));
	return sNode;
    }
    //This method gives the very first cActor only!!
    private static cActor getSourcecActor(eActor lNode){
	cActor ret = null;
	for(int e=0;e<lNode.getConnectionCount();++e){
	    if(lNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)lNode.getConnectionAt(e).getLocalConnection();
		ret= (cActor)le.getSource(); break;
	    }
	}
	return ret;
    }
    @SuppressWarnings("unchecked")
    public String[] applyMethod(String args[], String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
    	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		Stack<eActor> sortedList = new Stack<eActor>();
		removeParallelEdges(sGraph);
		//Make a temp graph for debugging
		GXLDocument doc = sGraph.getDocument();
		doc.write(new File("output/__temp_declusterStage1.gxl"));
		calculateStaticLevel(getLastNode((Actor)sGraph.getSourceNode()),0,sortedList,false);
		//remove the dummyTerminalNode from the sortedList
		sortedList.remove(sGraph.getSourceNode());
		//Now make the basic clusters
		Stack<Object> list = makeBasicClusters(sortedList,sGraph.getSourceNode());
		Stack<streamGraph> sortedClusterList = (Stack<streamGraph>)list.pop();
		ArrayList<cActor> cutArcs = (ArrayList<cActor>)list.pop();
		streamGraph mainGraph = (streamGraph)list.pop();
		int levels = doHeirarchicalClustering(sortedClusterList,cutArcs,mainGraph);
		//write out the mainGraph
		mainGraph.getDocument().write(new File("output/__temp_heirarchical_clustered_graph.gxl"));

		//Now do declustering --- the final step
		decluster(mainGraph,sGraph,levels);

		//This is just to put a new line
		System.out.println();
	    }
	}
	catch(Exception e){e.printStackTrace();}
	return rets;
    }

    @SuppressWarnings("unchecked")
	private static void decluster(streamGraph mainGraph, streamGraph origGraph,int levels) throws Exception{
	//Find out the number of processors allocated for this graph
	ArrayList<ArrayList> list = pArchParser(new File("pArch.gxl"));
	ArrayList<GXLNode> p = (ArrayList<GXLNode>)list.get(0);
	ArrayList<GXLEdge> e = (ArrayList<GXLEdge>)list.get(1);

	//Number of processors??
	int numProcessors = p.size();

	/**@bug*/
	//Declustering algorithm cannot take care of heterogeneous
	//architectures, i.e., archs where actors take different time to
	//run of different processors, and also the communication lines
	//take the same amount of time in the declustering algorithm.
	if(p.size() < 0) throw new RuntimeException("There are no processors allocated for graph aborting WILL ROBINSON!!!");

	//Calculate the total time required to run origGraph on a single
	//processor
	origGraph.clearVisited(origGraph.getSourceNode());
	long time = singleProcessorMakeSpan(origGraph.getSourceNode());
	origGraph.clearVisited(origGraph.getSourceNode());
	System.out.println("Single processor makespan: "+time);

	//Now start declustering and allocating stuff to processors
	ArrayList<ArrayList> allocs = new ArrayList<ArrayList>(10);
	long shortestMakeSpan = startDeclustering(mainGraph,origGraph,time,allocs,levels,p,time);
	System.out.println("shortestMakeSpan: "+shortestMakeSpan);
    }

    private static long startDeclustering(streamGraph mainGraph, streamGraph origGraph, long currMakeSpan,
					  ArrayList<ArrayList> allocs,int levels, ArrayList<GXLNode> processors,
					  long time)
	throws Exception{

	//Calculate the static level for the whole graph, including
	//communication nodes.
	Stack<eActor> throwAway = new Stack<eActor>();
	calculateStaticLevel(getLastNode(origGraph.getSourceNode()),0,throwAway,true);
	long shortestMakeSpan = time;
	ArrayList<streamGraph> bs = new ArrayList<streamGraph>();
	ArrayList<GXLNode> ps = new ArrayList<GXLNode>();
	//Get clusters C0 and C1
	while(levels > 0){
	    ArrayList<streamGraph> clusters = new ArrayList<streamGraph>(2);
	    getClusters(mainGraph,clusters,levels);
	    //Now get the smaller of the two clusters
	    Stack<streamGraph> sortedClusterList = sortClusters(clusters);
	    streamGraph smallerCluster = sortedClusterList.remove(0);
	    //Now assign all nodes in smallerCluster to each of the
	    //processor in the system
	    int counter=0;
	    //Get the nodes in the smallerCluster
	    ArrayList<streamGraph> basicClusters = new ArrayList<streamGraph>(2);
	    getBasicClusters(smallerCluster,basicClusters);
	    for(GXLNode p : processors){
		//Allocate the nodes to the processors
		for(streamGraph b : basicClusters)
		    allocateProcessors(b,p.getID(),origGraph);
		//Now list schedule this damn thing to get a makespan
		long tempTime = new org.IBM.declustering.listSchedule(origGraph,processors).schedule();
		//Only accept this allocation if this allocation
		//gives better timing than the previous one.
		if(tempTime <= shortestMakeSpan){
		    //This is needed, because list scheduling removes
		    //all allocations
		    for(streamGraph b : basicClusters)
			bs.add(b);
		    ps.add(p);
		    shortestMakeSpan = tempTime;
		}
	    }
	    //Here you have to add the processor allocation for the
	    //previous best cases, because they will be lost due to list
	    //scheduling
	    for(GXLNode p : ps){
		for(streamGraph b : bs)
		    allocateProcessors(b,p.getID(),origGraph);
	    }
	    --levels;
	}
	return shortestMakeSpan;
    }

    private static ArrayList<Actor> getNodes(streamGraph g){
	ArrayList<Actor> ret = new ArrayList<Actor>(10);
	for(int e=0;e<g.getGraphElementCount();++e)
	    if(g.getGraphElementAt(e) instanceof Actor) ret.add((Actor)g.getGraphElementAt(e));
	return ret;
    }

    private static void allocateProcessors(streamGraph b, String id, streamGraph origGraph){
	ArrayList<Actor> oNodes = getNodes(origGraph);
	ArrayList<Actor> bNodes = getNodes(b);
	for(Actor bNode : bNodes){
	    for(Actor oNode : oNodes){
		if(oNode.getID().equals(bNode.getID()))
		    oNode.setAttr("ProcessorAlloc",new GXLString(id));
	    }
	}
    }

    private static void getClusters(streamGraph sGraph, ArrayList<streamGraph> basicClusters, int level){
	if(sGraph.getAttr("heirarchicalGraph")!=null){
	    if(sGraph.getAttr("clusterLevel")!=null &&
	       (((GXLString)sGraph.getAttr("clusterLevel").getValue()).getValue().equals(new String(level+"")))){
		   //Get the two graphs inside graph
		   for(int e=0;e<sGraph.getGraphElementCount();++e){
		       if(sGraph.getGraphElementAt(e) instanceof eActor){
			   eActor node = (eActor)sGraph.getGraphElementAt(e);
			   if(node.getGraphCount()>1) throw new RuntimeException("node "+node.getID()+" contains more than one graph");
			   basicClusters.add((streamGraph)node.getGraphAt(0));
		       }
		   }
		   return;
	       }
	    else{
		for(int e=0;e<sGraph.getGraphElementCount();++e){
		    if(sGraph.getGraphElementAt(e) instanceof eActor){
			eActor node = (eActor)sGraph.getGraphElementAt(e);
			if(node.getGraphCount()>1) throw new RuntimeException("node "+node.getID()+" contains more than one graph");
			getClusters((streamGraph)node.getGraphAt(0),basicClusters,level);
		    }
		}
	    }
	}
    }

    private static long singleProcessorMakeSpan(Actor sNode){
	long time = 0;
	if(sNode.ifVisited()) return time;
	sNode.setVisited();
	if(sNode instanceof eActor){
	    if(sNode.getID().equals("dummyTerminalNode") || sNode.getID().equals("dummyStartNode"));
	    else
		// time = new Long(((GXLString)((eActor)sNode).getAttr("total_time_x86").getValue()).getValue()).longValue();
		time = ((eActor)sNode).getMultiProcessorTime();
	}
	else if(sNode instanceof cActor)
	    time = ((cActor)sNode).getMultiProcessorTime();
	for(int r=0;r<sNode.getConnectionCount();++r){
	    if(sNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le1 = (GXLEdge)sNode.getConnectionAt(r).getLocalConnection();
		Actor node1 = (Actor)le1.getTarget();
		time += singleProcessorMakeSpan(node1);
	    }
	}
	return time;
    }
    private static int doHeirarchicalClustering(Stack<streamGraph> sortedClusterList,ArrayList<cActor> cutArcs,
						 streamGraph mainGraph){

	int counter = 0;
	while(sortedClusterList.size()>1){
	    streamGraph me = sortedClusterList.remove(0);
	    streamGraph pCluster = getPairingCluster(me,sortedClusterList,cutArcs);
	    // System.out.println(pCluster.getID());
	    // if(me.getParent()!=null)
	    // 	System.out.println(((eActor)me.getParent()).getID());
	    // if(pCluster.getParent()!=null)
	    // 	System.out.println(((eActor)pCluster.getParent()).getID());

	    //Add this node to the heirarchical cluster graph
	    //Remove the current connections in the mainGraph
	    eActor meN = (eActor)me.getParent();
	    eActor pClusterN = (eActor)pCluster.getParent();
	    mainGraph.remove(meN);
	    mainGraph.remove(pClusterN);
	    //Add a new graph and a new node
	    streamGraph clusteredGraph = new streamGraph(meN.getID()+"--"+pClusterN.getID());
	    clusteredGraph.setEdgeMode("directed");
	    clusteredGraph.setAttr("heirarchicalGraph",new GXLString("true"));
	    ++counter;
	    clusteredGraph.setAttr("clusterLevel",new GXLString(""+counter));
	    clusteredGraph.add(meN);
	    clusteredGraph.add(pClusterN);
	    eActor clusteredNode = new eActor(meN.getID()+"---"+pClusterN.getID());
	    //Add the new graph to the node and the node to the mainGraph
	    clusteredNode.add(clusteredGraph);
	    mainGraph.add(clusteredNode);
	    //Now call the method to get the basic clusters in this clusteredGraph
	    ArrayList<streamGraph> basicClusters = new ArrayList<streamGraph>(2);
	    getBasicClusters(clusteredGraph,basicClusters);
	    long execTime = 0;
	    for(streamGraph s : basicClusters)
		execTime += new Long(((GXLString)s.getAttr("clusterExecTime").getValue()).getValue()).longValue();
	    clusteredGraph.setAttr("clusterExecTime",new GXLString(""+execTime));
	    //Now sort them again
	    ArrayList<streamGraph> ccclusters = new ArrayList<streamGraph>(sortedClusterList.size()+1);
	    while(!sortedClusterList.empty())
		ccclusters.add(sortedClusterList.remove(0));
	    //This in turn is adding the newly formed cluster (heirarchical) back into the sortedlist.
	    ccclusters.add(clusteredGraph);
	    //Now sort
	    sortedClusterList = sortClusters(ccclusters);
	    //DEBUGGING
	    // for(streamGraph s : sortedClusterList)
	    // 	System.out.println(s.getID()+"::::"+((GXLString)s.getAttr("clusterExecTime").getValue()).getValue());
	}
	mainGraph.setAttr("heirarchicalGraph",new GXLString("true"));
	return counter;
    }

    private static void getBasicClusters(streamGraph sGraph,ArrayList<streamGraph> basicClusters){
	if(sGraph.getAttr("heirarchicalGraph")!=null){
	    for(int e=0;e<sGraph.getGraphElementCount();++e){
		if(sGraph.getGraphElementAt(e) instanceof eActor){
		    eActor node = (eActor)sGraph.getGraphElementAt(e);
		    if(node.getGraphCount()>1) throw new RuntimeException("node "+node.getID()+" contains more than one graph");
		    getBasicClusters((streamGraph)node.getGraphAt(0),basicClusters);
		}
	    }
	}
	else{
	    basicClusters.add(sGraph);
	}
    }

    //DEBUGGING CODE
    private static void PRINT(ArrayList<Object> list,char val){
	for(Object o : list){
	    switch(val){
	    case 'c':
		System.out.println(((cActor)o).getID());
		break;
	    case 'e':
		System.out.println(((eActor)o).getID());
		break;
	    case 's':
		System.out.println(((streamGraph)o).getID()+":::"+
				   ((GXLString)((streamGraph)o).getAttr("clusterExecTime").getValue()).getValue());
		// System.out.println(((streamGraph)o).getID());
		break;
	    }
	}
    }
    @SuppressWarnings("unchecked")
    //me can be a heirarchical graph
    private static streamGraph getPairingCluster(streamGraph me, Stack<streamGraph> sortedClusterList, ArrayList<cActor> cutArcs){
	//Get the cutArcs in the me streamGraph
	ArrayList<streamGraph> basicClusters = new ArrayList<streamGraph>(4);
	getBasicClusters(me,basicClusters);
	ArrayList<cActor> myCutArcs = new ArrayList<cActor>(4);
	for(streamGraph s : basicClusters){
	    ArrayList<cActor> myCutArcs1 = getmycutArcs(s);
	    for(cActor a : myCutArcs1)
		myCutArcs.add(a);
	}
	streamGraph smallestPairingCluster = null;
	//Only if there are cut arcs in this cluster graph do you need
	//to do any of this.
	if(!myCutArcs.isEmpty()){
	    // PRINT((ArrayList<Object>)myCutArcs.clone(),'c');
	    //Get the source and target nodes of this cutArcs
	    ArrayList<eActor> sourceAndTargets = getSourceAndTargetNodes(myCutArcs,cutArcs);
	    // PRINT((ArrayList<Object>)sourceAndTargets.clone(),'e');
	    //Get the cluster that these source and targets nodes are in
	    ArrayList<streamGraph> clusters = getContainingClusters(sortedClusterList,sourceAndTargets);
	    // PRINT((ArrayList<Object>)clusters.clone(),'s');
	    //Get the smallest of these clusters
	    //clusters can contain a heirarchical graph
	    // System.out.println(clusters.size());
	    Stack<streamGraph> sCluster = sortClusters(clusters);
	    //Remove this cluster from the sortedClusterList
	    smallestPairingCluster = sCluster.remove(0);
	    sortedClusterList.remove(smallestPairingCluster);
	}
	else{
	    /**@bug This is a gross hack (maybe!!), because we are just
	     * piscking one cluster out of many that this cluster might
	     * be attached to*/
	    //Just pick the first cluster in the sortedClusterList
	    smallestPairingCluster = sortedClusterList.remove(0);
	}

	//Make a new node in a new Graph called the heirarchical cluster
	//graph with these two clusters as children
	return smallestPairingCluster;
    }

    //sortedClusterList can contain a heirarchical graph
    private static streamGraph getcCluster(Stack<streamGraph>sortedClusterList, eActor act){
	streamGraph ret = null;
	for(streamGraph sGraph : sortedClusterList){
	    ArrayList<streamGraph> sGraphs = new ArrayList<streamGraph>(4);
	    getBasicClusters(sGraph,sGraphs);
	    for(streamGraph s : sGraphs){
		for(int e=0;e<s.getGraphElementCount();++e){
		    if(s.getGraphElementAt(e) instanceof eActor){
			if(s.getGraphElementAt(e).getID().equals(act.getID())){
			    ret = sGraph; break;
			}
		    }
		}
	    }
	}
	return ret;
    }

    private static ArrayList<streamGraph> getContainingClusters(Stack<streamGraph> graphs, ArrayList<eActor> sourceAndTargets){
	ArrayList<streamGraph> ret = new ArrayList<streamGraph>(2);
	//get the streamGraph with the node in it
	for(eActor act : sourceAndTargets){
	    streamGraph ccluster = getcCluster(graphs,act);
	    if(ccluster == null) ; /*This means that this node has already been added*/
	    //add the ccluster in the ret arraylist
	    else{
		boolean add = true;
		for(streamGraph g : ret){
		    if(g.equals(ccluster)){add=false; break;}
		}
		if(add) ret.add(ccluster);
	    }
	}
	return ret;
    }

    //
    private static ArrayList<eActor> getSourceAndTargetNodes(ArrayList<cActor> myCutArcs, ArrayList<cActor> cutArcs){
	ArrayList<eActor> sourceAndTargets = new ArrayList<eActor>(2);
	for(cActor act : myCutArcs){
	    for(cActor act1 : cutArcs){
		if(act.getID().equals(act1.getID())){
		    for(int r=0;r<act1.getConnectionCount();++r){
			if(act1.getConnectionAt(r).getDirection().equals(GXL.IN)){
			    GXLEdge le = (GXLEdge)act1.getConnectionAt(r).getLocalConnection();
			    Actor node = (Actor)le.getTarget(); //This is always an eActor
			    sourceAndTargets.add((eActor)node);
			}
			else if(act1.getConnectionAt(r).getDirection().equals(GXL.OUT)){
			    GXLEdge le = (GXLEdge)act1.getConnectionAt(r).getLocalConnection();
			    Actor node = (Actor)le.getSource(); //This is always an eActor
			    sourceAndTargets.add((eActor)node);
			}
		    }
		    break;
		}
	    }
	}
	return sourceAndTargets;
    }


    //
    private static ArrayList<cActor> getmycutArcs(streamGraph sGraph){
	ArrayList<cActor> cutArcs = new ArrayList<cActor>(1);
	for(int e=0;e<sGraph.getGraphElementCount();++e){
	    if(sGraph.getGraphElementAt(e) instanceof cActor){
		if((((cActor)(sGraph.getGraphElementAt(e))).getAttr("toCut"))!=null)
		    cutArcs.add((cActor)(sGraph.getGraphElementAt(e)));
	    }
	}
	return cutArcs;
    }


    //This class can be moved into a single separate library, so that
    //both compilerStage3 and declusterStage1 can call it when
    //needed. Currently, we have this replicated here from
    //compilerStage3.
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
