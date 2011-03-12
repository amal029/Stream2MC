package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-09
 */


public class streamGraph extends GXLGraph{

    public streamGraph(String id){super(id);}
    private int actorCount;
    private eActor sNode = null;
    private eActor tNode = null;
    public eActor getSourceNode(){return sNode;}
    public eActor getTerminalNode(){return tNode;}
    /**
       This method requires that there be a single starting node and a
       single ending node. These can be dummy nodes either inserted by
       the compiler or by me.
     */
    private GXLNode dt = null;
    private GXLNode searchAndInsertDummyNodes(GXLGraph g,boolean val) throws Exception{
	int count = g.getGraphElementCount(); //Total number of nodes and edges
	ArrayList<GXLNode> nodes = new ArrayList<GXLNode>();
	ArrayList<GXLEdge> edges = new ArrayList<GXLEdge>();
	for(int e=0;e<count;++e){
	    GXLElement el = g.getGraphElementAt(e);
	    if(el instanceof GXLNode)
		nodes.add((GXLNode)el);
	    else if(el instanceof GXLEdge)
		edges.add((GXLEdge)el);
	}
	ArrayList<GXLNode> tSet = new ArrayList<GXLNode>();
	ArrayList<GXLNode> sSet = new ArrayList<GXLNode>();
	for (int i = 0; i<edges.size(); ++i){
	    tSet.add((GXLNode)edges.get(i).getTarget()); //This might throw a classCastException
	    sSet.add((GXLNode)edges.get(i).getSource()); //This might throw a classCastException
	}
	ArrayList<GXLNode> sNodes = new ArrayList<GXLNode>(1);
	ArrayList<GXLNode> tNodes = new ArrayList<GXLNode>(1);
	for(int i=0;i<nodes.size();++i){
	    boolean there = false;
	    for(int r=0;r<tSet.size();++r){
		if(nodes.get(i).equals(tSet.get(r))) {there = true; break;}
	    }
	    if(!there) sNodes.add(nodes.get(i));
	}
	for(int i=0;i<nodes.size();++i){
	    boolean there = false;
	    for(int r=0;r<sSet.size();++r){
		if(nodes.get(i).equals(sSet.get(r))) {there = true; break;}
	    }
	    if(!there) tNodes.add(nodes.get(i));
	}
	GXLNode sNode = null;
	if(val){
	    //Insert the nodes in the graph as required
	    //Adding the startingNode, bfore sNodes
	    sNode = new GXLNode("dummyStartNode");
	    sNode.setAttr("rep",new GXLString("0"));
	    g.add(sNode);
	    for(int e=0;e<sNodes.size();++e){
		GXLEdge sEdge = new GXLEdge(sNode,sNodes.get(e));
		g.add(sEdge);
	    }
	    //Adding the terminalNode, after tNodes
	    GXLNode tNode = new GXLNode("dummyTerminalNode");
	    tNode.setAttr("rep",new GXLString("0"));
	    g.add(tNode);
	    for(int e=0;e<tNodes.size();++e){
		GXLEdge sEdge = new GXLEdge(tNodes.get(e),tNode);
		g.add(sEdge);
	    }
	}
	else{
	    if(sNodes.size()>1) throw new RuntimeException("More than one dummy start node detected"); 
	    sNode = sNodes.get(0);
	}
	return sNode;
    }
    // private int gLabel = 0;
    // public void markMergeNodes() throws RuntimeException{
    // 	if(getSourceNode().getDocument().getDocumentElement().getGraphCount()>1)
    // 	    throw new RuntimeException("More than a single Graph in this file");
    // 	GXLGraph g = getSourceNode().getDocument().getDocumentElement().getGraphAt(0);
    // 	int totalElements = g.getGraphElementCount();
    // 	ArrayList<GXLEdge> edges = new ArrayList<GXLEdge>();
    // 	for(int e=0;e<totalElements;++e){
    // 	    GXLElement el = g.getGraphElementAt(e);
    // 	    if(el instanceof GXLEdge)
    // 		edges.add((GXLEdge)el);
    // 	}
    // 	for(int e=0;e<edges.size()-1;++e){
    // 	    GXLNode pMergeNode = (GXLNode)edges.get(e).getTarget();
    // 	    for(int r=0;r<edges.size();++r){
    // 		if(r==e) continue;
    // 		GXLNode mergeNode = (GXLNode)edges.get(r).getTarget();
    // 		if(pMergeNode.equals(mergeNode)){ ((Actor)pMergeNode).setIsMergeNode(true); break;}
    // 	    }
    // 	}
    // }
    // protected void addGuardsAndUpdates(Actor sNode){
    // 	if(sNode.ifVisited() && sNode.getIsMergeNode()){
    // 	    //If this this is already visited, I still need to check,
    // 	    //whether this is a merge node??
    // 	    sNode.setGuardLabels("L"+(++gLabel));
    // 	    //You have to return back, of course.
    // 	    return;
    // 	}
    // 	else if(sNode.ifVisited()) return;
    // 	//If it is DS or DT, nothing
    // 	if(sNode.getID().equals("dummyTerminalNode") || sNode.getID().equals("dummyStartNode")) ;
    // 	//If it is something else then start playing with it
    // 	else{
    // 	    sNode.setGuardLabels("L"+(++gLabel));
    // 	}
    // 	sNode.setVisited(true); //I have visited this node already.
    // 	//This is a recursive function.
    // 	for(int e=0;e<sNode.getConnectionCount();++e){
    // 	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
    // 		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
    // 		Actor node = (Actor)le.getTarget();
    // 		if(!node.getID().equals("dummyTerminalNode")){
    // 		    //Add the update label to this sNode for the node
    // 		    //guard
    // 		    if(!sNode.getID().equals("dummyStartNode"))
    // 			sNode.setUpdateLabels("L"+(gLabel+1));
    // 		}
    // 		addGuardsAndUpdates(node);
    // 	    }
    // 	}
    // }
    private void generateStreamGraph(GXLNode sNode){
	if(((GXLString)sNode.getAttr("Visit").getValue()).getValue().equals("true")) return;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    //Special case dummyterminal node, this one has no GXL.IN
	    //connection
	    if(sNode.getID().equals("dummyTerminalNode")){
		if(((GXLString)sNode.getAttr("Visit").getValue()).getValue().equals("false")){
		    if(sNode.getGraphCount()==0){
			++actorCount;
			if(sNode.getAttr("rate")!=null) add(new cActor(sNode));
			else {
			    eActor actor = new eActor(sNode);
			    add(actor);
			    if(actor.getID().equals("dummyStartNode")) this.sNode=actor;
			    else if(actor.getID().equals("dummyTerminalNode")) this.tNode=actor;
			}
		    }
		}
		//This is never needed, because there is no node going anywhere from terminalNode.
		// GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		// if(((GXLString)((GXLNode)le.getSource()).getAttr("Visit").getValue()).getValue().equals("false") || 
		//    ((GXLString)((GXLNode)le.getTarget()).getAttr("Visit").getValue()).getValue().equals("false")){
		//     if(((GXLNode)le.getSource()).getGraphCount()==0 && ((GXLNode)le.getTarget()).getGraphCount()==0)
		// 	add(new pRelations(le));
		// }
		sNode.setAttr("Visit",new GXLString("true"));
	    }
	    else if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		//Now do stuff
		if(((GXLString)sNode.getAttr("Visit").getValue()).getValue().equals("false")){
		    if(sNode.getGraphCount()==0){
			++actorCount;
			if(sNode.getAttr("rate")!=null) add(new cActor(sNode));
			else {
			    eActor actor = new eActor(sNode);
			    add(actor);
			    if(actor.getID().equals("dummyStartNode")) this.sNode=actor;
			    else if(actor.getID().equals("dummyTerminalNode")) this.tNode=actor;
			}
			//Say that I am done with this node.
			// System.out.println(sNode.getID());
		    }
		}
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(((GXLString)((GXLNode)le.getSource()).getAttr("Visit").getValue()).getValue().equals("false") || 
		   ((GXLString)((GXLNode)le.getTarget()).getAttr("Visit").getValue()).getValue().equals("false")){
		    if(((GXLNode)le.getSource()).getGraphCount()==0 && ((GXLNode)le.getTarget()).getGraphCount()==0)
			add(new pRelations(le));
		}
		sNode.setAttr("Visit",new GXLString("true"));
		GXLNode node = (GXLNode)le.getTarget();
		//You have to have this here
		generateStreamGraph(node);
	    }
	}
    }
    // Now we will start doing a Depth First traversal from the start node
    public void clearVisited(Actor sNode){
    	sNode.setVisited(false);
    	for(int e=0;e<sNode.getConnectionCount();++e){
    	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
    		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
    		Actor node = (Actor)le.getTarget();
    		clearVisited(node);
    	    }
    	}
    }
    /**
       @Author: Avinash Malik 

       Note: Please note that this method can be used to apply any
       function to the graph. It's like apply in functional languages,
       where apply is a lambda method.
     */
    public void simpleDFT(Actor sNode, applyFunctionsToStream function) throws Exception{
	// System.out.println(sNode.getID()+" "+sNode.getConnectionCount());
	if(sNode.ifVisited()) 
	    return; //Do nothing and return.
	function.applyFunction(sNode);
	sNode.setVisited(true); //I have visited this node already.
	//This is a recursive function.
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		if(sNode.getConnectionAt(e).isDangling())
		    return;
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		simpleDFT(node,function);
	    }
	}
    }
    private void simpleDFT(GXLNode sNode, applyFunctions function) throws Exception{
	// System.out.println(sNode.getID());
	function.applyFunction(sNode,null);
	if(dt == null && sNode.getID().equals("dummyTerminalNode"))
	    dt = sNode;
	//This is a recursive function.
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		if(sNode.getConnectionAt(e).isDangling())
		    return;
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		GXLNode node = (GXLNode)le.getTarget();
		simpleDFT(node,function);
	    }
	}
    }
    public streamGraph(GXLGraph g, boolean insertDummyNodes) throws Exception{
	super(g.getID());
	GXLNode sNode = null;
	sNode  = searchAndInsertDummyNodes(g,insertDummyNodes);
	if(insertDummyNodes){ 
	    simpleDFT(sNode,new applyFunctions(){
		    public void applyFunction(GXLNode node,GXLEdge e){
			if(node.getAttr("Visit")==null)
			    node.setAttr("Visit",new GXLString("false"));
		    }
		}); //sNode is the startNode of the graph
	}
	//Now make the streamGraph
	// System.out.println(((GXLString)dt.getAttr("Visit").getValue()).getValue());

	generateStreamGraph(sNode);
	setEdgeMode(g.getEdgeMode());
	setEdgeIDs(g.getEdgeIDs());
	GXLDocument doc = new GXLDocument();
	doc.getDocumentElement().add(this);
    }
    public Actor getActorAt(int i){
	return (Actor)getGraphElementAt(i);
    }
    public int getActorCount(){
	return actorCount;
    }

    // public interface applyFunctions {
    // 	public void applyFunction(GXLNode node,GXLEdge e);
    // }
}
