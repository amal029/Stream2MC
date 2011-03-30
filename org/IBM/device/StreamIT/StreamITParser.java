/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-28
 */
package org.IBM.device.StreamIT;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

/**
   @author Avinash Malik 

   This class is responsible for translating the gxl files compiled by
   streamIT compiler into something that is recongnizable by the MC
   translator.
   
   @note The work-after-partition.txt file and the gxl file should have
   the same name and should be in the same directory.
   
   @param The GXLGraph g
   @return The new GXLGraph g
 */
public class StreamITParser {
    public StreamITParser () {}
    private HashMap<String,String> map = null;
    private static void getAllEdges(GXLGraph g,ArrayList<GXLEdge> edges){
	for(int e=0;e<g.getGraphElementCount();++e){
	    if(g.getGraphElementAt(e) instanceof GXLEdge)
		edges.add((GXLEdge)g.getGraphElementAt(e));
	    if(g.getGraphElementAt(e) instanceof GXLNode){
		if(((GXLNode)g.getGraphElementAt(e)).getGraphCount()>0){
		    for(int y=0;y<((GXLNode)g.getGraphElementAt(e)).getGraphCount();++y){
			getAllEdges(((GXLNode)g.getGraphElementAt(e)).getGraphAt(y),edges);
		    }
		}
	    }
	}
    }
    private static int count =0;
    private static String removePart(String s){
	String v[] = s.split("_");
	String ret = "";
	for(int e=0;e<v.length-1;++e){
	    ret += v[e];
	    ret += e <v.length-2?"_":"";
	}
	return ret;
    }
    public GXLGraph parse(GXLGraph g, String fileName) throws Exception{
	//First parse the work-after-partition.txt file
	if(map == null)
	    map = new workFileParser(fileName).parse();
	//Now put the rate and rep and insert the communication actors
	//into the gxl
	/*
	  TODO

	  1.) Add rep to the gxl nodes (execution nodes) looking at
	  work-after-partition.txt file

	  2.) Add unit_time_x86 and total_time_x86 to execution and
	  computation nodes For execution nodes: unit_time_x86 =
	  unit_work total_time_x86 = total_work (total_work =
	  unit_work*rep).

	  3.) Insert communication actor nodes in the graph.

	  a.) Every execution actor is followed by a communication
	  actor.

	  b.) Every communication actor has a "rate" attribute, which
	  the execution actor should never have. The rate equal to the
	  number of bytes produced for every invocation of the source
	  execution actor. Rate is also obtained from the
	  work-after-partition.txt file.
	 */
	int count = g.getGraphElementCount(); //Total number of nodes and edges
	ArrayList<GXLEdge> edges = new ArrayList<GXLEdge>();
	getAllEdges(g,edges);
	GXLIDGenerator gen = new GXLIDGenerator(g.getDocument());
	//The new GXLGraph that I will be sending back
	GXLGraph ret = new GXLGraph(gen.generateGraphID());

	GXLEdge e = null;
	String name = null, name2= null;
	ArrayList<GXLNode> toRemoveSource = new ArrayList<GXLNode>();
	ArrayList<GXLNode> toRemoveTarget = new ArrayList<GXLNode>();
	ArrayList<GXLEdge> toRemoveEdge = new ArrayList<GXLEdge>();
	ArrayList<GXLEdge> toRemoveEdgeSpecial = new ArrayList<GXLEdge>();
	for(int r=0;r<edges.size();++r){
	    boolean thereS =false, thereT=false;
	    e = edges.get(r);
	    String sNameO = ((GXLString)((GXLNode)e.getSource()).getAttr("label").getValue()).getValue();
	    String tNameO = ((GXLString)((GXLNode)e.getTarget()).getAttr("label").getValue()).getValue();
	    sNameO = sNameO.replace('\\',':');
	    sNameO = removePart(sNameO.split(":")[0]);
	    sNameO = sNameO+":rep";
	    tNameO = tNameO.replace('\\',':');
	    tNameO = removePart(tNameO.split(":")[0]);
	    tNameO = tNameO+":rep";
	    try{
		if(map.get(sNameO)==null && map.get(tNameO)==null)
		    throw new NullPointerException();
		if(map.get(sNameO) != null){
		    thereS = true;
		    name = sNameO.split(":")[0];
		}
		if(map.get(tNameO)!= null){
		    thereT = true;
		    name2=tNameO.split(":")[0];
		}
	    }
	    catch(NullPointerException ne){
		thereS= thereT = false;
	    }
	    if(!thereS && !thereT){
		//This means this edge lies between a merge or split
		//nodes
		GXLNode source = (GXLNode)e.getSource();
		source.setAttr("rep",new GXLString("1"));
		source.setAttr("unit_time_x86",new GXLString("0"));
		source.setAttr("total_time_x86",new GXLString("0"));
		GXLNode target = (GXLNode)e.getTarget();
		target.setAttr("rep",new GXLString("1"));
		target.setAttr("unit_time_x86",new GXLString("0"));
		target.setAttr("total_time_x86",new GXLString("0"));
		toRemoveTarget.add(target);
		toRemoveSource.add(source);
		toRemoveEdgeSpecial.add(e);
	    }
	    if(thereS){
		//Then add the "rep", unit_time_x86, and total_time_x86
		GXLNode source = (GXLNode)e.getSource();
		source.setAttr("rep",new GXLString(map.get(name+":rep")));
		source.setAttr("unit_time_x86",new GXLString(map.get(name+":unit_work")));
		source.setAttr("total_time_x86",new GXLString(map.get(name+":total_work")));
		source.remove(e);
		e.remove(source);
		//Get the target
		GXLNode target = (GXLNode)e.getTarget();
		target.remove(e);
		e.remove(target);
		//Add a communication node after source node
		GXLNode commNode = new GXLNode(gen.generateEdgeID());
		commNode.setID(commNode.getID()+count); ++count;
		GXLEdge edgeC1 = new GXLEdge(source,commNode);
		GXLEdge edgeC2 = new GXLEdge(commNode,target);
		edgeC1.setDirected(true);
		edgeC2.setDirected(true);
		ret.add(commNode);
		ret.add(edgeC2);
		ret.add(edgeC1);
		//Set the commNode attributes
		commNode.setAttr("rep",new GXLString("1"));
		commNode.setAttr("rate",new GXLString(map.get(name+":out_bytes")));
		commNode.setAttr("sourceActorRate",new GXLString(map.get(name+":rep")));
		commNode.setAttr("work_x86",new GXLString("0"));
		//Remove the edge from the edges arraylist
		edges.remove(r);--r;
		//Finally, if the target is a split or join, then add
		//the rep Attr to it
		//Checking if target is split or join
		String tName = ((GXLString)target.getAttr("label").getValue()).getValue();
		tName = tName.replace('\\',':');
		if(tName.split(":")[1].equals("nwork=null")){
		    //Then 
		    target.setAttr("rep",new GXLString("1"));
		    target.setAttr("unit_time_x86",new GXLString("0"));
		    target.setAttr("total_time_x86",new GXLString("0"));
		}
		toRemoveTarget.add(target);
		toRemoveSource.add(source);
		toRemoveEdge.add(e);
	    }
	    if(thereT){
		GXLNode target = (GXLNode)e.getTarget();
		target.setAttr("rep",new GXLString(map.get(name2+":rep")));
		target.setAttr("unit_time_x86",new GXLString(map.get(name2+":unit_work")));
		target.setAttr("total_time_x86",new GXLString(map.get(name2+":total_work")));
		target.remove(e);
		e.remove(target);
		if(!thereS){
		    GXLNode source = (GXLNode)e.getSource();
		    source.remove(e);
		    e.remove(source);
		    //Add the new communication node above the target
		    GXLNode commNode = new GXLNode(gen.generateEdgeID());
		    commNode.setID(commNode.getID()+count); ++count;
		    GXLEdge edgeC1 = new GXLEdge(source,commNode);
		    GXLEdge edgeC2 = new GXLEdge(commNode,target);
		    edgeC1.setDirected(true);
		    edgeC2.setDirected(true);
		    ret.add(commNode);
		    ret.add(edgeC2);
		    ret.add(edgeC1);
		    commNode.setAttr("rep",new GXLString("1"));
		    commNode.setAttr("sourceActorRate",new GXLString(map.get(name+":rep")));
		    commNode.setAttr("rate",new GXLString(map.get(name2+":in_bytes")));
		    commNode.setAttr("work_x86",new GXLString("0"));
		    edges.remove(r);--r;
		    //Finally, if the target is a split or join, then add
		    //the rep Attr to it
		    //Checking if source is split or join
		    String tName = ((GXLString)source.getAttr("label").getValue()).getValue();
		    tName = tName.replace('\\',':');
		    if(tName.split(":")[1].equals("nwork=null")){
			source.setAttr("rep",new GXLString("1"));
			source.setAttr("unit_time_x86",new GXLString("0"));
			source.setAttr("total_time_x86",new GXLString("0"));
		    }
		    toRemoveTarget.add(target);
		    toRemoveSource.add(source);
		    toRemoveEdge.add(e);
		}
	    }
	}
	for(GXLNode node: toRemoveSource){
	    node.setAttr("label",new GXLString(node.getID()));
	    GXLElement parent = node.getParent();
	    parent.remove(node);
	    ret.add(node);
	}
	for(GXLNode node: toRemoveTarget){
	    node.setAttr("label",new GXLString(node.getID()));
	    GXLElement parent = node.getParent();
	    parent.remove(node);
	    ret.add(node);
	}
	for(GXLEdge edge : toRemoveEdgeSpecial){
	    GXLElement parent = edge.getParent();
	    parent.remove(edge);
	    ret.add(edge);
	}
	for(GXLEdge edge : toRemoveEdge){
	    GXLElement parent = edge.getParent();
	    parent.remove(edge);
	}
	GXLDocument doc = new GXLDocument();
	doc.getDocumentElement().add(ret);
	doc.write(new File("temp.gxl"));
	return doc.getDocumentElement().getGraphAt(0);
    }
    private class workFileParser{
	private File f=null;
	public workFileParser(String fileName){
	    this.f = new File(fileName.replaceFirst("gxl$","txt"));
	}
	public HashMap<String,String> parse() throws FileNotFoundException, IOException{
	    BufferedReader  br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(f))));
	    br.readLine(); //First one is just waisted as such
	    String str = null;
	    HashMap<String,String> map = new HashMap<String,String>();
	    while((str = br.readLine()) != null){
		String strs[] = str.split("\t");
		//filterName:<attribute>  = <value>
		map.put(strs[0].trim()+":rep",strs[1]);
		map.put(strs[0].trim()+":unit_work",strs[2]);
		map.put(strs[0].trim()+":total_work",strs[3]);
		map.put(strs[0].trim()+":in_bytes",strs[4]);
		map.put(strs[0].trim()+":out_bytes",strs[5]);
	    }
	    return map;
	}
    }
}

