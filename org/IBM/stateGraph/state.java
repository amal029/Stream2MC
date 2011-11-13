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
    private boolean visited = false;
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
    //This woun't be needed
    public void updateCurrentCost(float c) throws Exception{
	if(myCosts.size() == 1)
	    totalCost += myCosts.get(0);
	else if(myCosts.size() == 2){
	    totalCost += myCosts.get(0) > myCosts.get(1) ? myCosts.get(0) : myCosts.get(1);
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


