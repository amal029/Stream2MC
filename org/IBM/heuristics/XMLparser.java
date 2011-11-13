/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-10
 */
package org.IBM.heuristics;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;
import org.IBM.heuristics.lib.*;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;

public class XMLparser implements compilerStage {
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
	    File dir = new File("./output");
	    if(!dir.exists())
		dir.mkdir();
	    for(int e=0;e<args.length;++e){
		File f = new File(args[e]);
		SAXBuilder builder = new SAXBuilder(); 
		//DEBUG
		// System.out.println(f);
		Document doc = builder.build(f);

		//Print the document to standard output
		//DEBUG
		// printAllThingsUppaal(doc.getRootElement());
		
		GuardHash<String,ArrayList<state>> map = buildGuardMap(doc.getRootElement());
		//DEBUG
		// Set<String> list = map.keySet();
		// for(String guard : list){
		//     ArrayList<state> states = (ArrayList<state>)map.get(guard);
		//     System.out.print(guard+"--->");
		//     for(state s : states){
		// 	System.out.print(s.getID()+" ");
		//     }
		//     System.out.println();
		// }
		
		//Now we can set the update actions or more formally, we
		//can set the next state machines that will be excited
		//by this state.
		List<state> startingStates = setActions(map);

		//DEBUG
		// System.out.print("The starting states are: ");
		// for(state s : startingStates)
		//     System.out.print(s.getID()+" ");
		// System.out.println();
		
		/**Start breadth first search***/
		BFS bfs = new BFS(f,startingStates); //it starts on its own

	    }
	}
	catch(Exception e){e.printStackTrace();}
	return null;
    }
    
    //This method sets the update guards (and states) for all the
    //states. This method gives a list of the starting nodes. After this
    //method the map becomes useless
    private static List<state> setActions(GuardHash<String,ArrayList<state>> map){
	List<state> ret = new ArrayList<state>();
	addStartingStates(ret,map);
	setUpdateStates(map);
	
	return ret;
    }
    
    private static void setUpdateStates(GuardHash<String,ArrayList<state>> map){
	Collection<ArrayList<state>> values = map.values();
	Iterator<ArrayList<state>> iter = values.iterator();
	while(iter.hasNext()){
	    ArrayList<state> sl = iter.next();
	    //Now get its update guard
	    for(state s : sl){
		ArrayList<String> updateGuards = s.getUpdateGuards();
		for(String st : updateGuards){
		    //find the state list attached to this guard
		    if(map.containsKey(st)){
			ArrayList<state> updates = (ArrayList<state>)map.get(st); //this should always work
			s.setUpdateStates(updates);
			
			//Now set the parents for the update states
			for(state up : updates){
			    up.addParent(s);
			}
		    }
		    else throw new RuntimeException("The update state is not in the map for guard: "+st+ " and state: "+
						    s.getID());
		}
	    }
	}
    }
    
    private static void addStartingStates(List<state> ret, GuardHash<String,ArrayList<state>> map){
	Collection<ArrayList<state>> values = map.values();
	Iterator<ArrayList<state>> iter = values.iterator();
	while(iter.hasNext()){
	    ArrayList<state> sl = iter.next();
	    for(state s : sl){
		if(s.getID().startsWith("dummyStartNode")){
		    ret.add(s);
		}
	    }
	}
    }
    
    private static void printAllThingsUppaal(Element root){
	List<Element> children = new ArrayList<Element>();
	//print the text in this shitty thing
	System.out.println("<"+root.getName()+">");
	System.out.println(root.getTextTrim());
	children = root.getChildren();
	Iterator iter = children.iterator();
	while(iter.hasNext()){
	    Element child = (Element) iter.next();
	    printAllThingsUppaal(child);
	}
	System.out.println("</"+root.getName()+">");

    }
    
    //Get all the nodes in the state graph
    private static GuardHash<String,ArrayList<state>> buildGuardMap(Element root){
	List<Element> children = new ArrayList<Element>();
	children = root.getChildren("template");
	Iterator<Element> iter = children.iterator();
	GuardHash<String,ArrayList<state>> map = new GuardHash<String,ArrayList<state>>();
	String declaration = root.getChild("declaration").getTextTrim();
	//DEBUG
	// int templateCount = 0;
	while(iter.hasNext()){
	    Element child = iter.next();
	    makeGuardElement(child,map,declaration);
	    //DEBUG
	    // ++templateCount;
	}
	//DEBUG
	// System.out.println("Total states: "+templateCount);
	return map;
    }
    
    private static void makeGuardElement(Element element, GuardHash<String,ArrayList<state>> map,
					 String declaration){
	//Name of the state
	state s = new state(element.getChild("name").getTextTrim());
	
	
	//Now get the guard name Even if there are multiple transitions
	//in a state machine, jsut looking at the first one should be
	//enough for getting the guards.
	List<Element> children = element.getChild("transition").getChildren("label");
	for(Element child : children){
	    List<Attribute> attrs = child.getAttributes();
	    for(Attribute attr : attrs){
		if(attr.getValue().equals("guard")){
		    //This can be a joiner with multiple guards
		    //DEBUG
		    // System.out.print("State "+s.getID()+" ");

		    ArrayList<String> guards = getAllGuards(child.getTextTrim());
		    for(String g : guards)
			s.setGuardName(g);
		    //DEBUG
		    // System.out.println("Setting guard for state "+s.getID()+" "+child.getTextTrim().split("=")[0]);
		    //Add to the map
		    addToMap(s,map); //FIXME
		}
	    }
	}

	//Set myCost
	float f = 0f;
	if((f = getStateCost(declaration,element,s))!=-1)
	    s.setCost(f);
    }
    
    //This is an expensive method
    private static float getStateCost(String cDeclaration, Element element, state s){
	float ret = 0f;
	
	String sName = element.getChild("name").getTextTrim();

	//The cost of the state is always C<sName>. It is also possible
	//that you don't know what is the cost value, which happens in
	//case of rendezvous transitions.
	Scanner scan = new Scanner(cDeclaration);
	//DEBUG
	boolean done = false;
	while(scan.hasNextLine()){
	    String line = scan.nextLine();
	    if(line.startsWith("int")){
		String temp[] = line.split(" ")[1].split("=");
		// System.out.println(sName);
		if(temp[0].equals("C"+sName)){
		    ret = Float.valueOf(temp[1].split(";")[0].trim()).floatValue();
		    done = true;
		    values.put("C"+sName,ret);
		    //DEBUG
		    // if(sName.equals("CdummyStartNode1"))
		    // 	System.out.println("Setting node "+sName+" cost: "+ret);
		    break;
		}
	    }
	}

	//This is crap code --> can be made much much better
	if(done){
	    //set the guard updates
	    List<Element> children = element.getChild("transition").getChildren("label");
	    for(Element tutu : children){
		if(tutu.getAttributeValue("kind").equals("assignment")){
		    s.setUpdateGuards(getUpdateGuard(tutu.getTextTrim()));
		    break;
		}
	    }
	}
	else if(!done){
	    //DEBUG
	    // System.out.println("These nodes could not be allocated a cost: "+sName);

	    //These have to be synchronization channels
	    //Either senders or receivers

	    //See if this node is a sender?

	    //There can be more than one transitions, when there are
	    //more than 3 processors and this parallel can be exploited.
	    Element sync = null;
	    Element assign = null;
	    List<Element> trans = element.getChildren("transition");
	    List<Element> children = new ArrayList<Element>();

	    //How many transition does this state have??
	    s.setNumTransitions(trans.size());

	    for(Element t : trans){
		List<Element> chl = t.getChildren("label");
		for(Element chle : chl){
		    children.add(chle);
		}
		for(Element child : children){
		    List<Attribute> attrs = child.getAttributes();
		    for(Attribute attr : attrs){
			if(attr.getValue().equals("synchronisation"))
			    sync = child;
			else if(attr.getValue().equals("assignment"))
			    assign = child;
		    }
		}
		if(isWhat(sync,'!')){
		    //get the sender value
		    ret = getSenderValue(assign,cDeclaration);
		    s.setType("sender");
		}
		else if(isWhat(sync,'?')){
		    ret = getReceiverValue(assign,cDeclaration);
		    s.setType("receiver");
		}
		else throw new RuntimeException("Neither sender or receiver!!");

		//Set the channel names
		s.setChannelName(sync.getTextTrim());
		//set the update guard for this state
		s.setUpdateGuards(getUpdateGuard(assign.getTextTrim()));
	    }
	    

	    //Get the partner
	    s.setPartner(null// getPartner(element.getChild("declaration"))
			 );
	}
	return ret;
    }
    
    private static ArrayList<String> getUpdateGuard(String text){
	ArrayList<String> ret = new ArrayList<String>();
	String temp[] = text.split(",");
	for(String g : temp){
	    if(g.split("=").length > 1){
		if(g.split("=")[1].equals("1")){
		    //This means it is an update guard
		    ret.add(g.split("=")[0]);
		}
	    }
	}
	return ret;
    }
    
    private static float getSenderValue(Element assign,String cDeclaration){
	float ret = 0f;
	//I know that this is a sender
	String text = assign.getTextTrim();
	String temp[] = text.split(",");
	//Find the sender
	for(String s : temp){
	    if(s.startsWith("send")){
		//This is the one
		String t[] = s.split("=");
		String sName = t[1].trim();
		boolean done = false;

		//DEBUG
		if(sName.startsWith("send")){
		    // System.out.println("sender the cost: "+sName+" and not bothering");
		    ret= -1f;
		    break;
		}
		else
		    // System.out.println("Tyring to find for sender the cost: "+sName);

		//Search in the hash map, it should always be there
		if(values.containsKey(sName)){
		    //Now get the value and put it into the hash map as well
		    ret = values.get(sName).floatValue();
		    done=true;
		}
		else{
		    Scanner scan = new Scanner(cDeclaration);
		    //DEBUG
		    while(scan.hasNextLine()){
			String line = scan.nextLine();
			if(line.startsWith("int")){
			    String temp2[] = line.split(" ")[1].split("=");
			    if(temp2[0].equals(sName)){
				ret = Float.valueOf(temp2[1].split(";")[0].trim()).floatValue();
				done = true;
				break;
			    }
			}
		    }
		}
		if(!done)
		    throw new RuntimeException("Value of "+sName+" cannot be found");
		//add the s to the hashmap with this value
		values.put(t[0].trim(),ret);
		//DEBUG
		// System.out.println("Added to the map: "+t[0].trim()+"-->"+ret);
	    }
	}
	
	return ret;
    }
    
    private static float getReceiverValue(Element assign, String cDeclaration){
	float ret = 0f;
	//I know that this is a receiver
	String text = assign.getTextTrim();
	String temp[] = text.split(",");
	//Find the receiver
	for(String s : temp){
	    if(s.startsWith("tCost")|| s.startsWith("send")){
		//This is the one
		String sName[] = null;
		if(s.startsWith("tCost")){
		    String t[] = s.split("\\+=");
		    //There will always be two sNames
		    sName = t[1].trim().split("\\?")[0].split(">");
		}
		else if(s.startsWith("send")){
		    String t[] = s.split("=");
		    sName = t[1].trim().split("\\?")[0].split(">");
		}
		for(String sn : sName){
		    boolean done = false;
		    // System.out.println("Tyring to find for receiver the cost: "+sn);
		    //Search in the hash map, it should always be there
		    if(values.containsKey(sn)){
			//Now get the value and put it into the hash map as well
			ret = values.get(sn).floatValue();
			done = true;
		    }
		    else{
			    Scanner scan = new Scanner(cDeclaration);
			    while(scan.hasNextLine()){
				String line = scan.nextLine();
				if(line.startsWith("int")){
				    String temp2[] = line.split(" ")[1].split("=");
				    if(temp2[0].equals(sn)){
					ret = Float.valueOf(temp2[1].split(";")[0].trim()).floatValue();
					done = true;
					break;
				    }
				}
			    }
			}
		    if(!done)
			throw new RuntimeException("Value of "+sName+" cannot be found");
		    //add the s to the hashmap with this value
		    values.put(sn,ret);
		    //DEBUG
		    // System.out.println("Added to the map: "+sn+"-->"+ret);
		}
	    }
	}
	return ret;
    }
    
    private static HashMap<String,Float> values = new HashMap<String,Float>();
    
    private static boolean isWhat(Element sync, char match){
	String text = sync.getTextTrim();
	//DEBUG
	// System.out.println(text.trim().charAt(text.length()-1));
	if(text.trim().charAt(text.trim().length()-1) == match){
	    return true;
	}
	else
	    return false;
    }
    
    //This method adds the state to the map. If the key is the same then
    //the subsequent elements are added to the same array list
    private static void addToMap(state s, GuardHash<String,ArrayList<state>> map){
	ArrayList<String> guards = s.getGuards();
	for(String guard : guards){
	    String temp = guard.trim(); //guard1_guard2...
	    //This means we have already set one of the nodes previously
	    if(map.containsKey(temp)){
		ArrayList<state> states = (ArrayList<state>)map.get(temp);
		states.add(s);
	    }
	    else{
		ArrayList<state> states = new ArrayList<state>();
		states.add(s);
		map.put(temp,states);
	    }
	}
    }
    
    private static ArrayList<String> getAllGuards(String text){
	//DEBUG
	// System.out.println("Searching for guards in: "+text);

	ArrayList<String> ret = new ArrayList<String>();
	String temp[] = text.split("and");
	for(String g : temp){
	    //DEBUG
	    // System.out.println("Setting guards: "+g.split("=")[0]);
	    ret.add(g.split("==")[0]);
	}
	return ret;
    }
}
