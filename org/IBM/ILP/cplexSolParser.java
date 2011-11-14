/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-07-07
 */
package org.IBM.ILP;

import java.io.*;
import java.util.*;
import org.IBM.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

/**
   This class parses the results generated from CPLEX solver. The
   results generated by CPLEX are stored in the XML file. Use the
   following sequence of commands when using CPLEX
   
   read <full/path/to/file.lp>
   mipopt
   write /tmp/file.sol

 */

public class cplexSolParser implements compilerStage{
    public cplexSolParser () {}
    private Element el = null;
    public String[] applyMethod(String args[], String fNames[]){
	String rets[] = null;
	try{
	    
	    SAXBuilder builder = new SAXBuilder();
	    for(int e=0;e<args.length;++e){
		Document doc = builder.build(args[e]);
		el = doc.getRootElement();
		createVarHashMap(el);
	    }
	}
	catch(JDOMException e){e.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
	return rets;
    }
    
    private static HashMap<String,String> varMap = new HashMap<String,String>(5000);

    /**
       This method gets the value of the property. It is a recursive
       function.
       @args property
       @return value
     */
    @SuppressWarnings("unchecked")
    private void createVarHashMap(Element element){
	List<Element> l = element.getChildren();
	while(!l.isEmpty()){
	    Element child = l.remove(0);
	    if(child.getName().equals("variable"))
		varMap.put(child.getAttributeValue("name"),child.getAttributeValue("value"));
	    createVarHashMap(child);
	}
    }
    
    public static String getValue(String name){
	return varMap.get(name);
    }
    
    
    //DEBUG
    // public static void main(String args[]){
    // 	cplexSolParser parser = new cplexSolParser();
    // 	parser.applyMethod(args,null);
	
    // 	//DEBUG....some tests
    // 	System.out.println(parser.getValue("l"));
    // 	System.out.println(parser.getValue("a_dummyStartNode"));
    // 	System.out.println(parser.getValue("b_node2p1"));
    // 	System.out.println(parser.getValue("b_node2p2"));

    // }
}