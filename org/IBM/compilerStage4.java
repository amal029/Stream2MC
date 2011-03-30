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
    private static StringBuilder globalDeclarations = new StringBuilder();
    private static StringBuilder systemDeclaration = new StringBuilder();
    private static StringBuilder templateDeclaration = new StringBuilder();
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
	//Write the output into a text file
	BufferedWriter outG = new  BufferedWriter(new FileWriter(".tempG",true));
	BufferedWriter outS = new BufferedWriter(new FileWriter(".tempS",true));
	BufferedWriter outT = new BufferedWriter(new FileWriter(".tempT",true));
	outG.write(globalDeclarations.toString());
	outS.write(systemDeclaration.toString());
	outT.write(templateDeclaration.toString());
	outG.flush(); outS.flush(); outT.flush();
	outG.close(); outS.close(); outT.close();
	globalDeclarations = new StringBuilder(1);
	templateDeclaration = new StringBuilder(1);
	systemDeclaration = new StringBuilder(1);
	System.gc();
	if(sNode.isBuildParallel()){
	    outG = new  BufferedWriter(new FileWriter(".tempG",true));
	    outS = new BufferedWriter(new FileWriter(".tempS",true));
	    outT = new BufferedWriter(new FileWriter(".tempT",true));
	    sNode.buildParallel(globalDeclarations,templateDeclaration,systemDeclaration);
	    outG.write(globalDeclarations.toString());
	    outS.write(systemDeclaration.toString());
	    outT.write(templateDeclaration.toString());
	    outG.flush(); outS.flush(); outT.flush();
	    outG.close(); outT.close(); outS.close();
	    globalDeclarations = new StringBuilder(1);
	    templateDeclaration = new StringBuilder(1);
	    systemDeclaration = new StringBuilder(1);
	    System.gc();// have to call the gc, because it's very memory consuming
	}
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
		//Delete the .temp files
		File fifi = new File(".tempG");
		if(fifi.exists())
		    fifi.delete();
		fifi = new File(".tempS");
		if(fifi.exists())
		    fifi.delete();
		fifi = new File(".tempT");
		if(fifi.exists())
		    fifi.delete();
		StringBuilder finalBuffer = new StringBuilder();
		finalBuffer.append("<nta>\n");
		buildBackEndForUppaal(sGraph.getSourceNode());
		//First append the globalDeclaration
		BufferedReader read = new BufferedReader(new FileReader(".tempG"));
		String sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		finalBuffer.append("</declaration>\n");
		// finalBuffer.append(globalDeclarations.toString());
		//Second append the template declarations
		read = new BufferedReader(new FileReader(".tempT"));
		sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		// finalBuffer.append(templateDeclaration.toString());
		//Finally add the system Declarations
		read = new BufferedReader(new FileReader(".tempS"));
		sread = null;
		while((sread = read.readLine()) != null)
		    finalBuffer.append(sread+"\n");
		read.close();
		finalBuffer.append("system "+names+";</system>\n");
		// finalBuffer.append(systemDeclaration.toString());
		//Close the netwroked timed automata
		finalBuffer.append("</nta>");
		System.out.print(".......");
		//Write the file out onto the disk after stage4 processing
		File f = new File(fNames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		BufferedWriter out = new BufferedWriter(new FileWriter(new File("./output","__final__"+f.getName()+".xml")));
		out.write(finalBuffer.toString());
		out.close();
		rets[e] = "./output/__final__"+f.getName()+".xml";
		System.out.print("Done Compiling");
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
}
