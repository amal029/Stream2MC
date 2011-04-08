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
		if(lNode.getID().equals("node4"))
		    System.out.println("prevSL: "+SL);
		SL = SL>=x?SL:x;
		if(lNode.getID().equals("node4"))
		    System.out.println("newSL: "+SL);
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
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getSource();
		    calculateStaticLevel(node,x,sortedList);
		    ++counter;
		}
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

    public String[] applyMethod(String args[], String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
    	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		Stack<eActor> sortedList = new Stack<eActor>();
		calculateStaticLevel(getLastNode((Actor)sGraph.getSourceNode()),0,sortedList);
		sGraph.simpleDFT(((Actor)sGraph.getSourceNode()),
				 new applyFunctionsToStream(){
				     public void applyFunction(Actor actor){
					 System.out.println((actor.iseActor()?actor.getID()+":"+((GXLString)actor.getAttr("SL").getValue()).getValue():""));
				     }
				 });
		sGraph.clearVisited(((Actor)sGraph.getSourceNode()));
		System.out.println("**"+sortedList.size());
		for(int e1=0;e1<sortedList.size();++e1){
		    System.out.println(sortedList.get(e1).getID()+":"+
				       new Long(((GXLString)sortedList.get(e1).getAttr("SL").getValue()).getValue()).longValue());
		}
	    }
	}
	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
