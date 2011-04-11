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
    private void calculateStaticLevel(Actor lNode, long x, Stack <eActor> sortedList) throws RuntimeException{
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
		temp = new Long(((GXLString)lNode.getAttr("total_time_x86").getValue()).getValue()).longValue();
	    else if((lNode.getID().equals("dummyTerminalNode")) || (lNode.getID().equals("dummyStartNode"))) ;
	    else throw new RuntimeException(lNode.getID());
	    x+=temp;
	}
	//Now do a tail recursive call upwards in the graph
	int counter=0;
	for(int e=0;e<lNode.getConnectionCount();++e){
	    if(lNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		if(counter>0)
		    x=(new Long(((GXLString)lNode.getAttr("SL").getValue()).getValue()).longValue()+temp);
		GXLEdge le = (GXLEdge)lNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getSource();
		calculateStaticLevel(node,x,sortedList);
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
	for(int e=0;e<splitNode.getConnectionCount();++e){
	    if(splitNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)splitNode.getConnectionAt(e).getLocalConnection();
		cActor node = (cActor)le.getTarget(); //This has got to be the communication node
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
	    time = new Long(((GXLString)((eActor)node).getAttr("total_time_x86").getValue()).getValue()).longValue();
	if(node.getID().equals(untilHere)){
	    if(mergeNode == null){
		mergeNode = (eActor)node;
		MmergeNode = (eActor)node;
		return time;
	    }
	    else return time;
	}
	//Tail recursive call the calculate the SL in a upward going
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
		eActor eChild1 = null, eChild2=null;
		do{
		    eChild1 = sortedImmediateSuccessors.pop();
		}while(eChild1.getAttr("declustered")!=null && sortedImmediateSuccessors.size()>=2);
		do{
		    eChild2 = sortedImmediateSuccessors.pop();
		}while(eChild1.getAttr("declustered")!=null && sortedImmediateSuccessors.size()>=2);
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
		execTime = new Long(((GXLString)((eActor)node).getAttr("total_time_x86").getValue()).getValue()).longValue();
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
    //DEBUGGING
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
	//Now you have consider all the possible arcs that can be cut in
	//this circular graph.
	//Do a multiple DFT
	for(int r=0;r<sNode.getConnectionCount();++r){
	    if(sNode.getConnectionAt(r).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(r).getLocalConnection();
		Actor node = (Actor)le.getTarget(); //This is always a cActor
		Actor node2= null;
		int c=2;
		do{
		    if(c == 2){
			ret.clear();
			ret.push((cActor)node);
			ret.push((cActor)node2);
		    }
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
		    else
			bestTime = temp;
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
		calculateStaticLevel(getLastNode((Actor)sGraph.getSourceNode()),0,sortedList);
		//remove the dummyTerminalNode from the sortedList
		sortedList.remove(sGraph.getSourceNode());
		//Now make the basic clusters
		Stack<Object> list = makeBasicClusters(sortedList,sGraph.getSourceNode());
		Stack<streamGraph> sortedClusterList = (Stack<streamGraph>)list.pop();
		ArrayList<cActor> cutArcs = (ArrayList<cActor>)list.pop();
		streamGraph mainGraph = (streamGraph)list.pop();
		doHeirarchicalClustering(sortedClusterList,cutArcs,mainGraph);



		//This is just to put a new line
		System.out.println();
	    }
	}
	catch(Exception e){e.printStackTrace();}
	return rets;
    }

    private static void doHeirarchicalClustering(Stack<streamGraph> sortedClusterList,ArrayList<cActor> cutArcs,
						 streamGraph mainGraph){
    }
}
