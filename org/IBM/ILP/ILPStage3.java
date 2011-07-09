/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-07-07
 */

package org.IBM.ILP;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

/**
   This file writes the ILP formulation for scheduling with minimum
   buffer usage. This file needs to be called after calling stage2 and
   cplexSolParser
 */

public class ILPStage3 implements compilerStage{

    private static double elastic = 0.5;
    public ILPStage3 () {}
    
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
    /**
       This method makes the objective:
       minimize(\sum_{\forall (i,j)\in E}B_{ij})
       
       There is something wrong with this: see the objective function
       produced for the bitonicsort examples
     */
    private static boolean first = true;
    private static void makeObjective(Actor sNode, StringBuilder buf,String source){
	if(sNode.ifVisited()) return;
	sNode.setVisited();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    if(node.iscActor());
		    else{
			String target = node.getID();
			if(!first)
			    buf.append(" + ");
			else
			    first=false;
			buf.append("B_"+source+"_"+target);
			generals.add("B_"+source+"_"+target);
			// if(target.equals("dummyTerminalNode"));
			// else buf.append(" + ");
			source=target;
		    }
		    makeObjective(node,buf,source);
		}
	    }
	}
    }

    /**
       This method adds the processor allocation constraint:
       \forall i\in V \exsits x \in P, b_{ix} = 1
       
       XXX: This method always uses the name of the file and searches in
       the /tmp/ directory for the cplex solution file
       
       Ths CPLEX solution file should end with .sol
     */
    private static cplexSolParser parser = null;
    private static ArrayList<String> currAllocs = new ArrayList<String>();
    private static void addProcessorAllocations(Actor sNode, StringBuilder buf, String fName) throws IOException{
	String fNames[] = fName.trim().split("\\.");
	parser = new cplexSolParser();
	String names[] = new String[1];
	names[0] = "/tmp/"+fNames[0]+".sol";
    	parser.applyMethod(names,null);
	collectNodes(sNode);
	if(pCount == null)
	    setProcessorCount(sNode);

	//Now get the b_{ix} from the cplexSolParser and if its 1 then
	//add it to the constraints
	
	while(!nodes.isEmpty()){
	    Actor node = nodes.remove(0);
	    String source = node.getID();
	    //Making the processor allocations
	    for(int r=0;r<pCount.length;++r){
		String b = "b_"+source+"p"+pCount[r];
		String val = parser.getValue(b);

		//These values can be something other than 1, seems as
		//if CPLEX relaxes the binary constraints as well, which
		//is really stupid.

		float fval = new Float(val);
		if(fval > 0){
		    if(1-fval > elastic){
			buf.append(b+" = "+val+"\n");
		    }
		    else{
			currAllocs.add(b);
			buf.append(b+" = "+val+"\n");
		    }
		}
		else
		    buf.append(b+" = 0\n");
	    }
	    //Making the communication allocations
	    for(Actor act : nodes){
		String target = act.getID();
		for(int r=0;r<pCount.length;++r){
		    for(int e=0;e<pCount.length;++e){
			String bc = "b_"+source+"p"+pCount[r]+"_"+target+"p"+pCount[e];
			try{
			    String val = parser.getValue(bc);
			    if(val != null){
				Float fval = new Float(val);
				if(fval > 0){
				    if(1-fval > elastic)
					buf.append(bc+" = "+val+"\n");
				    else{
					currAllocs.add(bc);
					buf.append(bc+" = "+val+"\n");
				    }
				    // buf.append(bc+" = 1\n");
				    // currAllocs.add(bc);
				}
				else
				    buf.append(bc+" = 0\n");
			    }
			}
			catch(NullPointerException ne){;}
		    }
		}
	    }
	}
    }
    private static int pCount[] = null;
    private static void setProcessorCount(Actor sNode){
	String processors[] = ((GXLString)sNode.getAttr("__update_labels_with_processors").getValue()).getValue().split(";");

	pCount = new int[processors.length];
	for(int e=0;e<processors.length;++e)
	    pCount[e] = (e+1);
    }
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

    /**
       This method adds the activation matrix for all nodes that is:
       \sum_{t=0}^{\Pi-1} A(t,i) = 1, \forall i\in V
     */
    private static long pi = 0;
    
    private static ArrayList<String> generals = new ArrayList<String>();
    
    private static void addActivationMatrix(Actor sNode, StringBuilder buf){
	collectNodes(sNode);
	
	//First get the value of \Pi from cplexSolParser
	
	pi = new Float(parser.getValue("l")).longValue();
	
	int counter=0;
	while(!nodes.isEmpty()){
	    Actor node  = nodes.remove(0);
	    if(node.getID().equals("dummyStartNode")) continue;
	    for(int e=0;e<pi;++e){
		if(e < pi-1) 
		    buf.append("a_"+e+"_"+node.getID()+" + ");
		else
		    buf.append("a_"+e+"_"+node.getID()+" = 1");
		generals.add("a_"+e+"_"+node.getID());
	    }
	    buf.append("\n\n");
	    ++counter;
	}
    }

    private static long[] getExecutionCost(eActor node){
	if(node.getID().equals("dummyStartNode") || node.getID().equals("dummyTerminalNode")){
	    long ret[] = new long[pCount.length];
	    for(int e=0;e<ret.length;++e)
		ret[e] = 0;
	    return ret;
	}
	String [] costs = ((GXLString)node.getAttr("total_time_x86").getValue()).getValue().split(";");
	long ret[] = new long[costs.length];
	for(int r=0;r<costs.length;++r){
	    ret[r] = new Long(costs[r]).longValue();
	}
	return ret;
    }
    private static long[] getCommunicationCost(cActor node){
	String costs[] = ((GXLString)node.getAttr("work_x86").getValue()).getValue().split(";");
	long [] ret = new long[costs.length];
	for(int e=0;e<costs.length;++e)
	    ret[e] = new Long(costs[e]).longValue();
	return ret;
    }
    private static ArrayList<ArrayList> getCommCosts(eActor sNode){
	ArrayList<ArrayList> list = new ArrayList<ArrayList>(2);
	ArrayList<String> elist = new ArrayList<String>(4);
	ArrayList<long []> clist = new ArrayList<long[]>(4);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    if(node.iscActor()){
			long [] costs = getCommunicationCost((cActor)node);
			clist.add(costs);
			//get the name of the next execution actor.
			for(int e1=0;e1<node.getConnectionCount();++e1){
			    if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
				GXLEdge le1 = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
				if(le1.getAttr("parallelEdge")==null){
				    eActor node1 = (eActor)le1.getTarget();
				    elist.add(node1.getID());
				    break;//guaranteed to have only 1
				}
			    }
			}
		    }
		    else{
			;//give zero costs back
		    }
		}
	    }
	}
	list.add(elist); list.add(clist);
	return list;
    }
    /**
       This method adds the constraint that makes sure all computations
       for actors finish before the period.
     */
    
    @SuppressWarnings("unchecked")
    private static void addCompletionOfActivation(Actor sNode, StringBuilder buf){
	nodes.clear();
	collectNodes(sNode);
	int counter=0;
	for(Actor node: nodes){
	    long costs[] = getExecutionCost((eActor)node);
	    //Get communication costs for the node actor
	    ArrayList<ArrayList> list = getCommCosts((eActor)node);
	    ArrayList<String> elist = list.get(0);
	    ArrayList<long[]> clist = list.get(1);
	    if(elist.size() != clist.size())
		throw new RuntimeException();
	    //Now we need to go through all activation matrix elements
	    for(int w=0;w<pi;++w){
		buf.append("a_"+w+"_"+node.getID()+" + ");
		//Append the communication costs that you have
		int tcounter =0;
		while(tcounter < elist.size()){
		    int mcounter=0;
		    String actt = elist.get(0);
		    long[] ccosts = clist.get(0);
		    ++tcounter;
		    for(int e=0;e<pCount.length;++e){
			for(int r=0;r<pCount.length;++r)
			    buf.append(ccosts[e+r+mcounter]+" b_"+node.getID()+"p"+pCount[e]+"_"+actt+"p"+pCount[r]+" + ");
			mcounter+=pCount.length-1;
		    }
		}
		for(int e=0;e<costs.length;++e){
		    if(e < costs.length - 1)
			buf.append(costs[e]+" b_"+node.getID()+"p"+pCount[e]+" + ");
		    else
			buf.append(costs[e]+" b_"+node.getID()+"p"+pCount[e]+" <= "+pi+"\n");
		}
	    }
	    ++counter;
	    buf.append("\n");
	}
    }

    /**
       This method builds the usage matrix, the equation is too long to
       write.

       FIXME: I have taken a short cut, i.e., since I know the
       allocations, I just use the those costs. I can make this very
       general by using a truth table, see how the || operator has been
       implemented in ILPStage2.java, same thing can be done here.
       
     */
    @SuppressWarnings("unchecked")
    private static void addUsageMatrix(Actor sNode, StringBuilder buf){
	nodes.clear();
	collectNodes(sNode);
	int counter=0;
	while(!nodes.isEmpty()){
	    long nodeCost = 0;
	    Actor node = nodes.remove(0);
	    long costs[] = getExecutionCost((eActor)node);
	    //Get communication costs for the node actor
	    ArrayList<ArrayList> list = getCommCosts((eActor)node);
	    ArrayList<String> elist = list.get(0);
	    ArrayList<long[]> clist = list.get(1);
	    if(elist.size() != clist.size())
		throw new RuntimeException();
	    int tcounter = 0;
	    while(tcounter < elist.size()){
		int mcounter=0;
		String actt = elist.get(0);
		long[] ccosts = clist.get(0);
		++tcounter;
		for(int e=0;e<pCount.length;++e){
		    for(int r=0;r<pCount.length;++r){
			for(String s : currAllocs){
			    if(("b_"+node.getID()+"p"+pCount[e]+"_"+actt+"p"+pCount[r]).equals(s)){
				nodeCost += ccosts[e+r+mcounter];
			    }
			}
		    }
		    mcounter+=pCount.length-1;
		}
	    }
	    for(int e=0;e<costs.length;++e){
		for(String s : currAllocs){
		    if(("b_"+node.getID()+"p"+pCount[e]).equals(s)){
			nodeCost += costs[e];
		    }
		}
	    }
	    //Now we can build the usage matrix
	    for(int e=0;e<pi;++e){
		buf.append("u_"+e+"_"+node.getID()+" - ");
		generals.add("u_"+e+"_"+node.getID());
		int l = 0;
		while(l <= nodeCost){
		    if((e-l) >= 0){
			if(l == nodeCost)
			    buf.append("a_"+(e-l)+"_"+node.getID()+" = 0");
			else
			    buf.append("a_"+(e-l)+"_"+node.getID()+" - ");
		    }
		    //wrap around pi
		    else if((e-l) < 0){
			long index = pi+(e-l);
			if(l == nodeCost)
			    buf.append("a_"+index+"_"+counter+" = 0");
			else
			    buf.append("a_"+index+"_"+counter+" - ");
		    }
		    ++l;
		}
		buf.append("\n\n");
	    }
	    buf.append("\n\n");
	    ++counter;
	}
    }

    /**
       This method makes the usage matrix completion, i.e., it makes
       sure that at any given time step in the period \pi the total
       number of processors being used does not exceed N:
       
       \sum_{\forall i \in V} U(t,i) \leq N, \forall t \in
       \{0,...\pi-1\}
     */
    private static void addUsageMatrixCompletion(Actor sNode, StringBuilder buf){
	nodes.clear();
	collectNodes(sNode);
	for(int r=0;r<pi;++r){
	    int counter=0;
	    while(counter < nodes.size()){
		Actor node = nodes.get(counter);
		if(counter < nodes.size() -1)
		    buf.append("u_"+r+"_"+node.getID()+" + ");
		else
		    buf.append("u_"+r+"_"+node.getID()+" <= "+pCount.length);
		++counter;
	    }
	    buf.append("\n\n");
	}
    }

    /**
       This method gives the first instantiation times for all the nodes
       in the graph
     */
    private static void addProlougeTimes(Actor sNode, StringBuilder buf){
	nodes.clear();
	collectNodes(sNode);
	int counter=0;
	while(!nodes.isEmpty()){
	    Actor node = nodes.remove(0);
	    buf.append(pi+" k_"+node.getID()+" + ");
	    generals.add("k_"+node.getID());
	    generals.add("t_"+node.getID());
	    for(int e=0;e<pi;++e){
		if(e == 0)
		    buf.append("0");
		else
		    buf.append("1");
		buf.append(" a_"+e+"_"+node.getID());
		if(e < pi-1)
		    buf.append(" + ");
		else
		    buf.append(" - t_"+node.getID()+" = 0\n\n");
	    }
	    ++counter;
	}
    }

    /**
       This method makes sure that all dependencies the graph are
       satisfied. Too long to put here
       
       FIXME: The current code, does not handle cycles, although the
       theory to handle cycles is there.
     */
    
    @SuppressWarnings("unchecked")
    private static void addDependencyDeclarations(Actor sNode, StringBuilder buf){
	if(sNode.ifVisited()) return;
	sNode.setVisited(true);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    if(node.iscActor()){
			for(int e1=0;e1<node.getConnectionCount();++e1){
			    if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
				GXLEdge lce = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
				if(lce.getAttr("parallelEdge")==null){
				    //There should be only one eActor
				    //attached to this thing.
				    node = (Actor)lce.getTarget();
				    break;
				}
			    }
			}
		    }
		    //Now we know that node has to be an eActor
		    //Get the cost for sNode
		    long costs[] = getExecutionCost((eActor)sNode);
		    //Get communication costs for the sNode actor
		    ArrayList<ArrayList> list = getCommCosts((eActor)sNode);
		    ArrayList<String> elist = list.get(0);
		    ArrayList<long[]> clist = list.get(1);
		    if(elist.size() != clist.size())
			throw new RuntimeException();
		    buf.append("t_"+node.getID()+" - t_"+sNode.getID()+" - ");
		    //Append the communication costs that you have
		    while(!elist.isEmpty()){
			int mcounter=0;
			String actt = elist.remove(0);
			long[] ccosts = clist.remove(0);
			for(int z=0;z<pCount.length;++z){
			    for(int r=0;r<pCount.length;++r)
				buf.append(ccosts[z+r+mcounter]+" b_"+sNode.getID()+"p"+pCount[z]+"_"+actt+"p"+pCount[r]+" - ");
			    mcounter+=pCount.length-1;
			}
		    }
		    //FIXME: Remove the comment here
		    //buf.append(pi" distance_"+sNode.getID()+"_"+node.getID()+" - ");

		    for(int y=0;y<costs.length;++y){
			if(y < costs.length - 1)
			    buf.append(costs[y]+" b_"+sNode.getID()+"p"+pCount[y]+" - ");
			else
			    buf.append(costs[y]+" b_"+sNode.getID()+"p"+pCount[y]+" >= 0\n");
		    }

		    //Finally call the method recursively to passing in node
		    addDependencyDeclarations(node,buf);
		}
	    }
	}
    }

    /**
       This method adds the buffer constraints, too long to put the
       constraint here.
       
       FIXME: The cycles are not taken care of in the code, although the
       theory is there.
     */
    
    private static void addBufferConstraint(Actor sNode, StringBuilder buf){
	if(sNode.ifVisited()) return;
	sNode.setVisited(true);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    long Oi = 0; //the number of tokens produced
		    long Gi = sNode.getRep(); //the granularity
		    Actor node = (Actor)le.getTarget();
		    if(node.iscActor()){
			//Set the number of tokens produced
			Oi = ((cActor)node).getRate();
			for(int e1=0;e1<node.getConnectionCount();++e1){
			    if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
				GXLEdge lce = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
				if(lce.getAttr("parallelEdge")==null){
				    //There should be only one eActor
				    //attached to this thing.
				    node = (Actor)lce.getTarget();
				    break;
				}
			    }
			}
		    }
		    //Now we know that node has to be an eActor
		    buf.append(pi+" B_"+sNode.getID()+"_"+node.getID()+" - "+(Oi*Gi)+" t_"+node.getID()+" + "
			       +(Oi*Gi)+" t_"+sNode.getID()+" >= 0\n");

		    //Finally call the method recursively to passing in node
		    addBufferConstraint(node,buf);
		}
	    }
	}
    }

    public String[] applyMethod(String args[], String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    StringBuilder buf = new StringBuilder();
	    File f = new File(fNames[e]);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Stage-1
		//add the minimization function
		buf.append("min\n\n"); //This is the objective function
		buf.append("obj:");
		makeObjective(sGraph.getSourceNode(),buf,sGraph.getSourceNode().getID());
		buf.append("\n\n");
		clearVisited(sGraph.getSourceNode());

		buf.append("st\n\n"); //These are the constraints
		addProcessorAllocations(sGraph.getSourceNode(),buf,new File(fNames[e]).getName());
		clearVisited(sGraph.getSourceNode());
		buf.append("\n\n");
		
		addActivationMatrix(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		buf.append("\n\n");
		
		addCompletionOfActivation(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		buf.append("\n\n");

		addUsageMatrix(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());

		addUsageMatrixCompletion(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		
		addProlougeTimes(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		
		addDependencyDeclarations(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		buf.append("\n\n");

		addBufferConstraint(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
		buf.append("\n\n");
		
		//appending the bounds
		for(String s : generals)
		    buf.append(s+" >= 0\n");
		buf.append("\n");
		//Adding the integer variables
		buf.append("general\n");
		for(String s : generals)
		    buf.append(s+"\n");
		buf.append("\n");

		//This is the end of the ILP formulation
		buf.append("end");

		System.out.print(".......Done\n");
		//Write the file out onto the disk for next stage
		//processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();

		//This is writing the ilp formulation itself 
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__ilpswbuf__"+f.getName()+".lp")));
		out.write(buf.toString());
		out.close();
	    }
	}
    	// catch(SAXException se){se.printStackTrace();}
    	// catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
