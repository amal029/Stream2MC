/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2012-02-07
 
 * XXX: Please fix the FIXMES.
 */
package org.IBM.heuristics;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

public class pCarpenter implements compilerStage{
    
  private static ArrayList<ArrayList<Actor>> cBSets = new ArrayList<ArrayList<Actor>>();
    
  private static HashMap<String,ArrayList<String>> processorAllocations = 
    new HashMap<String,ArrayList<String>>();
  private static HashMap<String,float[]> costMap = new HashMap<String,float[]>();
    
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

  private static HashMap<String,Integer> jNodeMap = new HashMap<String,Integer>();
  private static ArrayList<Actor> getConnectedBasicSets(Actor sNode, ArrayList<Actor> list){
    if(sNode.ifVisited()) return list;
    if(sNode.getIsMergeNode()){
      //check if it is in the join node map
      if(jNodeMap.containsKey(sNode.getID())){
	int temp = jNodeMap.get(sNode.getID());
	if(temp+1 == getJoinParentNum(sNode)){
	  //then push it onto the list and continue
	  list.add(sNode);
	  sNode.setVisited(true);
	  //remove it from the map
	  jNodeMap.remove(sNode.getID());
	}
	else{
	  //increment the value in the map
	  jNodeMap.put(sNode.getID(),new Integer(temp+1));
	  //return
	  return list;
	}
      }
      else{
	//put the key in the map
	jNodeMap.put(sNode.getID(),new Integer(1));
	return list;
      }
    }
    else{
      list.add(sNode);
      sNode.setVisited(true);
    }

    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  //if the sNode is a splitNode then
	  if(sNode.getIsSplitNode() && !(sNode.getID().equals("dummyStartNode"))) {
	    //push the old list onto the list of lists
	    cBSets.add(list);
	    //now call the damn thing
	    ArrayList<Actor> listN = new ArrayList<Actor>();
	    list = getConnectedBasicSets(node,listN);
	  }
	  else{
	    list = getConnectedBasicSets(node,list);
	  }
	}
      }
    }
    return list;
  }
    
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

  private static ArrayList<GXLNode> p = null;
  public String[] applyMethod(String args[],String fNames[]){
    String rets[] = new String[args.length];
    try{
      HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
      //Getting the processor architecture
      ArrayList<ArrayList> plist = pArchParser(new File("pArch.gxl"));
      p = (ArrayList<GXLNode>)plist.get(0);
      //DEBUG
      // for(GXLNode np : p){
      // 	System.out.println(np.getID());
      // }
      for(int e=0;e<args.length;++e){
	streamGraph sGraph = graphs.get(args[e]);
	removeParallelEdges(sGraph);
	//mark all the joinnodes
	//first make the connected basic sets
	clearVisited(sGraph.getSourceNode());
	ArrayList<Actor> list = new ArrayList<Actor>();
	ArrayList<Actor> flist = getConnectedBasicSets(sGraph.getSourceNode(),list);
	cBSets.add(flist);
		
	//Now put all the basic sets onto a single processor

	//get the number of processors in the system
	setProcessorCount(sGraph.getSourceNode());
		
	//assign the basic sets to the first processor
	float makespan = assignCbSetsToRandomProcessor();
		
	//Now refine the assignment
	float nMakespan = refineAssignMent(makespan);
	if(nMakespan != 0) makespan = nMakespan;

	//List schedule the damn thing so that we can get the
	//values for without software pipelined execution.
	//Just assign the terminal node somewhere
	eActor termNode = sGraph.getTerminalNode();
	//set the SL for this fucker
	termNode.setAttr("SL",new GXLString("0"));

	//This is writing the GXL file after processing
	File f = new File(fNames[e]);
	File dir = new File("./output");
	if(!dir.exists())
	  dir.mkdir();
	sGraph.getDocument().write(new File("./output","__carpenter__"+f.getName()));

	long makespan_nsw = new org.IBM.declustering.listSchedule(sGraph,p).schedule();
	System.out.println("The makespan without software pipelining is:"+ makespan_nsw);

	//Return
	rets[e] = "./output/__critical_path__"+f.getName();
      }
    }
    catch(SAXException se){se.printStackTrace();}
    catch(IOException e){e.printStackTrace();}
    catch(Exception e){e.printStackTrace();}
    return rets;
  }

  private static float refineAssignMent(float currMakespan){
    if(freeList == null) return 0;

    //get the maximum makespan processor and see if this thing has more
    //than one basic sets allocated to it.
    
    //DEBUG
    int fCount = freeList.size();
    System.out.println("Free Processor count: "+fCount);

    int b = 0, ignore=-1,fCounter=0;
    Stack<String> toMove = new Stack<String>();
    while(!freeList.isEmpty()){
      // boolean allocated = false;
      b =0;
      ignore=-1;
      String freeProc = freeList.remove(0);
      //DEBUG
      System.out.println("Trying with freeProc: "+freeProc);

      while(b<((pCount.length)-fCount)){
	//First remove the processor from the freeList

	//First ignore nothing
	int mProcIndex = getMaxProcIndex(ignore);
	//from then on ignore the obtained index
	ignore = mProcIndex;
	//now get the variable CB sets assgined to this processor
	String proc = pCount[mProcIndex];
	ArrayList<String> list = processorAllocations.get(proc);
	//The list is the CBsets assigned to this processor
	//CB0, CB1, etc
	float newCost = 0f, currCost = 0f;
	//DEBUG
	System.out.println("proc: "+proc+" freeProc: "+freeProc+" list size: "+list.size());

	if(list.size() > 1) {
	  //Try moving everyone one-by-one to the freeList
	  for(String var : list) {
	    System.out.println("var: "+var);

	    int indexInToCBSet = Integer.valueOf(var.split("_")[1]);
	  
	    //First we need to emulate moving the set to the free
	    //processor. \textit{iff} the resultant makespan is better
	    //will we be really moving it to that processor.
	  
	    /**emulate movement*/
	  
	    //The current makespan of this basic set to its current allocation
	    currCost += getCBCost(cBSets.get(indexInToCBSet),Integer.valueOf(proc.split("_")[1]));
	    newCost += getCBCost(cBSets.get(indexInToCBSet),Integer.valueOf(freeProc.split("_")[1]));
	  
	    //Get the original makespan
	    float omSpan = makespan[0];
	    for(float gv : makespan){
	      if(omSpan < gv) {
		omSpan = gv;
	      }
	    }
	  
	    //DEBUG
	    System.out.println("Original makespan is: "+omSpan);

	    //Now get the makespan after the refinement
	    float mSpan = makespan[0];
	    for(int f=1;f<makespan.length;++f){
	      float gv = makespan[f];
	      if(f == mProcIndex)
		//subtract gv by the currCost
		gv -= currCost;
	      if(mSpan < gv)
		mSpan = gv;
	    }
	    //Now compare mSpan with newCost
	    if(mSpan < newCost)
	      mSpan = newCost;
	  
	    //DEBUG
	    System.out.println("New makespan is: "+mSpan);

	    //Now compare the original with the refined makespan, to check
	    //if the actual shift should be made
	    if(mSpan < omSpan){
	      //Then make the shift
	      toMove.push(var);

	      //DEBUG
	      System.out.println("Moving "+var);

	    }
	  }
	
	  //Now actually move the processorAllocations. Needed, because I
	  //don't wanna do copy on write inside the loop.
	  if(!toMove.isEmpty()){
	    ArrayList<String> nList = new ArrayList<String>();
	    while(!toMove.isEmpty()){
	      String var = toMove.pop();
	      list.remove(var);
	      //Add it to the new ProcessorAllocation
	      //make a new list and put that into the map
	      nList.add(var);
	    }
	    processorAllocations.put(freeProc,nList);

	    //The freeProc has now been allocated something
	    // freeList.remove(fCounter);
	    // ++fCounter;
	  }
	}
	++b;
      }
    }
    
    //Now calculate the new makespan and print it out
    return calcMakespan();
  }
    
  private static int getMaxProcIndex(int ignore){
    if(makespan == null) throw new RuntimeException("Free List is empty and also there are no processor assignments!!");
    float mSpan = 0;
    int index=0,mProcIndex=0;
    for(index=0;index<makespan.length;++index){
      float gv = makespan[index];
      if(index == ignore) continue;
      if(mSpan < gv) {
	mSpan = gv;
	mProcIndex = index;
      }
    }
    return mProcIndex;
  }
    
  private static float assignCbSetsToRandomProcessor(){
    //DEBUG
    int i =0;
    System.out.println();
    for(ArrayList<Actor> list : cBSets) {
      System.out.print("Set "+i+" has: ");
      for(Actor act : list) {
	System.out.print(act.getID()+" ");
      }
      System.out.println();
      ++i;
    }
	
    //calculate the processing time for CB sets for every processor
    for(i = 0; i < cBSets.size(); i++) {
      float[] list = new float[pCount.length];
      for(int j=0;j<pCount.length;++j)
	list[j] = getCBCost(cBSets.get(i),Integer.valueOf(pCount[j].split("_")[1]));
      //add it to the hashmap
      costMap.put(("CB_"+(i)),list);
      //DEBUG
      System.out.println("For set "+i+" processor costs are:");
      for (int k = 0; k < pCount.length; ++k)
	System.out.print(list[k]+",");
      System.out.println();

    }
	
    //assign the basic sets to the minimum cost processors
    //FIXME: I have not taken care of convexity
    makeInitialPartition();
    return calcMakespan();
  }
  //The currently available free processors.
  private static ArrayList<String> freeList = null;
  private static float[] makespan = null;
  //Calculate the current makespan
  private static float calcMakespan(){
    makespan = new float[pCount.length];
    int i=0,counter=0;
    for(String variable : pCount) {
      if(processorAllocations.containsKey(variable)) {
	//DEBUG
	System.out.print("On processor P_"+(Integer.valueOf(variable.split("_")[1]))+": ");
	ArrayList<String> list = processorAllocations.get(variable);
	for(String var : list) {
	  makespan[i] += costMap.get(var)[i];
	  int indexInToCBSet = Integer.valueOf(var.split("_")[1]);
	  ArrayList<Actor> llact = cBSets.get(indexInToCBSet);
	  //making things for list-scheduling
	  for(Actor act : llact) {
	    act.setAttr("SL",new GXLString(""+(new Float(costMap.get(var)[i]).longValue())));
	    act.setAttr("ProcessorAlloc",new GXLString(""+p.get(Integer.valueOf(variable.split("_")[1])).getID()));
	    //DEBUG
	    // System.out.println(act.getID()+"  SL  "+((GXLString)act.getAttr("SL").getValue()).getValue());
	    // System.out.println(act.getID()+"  ProcessorAlloc  "
	    // 		   +((GXLString)act.getAttr("ProcessorAlloc").getValue()).getValue());
	  }
	  System.out.println(" Allocated set is: "+var+" makespan is: "+makespan[i]);
	}
      }
      else {
	//DEBUG
	System.out.println("Nothing allocated to processor P"+(Integer.valueOf(variable.split("_")[1])));
	makespan[i] = 0;
	if(freeList == null)
	  freeList = new ArrayList<String>();
	freeList.add(variable);
	++counter;
      }
      ++i;
    }
    //DEBUG
    float mSpan = makespan[0];
    for(float gv : makespan){
      if(mSpan < gv) {
	mSpan = gv;
      }
    }
    System.out.println("Overall initial makespan is: "+mSpan);
    return mSpan;
  }
  // private static Hashmap<String,Integer> pointerToCBSets = new Hashmap<String,Integer>();
  private static void makeInitialPartition(){
    for(int i=0;i<cBSets.size();++i) {
      String name = "CB_"+i;
      float [] costs = costMap.get(name);
      //get the min cost index
      //DBEUG
      // System.out.println("For set "+i+": ");
      int index = getMinCostProcessorAlloc(costs,-1);
      //add it to the processor allocation
      if(processorAllocations.containsKey(pCount[index])){
	//then add the new CB to this list
	//get another minimal index
	int nIndex = getMinCostProcessorAlloc(costs,index);
	if(costs[nIndex] == costs[index]){
	  index = nIndex;
	  if(processorAllocations.containsKey(pCount[index])){
	    ArrayList<String> list = processorAllocations.get(pCount[index]);
	    list.add(name);
	    processorAllocations.put(pCount[index],list);
	  }
	  else{
	    //make a new list and put that into the map
	    ArrayList<String> list = new ArrayList<String>();
	    list.add(name);
	    processorAllocations.put(pCount[index],list);
	  }
	}
	else{
	  ArrayList<String> list = processorAllocations.get(pCount[index]);
	  list.add(name);
	  processorAllocations.put(pCount[index],list);
	}
      }
      else{
	//make a new list and put that into the map
	ArrayList<String> list = new ArrayList<String>();
	list.add(name);
	processorAllocations.put(pCount[index],list);
      }
    }
  }
    
  private static int getMinCostProcessorAlloc(float [] costs, int ignore){
    int index = 0;
    float minCost = costs[index];
	
    for(int i=1;i<costs.length;++i){
      if(i == ignore) continue;
      if(costs[i] < minCost) {
	index = i;
	minCost = costs[i];
      }
    }
    //DEBUG
    // System.out.println("Min cost proc alloc is: "+index+" and cost is: "+minCost);

    return index;
  }
    
  private static float getCBCost(ArrayList<Actor> list, int index){
    float totCost = 0;
    for(Actor act : list){
      float cost = 0;
      //DEBUG
      // System.out.print("Getting cost for actor: "+act.getID()+": ");

      if(act.getID().equals("dummyStartNode") || act.getID().equals("dummyTerminalNode")){
	//set the processor cost to 0
	cost = 0;
      }
      else if(act.iscActor()){
	//get the cost using work_x86
	String c = ((GXLString)act.getAttr("work_x86").getValue()).getValue().split(";")[index*pCount.length];
	cost = Float.valueOf(c);
      }
      else if(act.iseActor()){
	//get the cost using total_time_x86
	String c = ((GXLString)act.getAttr("total_time_x86").getValue()).getValue().split(";")[index];
	cost = Float.valueOf(c);
      }
      //DEBUG
      // System.out.println(cost);

      totCost += cost;
    }
    return totCost;
  }
    
  private static HashMap<String,Integer> jNumMap = new HashMap<String,Integer>();
  private static int getJoinParentNum(Actor sNode){
    int ret = 0;
    if(jNumMap.containsKey(sNode.getID())){
      ret = jNumMap.get(sNode.getID());
    }
    else{
      for(int e=0;e<sNode.getConnectionCount();++e){
	if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
	  GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	  if(le.getAttr("parallelEdge")==null){
	    ret++;
	  }
	}
      }
      jNumMap.put(sNode.getID(),new Integer(ret));
    }
    return ret;
  }
  private static String pCount[] = null;
  private static void setProcessorCount(Actor sNode){
    String processors[] = 
      ((GXLString)sNode.getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
    pCount = new String[processors.length];
    for(int e=0;e<processors.length;++e)
      pCount[e] = "P_"+e;
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
}
