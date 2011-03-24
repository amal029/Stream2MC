/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-14 

 * This the compiler stage that produces the XML file that is then fed into
 * the Uppaal model checker.
 */
package org.IBM;
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class compilerStage4 implements compilerStage{
    public compilerStage4 () {}
    private static StringBuffer globalDeclarations = new StringBuffer();
    private static StringBuffer systemDeclaration = new StringBuffer();
    private static StringBuffer templateDeclaration = new StringBuffer();
    /**
       @author Avinash Malik
       @date Fri Mar 18 13:31:15 GMT 2011


       This is the tail recursive method that build the final xml file from the
       stream graph for verification.
     */
    private static void buildBackEndForUppaal(Actor sNode)throws Exception{
	if(sNode.ifVisited()) return;
	//Make the single sequential state machine
	//Put the required 
	sNode.buildGlobals(globalDeclarations);
	//Build the template
	sNode.buildTemplate(templateDeclaration);
	//Build the system declaration
	names = sNode.buildSystem(systemDeclaration);
	//Now do the same for parallelism extraction from the system
	if(sNode.isBuildParallel())
	    sNode.buildParallel(globalDeclarations,templateDeclaration,systemDeclaration);
	sNode.setVisited();
	for(int e=0;e<sNode.getConnectionCount();++e){
	    if(sNode.getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)sNode.getConnectionAt(e).getLocalConnection();
		if(le.getAttr("parallelEdge")==null){
		    Actor node = (Actor)le.getTarget();
		    buildBackEndForUppaal(node);
		}
	    }
	}

    }
    private static String names = "";
    public String[] applyMethod(String args[],String fNames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Make the backend
		//This is the final String Buffer being written
		StringBuffer finalBuffer = new StringBuffer();
		finalBuffer.append("<nta>\n");
		buildBackEndForUppaal(sGraph.getSourceNode());
		//First append the globalDeclaration
		globalDeclarations.append("</declaration>\n");
		finalBuffer.append(globalDeclarations.toString());
		//Second append the template declarations
		finalBuffer.append(templateDeclaration.toString());
		//Finally add the system Declarations
		systemDeclaration.append("system "+names+";</system>\n");
		finalBuffer.append(systemDeclaration.toString());
		//Close the netwroked timed automata
		finalBuffer.append("</nta>");
		//Write the file out onto the disk after stage4 processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__final__"+f.getName()+".xml")));
		out.write(finalBuffer.toString());
		out.close();
		rets[e] = "./output/__final__"+f.getName()+".xml";
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
