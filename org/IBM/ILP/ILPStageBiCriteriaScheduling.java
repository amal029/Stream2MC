/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-12-20
 */
package org.IBM.ILP;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

public class ILPStageBiCriteriaScheduling implements compilerStage{
    public ILPStageBiCriteriaScheduling () {}

    //Value of M_\infty is..
    // private static String Minfty = "infinity"; //I think this is a big
    private static float Minfty = 12345000; //I think this is a big
					     //enough +\infty value

    //This holds all the binaries that would be initialized in the bin
    //part of ILP
    private static ArrayList<String> binaries = new ArrayList<String>();

    private static int pCount[] = null;
    /**
       This method adds the first ilp formulation \forall
       address_of_source_nodes=0. There is only one source node in our
       case, always!!
     */

    private static void addSourceDeclarations(Actor sNode, StringBuilder buf){
	buf.append("a_"+sNode.getID()+" = 0\n\n");
	buf.append("F_"+sNode.getID()+" = 0\n\n");
	setProcessorCount(sNode);
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
       
       s_ij = |b_ix - b_jy|

       Remember: s_ij are also binary variables
     */
    private static ArrayList<Actor> nodes = new ArrayList<Actor>();
    private static float Mycounter=0;
    // private static void addSDeclarations(Actor sNode,StringBuilder buf){
    // 	collectNodes(sNode); //Now we have all the nodes in nodes
    // 			     //Stack..
    // 	while(!nodes.isEmpty()){
    // 	    Actor node = nodes.remove(0);
    // 	    //Skip the communication nodes altogether..
    // 	    if(node.getID().startsWith("edge")) continue;
    // 	    for(Actor a : nodes){
    // 		//Skip the communication nodes altogether..
    // 		if(a.getID().startsWith("edge")) continue;
    // 		//for each processor x \in pCount
    // 		for(int e=0;e<pCount.length;++e){
    // 		    buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+" + temp_s"+a.getID()+"p"+pCount[e]+Mycounter+" = 1\n");
    // 		    buf.append("s_"+node.getID()+a.getID()+" - "+"b_"
    // 		    	       +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
    // 		    buf.append("s_"+node.getID()+a.getID()+" + "+"b_"
    // 		    	       +node.getID()+"p"+pCount[e]+" - "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
    // 		    buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+" = 1 -> s_"+node.getID()+a.getID()+" - "+"b_"
    // 		    	       +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" <= 0\n");
    // 		    buf.append("temp_s"+a.getID()+"p"+pCount[e]+Mycounter+" = 1 -> s_"+node.getID()+a.getID()+" + "+"b_"
    // 		    	       +node.getID()+"p"+pCount[e]+" - "+"b_"+a.getID()+"p"+pCount[e]+" <= 0\n");
    // 		    //Add it to the binaries list..
    // 		    binaries.add("temp_s"+node.getID()+"p"+pCount[e]+Mycounter);
    // 		    binaries.add("temp_s"+a.getID()+"p"+pCount[e]+Mycounter);
    // 		    ++Mycounter;
    // 		}
    // 		binaries.add("s_"+node.getID()+a.getID());
    // 	    }
    // 	}
    // }
    private static ArrayList<Actor> nodesBF = new ArrayList<Actor>();
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
		    buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+" + temp_s"+a.getID()+"p"+pCount[e]+Mycounter+" = 1\n");
		    buf.append("s_"+node.getID()+a.getID()+"_"+pCount[e]+" - "+"b_"
		    	       +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
		    buf.append("s_"+node.getID()+a.getID()+"_"+pCount[e]+" + "+"b_"
		    	       +node.getID()+"p"+pCount[e]+" - "+"b_"+a.getID()+"p"+pCount[e]+" >= 0\n");
		    buf.append("temp_s"+node.getID()+"p"+pCount[e]+Mycounter+" = 1 -> s_"+node.getID()+a.getID()+"_"+pCount[e]
			       +" - "+"b_"
		    	       +node.getID()+"p"+pCount[e]+" + "+"b_"+a.getID()+"p"+pCount[e]+" <= 0\n");
		    buf.append("temp_s"+a.getID()+"p"+pCount[e]+Mycounter+" = 1 -> s_"+node.getID()+a.getID()+"_"+pCount[e]+" + "+"b_"
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
		float counters[] = new float[pCount.length];
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

    /**
       This method declares the constraints in the ILP, which put order on a
       single processor, i.e.,

       r_ij + r_ji + s_ij = 1

       r_ij and r_ji are also binary variables
     */
    private static void addRDeclarations(Actor sNode, StringBuilder buf){
	collectNodes(sNode);
	while(!nodes.isEmpty()){
	    Actor node = nodes.remove(0);
	    //Skip communication actors..
	    if(node.getID().startsWith("edge")) continue;
	    for(Actor act : nodes){
		if(act.getID().startsWith("edge")) continue;
		buf.append("r_"+node.getID()+act.getID()+" + "
			   +"r_"+act.getID()+node.getID()+" + "
			   +"s_"+node.getID()+act.getID()+" = 1\n");
		//add to the binaries list..
		binaries.add("r_"+node.getID()+act.getID());
		binaries.add("r_"+act.getID()+node.getID());
	    }
	}
    }
    private static float getEnergyWeights(Actor node){
	if(node.getID().equals("dummyStartNode") || node.getID().equals("dummyTerminalNode")){
	    float ret = 0;
	    return ret;
	}
	float ret = new Float(((GXLString)node.getAttr("energy_weights").getValue()).getValue()).floatValue();
	return ret;
    }
    private static float getExecWeights(Actor node){
	if(node.getID().equals("dummyStartNode") || node.getID().equals("dummyTerminalNode")){
	    float ret = 0;
	    return ret;
	}
	float ret = new Float(((GXLString)node.getAttr("exec_weights").getValue()).getValue()).floatValue();
	return ret;
    }

    private static float[] getEnergyCost(eActor node){
	if(node.getID().equals("dummyStartNode") || node.getID().equals("dummyTerminalNode")){
	    float ret[] = new float[pCount.length];
	    for(int e=0;e<ret.length;++e)
		ret[e] = 0;
	    return ret;
	}
	String [] costs = ((GXLString)node.getAttr("total_energy_x86").getValue()).getValue().split(";");
	float ret[] = new float[costs.length];
	for(int r=0;r<costs.length;++r){
	    ret[r] = new Float(costs[r]).floatValue();
	}
	return ret;
    }
    private static float[] getExecutionCost(eActor node){
	if(node.getID().equals("dummyStartNode") || node.getID().equals("dummyTerminalNode")){
	    float ret[] = new float[pCount.length];
	    for(int e=0;e<ret.length;++e)
		ret[e] = 0;
	    return ret;
	}
	String [] costs = ((GXLString)node.getAttr("total_time_x86").getValue()).getValue().split(";");
	float ret[] = new float[costs.length];
	for(int r=0;r<costs.length;++r){
	    ret[r] = new Float(costs[r]).floatValue();
	}
	return ret;
    }

    /**
       This method creates the declaration, which order the scheduling of
       actors on a single processor ,i.e.,
       
       temp_ij + r_ij = 1

       a_j - a_i - \sum_{x=1}^N G T_{ix}(b_{ix}) >= -M\infty temp_ij

       this is for all verticies the graph...
     */

    private static void addSchedulingDeclarations(Actor sNode, StringBuilder buf){
	collectNodes(sNode); //I have all the nodes..
	while(!nodes.isEmpty()){
	    Actor node = nodes.remove(0);
	    if(node.getID().startsWith("edge")) continue;
	    for(Actor act : nodes){
		if(act.getID().startsWith("edge")) continue;
		//Get execution times for the node actor
		float nodeCost[] = getExecutionCost((eActor)node);
		//Build a_act >= a_node
		buf.append("temp_"+node.getID()+act.getID()+" + r_"+node.getID()+act.getID()+" = 1\n");
		buf.append("a_"+act.getID()+" - a_"+node.getID()+" - ");
		//Now append the costs... that you have
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
		//Build a_node >= a_act
		buf.append("temp_"+act.getID()+node.getID()+" + r_"+act.getID()+node.getID()+" = 1\n");
		buf.append("a_"+node.getID()+" - a_"+act.getID()+" - ");
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
    private static void addSchedulingDeclarationsWeighted(Actor sNode, StringBuilder buf){
	collectNodes(sNode); //I have all the nodes..
	while(!nodes.isEmpty()){
	    Actor node = nodes.remove(0);
	    if(node.getID().startsWith("edge")) continue;
	    for(Actor act : nodes){
		if(act.getID().startsWith("edge")) continue;
		//Get execution times for the node actor
		float nodeCost[] = getExecutionCost((eActor)node);
		float weight = getExecWeights(node);
		//Build a_act >= a_node
		buf.append("temp_"+node.getID()+act.getID()+" + r_"+node.getID()+act.getID()+" = 1\n");
		buf.append("F_"+act.getID()+" - F_"+node.getID()+" - ");
		//Now append the costs... that you have
		for(int e=0;e<nodeCost.length;++e){
		    if(e < nodeCost.length -1)
			buf.append(nodeCost[e]+" "+weight+" b_"+node.getID()+"p"+pCount[e]+" - ");
		    else
			buf.append(nodeCost[e]+" "+weight+" b_"+node.getID()+"p"+pCount[e]+" + "
				   +Minfty+" temp_"+node.getID()+act.getID()+" >= 0 \n");
		}
		// buf.append(nodeCost[e]+" b_"+node.getID()+"p"+pCount[e]+" - ");
		// buf.append(Minfty+" r_"+node.getID()+act.getID()+" >= - "+Minfty+"\n");

		//Get execution times for the act actor
		nodeCost = getExecutionCost((eActor)act);
		weight = getExecWeights(act);
		//Build a_node >= a_act
		buf.append("temp_"+act.getID()+node.getID()+" + r_"+act.getID()+node.getID()+" = 1\n");
		buf.append("F_"+node.getID()+" - F_"+act.getID()+" - ");
		//Now append the costs... that you have
		for(int e=0;e<nodeCost.length;++e){
		    if(e < nodeCost.length -1)
			buf.append(nodeCost[e]+" "+weight+" b_"+act.getID()+"p"+pCount[e]+" - ");
		    else
			buf.append(nodeCost[e]+" "+weight+" b_"+act.getID()+"p"+pCount[e]+" + "
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
		    float ccosts[] = new float[pCount.length*pCount.length];//zeros
		    if(node.iscActor()){
			ccosts = getCommunicationCost((cActor)node);
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
		    makeBuf((eActor)sNode,nnode,buf,ccosts);
		    addEdgeDeclarations(nnode,buf);
		}
	    }
	}
    }
    /**
       Edgedeclarations with weights same as edgeDeclarations
     */
    private static void addEdgeDeclarationsWeighted(Actor sNode, StringBuilder buf){
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
		    float ccosts[] = new float[pCount.length*pCount.length];//zeros
		    if(node.iscActor()){
			ccosts = getCommunicationCost((cActor)node);
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
		    makeBufWeighted((eActor)sNode,nnode,buf,ccosts);
		    addEdgeDeclarationsWeighted(nnode,buf);
		}
	    }
	}
    }

    /**
       This method does a DFS and makes the total energy consumption
       formulation (if you give in the boolean all energy consumption
       will be multiplied with the weight of that node)
       
       \Phi = \forall x \in P, \sum_{\forall i \in V}(\lambda_ix*b_ix) + 
       \sum_{g=1}^M \sum_{\forall (x,y) \in C (\Lambda_{ixgy}*b_{ixgy})
     */
    private static void addPhiOrMu(Actor sNode, StringBuilder buf,boolean w){
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
		    float ccosts[] = new float[pCount.length*pCount.length];//zeros
		    if(node.iscActor()){
			ccosts = getCommunicationEnergyCost((cActor)node);
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
		    makePhiOrMuBuf((eActor)sNode,nnode,buf,ccosts,w);
		    addPhiOrMu(nnode,buf,w);
		}
	    }
	}
    }
    private static float[] getCommunicationEnergyCost(cActor node){
	String costs[] = ((GXLString)node.getAttr("energy_x86").getValue()).getValue().split(";");
	float [] ret = new float[costs.length];
	for(int e=0;e<costs.length;++e)
	    ret[e] = new Float(costs[e]).floatValue();
	return ret;
    }
    private static float[] getCommunicationCost(cActor node){
	String costs[] = ((GXLString)node.getAttr("work_x86").getValue()).getValue().split(";");
	float [] ret = new float[costs.length];
	for(int e=0;e<costs.length;++e)
	    ret[e] = new Float(costs[e]).floatValue();
	return ret;
    }

    private static ArrayList<float[]> oCCosts = new ArrayList<float[]>(4);
    private static ArrayList<Float> weights = new ArrayList<Float>();
    private static ArrayList<Float> pWeights = new ArrayList<Float>();

    private static ArrayList<Actor> getPhiOthers(eActor sNode, String notThis){
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
					oCCosts.add(getCommunicationEnergyCost((cActor)node));
					weights.add(getExecWeights(node));
					pWeights.add(getEnergyWeights(node));
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
					weights.add(getExecWeights(node));
					pWeights.add(getEnergyWeights(node));
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

    private static ArrayList<Actor> getPhiOthersMerge(Actor sNode, String notThis){
	ArrayList<Actor> others = new ArrayList<Actor>(4);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getSource();
		    if(node.iscActor()){
			for(int e1=0;e1<node.getConnectionCount();++e1){
			    if(node.getConnectionAt(e1).getDirection().equals(GXL.OUT)){
				GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
				if(len.getAttr("parallelEdge")==null){
				    Actor nnode = (eActor)len.getSource();
				    if(!nnode.getID().equals(notThis)){
					others.add(nnode);
					oCCosts.add(getCommunicationEnergyCost((cActor)node));
					weights.add(getExecWeights(node));
					pWeights.add(getEnergyWeights(node));
				    }
				    break; 
				}
			    }
			}
		    }
		    else{
			//It is possible that two merges are connected
			//to each other. In that case let others remain
			//empty.
			;
		    }
		}
	    }
	}
	return others;
    }
    private static ArrayList<Actor> getOthersMerge(Actor sNode, String notThis){
	ArrayList<Actor> others = new ArrayList<Actor>(4);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getSource();
		    if(node.iscActor()){
			for(int e1=0;e1<node.getConnectionCount();++e1){
			    if(node.getConnectionAt(e1).getDirection().equals(GXL.OUT)){
				GXLEdge len = (GXLEdge)node.getConnectionAt(e1).getLocalConnection();
				if(len.getAttr("parallelEdge")==null){
				    Actor nnode = (eActor)len.getSource();
				    if(!nnode.getID().equals(notThis)){
					others.add(nnode);
					oCCosts.add(getCommunicationCost((cActor)node));
					weights.add(getExecWeights(node));
					pWeights.add(getEnergyWeights(node));
				    }
				    break; 
				}
			    }
			}
		    }
		    else{
			//It is possible that two merges are connected
			//to each other. In that case let others remain
			//empty.
			;
		    }
		}
	    }
	}
	return others;
    }

    private static void makePhiOrMuBuf(eActor source, eActor target, StringBuilder buf, float ccosts[],
				   boolean w){
	float costs[] = getEnergyCost(source);
	float weight = 0;
	if(w)
	    weight = getEnergyWeights(source);
	// buf.append("lambda_"+target.getID()+" - L_"+source.getID()+" - ");
	for(int e=0;e<costs.length;++e){
	    if(w)
		buf.append(costs[e]+" "+weight+" b_"+source.getID()+"p"+pCount[e]+" - ");
	    else
		buf.append(costs[e]+" b_"+source.getID()+"p"+pCount[e]+" - ");
	}
	//These are the communication costs
	ArrayList<Actor> others = new ArrayList<Actor>(4);
	if(source.getIsSplitNode() && target.getIsMergeNode())
	    throw new RuntimeException("Currently we cannot handle splits and merges" 
				       +"connected to each other without any nodes between them, sorry :-(");
	//Write the others first...
	int counter=0;
	if(source.getIsSplitNode()){
	    //This needs to change
	    others = getPhiOthers(source,target.getID());
	    while(!others.isEmpty()){
		Actor o = others.remove(0);
		counter=0;
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			if(w)
			    buf.append(ccost+" "+pWeights.remove(0)+" b_"
				       +source.getID()+"p"+pCount[e]+"_"+o.getID()+"p"+pCount[r]+" - ");
			else
			    buf.append(ccost+" b_"
				       +source.getID()+"p"+pCount[e]+"_"+o.getID()+"p"+pCount[r]+" - ");

		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	//Now if the target is a merge node then...
	else if(target.getIsMergeNode()){
	    //This needs to change too
	    others = getPhiOthersMerge(target,source.getID());
	    while(!others.isEmpty()){
		Actor o = others.remove(0);
		counter=0;
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			if(w)
			    buf.append(ccost+" "+pWeights.remove(0)
				       +" b_"+o.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
			else
			    buf.append(ccost+" b_"+o.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");

		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	counter=0;
	for(int e=0;e<costs.length;++e){
	    for(int r=0;r<costs.length;++r){
		float ccost = ccosts[e+r+counter];
		// if(e == costs.length -1 && r == costs.length -1)
		//     buf.append(ccost+" "+weight
		// 	       +" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" >= 0\n");
		// else
		if(w)
		    buf.append(ccost+" "+weight+
			       " b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
		else
		    buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
	    }
	    counter+=(costs.length-1);
	}
    }
    //a_j - a_i - \sum_{x=1}^{N}(b_{ix} T_i)G - \sum_{x,y=1}^{N}(b_{ixjy} D_{xy}) >= 0, x \neq y
    private static void makeBufWeighted(eActor source, eActor target, StringBuilder buf, float ccosts[]){
	float costs[] = getExecutionCost(source);
	float weight = getExecWeights(source);
	buf.append("F_"+target.getID()+" - F_"+source.getID()+" - ");
	for(int e=0;e<costs.length;++e)
	    buf.append(costs[e]+" "+weight+" b_"+source.getID()+"p"+pCount[e]+" - ");
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
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			buf.append(ccost+" "+weights.remove(0)+" b_"
				   +source.getID()+"p"+pCount[e]+"_"+o.getID()+"p"+pCount[r]+" - ");
		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	//Now if the target is a merge node then...
	else if(target.getIsMergeNode()){
	    others = getOthersMerge(target,source.getID());
	    while(!others.isEmpty()){
		Actor o = others.remove(0);
		counter=0;
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			buf.append(ccost+" "+weights.remove(0)
				   +" b_"+o.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	counter=0;
	for(int e=0;e<costs.length;++e){
	    for(int r=0;r<costs.length;++r){
		float ccost = ccosts[e+r+counter];
		if(e == costs.length -1 && r == costs.length -1)
		    buf.append(ccost+" "+weight
			       +" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" >= 0\n");
		else
		    buf.append(ccost+" "+weight+
			       " b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
	    }
	    counter+=(costs.length-1);
	}
    }
    private static void makeBuf(eActor source, eActor target, StringBuilder buf, float ccosts[]){
	float costs[] = getExecutionCost(source);
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
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+o.getID()+"p"+pCount[r]+" - ");
		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	//Now if the target is a merge node then...
	else if(target.getIsMergeNode()){
	    others = getOthersMerge(target,source.getID());
	    while(!others.isEmpty()){
		Actor o = others.remove(0);
		counter=0;
		float occosts[] = oCCosts.remove(0);
		for(int e=0;e<costs.length;++e){
		    for(int r=0;r<costs.length;++r){
			float ccost = occosts[e+r+counter];
			buf.append(ccost+" b_"+o.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
		    }
		    counter+=(costs.length-1);
		}
	    }
	}
	counter=0;
	for(int e=0;e<costs.length;++e){
	    for(int r=0;r<costs.length;++r){
		float ccost = ccosts[e+r+counter];
		if(e == costs.length -1 && r == costs.length -1)
		    buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" >= 0\n");
		else
		    buf.append(ccost+" b_"+source.getID()+"p"+pCount[e]+"_"+target.getID()+"p"+pCount[r]+" - ");
	    }
	    counter+=(costs.length-1);
	}
    }

    /**
       This method adds the final latency declaration...i.e., 
       
       l - a_k - \sum_{x=1}^{N}(b_{kx} T_{kx})G >= 0

       Note, in our case there is only a single terminal node the
       dummyTerminalNode
     */
    private static void addLatencyDeclaration(eActor tNode, StringBuilder buf){
	float costs[] = getExecutionCost(tNode);
	buf.append("l - a_"+tNode.getID()+" - ");
	for(int e=0;e<costs.length;++e){
	    if(e < costs.length - 1)
		buf.append(costs[e]+" b_"+tNode.getID()+"p"+pCount[e]+" - ");
	    else
	    	buf.append(costs[e]+" b_"+tNode.getID()+"p"+pCount[e]+" >= 0\n");
	}
    }

    private static void addLatencyDeclarationWeighted(eActor tNode, StringBuilder buf){
	float costs[] = getExecutionCost(tNode);
	float weight = getExecWeights(tNode);
	buf.append("F - F_"+tNode.getID()+" - ");
	for(int e=0;e<costs.length;++e){
	    if(e < costs.length - 1)
		buf.append(costs[e]+" "+weight+" b_"+tNode.getID()+"p"+pCount[e]+" - ");
	    else
	    	buf.append(costs[e]+" "+weight+" b_"+tNode.getID()+"p"+pCount[e]+" - Mu >= 0\n");
	}
    }
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    StringBuilder buf = new StringBuilder();
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Stage-1
		//add the minimization function
		buf.append("min\n"); //This is the objective function
		buf.append("obj: F\n\n");

		buf.append("st\n"); //These are the constraints

		addSourceDeclarations(sGraph.getSourceNode(),buf);
		clearVisited(sGraph.getSourceNode());
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

		addLatencyDeclaration(sGraph.getTerminalNode(),buf);
		buf.append("\n\n");
		clearVisited(sGraph.getSourceNode());
		
		addEdgeDeclarationsWeighted(sGraph.getSourceNode(),buf);
		buf.append("\n\n");
		clearVisited(sGraph.getSourceNode());
		
		addSchedulingDeclarationsWeighted(sGraph.getSourceNode(),buf);
		buf.append("\n\n");
		clearVisited(sGraph.getSourceNode());
		
		addLatencyDeclarationWeighted(sGraph.getTerminalNode(),buf);
		buf.append("\n\n");
		clearVisited(sGraph.getSourceNode());
		
		buf.append("Phi - ");
		addPhiOrMu(sGraph.getSourceNode(),buf,false);
		buf.append(" 0 = 0\n\n");
		clearVisited(sGraph.getSourceNode());

		
		buf.append("Mu - ");
		addPhiOrMu(sGraph.getSourceNode(),buf,true);
		buf.append(" 0 = 0\n\n");
		clearVisited(sGraph.getSourceNode());
		
		//adding the binary variables in this graph
		buf.append("bin\n");
		addBinaryDeclarations(buf);

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
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__ilp__"+f.getName()+".lp")));
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
