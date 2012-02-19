/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2012-01-26 

 * This class implements the algorithm described in the * paper titled:
 * "Scheduling of stream-based real-time applications for *
 * heterogeneous systems, LCTES 2011"
 */
package org.IBM.heuristics.lib;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;
import org.IBM.heuristics.lib.*;
public class realTimeBiCriteriaScheduling implements compilerStage{

    //This is the deadline
    private static float DEADLINE = 71911;
    //This is the slack >= 1
    private static float alpha = 0;
    
    //The number of eActors
    private static int EACTORS = 0;
    
    //This is the final energy consumption
    private static float ENERGY = 0;

    private static void getHSet(Actor sNode, TreeList hSet){
	if(sNode.ifVisited()) return;
	//Calculate the variance of the node
	
	//calculate the average execution time 
	String execTime[] = null;
	
	if((sNode.getID().equals("dummyStartNode")) || (sNode.getID().equals("dummyTerminalNode")));
	else{
	    if(sNode.iseActor()){
		execTime = ((GXLString)sNode.getAttr("total_time_x86").getValue()).getValue().split(";");
		EACTORS += 1;
	    }
	    else
		execTime = ((GXLString)sNode.getAttr("work_x86").getValue()).getValue().split(";");
	    
	    float avgTime = 0;
	    for(String s : execTime)
		avgTime += Float.valueOf(s);
	    avgTime /= execTime.length;
	    
	    //DEBUG
	    // System.out.println("sNode: "+sNode.getID()+" avgTime: "+avgTime);

	
	    //Now get the variance
	    float sumDiffTimes = 0;
	    for(int i=0;i<execTime.length;++i)
		sumDiffTimes += ((Float.valueOf(execTime[i])-avgTime)*
				 (Float.valueOf(execTime[i])-avgTime));
	    //DEBUG
	    // System.out.println("sNode: "+sNode.getID()+" sumDiffTimes: "+sumDiffTimes);
	    if(avgTime == 0)
		sNode.setAttr("variance",new GXLString(0.0+""));
	    else
		sNode.setAttr("variance",new GXLString(((sumDiffTimes)/avgTime)+""));
	    //DEBUG
	    // System.out.println("sNode: "+sNode.getID()+" var: "
	    // 		       +((GXLString)sNode.getAttr("variance").getValue()).getValue());

	    hSet.add(sNode);
	}
	sNode.setVisited(true);

	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    getHSet(node,hSet);
		}
	    }
	}
	
    }
    
    private void getAllocation(TreeList<Actor> hSet){
	
	int numP = 0;
	Iterator<Actor> iter = hSet.iterator();
	while(iter.hasNext()){
	    Actor act = iter.next();
	    if(act.iseActor())
		numP = ((GXLString)act.getAttr("total_time_x86").getValue()).getValue().split(";").length;
	}
	//DEBUG
	// System.out.println("numP: "+numP);

	
	/**
	   FIXME: How to assign communication actors??  

	   Possible soln: Assign all communication actors after the
	   computation actors have been assigned. Once computation
	   actors are assigned, then communication actors are forced are
	   constrained to a single allocation.
	 */
	//The algorithm -- we only assign the eActors from hSet
	
	//First get all the computation actors from hSet into a
	//collection
	
	//eList will have things in descending order according to
	//comparator ComparatorH
	Stack<eActor> eList = new Stack<eActor>();
	iter = hSet.iterator();
	while(iter.hasNext()){
	    Actor act = iter.next();
	    //DEBUG
	    // System.out.println("act is: "+act.getID());
	    if(act.iseActor()){
		eList.push((eActor)act);
	    }
	}
	
	ArrayList<processor> pList = new ArrayList<processor>(numP);
	//Initialize the processors
	for(int i=0;i<numP;++i){
	    pList.add(new processor(i));
	}
	
	//DEBUG
	// System.out.println(eList.size());

	//Now implement the algorithm
	while(!eList.isEmpty()){
	    eActor act = eList.pop();
	    //DEBUG
	    // System.out.println("Trying to allocate: "+act.getID());

	    String execTime[] = ((GXLString)act.getAttr("total_time_x86").getValue()).getValue().split(";");
	    String Energy[] = ((GXLString)act.getAttr("total_energy_x86").getValue()).getValue().split(";");
	    
	    //An ascending list of energy values
	    TreeList<Float> energySet = new TreeList<Float>(new ComparatorE());
	    for(String s : Energy){
		float temp = Float.valueOf(s);
		energySet.add(temp);
	    }
	    
	    //DEBUG
	    // System.out.println("energySet size: "+energySet.size());
	    ArrayList<Integer> dIndices = new ArrayList<Integer>(numP);
	    
	    while(!energySet.isEmpty()){
		//remove the minimum first
		float min = energySet.pollFirst();
		//Now find the processor that this belongs to
		int index = getProcessor(Energy,min,dIndices);
		dIndices.add(index);

		//DEBUG
		// System.out.println("index : "+index);

		//Now see if putting this actor on the processor will
		//still meet the deadline
		if(meetsDeadLine(pList.get(index),pList,Float.valueOf(execTime[index]))){
		    //put it on this processor and break
		    pList.get(index).seteActor(act);
		    //DEBUG
		    // System.out.println("Allocated node: "+act.getID()+" on processor: "+index);
		    break;
		}
	    }
	}
	
	//Calculate the total energy consumption
	/**
	   FIXME: The communication energy consumption is not taken into
	   account
	 */
	float deadline = 0;
	for(processor p : pList){
	    ENERGY += p.getEnergyTimeOfAllocActors();
	    deadline += p.getExecTimeOfAllocActors();
	}
	System.out.println("My makespan is: "+deadline);
	if(deadline > DEADLINE){
	    float error = ((deadline-DEADLINE)/DEADLINE)*100;
	    System.err.println("Error from optimal(%): "+error);

	}
	
    }
    
    private static boolean meetsDeadLine(processor p, ArrayList<processor> pList, float val){
	
	//First check that all allocations on this processor "p" do not exceed the deadline
	float pTime = p.getExecTimeOfAllocActors();
	if((pTime + val) > DEADLINE) return false;
	else {
	    //Check that all communication actors can be allocated and
	    //the addition of those times does not also exceed the
	    //DEADLINE
	    /** FIXME*/
	    return true;
	}
	
    }
    
    private static int getProcessor(String Energy[], float val, ArrayList<Integer> done){
	int ret=-1;
	for(int i=0;i<Energy.length;++i){
	    if(Float.valueOf(Energy[i]) == val){
		ret = i;
		for(int y : done){
		    if(y == i) {ret = -1; break;}
		}
		if(ret != -1) break;
	    }
	}
	return ret;
    }
    
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    StringBuilder buf = new StringBuilder();
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		
		float time = System.nanoTime();
		//Get the nodes in the SDF graph put into the decreasing
		//order using variance
		clearVisited(sGraph.getSourceNode());
		TreeList hSet = new TreeList<Actor>(new ComparatorH());
		getHSet(sGraph.getSourceNode(),hSet);
		//DEBUG
		// System.out.println("hSet size: "+hSet.size());

		clearVisited(sGraph.getSourceNode());
		
		//find the best allocation onto processors that
		//minimizes energy consumption and still meets the
		//deadline. The deadline is obtained from our
		//bi-criteria scheduling algorithm
		getAllocation(hSet);
		System.out.println("Total energy consumption for DEADLINE: "+DEADLINE+" is : "+ENERGY);
		float totTime = (System.nanoTime()-time)/1000000;
		System.out.println("Time taken (ms): "+totTime);
		System.out.print(".......Done\n");
		//Write the file out onto the disk for next stage
		//processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();

		//This is writing the GXL file after processing
		sGraph.getDocument().write(new File("./output","__real_time_bicriteria__"+f.getName()));
		rets[e] = "./output/__ilpbicriteria__"+f.getName();
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
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

    private class ComparatorE implements Comparator<Float>{
	
	public int compare(Float one, Float two){
	    if(one > two) return 1;
	    else if(one < two) return -1;
	    else return 0;
	}
    }
    private class ComparatorH implements Comparator<Actor>{
	
	public int compare(Actor one, Actor two){
	    float var1 = Float.valueOf(((GXLString)one.getAttr("variance").getValue()).getValue());
	    float var2 = Float.valueOf(((GXLString)two.getAttr("variance").getValue()).getValue());
	    if(var1 > var2) return 1;
	    else if(var1 < var2) return -1;
	    else return 0;
	}
    }

    private class processor {
	
	//The list of computation actors assigned to this processor
	private ArrayList<eActor> list = new ArrayList<eActor>();
	private int myIndex = -1;
	public processor(int index){
	    this.myIndex = index; //which processor am I?
	}
	
	public void seteActor(eActor actor){
	    list.add(actor);
	}
	
	public float getExecTimeOfAllocActors(){
	    float execTime = 0;
	    for(eActor a : list){
		execTime += 
		    Float.valueOf(((GXLString)a.getAttr("total_time_x86").getValue()).getValue().split(";")[myIndex]);
	    }
	    return execTime;
	}
	
	public float getEnergyTimeOfAllocActors(){
	    float execTime = 0;
	    for(eActor a : list){
		execTime += 
		    Float.valueOf(((GXLString)a.getAttr("total_energy_x86").getValue()).getValue().split(";")[myIndex]);
	    }
	    return execTime;
	}
    }
    
    //Not an optimized sorted list
    //Neither is it complete, i.e., for general usage.
    //Different from a set
    private class TreeList<T> {
	private ArrayList<T> list = new ArrayList<T>();
	private Comparator<T> comp = null;
	public TreeList(Comparator<T> val){
	    this.comp = val;
	}
	
	public void add(T val){
	    for(int i =list.size()-1;i>=0;--i){
		int ret = comp.compare(val,list.get(i));
		if(ret >= 0){
		    list.add(i+1,val);
		    break;
		}
	    }
	    if(list.isEmpty())
		list.add(val);
	}
	
	public T pollFirst(){
	    return list.remove(0);
	}
	
	public Iterator<T> iterator(){
	    return list.iterator();
	}
	
	public boolean isEmpty(){
	    return list.isEmpty();
	}
	
	public int size(){
	    return list.size();
	}
	
    }
    
}


