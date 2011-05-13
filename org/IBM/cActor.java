package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class cActor extends Actor{
    public cActor(String id){super(id);}
    public cActor(GXLNode e){super(e);}
    public long getRate(){
	return getVals("rate");
    }
    private long getSourceActorRep(){
	return getVals("sourceActorRep");
    }
    public long getRep(){
	long ret = super.getRep();
	if(ret != 1) throw new RuntimeException("Communication Actor "+this.getID()+" has invalid repition value: "+ret);
	else return ret;
    }
    public boolean checkcActor(){
	boolean ret = true;
	for(int e=0;e<getConnectionCount();++e){
	    if(getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		if(node.getIsMergeNode()){ ret = false; break;}
	    }
	    if(getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getSource();
		if(node.getIsSplitNode()){ ret = false; break;}
	    }
	}
	return ret;
    }
    private String getWorkLabel() throws RuntimeException{
	if(getAttr("work_x86")==null) return null;
	else return ((GXLString)this.getAttr("work_x86").getValue()).getValue();
    }
    private void setWorkLabel(String label){
	if(getAttr("work_x86")==null) throw new RuntimeException("Communication node "+getID()+" does not do any work!!");
	else setAttr("work_x86",new GXLString(label));
    }
    //This is a bit more complex then eActor's updates
    private static Hashtable<String,iniParser> parsers = new Hashtable<String,iniParser>(2);
    protected void updateLabels(ArrayList<GXLNode> p, ArrayList<GXLEdge> c)throws Exception{
	String guardLabels = getGuardLabels();
	String updateLabels = getUpdateLabels();
	String workLabel = getWorkLabel();
	String workLabels = null;
	String gls[] = null, uls[]=null;
	if(guardLabels == null || updateLabels==null || workLabel == null)
	    throw new RuntimeException("Node "+getID()+" has null guard or update or work label");
	else{
	    gls = getSplitLabels(guardLabels,",");
	    uls = getSplitLabels(updateLabels,",");
	    guardLabels = ""; updateLabels = "";
	    workLabels="";
	}
	// There is never a possiblity that dummyTerminalNode
	//or dummyStartNode are cActors, so no worries here
	org.IBM.iniParser parser=null;
	for(int r=0;r<c.size();++r){
	    //Now let us get the actual time required to send this data
	    //through.

	    //Update the guards
	    //DEBUG
	    String sID = ((GXLNode)c.get(r).getSource()).getID();
	    String tID = ((GXLNode)c.get(r).getTarget()).getID();

	    String latencyFile = ((GXLString)c.get(r).getAttr("latency_file").getValue()).getValue();
	    //Now tokenize this to get the file name and the parser that
	    //needs to be called to parse it and get values.
	    String tokens[] = latencyFile.split(":");
	    if(tokens[0].equals("Name"))
		latencyFile = tokens[1];

	    //Now call the parser with the fileName
	    if(parsers.isEmpty()){
		parser = new org.IBM.iniParser(latencyFile);
		parsers.put(latencyFile,parser);
	    }
	    else if(!parsers.containsKey(latencyFile)){
		parser = new org.IBM.iniParser(latencyFile);
		parsers.put(latencyFile,parser);
	    }
	    else if(parsers.containsKey(latencyFile)){
		parser = parsers.get(latencyFile);
	    }

	    //Find the number of bytes that this actor will send and receive
	    //And the latency for the communication
	    //Set the latency for calculation by the model checker
	    workLabels += parser.getLatency(getRate()*getSourceActorRep()+"")+";";
	    for(int t=0;t<gls.length;++t)
		guardLabels += gls[t]+"$"+sID+";";
	    for(int t=0;t<uls.length;++t)
		updateLabels += uls[t]+"$"+tID+";";
	} 
	System.out.print("...");
	System.out.flush();
	setAttr("__guard_labels_with_processors",new GXLString(guardLabels));
	setAttr("__update_labels_with_processors",new GXLString(updateLabels));
	setWorkLabel(workLabels);
    }
    //Have the complete these methods
    private static String[] minimize(String guards[]){
	ArrayList<String> list = new ArrayList<String>(1);
	boolean put = false;
	for(int e=0;e<guards.length;++e){
	    put = false;
	    String temp = guards[e];
	    for(int t=0;t<list.size();++t){
		if(temp.equals(list.get(t))){
		    put= true; 
		    break;
		}
	    }
	    if(!put)
		list.add(guards[e]);
	}
	return list.toArray(new String[0]);
    }
    protected void buildGlobals(StringBuilder buf)throws Exception{
	//This can never be the starting actor or the last actor.  Also
	//note that this cannot have more than one precedence guard or
	//precedence update labels, i.e., no need to split with ","
	String guards [] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String costs [] = ((GXLString)getAttr("work_x86").getValue()).getValue().split(";");
	String nguards[] = minimize(guards);
	//Now put the guard labels in the global declaration buffer
	buf.append("//Guard for communication node "+getID()+"\n");
	buf.append("bool ");

	for(int e=0;e<nguards.length;++e){
	    //Replace $ and : with _
	    String temp = nguards[e];
	    temp = temp.replace('$','_');
	    temp = temp.replace(':','_');
	    buf.append(temp);
	    if(e == nguards.length-1)
		buf.append(";\n");
	    else buf.append(", ");
	}
	//Now put the cost variable for this communication actor Note
	//this is different from the above nguards array, because the
	//guards can be repeated for communication actors. For example,
	//AP1P2---->A'P1P2 and AP1P1-->A'P1P1. Note how the two state
	//machines have the same first P1, which is the guard and P2,P1
	//are the updates for the two state machines, respectively.
	for(int e=0;e<guards.length;++e){
	    Actor.globalCostDeclBuild(buf,getID(),costs[e]); //Putting in the
							//cost variable
							//for this node
							//on this
							//processor.
	}
	Actor.countS =1;
    }
    protected String buildSystem(StringBuilder buf)throws Exception{
	// Instantiate the state machines
	String guards[] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	for(int y=0;y<guards.length;++y)
	    Actor.sysBuild(buf,getID());
	Actor.countS=1;
	return Actor.names;
    }
    protected void buildTemplate(StringBuilder buf)throws Exception{
	String guards [] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String updates [] = ((GXLString)getAttr("__update_labels_with_processors").getValue()).getValue().split(";");

	if(guards.length != updates.length)
	    throw new RuntimeException("Guard labels not equal to Update Label for Communication node"+ getID());
	String temp[]=new String[1],utemp[]=new String[1];
	for(int e=0;e<guards.length;++e){
	    buf.append("<!-- template for communication node "+getID()+" -->\n");
	    buf.append("<template>\n");
	    //Make the local declarations and the name of this node
	    Actor.tempBuild(buf,getID(),Actor.CHOICE.valueOf("SEQ"));
	    //Make the transition system
	    temp[0] = guards[e];
	    utemp[0] = updates[e];
	    transBuild(buf,getID(),temp,utemp,Actor.CHOICE.valueOf("SEQ"),false);
	    buf.append("</template>\n");
	}
	Actor.countS=1;
    }
    @SuppressWarnings("unchecked")
    protected void buildParallel (StringBuilder gb, StringBuilder tb, StringBuilder sb) throws Exception{
	/**
	   @see Actor.java and eActor.java
	 */
	ArrayList<ArrayList> finals = extractCompleteParallelism();
	int count =0;
	for(ArrayList i : finals){
	    buildParallelGlobal(gb,i,count);
	    buildParallelTemplate(gb,tb,i,count);
	    buildParallelSystem(sb,i,count);
	    ++count;
	}
    }

    //These are the methods required for the declustering algorithm to
    //work
    // Sat Apr 9 14:07:28 IST 2011
    public long getMultiProcessorTime(){
	String tokens[] = ((GXLString)getAttr("work_x86").getValue()).getValue().split(";");
	long singleProcessorTime = (new Long(tokens[0]).longValue());
	for(int e=1;e<tokens.length;++e){
	    singleProcessorTime = singleProcessorTime>=(new Long(tokens[e]).longValue())?
		singleProcessorTime:(new Long(tokens[e]).longValue());
	}
	return singleProcessorTime;
    }
    public long getSingleProcessorTime(){
	String tokens[] = ((GXLString)getAttr("work_x86").getValue()).getValue().split(";");
	long singleProcessorTime = (new Long(tokens[0]).longValue());
	for(int e=1;e<tokens.length;++e){
	    singleProcessorTime = singleProcessorTime<=(new Long(tokens[e]).longValue())?
		singleProcessorTime:(new Long(tokens[e]).longValue());
	}
	return singleProcessorTime;
    }

    protected void divide(Long factor){
	if(factor == null) factor = new Long(1);
	long div = factor.longValue();
	String work[] = ((GXLString)getAttr("work_x86").getValue()).getValue().split(";");
	int counter=0;
	for(String s : work){
	    long myTime = new Long(s).longValue();
	    if(myTime/div > 0){ 
		myTime/=div;
		work[counter] = new String(myTime+"");
	    }
	    ++counter;
	}
	String b = "";
	for(String s : work)
	    b+=s+";";
	setAttr("work_x86",new GXLString(b));
    }
}
