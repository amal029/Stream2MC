package org.IBM;
/**
 * This class the top level class for the creation of the model checking
 * code
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-03-07
 */
import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import java.lang.reflect.*;
import java.lang.Class;

public class createMcModel {
    public createMcModel(){}
    /**
     * This is the first compiler stage
     * @author Avinash Malik
     * @date Tue Mar  8 14:56:34 GMT 2011
     * @param .gxl file names to parse
     */
    private String [] stage1 (String args[]){
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
    //These should be written as a plugin, so that one can load them
    //dynamically.
    private String [] stage2(String args[],String fnames[]){
	String rets[] = new String[args.length];
    	try{
    	    HashMap<String,streamGraph> graphs = new streamGraphParser().parse(args);
	    for(int e=0;e<args.length;++e){
    		streamGraph sGraph = graphs.get(args[e]);
		//Write the file out onto the disk for stage2 processing
		File f = new File(fnames[e]);
		File dir = new File("./output");
		if(!dir.exists())
		    dir.mkdir();
		sGraph.getDocument().write(new File("./output","__stage2__"+f.getName()));
		rets[e] = "./output/__stage2__"+f.getName();
	    }
    	}
    	catch(SAXException se){se.printStackTrace();}
    	catch(IOException e){e.printStackTrace();}
    	catch(Exception e){e.printStackTrace();}
	return rets;
    }
    private static String tokenize(String arg){
	StringTokenizer token = new StringTokenizer(arg,"=");
	token.nextToken();
	return token.nextToken();
    }
    private static int[] getStages(String arg){
	String val = tokenize(arg);
	StringTokenizer token = new StringTokenizer(val,",");
	int ret[] = new int[token.countTokens()];
	int i=0;
	while(token.hasMoreTokens())
	    ret[i++] = new Integer(token.nextToken()).intValue();
	return ret;
    }
    private static String[] getStageClassNames(String arg){
	String val = tokenize(arg);
	StringTokenizer token = new StringTokenizer(val,",");
	String[] ret = new String[token.countTokens()];
	int i=0;
	while(token.hasMoreTokens())
	    ret[i++] = token.nextToken();
	return ret;
    }
    public static void main(String[] args) {

	//Need to put in dynamic class loading looking at the
	try{
	    if(args.length != 2){
		System.out.println("Usage: org.IBM.createMcModel -DcompilerStageFiles=<abs-class-names> <gxl-file-names-to-act-on>");
		System.exit(1);
	    }
	    //Now do processing
	    String stageClassNames[] = getStageClassNames(args[0]);
	    createMcModel model = new createMcModel();
	    String a[] = null, b[] = null;
	    a = addArgs(args);
	    //Load the compiler stages dynamically.

	    compilerStage compilerStages[] = new compilerStage[stageClassNames.length];
	    Method applyMethod[] = new Method[stageClassNames.length];
	    ClassLoader myloader = ClassLoader.getSystemClassLoader();
	    for(int e=0;e<stageClassNames.length;++e){
		compilerStages[e] = (compilerStage)myloader.loadClass(stageClassNames[e]).newInstance();
		applyMethod[e] = compilerStages[e].getClass().getMethod("applyMethod",new Class[]{String[].class,String[].class});
	    }
	    //Start the compiler stages
	    for(int w=0;w<applyMethod.length;++w){
		if(w ==0)
		    b  = (String[])applyMethod[w].invoke(compilerStages[w],new Object[]{a,b});
		else
		    b  = (String[])applyMethod[w].invoke(compilerStages[w],new Object[]{b,a});
	    }
	}
	catch(Exception e){e.printStackTrace();}
    }
    private static String[] addArgs(String args[]){
	String a[] = new String[args.length-1];
	int c=0;
	for(int e=1;e<args.length;++e){
	    a[c] = args[e];
	    ++c;
	}
	return a;
    }
}
