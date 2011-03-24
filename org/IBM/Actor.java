package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class Actor extends GXLNode{
    public Actor(String id){super(id);}
    private boolean visit = false;
    public boolean ifVisited(){return visit;}
    public void setVisited(boolean v){visit=v;}
    protected void updateLabels(ArrayList<GXLNode> p, ArrayList<GXLEdge> c) throws Exception{
	throw new RuntimeException();
    }
    public void setVisited(){setVisited(true);}
    /**
      This adds the guard labels in the form L1,L2,L3..., which is
      basically a string, these should be converted to integers when
      generating code for MC. A new Attribute is added if one already
      does not exist.
      @return void
      @param String label to append to the GuradLabels
     */
    public void setGuardLabels(String guard){
	String currLabels = getGuardLabels();
	//Add a new attribute
	if(currLabels==null){
	    this.setAttr("guardLabels",new GXLString(guard));
	}
	else{
	    currLabels+=","+guard;
	    this.setAttr("guardLabels",new GXLString(currLabels));
	}
    }
    /**
       This method provides the labels that are to be guards for this actor.
       A null may be returned by this method.
       @author Avinash Malik
       @date Tue Mar  8 11:38:34 GMT 2011
       @return The guards for this actor in the form L1,L2,L3,... or null
     */
    public String getGuardLabels(){
	if(this.getAttr("guardLabels")==null)
	    return null;
	else return ((GXLString)this.getAttr("guardLabels").getValue()).getValue();
    }
    public String getProcessorUpdateLabels(){
	if(this.getAttr("__update_labels_with_processors")==null)
	    return null;
	else return ((GXLString)this.getAttr("__update_labels_with_processors").getValue()).getValue();
    }
    public String getProcessorGuardLabels(){
	if(this.getAttr("__guard_labels_with_processors")==null)
	    return null;
	else return ((GXLString)this.getAttr("__guard_labels_with_processors").getValue()).getValue();
    }
    /**
       This method provides the labels that are to be updated by this actor.
       A null may be returned by this method.
       @return The update label string with labels as L1,L2,L3,... or null
       @param void
     */
    public String getUpdateLabels(){
	if(this.getAttr("updateLabels")==null) return null;
	else return ((GXLString)getAttr("updateLabels").getValue()).getValue();
    }
    public void setUpdateLabels(String label){
	String currLabels = getUpdateLabels();
	if(currLabels==null){
	    this.setAttr("updateLabels",new GXLString(label));
	}
	else{
	    currLabels+=","+label;
	    this.setAttr("updateLabels",new GXLString(currLabels));
	}
    }
    protected String[] getSplitLabels(String labels,String split){
	return labels.split(split);
    }
    protected Stack<Actor> splitStack = new Stack<Actor>();
    protected Stack<Integer> splitIndex = new Stack<Integer>();
    public boolean getIsMergeNode(){
	if(this.getAttr("mergeNode") == null ) return false;
	else if(((GXLString)this.getAttr("mergeNode").getValue()).getValue().equals("true")) return true;
	else return false;
    }
    public boolean getIsSplitNode(){
	if(this.getAttr("splitNode") == null) return false;
	else if(((GXLString)this.getAttr("splitNode").getValue()).getValue().equals("true")) return true;
	else return false;
    }
    public void setIsMergeNode(boolean value){
	this.setAttr("mergeNode",new GXLString(value+""));
    }
    public void setIsSplitNode(boolean value){
	this.setAttr("splitNode",new GXLString(value+""));
    }
    protected void setSplitNode(String val){
	this.setAttr("splitNode",new GXLString(val));
    }
    protected void setMergeNode(String val){
	this.setAttr("myMergeNode",new GXLString(val));
    }
    protected String getMergeNode(){
	if(this.getAttr("myMergeNode") == null) return null;
	else return ((GXLString)this.getAttr("myMergeNode").getValue()).getValue();
    }
    protected String getSplitNode(){
	if(this.getAttr("splitNode") == null) return null;
	else return ((GXLString)this.getAttr("splitNode").getValue()).getValue();
    }
    //If this is not called, this attribute will always be "null"
    protected void setStructureLabelAndIndex(String val){
	this.setAttr("strucutureLabelAndIndex",new GXLString(val));
    }
    protected String getStructureLabelAndIndex(){
	if(this.getAttr("strucutureLabelAndIndex") == null) return null;
	else return ((GXLString)this.getAttr("strucutureLabelAndIndex").getValue()).getValue();
    }
    public Actor(GXLNode n){
	super(n.getID());
	for(int e=0;e<n.getAttrCount();++e){
	    GXLAttr at = n.getAttrAt(e);
	    String s= at.getName();
	    GXLValue t = at.getValue();
	    /**
	      @bug This is a very bad hack, I know that all values are String Type
	      and hence this works. In the general case this will never work
	    */
	    this.setAttr(s,new GXLString(((GXLString)t).getValue()));
	    if(at.getKind()!=null)
	    	this.getAttr(s).setKind(at.getKind());
	}
    }
    public boolean iscActor(){if(this.getAttr("rate")==null) return false; else return true;}
    public boolean iseActor(){if(this.getAttr("rate")==null) return true; else return false;}
    public long getVals(String attr){
	long rate = 0;
	GXLAttr ret = null;
	if((ret=this.getAttr(attr))==null){
	    return rate;
	}
	else{
	    rate = new Long(((GXLString)ret.getValue()).getValue()).longValue();
	    return rate;
	}
    }
    //They both have repition vectors.
    public long getRep(){
	return getVals("rep");
    }
    public long getDelay(){
	return getVals("delay");
    }
    public String getLabel(){
	GXLAttr ret = null;
	if((ret=this.getAttr("label"))==null)
	    return "";
	else
	    return (((GXLString)ret.getValue()).getValue());
    }
    protected void setProcessorUpdates(String val){
	setAttr("pUpdates",new GXLString(val));
    }
    protected String getProcessorUpdates(){
	if(this.getAttr("pUpdates")==null)
	    return null;
	else return ((GXLString)getAttr("pUpdates").getValue()).getValue();
    }
    protected void setProcessorGuards(String val){
	this.setAttr("pGuards",new GXLString(val));
    }
    protected String getProcessorGuards(){
	if(this.getAttr("pGuards")==null)
	    return null;
	else
	    return ((GXLString)getAttr("pGuards").getValue()).getValue();
    }
    protected void buildGlobals(StringBuffer buf) throws Exception{
	throw new RuntimeException();
    }
    protected static String names=null;
    //We can use countS even for cost. This means that countS will be
    //incremented at global declaration time not at template generation
    //time.
    protected static int mCount2=1,mCount=1;
    protected static int countS = 1;
    //This is pretty nice, you can also put methods inside enums
    //including a constructor, cool!!  SEQ --> just the add function
    //PARS --> Send the cost of this computation over some channel name
    //"C" PARR --> Find the max of the received cost over some channel
    //"C" and your own cost and add the max to the total cost tCost.
    protected static enum CHOICE {SEQ, PARS, PARR, PARM}
    protected static void sysBuild(StringBuffer buf, String ID){
	buf.append("S"+ID+countS+" = "+ID+countS+"();\n");
	if(names == null)
	    names = "S"+ID+countS;
	else
	    names += ",S"+ID+countS;
	++countS;
    }
    protected String buildSystem(StringBuffer buf)throws Exception{
	throw new RuntimeException();
    }
    protected static void globalCostDeclBuild(StringBuffer buf, String ID, String value){
	buf.append("int C"+ID+countS+"="+value+";\n");
	++countS;
    }
    private static void localDeclBuild(StringBuffer buf, String ID, CHOICE c){
	buf.append("<declaration>\n");
	//Make the cost function according the SEQ or PAR
	switch(c){
	case SEQ:
	    buf.append("void add(){tCost += C"+ID+countS+";}\n");
	    break;
	case PARS:
	    //You need to know if this is the sender of the receiver
	    break;
	case PARR:
	    //You need to know if this is the sender of the receiver
	    break;
	}
	buf.append("</declaration>\n");
    }
    private static int c = 1;

    @SuppressWarnings("fallthrough")
    protected static void transBuildPar(StringBuffer buf, String ID, String guards[], String [] updates, 
					CHOICE C, boolean terminalNode, String chanName, String sendName,
					String cost){
	buf.append("<!-- The transition system -->\n");
	String id1 = "id"+c, id2="id"+(++c),id3=null;
	String cName[] = chanName.split("\\?");
	switch(C){
	case PARM:
	    id3 = "id"+(++c);
	    buf.append("<location id = \""+id3+"\"><urgent/></location>");
	default:
	    buf.append("<location id = \""+id1+"\"><urgent/></location>");
	    buf.append("<location id = \""+id2+"\"><urgent/></location>");
	    break;
	}
	//Now tell which one is the init?
	buf.append("<init ref=\""+id1+"\"/>\n");
	//Now make the transition between id1-->id2
	buf.append("<transition><source ref=\""+id1+"\"/><target ref=\""+id2+"\"/>\n");
	switch(C){
	case PARS:
	    buf.append("<label kind=\"synchronisation\">"+cName[0]+"!</label>\n");
	    bb(buf,ID,guards,updates,CHOICE.valueOf("SEQ"),terminalNode,sendName+"?PARS?"+cost,id1,id2);
	    break;
	case PARR:
	    buf.append("<label kind=\"synchronisation\">"+cName[0]+"?</label>\n");
	    bb(buf,ID,guards,updates,CHOICE.valueOf("SEQ"),terminalNode,sendName+"?PARR?"+cost,id1,id2);
	    break;
	case PARM:
	    buf.append("<label kind=\"synchronisation\">"+cName[0]+"?</label>\n");
	    bb(buf,ID,guards,new String[0],CHOICE.valueOf("SEQ"),terminalNode,sendName+"?PARM?"+cost,id1,id2);
	    //Now make another one if this is PARM
	    buf.append("<transition><source ref=\""+id2+"\"/><target ref=\""+id3+"\"/>\n");
	    buf.append("<label kind=\"synchronisation\">"+cName[1]+"!</label>\n");
	    //Should not have any guards or updates
	    //because, the guards and updates have already been taken care of in the previous transition
	    bb(buf,ID,new String[0],updates,CHOICE.valueOf("SEQ"),terminalNode,sendName+"?PARS?"+sendName,id2,id3);
	    break;
	default:
	    throw new RuntimeException();
	}
	++c;
    }

    private static void bb(StringBuffer buf, String ID, String guards[], String [] updates, 
			   CHOICE C, boolean terminalNode, String sendName,String id1, String id2){
	switch(C){
	case SEQ:
	    buf.append("<label kind=\"guard\">");
	    for(int e=0;e<guards.length;++e){
		String temp = guards[e];
		temp = temp.replace('$','_');
		temp = temp.replace(':','_');
		buf.append(temp+"==1");
		if(!(e == guards.length-1))
		    buf.append(" and ");
	    }
	    buf.append("</label>\n");
	    break;
	}
	//Put the update labels, currently this will only work for SEQ
	switch(C){
	case SEQ:
	    buf.append("<label kind=\"assignment\">");
	    for(int e=0;e<guards.length;++e){
		String temp = guards[e];
		temp = temp.replace('$','_');
		temp = temp.replace(':','_');
		buf.append(temp+"=0,");
	    }
	    if(!terminalNode){
		for(int e=0;e<updates.length;++e){
		    String temp = updates[e];
		    temp = temp.replace('$','_');
		    temp = temp.replace(':','_');
		    buf.append(temp+"=1,");
		}
	    }
	    if(sendName != null){
		String temp[] = sendName.split("\\?");
		if(temp[1].equals("PARS"))
		    buf.append(temp[0]+" = "+temp[2]);
		else if(temp[1].equals("PARR"))
		    buf.append("tCost += "+temp[0]+"&gt;"+temp[2]+"?"+temp[0]+":"+temp[2]);
		else if(temp[1].equals("PARM"))
		    buf.append(temp[0]+" = "+temp[0]+"&gt;"+temp[2]+"?"+temp[0]+":"+temp[2]);
	    }
	    else
		buf.append("add()");
	    buf.append("</label>\n");
	    break;
	}
	buf.append("</transition>\n");
	//increment the counter and return
	if(terminalNode)
	    buf.append("<transition><source ref=\""+id2+"\"/><target ref=\""+id2+"\"/></transition>\n");
    }
    protected static void transBuild(StringBuffer buf, String ID, String guards[], String [] updates, 
				     CHOICE C, boolean terminalNode){
	buf.append("<!-- The transition system -->\n");
	String id1 = "id"+c, id2="id"+(++c);
	buf.append("<location id = \""+id1+"\"><urgent/></location>");
	if(!terminalNode)
	    buf.append("<location id = \""+id2+"\"><urgent/></location>");
	else
	    buf.append("<location id = \""+id2+"\"><name>TERMINATE</name><urgent/></location>");
	//Now tell which one is the init?
	buf.append("<init ref=\""+id1+"\"/>\n");
	//Now make the transition between id1-->id2
	buf.append("<transition><source ref=\""+id1+"\"/><target ref=\""+id2+"\"/>\n");
	//Put the guard labels, currently this will only work for SEQ
	bb(buf,ID,guards,updates,C,terminalNode,null,id1,id2);
	++c;
    }
    protected static void tempBuild(StringBuffer buf, String ID, CHOICE c){
	buf.append("<name>"+ID+countS+"</name>\n");
	localDeclBuild(buf,ID,c);
	++countS;
    }
    protected void buildTemplate(StringBuffer buf)throws Exception{
	throw new RuntimeException();
    }
    /**
       @author Avinash Malik
       @date Mon Mar 21 11:38:41 GMT 2011
       @param globaldeclarations buffer, templateDeclaration buffer, and
       systemDeclaration buffer.
       @return void

       This function builds the state machines for uppaal, which has
       channels.
     */
    protected void buildParallel(StringBuffer gb, StringBuffer tb, StringBuffer sb) throws Exception {
	throw new RuntimeException();
    }
    private void extract(Actor sNode, ArrayList<String> list,boolean val){
	ArrayList<String> pProcessors = new ArrayList<String>();
	if(val){
	    //First thing you do is get out the last element of the array
	    //and see if you have a processor allocation that can be put in
	    //this list.
	    //SNode is null for the very first time
	    String guards [] = ((GXLString)sNode.getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	    //Now extract the possible processors that I can be assigned to
	    for(int r=0;r<guards.length;++r)
		guards[r] = guards[r].split(",")[0].split("\\$")[1];
	    guards = removeRepetition(guards);
	    //Now check with list.size()-1, if it possible to fit myself in there
	    String currAlloc[] = list.get(list.size()-1).split("-");
	    for(int r=0;r<currAlloc.length;++r)
		currAlloc[r] = currAlloc[r].split("_")[1];
	    boolean add = true;
	    int y=0;
	    for(;y<guards.length;++y){
		String myPAlloc = guards[y];
		add = true;
		for(int e=0;e<currAlloc.length;++e){
		    if(myPAlloc.equals(currAlloc[e])){
			add =false; break;
		    }
		}
		if(add){
		    pProcessors.add(guards[y]);
		}
	    }
	}
	if(pProcessors.isEmpty() && val) return; //No point going forward

	if(val){
	    int addPointer = list.size();
	    for(int y=0;y<pProcessors.size();++y){
		list.add(list.get(addPointer-1)+"-"+sNode.getID()+"_"+pProcessors.get(y));
		// list.add(list.get(list.size()-1)+"-"+sNode.getID()+"_"+pProcessors.get(y));
		// This is pretty much the tail recursion
		int myPointer = list.size()-1;
		int count = 0;
		for(int e=0;e<sNode.getConnectionCount();++e){
		    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
			GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
			if(le.getAttr("parallelEdge")!=null){
			    //move myself to the last place in the list if the
			    //count > 0
			    if(count > 0){
				String me = list.remove(myPointer);
				list.add(me);
				myPointer = list.size()-1;
			    }
			    ++count;
			    extract((Actor)le.getTarget(),list,true);
			}
		    }
		}
	    }
	}
	else{
	    // This is pretty much the tail recursion
	    int myPointer = list.size()-1;
	    int count = 0;
	    for(int e=0;e<sNode.getConnectionCount();++e){
		if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		    GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		    if(le.getAttr("parallelEdge")!=null){
			//move myself to the last place in the list if the
			//count > 0
			if(count > 0){
			    String me = list.remove(myPointer);
			    list.add(me);
			    myPointer = list.size()-1;
			}
			++count;
			extract((Actor)le.getTarget(),list,true);
		    }
		}
	    }
	}
    }
    /**
       This is one of the most important algorithms in the whole
       compiler.  It extract parallelism from the actor graph by looking
       at the processor architecture. There is significant state space
       explosion.
       @author Avinash Malik
       @date Tue Mar 22 14:47:35 GMT 2011
     */
    protected ArrayList<ArrayList> extractCompleteParallelism() throws Exception{
	String guards [] = ((GXLString)getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	//Now extract the possible processors that I can be assigned to
	for(int r=0;r<guards.length;++r)
	    guards[r] = guards[r].split(",")[0].split("\\$")[1];
	//Now make a DFT search building the list for parallel nodes
	ArrayList<ArrayList> ret = new ArrayList<ArrayList>();
	for(int w=0;w<guards.length;++w){
	    ArrayList<String> nIDAndProcessors= new ArrayList<String>();
	    nIDAndProcessors.add(getID()+"_"+guards[w]);
	    extract(this,nIDAndProcessors,false);
	    //remove yourself from this mess
	    nIDAndProcessors.remove(getID()+"_"+guards[w]);
	    ret.add(nIDAndProcessors);
	}
	return ret;
    }

    protected ArrayList<Actor> getParallelActors(){
	//Get all the nodes that can potentially run in parallel
	ArrayList<Actor> pNodes = new ArrayList<Actor>();
	for(int e=0;e<getConnectionCount();++e){
	    if(getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")!=null)
		    pNodes.add((Actor)le.getTarget());
	    }
	}
	return pNodes;
    }
    protected boolean isBuildParallel()throws Exception{
	boolean ret = false;
	for(int e=0;e<getConnectionCount();++e){
	    if(getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")!=null){
		    ret= ((GXLString)le.getAttr("parallelEdge").getValue()).getValue().equals("true")?true:false;
		    break;
		}
	    }
	}
	return ret;
    }
    private String[] removeRepetition(String list[]){
	ArrayList<String> ret = new ArrayList<String>();
    	for(Object i : list){
	    boolean doit =true;
	    for(Object j : ret){
		if(i.equals(j)){doit = false; break;}
	    }
	    if(doit)
		ret.add((String)i);
    	}
	String p[] = new String[0];
    	return ret.toArray(p);
    }
    private ArrayList<Actor> pNodes = new ArrayList<Actor>();
    private void buildSourceTemplate(StringBuffer tb, String source, String dest, String chanName, String sendName,
				     int r, int count, String osource, CHOICE C){

	 //I need to find the guards and
	//updates of the not just the source actor, but also the
	//destination actor. Please note that the destination actor,
	//might also possibly be a communication actor with same guard
	//label and multiple different update labels, which makes the
	//thing a bit more difficult to handle. Thus, if the source is a
	//comm actor then the transBuildPar should be called in a loop.
	
	//First get all the parallel actors.
	Actor sNode = null;
	if(pNodes.isEmpty())
	    pNodes = getParallelActors();
	//Now if the source is me myself, no problemo, I have the guards and updates sorted out
	if(osource.split("_")[0].equals(getID())) sNode=this;
	//Else get the node that matches the source
	else{
	    for(Actor i : pNodes){
		if(i.getID().equals(osource.split("_")[0])){sNode=i; break;}
	    }
	}

	// Find the guards and updates that this
	//source actor needs
	String guards [] = ((GXLString)sNode.getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
	String updates [] = ((GXLString)sNode.getAttr("__update_labels_with_processors").getValue()).getValue().split(";");
	if(guards.length != updates.length) throw new RuntimeException();
	ArrayList<String>gguards = new ArrayList<String>();
	ArrayList<String> gupdates = new ArrayList<String>();
	ArrayList<Integer> gnums = new ArrayList<Integer>();
	for(int w=0;w<guards.length;++w){
	    String temp = guards[w].split(",")[0].split("\\$")[1];
	    if(temp.equals(osource.split("_")[1])){
		gguards.add(guards[w]); gnums.add(w);
		gupdates.add(updates[w]);
	    }
	}
	if(gguards.size()!=gupdates.size()) throw new RuntimeException();
	//There is a loop here, because of communication actors. Now I
	//know, which guards and update labels are there
	for(int w=0;w<gguards.size();++w){
	    //Making the template for the source node
	    tb.append("<!-- template for execution node "+source+"_"+r+"_"+(count+(++mCount))+"-->\n");
	    tb.append("<template>\n");
	    //Give this god-damn thing a name
	    tb.append("<name>"+source+"_"+r+"_"+(count+(mCount))+"</name>\n");
	    //Put the declarations
	    tb.append("<declaration/>\n");// This does not have any declaration
	    //End the declaration
	    transBuildPar(tb,getID(),gguards.get(w).split(","),gupdates.get(w).split(","),C,false,
			  chanName,sendName,("C"+getID()+(w+1)));
	    //End the template declaration
	    tb.append("</template>\n");
	}
    }
    protected void buildParallelSystem(StringBuffer sb, ArrayList<String> list, int count){
	//Now just make the names here
	for(int r=0;r<list.size();++r){
	    String temp[] = list.get(r).split("-");
	    for(int e=0;e<temp.length;++e){
		String source = temp[e];
		String osource = temp[e];
		//First get all the parallel actors.
		Actor sNode = null;
		if(pNodes.isEmpty())
		    pNodes = getParallelActors();
		//Now if the source is me myself, no problemo, I have the guards and updates sorted out
		if(osource.split("_")[0].equals(getID())) sNode=this;
		//Else get the node that matches the source
		else{
		    for(Actor i : pNodes){
			if(i.getID().equals(osource.split("_")[0])){sNode=i; break;}
		    }
		}
		source = source.replace(':','_');
		// Find the guards and updates that this
		//source actor needs
		String guards [] = ((GXLString)sNode.getAttr("__guard_labels_with_processors").getValue()).getValue().split(";");
		ArrayList<String>gguards = new ArrayList<String>();
		for(int w=0;w<guards.length;++w){
		    String temp1 = guards[w].split(",")[0].split("\\$")[1];
		    if(temp1.equals(osource.split("_")[1])){
			gguards.add(guards[w]);
		    }
		}
		//There is a loop here, because of communication actors. Now I
		//know, which guards and update labels are there
		for(int w=0;w<gguards.size();++w){
		    // tb.append("<name>"+source+"_"+r+"_"+(count+(mCount))+"</name>\n");
		    sb.append("S"+source+"_"+r+"_"+(count+(++mCount2))+"="+source+"_"+r+"_"+(count+(mCount2))+"();\n");
		    names+=",S"+source+"_"+r+"_"+(count+(mCount2));
		}
	    }
	}
    }
    protected void buildParallelTemplate(StringBuffer tb, ArrayList<String> list, int count){
	//Build the template first
	for(int r=0;r<list.size();++r){
	    String temp[] = list.get(r).split("-");
	    String source=null,dest=null,chanName=null,sendName=null,osource=null;
	    String prevChanName=null;
	    for(int e=0;e<temp.length;++e){
		source = temp[e];
		osource = source;
		source = source.replace(':','_');
		prevChanName=chanName;
		if(e < temp.length-1){
		    dest = temp[e+1];
		    dest = dest.replace(':','_');
		    //Channel name is:
		    chanName = "chan_"+source+"_"+dest+"_"+r+"_"+count;
		}
		if(e == 0) {
		    sendName = "send_"+source+"_"+dest+"_"+r+"_"+count;
		    //Every node needs a separate template
		    buildSourceTemplate(tb,source,dest,chanName,sendName,r,count,osource,Actor.CHOICE.valueOf("PARS"));
		}
		else if(0<e && e<(temp.length-1)){
		    //Here we need to send two channel names one that is
		    //the receiver and the other that is the sender.
		    buildSourceTemplate(tb,source,dest,prevChanName+"?"+chanName,sendName,r,count,osource,Actor.CHOICE.valueOf("PARM"));
		}
		else if(e == temp.length-1){
		    buildSourceTemplate(tb,source,dest,prevChanName,sendName,r,count,osource,Actor.CHOICE.valueOf("PARR"));
		}
	    }
	}
    }
    protected void buildParallelGlobal(StringBuffer gb, ArrayList<String> list,int count){
	//Making the channels
	for(int r=0;r<list.size();r++){
	    String temp[] = list.get(r).split("-");
	    //Minimum you will have atleast 2 running in parallel
	    //Do for the first time
	    String Source = temp[0];
	    Source = Source.replace(':','_');
	    String Dest = temp[1];
	    Dest = Dest.replace(':','_');
	    //This the variable that is used to send data via channels
	    //for this parallel system.
	    gb.append("//Making the sending variable for the channels\n");
	    gb.append("int send_"+Source+"_"+Dest+"_"+r+"_"+count+";\n");
	    gb.append("//Making channels for node "+getID()+"\n");
	    gb.append("urgent chan ");
	    for(int e=0;e<temp.length;++e){
		if(e == temp.length-1) break;
		String source = temp[e];
		source = source.replace(':','_');
		String dest = temp[e+1];
		dest = dest.replace(':','_');
		gb.append("chan_"+source+"_"+dest+"_"+r+"_"+count);
		if(e < temp.length-2)
		    gb.append(", ");
	    }
	    gb.append(";\n");
	}
    }
}
