/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-06-07
 */
package org.IBM.ILP;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

/**
   This class creates the ILP formulation with software pipelining

   Important: This ILP formulation assumes that the receive takes zero
   time.
*/

public class ILPStage2 implements compilerStage{
  public ILPStage2 () {}

  //Value of M_\infty is..
  // private static String Minfty = "infinity"; //I think this is a big
  private static long Minfty = 12345000; //I think this is a big
  //enough +\infty value

  //This holds all the binaries that would be initialized in the bin
  //part of ILP
  private static ArrayList<String> binaries = new ArrayList<String>();

  private static int pCount[] = null;
  /**
     This method adds the source declarations, i.e., all nodes that
     have their starting activation time =0
     a_nodeID = 0

     In the sw pipeline case all nodes that are not strongly connected
     have their a_nodeID=0; 
       
     For the strongly connected components in the graph, the start of
     the loopback (delay-token) only has a_nodeID=0;
  */
  // FIXME: Currently I do not account for loopbacks at all and assume
  // that all nodes have a_nodeID=0; --> works for all the benchmarks
  private static void addSourceDeclarations(Actor sNode, StringBuilder buf){
    if(sNode.ifVisited()) return;
    sNode.setVisited();
    if((((GXLString)sNode.getAttr("actTime").getValue()).getValue()).equals("0")){
      if(sNode.iseActor())
	buf.append("a_"+sNode.getID()+" = 0\n");
    }
    if(pCount == null)
      setProcessorCount(sNode);
    // for(int e=0;e<sNode.getConnectionCount();++e){
    //     if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
    // 	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
    // 	if(le.getAttr("parallelEdge")==null){
    // 	    Actor node = (Actor)le.getTarget();
    // 	    addSourceDeclarations(node,buf);
    // 	}
    //     }
    // }
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
  private static void addBinaryDeclarations(StringBuilder buf){
    for(String s : binaries){
      buf.append(s+"\n");
    }
    buf.append("\n\n");
  }

  private static void setProcessorCount(Actor sNode){
    String processors[] = ((GXLString)sNode.getAttr("__update_labels_with_processors").getValue()).getValue().split(";");

    pCount = new int[processors.length];
    for(int e=0;e<processors.length;++e)
      pCount[e] = (e+1);
  }

  /**
     This method adds the second declarations, i.e.,

     \sum b_nodeID_processorID1()+b_nodeID()_processorID2()+...=1
  */
  private static void addBinaryProcessorDeclarations(Actor sNode, StringBuilder buf){
    if(sNode.ifVisited()) return;
    //Now add the declarations
    if(!sNode.getID().startsWith("edge")){
      for(int e=0;e<pCount.length;++e){
	if(e < pCount.length - 1)
	  buf.append("b_"+sNode.getID()+"p"+pCount[e]+" + ");
	else
	  buf.append("b_"+sNode.getID()+"p"+pCount[e]);

	//add the binaries to the arraylist
	binaries.add("b_"+sNode.getID()+"p"+pCount[e]);
      }
      buf.append(" =  1\n");
    }
    sNode.setVisited();
    //Start updating the guards and update labels as: label$processor_num;label$processor_num
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  addBinaryProcessorDeclarations(node,buf);
	}
      }
    }
  }
  /**
     This method adds the s declarations, i.e.,
       
     s_ij = |b_ix - b_jx|

     Remember: s_ij are also binary variables
       
     FIXME: Urgent
     This does not work for more than 2 cores, there is some conflict,
     which I am currently unable to understand.
       
     TODO: I now understand the problem. We need to change this constraint to:
     s_ijx = |b_ix - b_jx|
       
     Another constraint will be:
     s_ij = max(s_ijx), \forall x \in P
       
     //Done
     */
  private static ArrayList<Actor> nodes = new ArrayList<Actor>();
  private static long Mycounter=0;
  private static void addSDeclarations(Actor sNode,StringBuilder buf){
    collectNodesBF(sNode); //Now we have all the nodes in nodes
    //Stack..
    //DEBUG
    // for(Actor a : nodesBF)
    //     System.out.println(a.getID());
    while(!nodesBF.isEmpty()){
      Actor node = nodesBF.remove(0);
      //Skip the communication nodes altogether..
      if(node.getID().startsWith("edge")) continue;
      for(Actor a : nodesBF){
	//Skip the communication nodes altogether..
	if(a.getID().startsWith("edge")) continue;
	//for each processor x \in pCount
	for(int e=0;e<pCount.length;++e){
	  buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+
		     " + temp_s"+a.getID()+"p"+pCount[e]+Mycounter+" = 1\n");
	  buf.append("s_"+node.getID()+a.getID()+"_"+pCount[e]+" - "+"b_"
		     +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
	  buf.append("s_"+node.getID()+a.getID()+"_"+pCount[e]+" + "+"b_"
		     +node.getID()+"p"+pCount[e]+" - "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
	  buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+
		     " = 1 -> s_"+node.getID()+a.getID()+"_"+pCount[e]
		     +" - "+"b_"
		     +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" <= 0\n");
	  buf.append("temp_s"+a.getID()+"p"+pCount[e]+Mycounter+
		     " = 1 -> s_"+node.getID()+a.getID()+"_"+pCount[e]+" + "+"b_"
		     +node.getID()+"p"+pCount[e]+" - "+"b_"+a.getID()+"p"+pCount[e]+" <= 0\n");
	  //Add it to the binaries list..
	  binaries.add("temp_s"+node.getID()+"p"+pCount[e]+Mycounter);
	  binaries.add("temp_s"+a.getID()+"p"+pCount[e]+Mycounter);
	  binaries.add("s_"+node.getID()+a.getID()+"_"+pCount[e]);
	  ++Mycounter;
	}
	//Add the max funtion. This thing also needs an or and
	//<=, else instead of 0, s_ij can take a value of 1,
	//which gives wrong results.
	long counters[] = new long[pCount.length];
	for(int e=0;e<pCount.length;++e)
	  buf.append("s_"+node.getID()+a.getID()+" - "+"s_"+node.getID()+a.getID()+"_"+pCount[e]+" >= 0\n");
	for(int e=0;e<pCount.length;++e){
	  counters[e] = Mycounter;
	  if(e == pCount.length - 1)
	    buf.append("temp_s_max"+Mycounter+"_"+e+" = 1\n");
	  else
	    buf.append("temp_s_max"+Mycounter+"_"+e+" + ");
	  //Made it into a binary variable
	  binaries.add("temp_s_max"+Mycounter+"_"+e);
	  ++Mycounter;
	}
	//Now add the indicator variables
	for(int e=0;e<counters.length;++e){
	  buf.append("temp_s_max"+counters[e]+"_"+e+" = 1 -> s_"+node.getID()+a.getID()+
		     " - s_"+node.getID()+a.getID()+"_"+pCount[e]+" <= 0\n");
	}
	binaries.add("s_"+node.getID()+a.getID());
      }
    }
  }

  //FIXME: Find out the strongly connected components.
  private static void setStronglyConnected(Actor sNode){
    if(sNode.ifVisited()) return;
    sNode.setVisited();
    sNode.setAttr("actTime",new GXLString("0"));
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  setStronglyConnected(node);
	}
      }
    }
  }

  /**
     This method adds the binary variables, which decide what communication
     path to take, i.e.,

     b_{ixjy} - b_{ix} - b_{jy} >= -1 for all verticies
  */
  private static void addCommDeclarations(Actor sNode, StringBuilder buf){
    if(sNode.ifVisited()) return;
    sNode.setVisited();
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  eActor nnode = null;
	  if(node.iscActor()){
	    node.setVisited();
	    for(int e1=0;e1<node.getConnectionCount();++e1){
	      if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
		GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
		if(len.getAttr("parallelEdge")==null){
		  nnode = (eActor)len.getTarget();
		}
	      }
	    }
	  }
	  else nnode = (eActor)node;
	  makeBuff((eActor)sNode,nnode,buf);
	  addCommDeclarations(nnode,buf);
	}
      }
    }
  }
  private static void makeBuff(eActor source, eActor target, StringBuilder buf){
    for(int e=0;e<pCount.length;++e){
      for(int r=0;r<pCount.length;++r){
	buf.append("b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+
		   " - b_"+source.getID()+"p"+pCount[e]+" - b_"+target.getID()+"p"+pCount[r]+" >= -1\n");
	binaries.add("b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]);
      }
    }
  }

  private static boolean print = false;
  private static boolean isRechable(Actor sNode, Actor tNode){
    boolean ret = false;
    if(sNode.getID().equals(tNode.getID())){
      ret=true; 
      return ret;
    }
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  ret = isRechable(node,tNode);
	  if(ret) break;
	}
      }
    }
    return ret;
  }
  private static ArrayList<Actor> nodesBF = new ArrayList<Actor>();
  //This is an iterative method...
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
  /**
     This method declares the constraints in the ILP, which put order on a
     single processor, i.e.,

     r_ij + r_ji + s_ij = 1

     r_ij and r_ji are also binary variables

     This one is special, if you know that i-->j is the connection
     then it should be r_ij + s_ij = 1.
       
     The only time r_ij + r_ji is possible iff j is not rechable from
     i, i.e., "j" is not in "i"'s rechability set.

     //Do a depth first traversal to make sure that j is rechable from
     //i
       
     //Always collecte nodes in this case in a Breadth first order
     */
  private static void addRDeclarations(Actor sNode, StringBuilder buf){
    collectNodesBF(sNode);
    //DEBUG
    // for(Actor a : nodesBF)
    //     System.out.println(a.getID());
    while(!nodesBF.isEmpty()){
      Actor node = nodesBF.remove(0);
      //Skip communication actors..
      if(node.getID().startsWith("edge")) continue;
      for(Actor act : nodesBF){
	if(act.getID().startsWith("edge")) continue;
	if(!isRechable(node,act)){
	  buf.append("r_"+node.getID()+act.getID()+" + "
		     +"r_"+act.getID()+node.getID()+" + "
		     +"s_"+node.getID()+act.getID()+" = 1\n");
	}
	else{
	  buf.append("r_"+node.getID()+act.getID()+" + "
		     +"s_"+node.getID()+act.getID()+" = 1\n");
	  buf.append("r_"+act.getID()+node.getID()+" = 0\n");
	}
	//add to the binaries list..
	binaries.add("r_"+node.getID()+act.getID());
	binaries.add("r_"+act.getID()+node.getID());
      }
    }
  }
  private static ArrayList<long[]> oCCosts = new ArrayList<long[]>(4);
  private static ArrayList<Actor> getOthers(eActor sNode, String notThis){
    ArrayList<Actor> others = new ArrayList<Actor>();
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  if(node.iscActor()){
	    for(int e1=0;e1<node.getConnectionCount();++e1){
	      if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
		GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
		if(len.getAttr("parallelEdge")==null){
		  Actor nnode = (eActor)len.getTarget();
		  if(!nnode.getID().equals(notThis)){
		    others.add(nnode);
		    oCCosts.add(getCommunicationCost((cActor)node));
		  }
		  break; 
		}
	      }
	    }
	  }
	  else{
	    //It is possible that two splits are connected
	    //to each other. In that case let others remain
	    //empty.
	    ;
	  }
	}
      }
    }
    return others;
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
     This method creates the declaration, which order the scheduling of
     actors on a single processor ,i.e.,
       
     temp_ij + r_ij = 1

     a_j - a_i - \sum_{x=1}^N G T_{ix}(b_{ix}) + \sum_{k=1}^M
     \sum_{x=1}^N b_kix D_kxy >= -M\infty temp_ij

     this is for all verticies the graph...
  */

  @SuppressWarnings("unchecked")
  private static void addSchedulingDeclarations(Actor sNode, StringBuilder buf){
    collectNodes(sNode); //I have all the nodes..
    while(!nodes.isEmpty()){
      Actor node = nodes.remove(0);
      if(node.getID().startsWith("edge")) continue;
      for(Actor act : nodes){
	if(act.getID().startsWith("edge")) continue;
	//Get execution times for the node actor
	long nodeCost[] = getExecutionCost((eActor)node);

	//Get communication costs for the node actor
	ArrayList<ArrayList> list = getCommCosts((eActor)node);
	ArrayList<String> elist = list.get(0);
	ArrayList<long[]> clist = list.get(1);
	if(elist.size() != clist.size())
	  throw new RuntimeException();

	//Build a_act >= a_node
	buf.append("temp_"+node.getID()+act.getID()+" + r_"+node.getID()+act.getID()+" = 1\n");
	buf.append("a_"+act.getID()+" - a_"+node.getID()+" - ");

	//Append the communication costs that you have
	while(!elist.isEmpty()){
	  int mcounter=0;
	  String actt = elist.remove(0);
	  long[] ccosts = clist.remove(0);
	  for(int e=0;e<pCount.length;++e){
	    for(int r=0;r<pCount.length;++r)
	      buf.append(ccosts[e+r+mcounter]+" b_"+node.getID()+"p"+pCount[e]+"_"+actt+"p"+pCount[r]+" - ");
	    mcounter+=(pCount.length-1);
	  }
	}

	//Now append the execution costs... that you have
	for(int e=0;e<nodeCost.length;++e){
	  if(e < nodeCost.length -1)
	    buf.append(nodeCost[e]+" b_"+node.getID()+"p"+pCount[e]+" - ");
	  else
	    buf.append(nodeCost[e]+" b_"+node.getID()+"p"+pCount[e]+" + "
		       +Minfty+" temp_"+node.getID()+act.getID()+" >= 0 \n");
	}
	// buf.append(nodeCost[e]+" b_"+node.getID()+"p"+pCount[e]+" - ");
	// buf.append(Minfty+" r_"+node.getID()+act.getID()+" >= - "+Minfty+"\n");

	//Get execution times for the act actor
	nodeCost = getExecutionCost((eActor)act);


	//Get communication costs for the node actor
	list = getCommCosts((eActor)node);
	elist = list.get(0);
	clist = list.get(1);
	if(elist.size() != clist.size())
	  throw new RuntimeException();

	//Build a_node >= a_act
	buf.append("temp_"+act.getID()+node.getID()+" + r_"+act.getID()+node.getID()+" = 1\n");
	buf.append("a_"+node.getID()+" - a_"+act.getID()+" - ");


	//Append the communication costs that you have
	while(!elist.isEmpty()){
	  int mcounter=0;
	  String actt = elist.remove(0);
	  long[] ccosts = clist.remove(0);
	  for(int e=0;e<pCount.length;++e){
	    for(int r=0;r<pCount.length;++r)
	      buf.append(ccosts[e+r+mcounter]+" b_"+node.getID()+"p"+pCount[e]+"_"+actt+"p"+pCount[r]+" - ");
	    mcounter+=pCount.length-1;
	  }
	}

	//Now append the costs... that you have
	for(int e=0;e<nodeCost.length;++e){
	  if(e < nodeCost.length -1)
	    buf.append(nodeCost[e]+" b_"+act.getID()+"p"+pCount[e]+" - ");
	  else
	    buf.append(nodeCost[e]+" b_"+act.getID()+"p"+pCount[e]+" + "
		       +Minfty+" temp_"+act.getID()+node.getID()+" >= 0 \n");
	}
	//add to binaries
	binaries.add("temp_"+act.getID()+node.getID());
	binaries.add("temp_"+node.getID()+act.getID());
	// for(int e=0;e<nodeCost.length;++e)
	//     buf.append(nodeCost[e]+" b_"+act.getID()+"p"+pCount[e]+" - ");
	// buf.append(Minfty+" r_"+act.getID()+node.getID()+" >= - "+Minfty+"\n");
      }
    }
  }
  /**
     This is the main constraint that increments the starting time for
     all the nodes it depends upon the edge, i.e.,
       
     a_j - a_i - \sum_{x=1}^{N}(b_{ix} T_i)G -
     \sum_{x,y=1}^{N}(b_{ixjy} D_{xy}) >= 0

     Here you need to move edge by edge and not vertex by vertex.
     This is a tailrecursive method

     Additional: If i is a split node, then make extra communication
     costs \sum

     If j is a merge node then make additional communication costs
     \sum

     XXX 

     This case is special in the sw pipelined version. We do this iff
     the actors do not have the attribute actTime set to zero. This in
     turn shows that the edges are present and not broken like in sw
     pipelining.
  */
  private static void addEdgeDeclarations(Actor sNode, StringBuilder buf){
    if(sNode.ifVisited()) return;
    sNode.setVisited();
    for(int e=0;e<sNode.getConnectionCount();++e){
      if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
	GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
	if(le.getAttr("parallelEdge")==null){
	  Actor node = (Actor)le.getTarget();
	  //node is possibly a communication node, if yes then
	  //we need to get the next computation node and then
	  //send that in.  
	  eActor nnode = null;
	  long ccosts[] = new long[pCount.length*pCount.length];//zeros
	  if(node.iscActor()){
	    node.setVisited();
	    for(int e1=0;e1<node.getConnectionCount();++e1){
	      if(node.getConnectionAt(e1).getDirection().equals(GXL.IN)){
		GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
		if(len.getAttr("parallelEdge")==null){
		  nnode = (eActor)len.getTarget();
		  break; //assured that a
		  //communication actor cannot
		  //have more than one child
		}
	      }
	    }
	  }
	  //Can happen when there are two merge one after the
	  //other
	  else
	    nnode = (eActor)node;
	  //Do computation here....
	  if(!((GXLString)node.getAttr("actTime").getValue()).getValue().equals("0")){
	    ccosts = getCommunicationCost((cActor)node);
	    makeBuf((eActor)sNode,nnode,buf,ccosts);
	  }
	  addEdgeDeclarations(nnode,buf);
	}
      }
    }
  }
  private static void makeBuf(eActor source, eActor target, StringBuilder buf, long ccosts[]){
    long costs[] = getExecutionCost(source);
    buf.append("a_"+target.getID()+" - a_"+source.getID()+" - ");
    for(int e=0;e<costs.length;++e)
      buf.append(costs[e]+" b_"+source.getID()+"p"+pCount[e]+" - ");
    //These are the communication costs
    ArrayList<Actor> others = new ArrayList<Actor>(4);
    if(source.getIsSplitNode() && target.getIsMergeNode())
      throw new RuntimeException("Currently we cannot handle splits and merges" 
				 +"connected to each other without any nodes between them, sorry :-(");
    //Write the others first...
    int counter=0;
    if(source.getIsSplitNode()){
      others = getOthers(source,target.getID());
      while(!others.isEmpty()){
	Actor o = others.remove(0);
	counter=0;
	long occosts[] = oCCosts.remove(0);
	for(int e=0;e<costs.length;++e){
	  for(int r=0;r<costs.length;++r){
	    long ccost = occosts[e+r+counter];
	    buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+o.getID()+"p"+pCount[r]+" - ");
	  }
	  counter+=(costs.length-1);
	}
      }
    }

    /**XXX not needed, receive is 0 cost*/

    //Now if the target is a merge node then...
    // else if(target.getIsMergeNode()){
    //     others = getOthersMerge(target,source.getID());
    //     while(!others.isEmpty()){
    // 	Actor o = others.remove(0);
    // 	counter=0;
    // 	long occosts[] = oCCosts.remove(0);
    // 	for(int e=0;e<costs.length;++e){
    // 	    for(int r=0;r<costs.length;++r){
    // 		long ccost = occosts[e+r+counter];
    // 		buf.append(ccost+" b_"+o.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
    // 	    }
    // 	    counter+=(costs.length-1);
    // 	}
    //     }
    // }
    counter=0;
    for(int e=0;e<costs.length;++e){
      for(int r=0;r<costs.length;++r){
	long ccost = ccosts[e+r+counter];
	if(e == costs.length -1 && r == costs.length -1)
	  buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" >= 0\n");
	else
	  buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
      }
      counter+=(costs.length-1);
    }
  }

  /**XXX Not needed in this case, receive is 0 cost*/

  // private static ArrayList<Actor> getOthersMerge(Actor sNode, String notThis){
  // 	ArrayList<Actor> others = new ArrayList<Actor>(4);
  // 	for(int e=0;e<sNode.getConnectionCount();++e){
  // 	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
  // 		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
  // 		if(le.getAttr("parallelEdge")==null){
  // 		    Actor node = (Actor)le.getSource();
  // 		    if(node.iscActor()){
  // 			for(int e1=0;e1<node.getConnectionCount();++e1){
  // 			    if(node.getConnectionAt(e1).getDirection().equals(GXL.OUT)){
  // 				GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
  // 				if(len.getAttr("parallelEdge")==null){
  // 				    Actor nnode = (eActor)len.getSource();
  // 				    if(!nnode.getID().equals(notThis)){
  // 					others.add(nnode);
  // 					oCCosts.add(getCommunicationCost((cActor)node));
  // 				    }
  // 				    break; 
  // 				}
  // 			    }
  // 			}
  // 		    }
  // 		    else{
  // 			//It is possible that two merges are connected
  // 			//to each other. In that case let others remain
  // 			//empty.
  // 			;
  // 		    }
  // 		}
  // 	    }
  // 	}
  // 	return others;
  // }
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
  private static void collectNodes(Actor sNode){
    if(sNode.ifVisited()) return;
    sNode.setVisited();
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
     This method adds the final latency declaration...i.e., 
       
     l - a_k - \sum_{x=1}^{N}(b_{kx} T_{kx})G >= 0

     XXX
       
     In sw pipelining all nodes need to be considered when performing this
     latency calculation.
       
     Communication costs for nodes also needs to be included

     FIXME: The current algorithm to consider all nodes is a bad
     hack. It will work, but normally you should only need to consider
     two types of nodes:
       
     1.) Nodes that have their actTime attr's set to 0
       
     2.) The terminal node of the strongly connected components
  */
  @SuppressWarnings("unchecked")
  private static void addLatencyDeclaration(eActor tNode, StringBuilder buf){
    nodes.clear();
    collectNodes(tNode);
    for(Actor node: nodes){
      if(node.iscActor()) continue;
      long costs[] = getExecutionCost((eActor)node);
      //Get communication costs for the node actor
      ArrayList<ArrayList> list = getCommCosts((eActor)node);
      ArrayList<String> elist = list.get(0);
      ArrayList<long[]> clist = list.get(1);
      if(elist.size() != clist.size())
	throw new RuntimeException();
      buf.append("l - a_"+node.getID()+" - ");
      //Append the communication costs that you have
      while(!elist.isEmpty()){
	int mcounter=0;
	String actt = elist.remove(0);
	long[] ccosts = clist.remove(0);
	for(int e=0;e<pCount.length;++e){
	  for(int r=0;r<pCount.length;++r)
	    buf.append(ccosts[e+r+mcounter]+" b_"+node.getID()+"p"+pCount[e]+"_"+actt+"p"+pCount[r]+" - ");
	  mcounter+=pCount.length-1;
	}
      }
      for(int e=0;e<costs.length;++e){
	if(e < costs.length - 1)
	  buf.append(costs[e]+" b_"+node.getID()+"p"+pCount[e]+" - ");
	else
	  buf.append(costs[e]+" b_"+node.getID()+"p"+pCount[e]+" >= 0\n");
      }
    }
  }

  public String[] applyMethod(String args[], String fNames[]){
    String rets[] = new String[args.length];
    try{
      HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
      StringBuilder buf = new StringBuilder();
      for(int e=0;e<args.length;++e){
	streamGraph sGraph = graphs.get(args[e]);
	//Stage-1
	//add the minimization function
	buf.append("min\n"); //This is the objective function
	buf.append("obj: l\n\n");

	buf.append("st\n"); //These are the constraints
	clearVisited(sGraph.getSourceNode());

	setStronglyConnected(sGraph.getSourceNode());
	clearVisited(sGraph.getSourceNode());

	addSourceDeclarations(sGraph.getSourceNode(),buf);
	clearVisited(sGraph.getSourceNode());
	buf.append("\n\n");

	addBinaryProcessorDeclarations(sGraph.getSourceNode(),buf);
	clearVisited(sGraph.getSourceNode());
	buf.append("\n\n");

	addSDeclarations(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	addCommDeclarations(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	addRDeclarations(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	addSchedulingDeclarations(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	addEdgeDeclarations(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	addLatencyDeclaration(sGraph.getSourceNode(),buf);
	buf.append("\n\n");
	clearVisited(sGraph.getSourceNode());

	// //adding the binary variables in this graph
	buf.append("bin\n");
	addBinaryDeclarations(buf);
		
	//generals
	// buf.append("general\n");
	// buf.append("l\n");

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
	BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__ilpsw__"+f.getName()+".lp")));
	out.write(buf.toString());
	out.close();
		
	//This is writing the GXL file after processing with the
	//ILP
	sGraph.getDocument().write(new File("./output","__ilp__"+f.getName()));
	rets[e] = "./output/__ilp1__"+f.getName();
      }
    }
    catch(SAXException se){se.printStackTrace();}
    catch(IOException e){e.printStackTrace();}
    catch(Exception e){e.printStackTrace();}
    return rets;
  }
}
