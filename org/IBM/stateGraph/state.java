/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-08
 */

package org.IBM.stateGraph;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class state extends GXLNode{
    
    private boolean isJoinNode = false;
    private int pActors = 0;
    private boolean visited = false;
    private ArrayList<String> joinNodes = new ArrayList<String>();
    private String id = "";
    private ArrayList<Float> myCosts = new ArrayList<Float>();
    private float totalCost = 0f;
    private ArrayList<String> channel = new ArrayList<String>(2);
    private ArrayList<state> partner = null;
    private String partnerName = null;
    private int numTransitions = 1;
    private ArrayList<String> type = new ArrayList<String>(); //sender or receiver or sender/receiver
    private ArrayList<String> guards = new ArrayList<String>();
    private ArrayList<Boolean> guardValue = new ArrayList<Boolean>();
    private ArrayList<String> updates = new ArrayList<String>();
    private ArrayList<state> updateElements = new ArrayList<state>();
    private boolean done = false;
    private ArrayList<state> parents = new ArrayList<state>();
    private int allocCounter = 0;
    
    public void setNumJoinParents(int p){
	pActors = p;
    }
    
    public int getNumJoinParents(){
	return pActors;
    }
    
    public void setIsJoinNode(){
	isJoinNode = true;
    }
    
    public boolean getIsJoinNode(){
	return isJoinNode;
    }
    
    public void incAllocCounter(){
	++allocCounter;
    }
    
    public int getAllocCounter(){
	return allocCounter;
    }
    
    // public boolean isJoinNode(){
    // 	return isJoinNode;
    // }
    
    public void setIsJoinNode(boolean doneOnce){
	this.isJoinNode = doneOnce;
    }
    
    public void addJoinNode(String j){
	joinNodes.add(j);
    }
    
    public ArrayList<String> getJoinNodes(){
	return joinNodes;
    }
    
    public ArrayList<String> getChannel(){
	return channel;
    }
    
    public ArrayList<state> getPartners(){
	return partner;
    }
    
    public void setPartnerState(state partner){
	if(this.partner == null)
	    this.partner = new ArrayList<state>();
	this.partner.add(partner);
    }
    
    public boolean ifVisited(){
	return visited;
    }
    
    public void setVisited(){
	visited = true;
    }
    public void setVisited(boolean val){
	visited = val;
    }
    
    public void clearParents(){
	parents.clear();
    }
    
    public void setParent(int i, state parent){
	parents.remove(i);
	parents.add(i,parent);
    }
    
    public void addParent(state parent){
	parents.add(parent);
    }
    
    public ArrayList<state> getParents(){
	return parents;
    }
    
    public boolean getDone(){
	return done;
    }
    
    public void setDone(boolean done){
	this.done = done;
    }
    
    public void setNumTransitions(int val){
	this.numTransitions = val;
    }
    
    public void setPartner(String partnerName){
	this.partnerName = partnerName;
    }
    public void setChannelName(String channel){
	this.channel.add(channel);
    }
    public ArrayList<String> getTypes(){
	return this.type;
    }

    public void setType(String type){
	this.type.add(type);
	//DEBUG
	// System.out.println("Setting "+getID()+" to "+this.type);
    }
    public state(String id){
	super(id);
    }
    public void setGuardName(String id){
	guards.add(id);
    }
    public ArrayList<String> getGuards(){
	return guards;
    }
    public void setGuardValue(boolean val){
	guardValue.add(val);
    }
    
    public float getCurrentCost(){
	return totalCost;
    }
    public ArrayList<Float> getCost(){
	return myCosts;
    }
    
    private static float max = -1;  //every one will point to this
				    //single variable.
    
    public void updateSenderReceiverCost(float parentCost) throws Exception{
	if(getParents().size() != 1)
	    throw new RuntimeException("I "+getID()
				       +" am a sender-receiver type node and my parent is not a single node. Its size is: "+
				       getParents().size());
	if(myCosts.size() > 2)
	    throw new RuntimeException("I have wrong number of costs: "+myCosts.size());
	if(getParents().get(0).getTypes().get(0).equals("sender"))
	    max = myCosts.get(0)>myCosts.get(1)?myCosts.get(0):myCosts.get(1);
	else if((getParents().get(0).getTypes().size() == 2) && 
		(getParents().get(0).getTypes().get(0).equals("receiver") && 
		 getParents().get(0).getTypes().get(1).equals("sender")))
	    max = max > myCosts.get(1) ? max : myCosts.get(1);
	totalCost = parentCost;
		
	    
    }
    
    //XXX: Assumption that the Cnode cost is the second one in the list
    //I think it is a correct assumption, looking at the XMLparser
    public void updateReceiverCost(float parentCost) throws Exception{
	
	if(getParents().size() == 1 && getParents().get(0).getTypes().get(0).equals("sender")){
	    totalCost = (parentCost+= myCosts.get(0)>myCosts.get(1)?
			 myCosts.get(0):myCosts.get(1));
	}
	else if(getParents().size() == 1 &&
		getParents().get(0).getTypes().get(0).equals("receiver") &&
		getParents().get(0).getTypes().get(1).equals("sender") ){
	    totalCost = (parentCost+= max > myCosts.get(1) ? max : myCosts.get(1));
	}
	else if(getParents().size() == 0) throw new RuntimeException("I have no parents, receiver: "+getID());
    }
    
    public void updateSenderCost(float parentCost) throws Exception{
	if(myCosts.size() == 1){
	    totalCost = parentCost;
	    max = myCosts.get(0); //sending this value
	}
	else throw new RuntimeException("Size of cost array is: "
					+myCosts.size()+" for state: "+ getID()+", which is a sender");
    }
    public void updateCurrentCost(float parentCost) throws Exception{
	if(myCosts.size() == 1)
	    totalCost += parentCost+myCosts.get(0);
	else if(myCosts.size() == 2){
	    totalCost = (parentCost += myCosts.get(0) > myCosts.get(1) ? myCosts.get(0) : myCosts.get(1));
	}
	else throw new RuntimeException("Size of cost array is: "
					+myCosts.size()+" for state: "+ getID());
    }
    public void setCost(float val){
	myCosts.add(val);
	//DEBUG
	// System.out.println(getID()+" has cost: "+myCosts);
    }
    public void setUpdateGuards(ArrayList<String> updates){
	for(String s : updates){
	    if(s!=null){
		this.updates.add(s);
		//DEBUG
		// System.out.println(getID()+" updates --> "+updates);
	    }
	}
    }
    public void setUpdateGuard(String updates){
	if(updates != null)
	    this.updates.add(updates);

    }
    public ArrayList<String> getUpdateGuards(){
	return updates;
    }
    
    public void setUpdateStates(ArrayList<state> s){
	//DEBUG
	// System.out.print("Setting update states for "+getID()+" ");

	for(state st : s){
	    //DEBUG
	    // System.out.print(st.getID()+" ");
	    updateElements.add(st);
	}
	// System.out.println();

    }
    
    public void setUpdateState(state s){
	updateElements.add(s);
	//DEBUG
	// for(state st : updateElements){
	//     System.out.print(st.getID());
	// }
	// System.out.println();
    }
    
    public ArrayList<state> getUpdateStates(){
	return updateElements;
    }
}


