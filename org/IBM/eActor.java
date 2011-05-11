package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class eActor extends Actor{
    private String execFileName;
    private String execFilePath;
    public eActor(String id){super(id);}
    public eActor(GXLNode e){super(e);}
    public long getExecTime(String attr){
	return getVals(attr);
    }
    //Labels will now like like this: Label$processor;Label$processor....
    protected void updateLabels(ArrayList<GXLNode> p, ArrayList<GXLEdge> c)throws Exception{
	String guardLabels = getGuardLabels();
	String updateLabels = getUpdateLabels();
	String gls[] = null, uls[]=null;
	String costLabels = "";
	if(guardLabels == null || (updateLabels==null && !getID().equals("dummyTerminalNode")))
	    throw new RuntimeException("Node "+getID()+" has null guard or update label");
	else{
	    gls = getSplitLabels(guardLabels,",");
	    if(!getID().equals("dummyTerminalNode"))
		uls = getSplitLabels(updateLabels,",");
	    guardLabels = ""; updateLabels = "";
	}
	//Check if there is just one total_time_x86. If so, then
	//increase it p.size() number of times.
	String meCost = null;
	if(getID().equals("dummyStartNode") || getID().equals("dummyTerminalNode"));
	else{
	    String myCost[]=((GXLString)getAttr("total_time_x86").getValue()).getValue().split(";");
	    if(myCost.length == 1)
		meCost=myCost[0];
	    else if(myCost.length != p.size())
		throw new RuntimeException("The total_time_x86 variable for node: "+getID()+" is not correctly defined");
	}
	for(int e=0;e<p.size();++e){
	    String pID = p.get(e).getID();
	    //The very first node (dummy one) should never be allocated
	    //to different processors
	    if(!getID().equals("dummyStartNode")){
		for(int t=0;t<gls.length;++t){
		    guardLabels += gls[t]+"$"+pID;
		    if(t < gls.length-1)
			guardLabels += ",";
		    else guardLabels += ";";
		}
	    }else guardLabels = gls[0];
	    //A null value is possible, if this is ther dummy terminal node
	    if(uls != null){
		for(int t=0;t<uls.length;++t){
		    updateLabels += uls[t]+"$"+pID;
		    if(t < uls.length-1)
			updateLabels += ",";
		    else updateLabels  += ";";
		}
	    }
	    if(getID().equals("dummyStartNode") || getID().equals("dummyTerminalNode"));
	    else if(meCost!=null){
		costLabels += meCost+";";
	    }
	}
	if(meCost != null)
	    setAttr("total_time_x86",new GXLString(costLabels));
	setAttr("__guard_labels_with_processors",new GXLString(guardLabels));
	setAttr("__update_labels_with_processors",new GXLString(updateLabels));
    }
    protected void buildGlobals(StringBuilder buf)throws Exception{
	//Special case of start node
	if(getID().equals("dummyStartNode")){
	    buf.append("<!-- Making the global declarations -->\n");
	    buf.append("<declaration>\n");
	    buf.append("int tCost=0;\n"); //This is the total cost of running this state space system
	    buf.append("//Guard for node "+getID()+"\n");
	    buf.append("bool "+((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue()+"=1;\n");
	    String updates [] = ((GXLString)getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
	    //Get the cost of doing this node.
	    for(int e=0;e<updates.length;++e)
		Actor.globalCostDeclBuild(buf,getID(),"0"); //Putting in the cost variable for this node.
	    Actor.countS=1;
	    return;
	}
	//All other cases
	buf.append("//Guard for execution node "+getID()+"\n");
	String guards[] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String myCosts[] = null;
	if(!getID().equals("dummyTerminalNode"))
	    myCosts = ((GXLString)getAttr("total_time_x86").getValue()).getValue().split(";");
	for(int y=0;y<guards.length;++y){
	    //Make $ and : into _
	    String temps = guards[y];
	    String temp[] = temps.split(","); //If there are more than one precedence relation guards
	    buf.append("bool ");
	    for(int e=0;e<temp.length;++e){
		temp[e] = temp[e].replace('$','_');
		temp[e] = temp[e].replace(':','_');
		buf.append(temp[e]+" = 0");
		if(e==(temp.length)-1)
		    buf.append(";\n");
		else buf.append(", ");
	    }
	    String myCost = "0";
	    if(getID().equals("dummyTerminalNode")) myCost="0";
	    else if(getAttr("total_time_x86")==null && !getID().equals("dummyTerminalNode"))
		throw new RuntimeException(getID()+" does not know how long it will take!!");
	    else
		myCost = myCosts[y];
		// myCost=((GXLString)getAttr("total_time_x86").getValue()).getValue();
	    Actor.globalCostDeclBuild(buf,getID(),myCost); //Putting in the
							//cost variable
							//for this node
							//on this
							//processor.
	}
	Actor.countS =1;
    }
    //This is the final thing that is built
    protected String buildSystem(StringBuilder buf)throws Exception{
	if(getID().equals("dummyStartNode")){
	    buf.append("<!-- Start the system declaration, i.e., instantiating the states -->\n");
	    buf.append("<system>\n");
	    String updates[] = ((GXLString)getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
	    for(int y=0;y<updates.length;++y)
		Actor.sysBuild(buf,getID());
	    Actor.countS=1;
	    return Actor.names;
	}
	//Instantiate the state machines
	String guards[] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	for(int y=0;y<guards.length;++y)
	    Actor.sysBuild(buf,getID());
	Actor.countS=1;
	return Actor.names;
    }
    //This one is easy to build, but it is hard for communication
    //actors.
    protected void buildTemplate(StringBuilder buf)throws Exception{
	String guards [] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String updates [] = ((GXLString)getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
	if(!getID().equals("dummyTerminalNode")){
	    if(!getID().equals("dummyStartNode")){
		if(guards.length != updates.length)
		    throw new RuntimeException("Guard labels not equal to Update Label for node "+ getID());
	    }
	}
	int term = (getID().equals("dummyStartNode"))?updates.length:guards.length;
	for(int e=0;e<term;++e){
	    //This is just building the sequential state machines, the
	    //parallel still need to be built --> Done
	    buf.append("<!-- template for execution node "+getID()+" -->\n");
	    buf.append("<template>\n");
	    //Make the local declarations and the name of this node
	    Actor.tempBuild(buf,getID(),Actor.CHOICE.valueOf("SEQ"));
	    //Make the transition system
	    String temp = "";
	    String utemp = "";
	    if(getID().equals("dummyStartNode"))
		temp = guards[0];
	    else temp = guards[e];
	    if(!getID().equals("dummyTerminalNode"))
		utemp = updates[e];
	    String guardst [] = temp.split(",");
	    String updatest [] = null;
	    if(!getID().equals("dummyTerminalNode"))
		updatest = utemp.split(",");
	    if(!getID().equals("dummyTerminalNode"))
		transBuild(buf,getID(),guardst,updatest,Actor.CHOICE.valueOf("SEQ"),false);
	    else
		transBuild(buf,getID(),guardst,updatest,Actor.CHOICE.valueOf("SEQ"),true);
	    buf.append("</template>\n");
	}
	Actor.countS =1;
    }
    /**
       @author Avinash Malik
       @date Mon Mar 21 11:38:41 GMT 2011
       @param globaldeclarations buffer, templateDeclaration buffer, and
       systemDeclaration buffer.
       @return void
       @bug This algorithm can be sped up massively.

       This function builds the state machines for uppaal, which has
       channels.
     */

    //This will never happen at the start ot end nodes
    @SuppressWarnings("unchecked")
    protected void buildParallel (StringBuilder gb, StringBuilder tb, StringBuilder sb) throws Exception{
	/**
	   @author Avinash Malik 

	   This algorithm is one of the most important algorithm. It
	   extracts all possible combinations of actors allocated to
	   processors. There is significant state space explosion.
	 */
	ArrayList<ArrayList> finals = extractCompleteParallelism();
	int count =0;
	for(ArrayList i : finals){
	    // System.out.println("Building the parallel globals");
	    buildParallelGlobal(gb,i,count);
	    // System.out.println("Finished building the parallel Globals");
	    buildParallelTemplate(tb,i,count);
	    // System.out.println("Finished building the parallel Template");
	    buildParallelSystem(sb,i,count);
	    // System.out.println("Finished building the parallel System");
	    ++count;
	}
    }
    protected void divide(Long factor){
	if(factor == null) factor = new Long(1);
	else if(getID().equals("dummyTerminalNode") || getID().equals("dummyStartNode")) return;
	long div = factor.longValue();
	String myTimes [] = ((GXLString)getAttr("total_time_x86").getValue()).getValue().split(";");
	int counter=0;
	for(String time : myTimes){
	    Long myTime = new Long(time).longValue();
	    if(myTime/div > 0)
		myTime/=div;
	    myTimes[counter]=myTime+"";
	    ++counter;
	}
	String b="";
	for(String s : myTimes)
	    b+=s+";";
	setAttr("total_time_x86",new GXLString(b));
    }
    public long getMultiProcessorTime(){
	String tokens[] = ((GXLString)getAttr("total_time_x86").getValue()).getValue().split(";");
	long singleProcessorTime = (new Long(tokens[0]).longValue());
	for(int e=1;e<tokens.length;++e){
	    singleProcessorTime = singleProcessorTime>=(new Long(tokens[e]).longValue())?
		singleProcessorTime:(new Long(tokens[e]).longValue());
	}
	return singleProcessorTime;
    }
    public long getSingleProcessorTime(){
	String tokens[] = ((GXLString)getAttr("total_time_x86").getValue()).getValue().split(";");
	long singleProcessorTime = (new Long(tokens[0]).longValue());
	for(int e=1;e<tokens.length;++e){
	    singleProcessorTime = singleProcessorTime<=(new Long(tokens[e]).longValue())?
		singleProcessorTime:(new Long(tokens[e]).longValue());
	}
	return singleProcessorTime;
    }
}
