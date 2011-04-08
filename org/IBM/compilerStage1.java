/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-08
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class compilerStage1 implements compilerStage{
    public compilerStage1 () {}
    private void clearVisited(Actor sNode){
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
       @author Avinash Malik
       @date Tue Mar 8 12:38:17 GMT 2011
       @return void
       This is a very expensive algorithm for marking merge nodes.
       @bug We can make this algorithm much better by just looking at
       the nodes themselves rather than comparing with edges
     */
    private void markMergeAndSplitNodes(Actor sNode) throws RuntimeException{
	if(sNode.getDocument().getDocumentElement().getGraphCount()>1)
	    throw new RuntimeException("More than a single Graph in this file");
	GXLGraph g = sNode.getDocument().getDocumentElement().getGraphAt(0);
	int totalElements = g.getGraphElementCount();
	ArrayList<GXLEdge> edges = new ArrayList<GXLEdge>();
	for(int e=0;e<totalElements;++e){
	    GXLElement el = g.getGraphElementAt(e);
	    if(el instanceof GXLEdge)
		edges.add((GXLEdge)el);
	}
	for(int e=0;e<edges.size()-1;++e){
	    GXLNode pMergeNode = (GXLNode)edges.get(e).getTarget();
	    GXLNode pSplitNode = (GXLNode)edges.get(e).getSource();
	    for(int r=0;r<edges.size();++r){
		if(r==e) continue;
		GXLNode mergeNode = (GXLNode)edges.get(r).getTarget();
		GXLNode splitNode = (GXLNode)edges.get(r).getSource();
		if(pMergeNode.equals(mergeNode)){ ((Actor)pMergeNode).setIsMergeNode(true); break;}
		if(pSplitNode.equals(splitNode)){ ((Actor)pSplitNode).setIsSplitNode(true); break;}
	    }
	}
    }
    /**
       TODO: when building the state machines
       This needs to be updated to reflect that communication Actors are
       different to execution actors and they have more guards, one for each
       processor allocation. Also, the source actor, which feeds into the
       communication actor has more updates
     */
    private int gLabel = 0;
    private void addGuardsAndUpdates(Actor sNode){
	if(sNode.ifVisited() && sNode.getIsMergeNode()){
	    //If this this is already visited, I still need to check,
	    //whether this is a merge node??
	    sNode.setGuardLabels("L"+(++gLabel));
	    //You have to return back, of course.
	    return;
	}
	else if(sNode.ifVisited()) return;
	sNode.setGuardLabels("L"+(++gLabel));
	sNode.setVisited(true); //I have visited this node already.
	//This is a tail recursive function.
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		sNode.setUpdateLabels("L"+(gLabel+1));
		addGuardsAndUpdates(node);
	    }
	}
    }
    /**
       @author Avinash Malik
       @date Sat Mar 12 11:35:22 GMT 2011 

       This is the most important algorithm in the whole compiler. This
       algorithm goes through the stream graph and:

       1.) Makes sure that the graph is well structured in the strong
       sense, i.e., out degree of every split node is equal to the in
       degree of every corresponding merge node.

       2.) Puts labels for every node, which indicates, which splits,
       which branch does this node lie in.


       This is compiler stage 1B.
       @param starting Node of the graph
       @return void
     */
    private void checkStructureAndMarkNodes(Actor sNode, Stack<Actor> stack,Stack<Integer>index) throws RuntimeException{
	if(stack.size()!=index.size()) 
	    throw new RuntimeException("This is not a structured graph, we currently cannot handle such graphs");
	if(sNode.getIsMergeNode()){
	    Actor act = stack.pop();
	    index.pop();
	    //Adding to this actor, it's corresponding split node and
	    //vice versa
	    if(act.getMergeNode()!=null){
		if(act.getMergeNode().equals(sNode.getID()));
		else{
		    throw new RuntimeException("This is not a structured Graph, node: "+act.getID()+
					       " branches into two distinct merge nodes" +act.getMergeNode()+" "+sNode.getID());
		}
	    }
	    else{
		sNode.setSplitNode(act.getID());
		act.setMergeNode(sNode.getID());
	    }
	}
	//Put label on this snode for all (hierarchic) splits that this
	//sNode is child of
	boolean doit = true;
	int z=stack.size()-1;
	if(sNode instanceof cActor)
	    doit = ((cActor)sNode).checkcActor();
	if(!doit) z=stack.size()-2;
	String val="";
	for(;z>=0;--z)
	    val += stack.get(z).getID()+"["+index.get(z).intValue()+"]:";
	if(!stack.isEmpty() && !val.equals(""))
	    sNode.setStructureLabelAndIndex(val);
	int count=0;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		if(sNode.getIsSplitNode()){
		    if(count==0){
			for(int r=0;r<stack.size();++r){
			    sNode.splitStack.push(stack.get(r));
			    sNode.splitIndex.push(index.get(r));
			}
		    }
		    else{
			for(int r=0;r<sNode.splitStack.size();++r){
			    stack.push(sNode.splitStack.get(r));
			    index.push(sNode.splitIndex.get(r));
			}
		    }
		    stack.push(sNode); //Pushed it on the stack
		    index.push(new Integer(count));
		}
		++count;
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		checkStructureAndMarkNodes(node,stack,index);
	    }
	}
	//clear split stack and split index
	sNode.splitStack.clear(); sNode.splitIndex.clear();
    }
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{

    	    HashMap<String,streamGraph> graphs = new streamGraphParser(true).parse(args);
    	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
    		//You have the streamGraph in hand
		//Do a DFT and check the values of the actors
		// System.out.println(sGraph.getActorCount());

		//Mark all the mergeNodes
		System.out.print(".......");
		markMergeAndSplitNodes(((Actor)sGraph.getSourceNode()));
		clearVisited(((Actor)sGraph.getSourceNode()));
		//now add guards and updates
		addGuardsAndUpdates(((Actor)sGraph.getSourceNode()));
		clearVisited(((Actor)sGraph.getSourceNode()));
		// sGraph.simpleDFT(((Actor)sGraph.getSourceNode()),
		// 		 new  applyFunctionsToStream(){
		// 		     public void applyFunction(Actor actor){
		// 			 System.out.println(actor.getID()+"\n Guard Labels: "+actor.getGuardLabels()+"\n Update Labels: "+actor.getUpdateLabels());

		// 		     }
		// 		 });
		// clearVisited(((Actor)sGraph.getSourceNode()));

		//Calling compiler stae 1B
		//Make the structure labels and check that the graph is actually structured.
		Stack<Actor> stack = new Stack<Actor>();
		Stack<Integer> index = new Stack<Integer>();
		checkStructureAndMarkNodes(((Actor)sGraph.getSourceNode()),stack,index);
		System.out.print(".......");
		//Print stuff out to see what exactly happened
		// sGraph.simpleDFT(((Actor)sGraph.getSourceNode()),
		// 		 new  applyFunctionsToStream(){
		// 		     public void applyFunction(Actor actor){
		// 			 System.out.println(actor.getID()+"\n Structure Labels: "+
		// 					    actor.getStructureLabelAndIndex());
		// 		     }
		// 		 });
		clearVisited(((Actor)sGraph.getSourceNode()));
		//Write the file out onto the disk for stage2 processing
		File f = new File(args[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage1__"+f.getName()));
		rets[e] = "./output/__stage1__"+f.getName();
    	    }
	    
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
