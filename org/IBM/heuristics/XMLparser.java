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
		System.out.println(f);
		Document doc = builder.build(f);

		//Print the document to standard output
		//DEBUG
		printAllThingsUppaal(doc.getRootElement());
		
	    }
	}
	catch(Exception e){e.printStackTrace();}
	return null;
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
}
