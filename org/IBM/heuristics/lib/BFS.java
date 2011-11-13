/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-13
 */
package org.IBM.heuristics.lib;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;
import org.IBM.heuristics.lib.*;

public class BFS {
    private static Queue<state> workList = new LinkedList<state>();
    private static Queue<Queue<state>> doneList = new LinkedList<Queue<state>>();
    private static Queue<Queue<state>> tempList = new LinkedList<Queue<state>>();
    
    //This holds all the current guard objects.
    private static HashMap<String,Guard> map = new HashMap<String,Guard>();
    private static GXLIDGenerator gen = null;
    
    private static state root = null;
    private static stateGraph SG = null;
    
    public BFS (File f, List<state> startingStates) throws Exception{
	//Make the root node in here
	root = new state("rootNode");
	String fName = f.getName().replaceFirst("\\.xml","");
	SG = new stateGraph("__stateGraph__"+fName,root);
	SG.add(root);
	//The GXL document
	GXLDocument doc = new GXLDocument();
	doc.getDocumentElement().add(SG);
	gen = new GXLIDGenerator(doc);
	    
	//Put the starting states onto the workList
	for(state s : startingStates)
	    workList.offer(s);
	    
	//Do the BFS
	bfs();
	    
	    
	//write the state graph onto disk for debugging
	SG.getDocument().write(new File("./output","__state_graph"+fName));
    }
    
    private static void bfs() throws Exception{
	
	while(true){
	    while(!workList.isEmpty()){
		state s = workList.poll();

		//If worklist is empty than break it
		// XXX: check if this is correct!!
		if(s.getDone()){
		    s.setGuardValue(true);
		    s.setDone(false);
		    state t =null;
		    while((t = workList.poll()) != null){
			t.setGuardValue(true);
			t.setDone(false);
		    }
		    workList.clear(); break;
		}

		//remove the guard and set its value to zero
		//also set the name of the guard
		//and put it in the map
		for(String guard : s.getGuards()){
		    Guard g = new Guard(guard);
		    g.setGuardValue(false);
		    map.put(guard,g);
		}

		//build all possible combinations
		Queue<state> list = new LinkedList<state>();
		for(state sw : workList){
		    list.offer(sw);
		}
		buildCombinations(s,list);
		//empty the map
		map.clear();
	    }
	    if(doneList.isEmpty()) break;
	    else{
		//push the update nodes onto the work list
		//using the macro nodes
		
		Queue<state> q = doneList.poll();
		state leaf = ((LinkedList<state>)q).get(q.size()-1);
		while(!q.isEmpty()){
		    state curr = q.poll();
		    for(state update : curr.getUpdateStates()){
			//See if it's a join node
			if(update.getParents().size() > 1){
			    //yes it is a join node
			    //Check if this join node can be processed??
			    throw new RuntimeException("No support yet for join nodes, yet!!");
			}
			else{
			    
			    boolean add = false;
			    //Check if this state has a partner
			    ArrayList<state> partner = update.getPartners();
			    if(partner!= null){
				//check if the partner is also in the
				//updates of any of the curr states
				int counter=0;
				for(state p : partner){
				    for(state cs : q){
					for(state up : cs.getUpdateStates()){
					    if(up == p) {
						++counter; 
						//Add any more partners
						//if present, make sure
						//you don't add update
						//again to the list.
						List<state> addPs = p.getPartners();
						for(int e=0;e<addPs.size();++e){
						    if(addPs.get(e) == update){
							addPs.remove(e); break;
						    }
						}
						for(state q1 : partner){
						    for(int e=0;e<addPs.size();++e){
							if(addPs.get(e) == q1){
							    addPs.remove(e);
							    --e;
							}
						    }
						}
						partner.addAll(addPs);
						break;
					    }
					}
				    }
				}
				if(counter == partner.size()) add = true;
			    }
			    else add = true; //this is not a rendezvous state
			    if(add){
				//reset the parent to point to the leaf
				//instead of the real-parent
				update.setParent(0,leaf);
				workList.add(update);
			    }
			}
		    }
		}
	    }
	}
    }

    private static void buildCombinations(state s,Queue<state> workList)throws Exception{
	//Put it in the temp list
	Queue<state> list = new LinkedList<state>();
	
	if(s.getPartners() != null){
	    //If this is a rendezvous state
	    //get all partner rendezvous states
	    
	    List<state> list1 = s.getPartners();
	    Queue<state> rendezvousList = new LinkedList<state>();
	    rendezvousList.offer(s);
	    for(state partner : list1){
		boolean add = true;
		for(state rq : rendezvousList){
		    if(partner == rq){
			add = false; break;
		    }
		}
		if(add){
		    // rendezvousList.offer(((LinkedList<state>)BFS.workList).remove(partner));
		    // list1.addAll(((LinkedList<state>)rendezvousList).get(rendezvousList.length()-1).getPartners());
		}
	    }
	}
	else{
	    while(true){
		Queue<state> macroNode = new LinkedList<state>();
		tempList.offer(macroNode); //added to the tempList
		macroNode.offer(s);
		while(!workList.isEmpty()){
		    state ps = workList.poll(); //removed
		    //Taking care of rendzevous
		    if(ps.getPartners()!=null) continue;
		    //get the guards for ps
		    for(String g : ps.getGuards()){
		
			//Check if the guard is in the map if it is then what is
			//it's value.  if the value is false then that means I
			//cannot run this thing with the current node
			if(!map.containsKey(g)){
			    //This is the best possible case, but requires a lot
			    //of work
		    
			    /**
			       TODO:
			       3.) How to take care of rendezvous constructs??
			       4.) How to take care of multiple accesses to join nodes
			    */
			    for(String sg : ps.getGuards()){
				Guard gn = new Guard(sg);
				gn.setGuardValue(false);
				map.put(sg,gn);
			    }
			
			    //add to the macroNode
			    macroNode.offer(ps);
			    break;
			}
			else if(map.containsKey(g) && map.get(g).getGuardValue() == false){
			    //This means it has the same guard as me, so lets
			    //just throw it out.
		    
			    //Only put it in the list if the s.getGuards and
			    //ps.getGuards are not exactly the same.
			    boolean add = true;
			    for(String h : ps.getGuards()){
				for(String k : s.getGuards()){
				    if(h.equals(k)){
					add = false; break;
				    }
				}
				if(!add) break;
			    }
			    if(add)
				list.offer(ps);
			    break;
			}
			else if(map.containsKey(g) && map.get(g).getGuardValue() == true)
			    throw new RuntimeException("The guard "+g+" for state" +s.getID()
						       +" is in map, but its value is true");
		    }
		}
		while(!list.isEmpty()){
		    state ps = list.poll();
		    workList.offer(ps);
		    //You also have to clear the map
		    for(String g : ps.getGuards()){
			map.remove(g);
		    }
		}
		if(workList.isEmpty()) break;
	    }
	}
	//Set the done value
	s.setDone(true);
	
	//Attach the macro node to the root Node
	attachToRoot(s);
	
	BFS.workList.offer(s); //put it at the back of the main list
    }
    
    private static void attachToRoot(state s) throws Exception{
	if(s.getID().startsWith("dummyStartNode")){
	    //attach it to the root node
	    s.addParent(root);
	}
	
	//First join all the macro action nodes together
	while(!tempList.isEmpty()){
	    Queue<state> q = tempList.poll();
	    int counter = 0;
	    //Add to the done list
	    doneList.offer(q);
	    ArrayList<state> parents = new ArrayList<state>();
	    for(state sm : q){
		if(counter == 0){
		    if(!sm.getID().equals(s.getID())) 
			throw new RuntimeException("Something went wrong in building the macro action nodes "
						   +s.getID()+"!="+sm.getID());
		    
		    parents = getParent(root,sm);
		    clearVisited(root);
		    //DEBUG
		    // System.out.println(sm.getParents().size()+","+sm.getParents().get(0).getID());

		    if(parents.size() != sm.getParents().size())
			throw new RuntimeException("I cannot find all my parents and yet I have been scheduled "+
						   sm.getID());
		}
		//Build a new state with the same name as sm
		
		state snew = null;
		for(state parent : parents){
		    snew = new state(sm.getID());
		    SG.add(snew);
		    stateEdge edge = new stateEdge(parent,snew);
		    SG.add(edge);
		    
		    
		    //set the costs
		    //First copy the cost from sm to snew
		    for(float f : sm.getCost()){
			snew.setCost(f);
		    }
		    snew.updateCurrentCost(parent.getCurrentCost());
		    //set the cost as an attribute
		    snew.setAttr("cost",new GXLString(""+snew.getCurrentCost()));
		    
		    //set the update guards high
		}
		
		parents.clear();
		parents.add(snew);
		
		++counter;
	    }
	}
    }

    private static ArrayList<state> getParent(state sNode,state s){
	ArrayList<state> ret= null;
	if(sNode.ifVisited()) return ret; 
	sNode.setVisited();
	for(state parent : s.getParents()){
	    if(sNode.getID().equals(parent.getID())){
		ret = new ArrayList<state>();
		ret.add(sNode);
		break;
	    }
	}
	if(ret!=null) return ret;
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    state node = (state)le.getTarget();
		    ret = getParent(node,s);
		}
	    }
	}
	return ret;
    }

    private static void clearVisited(state sNode){
	sNode.setVisited(false);
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    state node = (state)le.getTarget();
		    clearVisited(node);
		}
	    }
	}
    }
    
	
    
}
