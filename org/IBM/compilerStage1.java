/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-08
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class compilerStage1 implements compilerStage{
    public compilerStage1 () {
	
    }
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{

    	    HashMap<String,streamGraph> graphs = new streamGraphParser(true).parse(args);
    	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
    		//You have the streamGraph in hand
		//Do a DFT and check the values of the actors
		System.out.println(sGraph.getActorCount());

		//Mark all the mergeNodes
		sGraph.markMergeNodes();
		sGraph.clearVisited(((Actor)sGraph.getSourceNode()));
		//now add guards and updates
		sGraph.addGuardsAndUpdates(((Actor)sGraph.getSourceNode()));
		sGraph.clearVisited(((Actor)sGraph.getSourceNode()));
		sGraph.simpleDFT(((Actor)sGraph.getSourceNode()),
				 new  applyFunctionsToStream(){
				     public void applyFunction(Actor actor){
					 System.out.println(actor.getID()+"\n Guard Labels: "+actor.getGuardlabels()+"\n Update Labels: "+actor.getUpdateLabels());

				     }
				 });
		sGraph.clearVisited(((Actor)sGraph.getSourceNode()));
		//Write the file out onto the disk for stage2 processing
		File f = new File(args[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage1__"+f.getName()));
		rets[e] = "./output/__stage1__"+f.getName();
    	    }
	    
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
