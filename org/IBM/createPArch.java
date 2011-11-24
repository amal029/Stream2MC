package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;


public class createPArch{
    private static StringBuffer readViaSSH(String arg,String fileName,String tempFile){
        String command = "ssh "+arg+" ~/command.sh".toString();
        try{
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String s;
            while((s=stdError.readLine()) != null)
                System.out.println("Error: "+s);
        }
        catch(IOException e){e.printStackTrace();}
        return readFile(tempFile);
    }

    private static StringBuffer readFile(String fileName){
        File file = new File(fileName);
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(file));
            String text = null;

            // repeat until all lines is read
            while ((text = reader.readLine()) != null) {
                contents.append(text)
                    .append(System.getProperty("line.separator"));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return contents;
    }
    private static int isHtOrCoreOrNUMA(ArrayList<String> lp, ArrayList<Integer>pid, ArrayList<Integer>cores, ArrayList<Integer> s, ArrayList<Integer> cpids){
        int currpid = -1;
        int cpid = 0;
        int ret = 0x0000;
        boolean HT=false;
        int ccores =0;
        for(int e=0;e<lp.size();++e){
            if(s.get(e) > cores.get(e)){
                HT=true;
            }
            if(pid.get(e) > currpid){
                currpid=pid.get(e).intValue();
                ++cpid;
                ccores += cores.get(e);
            }
        }
        cpids.add(0,new Integer(cpid));
        if(cpid >= 1 && new File(System.getenv("HOME")+"/.distance0").exists())ret = 0x1; 
        if(ccores>cpid)ret|=0x2;//It is multiple cores within a CPU
        if(HT)ret |= 0x4;//It is Hyper threaded
        return ret;
    }
    private static Integer tokenizeInteger(String s){
        Integer i = null;
        StringTokenizer token = new StringTokenizer(s,":");
        token.nextToken();
        i = new Integer(new StringTokenizer(token.nextToken()," ").nextToken());
        return i;
    }
    private static ArrayList<ArrayList> getInfo(String string){
        Scanner scan = new Scanner(string);
        ArrayList<String> lProcessors = new ArrayList<String>();
        ArrayList<Integer> pID = new ArrayList<Integer>();
        ArrayList<Integer> cID = new ArrayList<Integer>();
        ArrayList<Integer> cores = new ArrayList<Integer>();
        ArrayList<Integer> siblings = new ArrayList<Integer>();
        ArrayList<ArrayList> list = new ArrayList<ArrayList>(10);
        try{
            while(scan.hasNextLine()){
                String temp = scan.nextLine();
                if(temp.startsWith("processor"))
                    lProcessors.add(temp);
                else if(temp.startsWith("physical"))
                    pID.add(tokenizeInteger(temp));
                else if(temp.startsWith("core"))
                    cID.add(tokenizeInteger(temp));
                else if(temp.startsWith("cpu cores"))
                    cores.add(tokenizeInteger(temp));
                else if(temp.startsWith("siblings"))
                    siblings.add(tokenizeInteger(temp));
            }
            list.add(lProcessors); list.add(pID);
            list.add(cID);list.add(cores);list.add(siblings);
        }
        catch(NoSuchElementException e){return list;}
        catch(IllegalStateException ie){ie.printStackTrace();}
        return list;
    }
    private static ArrayList<GXLNode> makeProcessorNodes(ArrayList<String> nodes,
							 String machineName,
							 ArrayList<Integer>pID,ArrayList<Integer>cID){
        ArrayList<GXLNode> list = new ArrayList<GXLNode>(10);
        for(int e=0;e<nodes.size();++e){
            StringTokenizer token = new StringTokenizer(nodes.get(e),":");
            token.nextToken();
            String nodeNum = token.nextToken();
            GXLNode node = new GXLNode(machineName+":"+"logicalProcessor:"+
				       new StringTokenizer(nodeNum," ").nextToken()+":CPU:"+pID.get(e)+":Core:"+cID.get(e));
            if(pID.size() > e && cID.size()>e)
                node.setAttr("label",new GXLString(machineName+":"+"logicalProcessor:"+
						   new StringTokenizer(nodeNum," ").nextToken()+"\\nCPU:"+pID.get(e)+"\\nCore:"+cID.get(e)));
            list.add(node);
        }
        return list;
    }
    private static ArrayList<GXLEdge> makeEdges(ArrayList<GXLNode> nodes, ArrayList<Integer> pID, ArrayList<Integer>cID,
						int[][]mem_access_times,int currMachine){
        /*
         * TODO:
         * 1.) Go through all the nodes and make edges for them
         * Check:
         */
        ArrayList<GXLEdge> list = new ArrayList<GXLEdge>(10);
        for(int r=0;r<nodes.size();++r){
            for(int e=0;e<nodes.size();++e){
                GXLEdge edge = new GXLEdge(nodes.get(r),nodes.get(e));
                list.add(edge);
                //Set the attributes for this edge
                int comm_time = -1;
                if(mem_access_times != null)
                    comm_time = ((mem_access_times[pID.get(r).intValue()][pID.get(r).intValue()]+
				  mem_access_times[pID.get(e).intValue()][pID.get(r).intValue()])+
				 (mem_access_times[pID.get(r).intValue()][pID.get(e).intValue()]+
				  mem_access_times[pID.get(e).intValue()][pID.get(e).intValue()]))/2;
                edge.setAttr("latency",new GXLInt(comm_time));
                edge.setAttr("latency_file",new GXLString("Name:"+cFiles.get(currMachine)+":"+"Type:ini:Parser:org.IBM.iniParser"));
                edge.setAttr("label",new GXLString("NUMA latency: "+(comm_time==-1?0:comm_time)+"\\nCore latency, check file: "+cFiles.get(currMachine)));
            }
        }
        return list;
    }
    /***
	TODO:
	1.) We need to know the number of cores within this machine the graph is a 
	flat graph
	We have a look at it hierarchically:
	a.) Look at the number of CPUs in the machine
	b.) We look at the number of cores within each CPU
	c.) Finally, we look at the number of hyper threads within the each core
    **/
    @SuppressWarnings("unchecked")
        private static GXLGraph completeMachineGraph(GXLGraph machine, String arg,String machineName,int currMachine){
	StringBuffer procFileBuffer = readViaSSH(arg,"/proc/cpuinfo",System.getenv("HOME")+"/.cpuinfo");
	ArrayList<ArrayList> infoList = getInfo(procFileBuffer.toString());
	ArrayList<String> logicalProcessors = infoList.get(0); //First is the set of logicalProcessors
	ArrayList<Integer> pID = infoList.get(1);
	ArrayList<Integer> cID = infoList.get(2);
	ArrayList<Integer> cores = infoList.get(3);
	ArrayList<Integer> siblings = infoList.get(4);
	ArrayList<GXLNode> nodes = makeProcessorNodes(logicalProcessors,machineName,pID,cID);
	ArrayList<Integer> cpids = new ArrayList<Integer>(1);
	int ret = isHtOrCoreOrNUMA(logicalProcessors, pID, cores, siblings,cpids);
	int mem_access_times[][] = null;
	if((ret&0x0001)==1){
	    mem_access_times = new int[cpids.get(0).intValue()][cpids.get(0).intValue()];
	    for(int r=0;r<cpids.get(0).intValue();++r){
		if(new File(System.getenv("HOME")+"/.distance"+r).exists()){
		    StringBuffer temp = readFile(System.getenv("HOME")+"/.distance"+r);
		    Scanner scan = new Scanner(temp.toString());
		    while(scan.hasNextLine()){
			String stemp = scan.nextLine();
			StringTokenizer token = new StringTokenizer(stemp," ");
			for(int y=0;y<cpids.get(0).intValue();++y)
			    mem_access_times[r][y] = new Integer(token.nextToken()).intValue();
		    }
		}
		else
		    throw new RuntimeException("I am confused: Don't know if machine "
					       +machineName+" is NUMA or not\n HELP!!!!");
	    }
	}
	if((ret&0x0001)==1){
	    // System.out.println("Machine "+machineName+" is a multi-core processor, which is currently not supported");
	}
	for(int e=0;e<nodes.size();++e)
	    machine.add(nodes.get(e));
	//Make edges between nodes
	/**
	 * FIXME: Later on this will also take in a URI
	 * which will tell the time between cores
	 * that file will be a .ini file, just like the network
	 * times--->DONE
	 */
	ArrayList<GXLEdge> edges = makeEdges(nodes,pID,cID,mem_access_times,currMachine);
	for(int e=0;e<edges.size();++e)
	    machine.add(edges.get(e));
	return machine;
    }
    private static ArrayList<String> users=new ArrayList<String>();
    private static ArrayList<String> machines=new ArrayList<String>();
    private static ArrayList<String> nFiles=new ArrayList<String>();
    private static ArrayList<String> cFiles=new ArrayList<String>();
    private int getOOptions(String args[],int e)throws optionsException{
	if(args[e].equals("-nlf") && users.size()<=1)
	    throw new optionsException("Network file only valid when there are at least two machines");
	else if(args[e].equals("-clf") && users.size()<1)
	    throw new optionsException("Core latency file only valid when there is at least one user@machine");
	if(args[e].equals("-nlf")){
	    do{
		++e;
		if(args[e].equals("-clf")) break;
		nFiles.add(args[e]);
	    }while(e<args.length-1);
	}
	if(args[e].equals("-clf")){
	    do{
		++e;
		if(args[e].equals("-nlf")) break;
		cFiles.add(args[e]);
	    }while(e<args.length-1);
	}
	return e;
    }
    private void getOptions(String args[]) throws optionsException{
	if(args.length<1)
	    throw new optionsException();
	int e=0;
	for(e=0;e<args.length;++e){
	    if(!(args[e].equals("-nlf") || args[e].equals("-clf"))){
		StringTokenizer token = new StringTokenizer(args[e],"@");
		users.add(token.nextToken());
		machines.add(token.nextToken());
	    }
	    if(args[e].equals("-clf")){e=getOOptions(args,e);}
	    if(args[e].equals("-nlf")){e=getOOptions(args,e);}
	}
	if(users.size()<2);
	else if(users.size()==nFiles.size() && nFiles.size()==machines.size());
	else throw new optionsException("The -nlf option is incorrect");
	if(users.size()==cFiles.size());
	else throw new optionsException("The -clf option is incorrect");
    }
    private static void showUsage(){
	System.out.println("Usage: createPArch user1@machine-name1 user2@machine-name2 ....\n"+
			   "-nlf <absolute path to network latency .ini files between machine1, machine2, .... separated by space>\n"+
			   "-clf <absolute path to core latency .ini files between cores on machine1, machine2, ..... separated by space>");
    }
    public static void main(String args[]){
	try{
	    new createPArch().getOptions(args);//This is so stupid
	}catch(optionsException e){System.out.println("\n"+e.toString()+"\n");showUsage();System.exit(1);}
        try{
            //Make the top-level single gxl graph and node
            GXLDocument topDoc = new GXLDocument();
            GXLGraph topGraph = new GXLGraph("topGraph");
            topGraph.setEdgeMode("directed");
            topGraph.setEdgeIDs(true);
            GXLNode topNode = new GXLNode("cluster");
            topGraph.add(topNode);
            GXLGraph topNodeg = new GXLGraph("clusterArch");
            topNodeg.setAttr("label",new GXLString("clusterArch"));
            topNodeg.getAttr("label").setKind("graph");
            topNode.add(topNodeg);
            //Now make i args.length number of graphs
            for(int i=0;i<users.size();++i){
                String machineName = machines.get(i);
                GXLNode node = new GXLNode("machine_"+machineName+"_node");
                node.setAttr("label",new GXLString("machine_"+machineName+"_node"));
                GXLGraph gr = new GXLGraph("machine_"+machineName+"_graph");
                gr.setAttr("label",new GXLString("machine_"+machineName+"_graph"));
                gr.getAttr("label").setKind("graph");
                gr.setEdgeMode("directed");
                gr.setEdgeIDs(true);
                gr = completeMachineGraph(gr,new String(users.get(i)+"@"+machineName),machineName,i);
                node.add(gr);
                topNodeg.add(node);
            }
	    new createPArch().makeNetworkConnections(topGraph); //This is again so stupid
            topDoc.getDocumentElement().add(topGraph);
            topDoc.write(new File("pArch.gxl"));
        }
        catch(IOException e){e.printStackTrace();}
        catch(NoSuchElementException es){es.printStackTrace();}
        catch(Exception ep){ep.printStackTrace();}
    }
    @SuppressWarnings("unchecked")
	private static GXLGraph makeNetworkConnections(GXLGraph topGraph)throws Exception{
	//Now start making the network connections between machines
	
	//Get the cluster
	GXLNode cluster = PArchParser.getClusterAt(topGraph,0);
	//Get the cluster architecture
	GXLGraph cArch = PArchParser.getClusterArch(cluster);

	//How many machines are there in this architecture
	//I know there is just one cluster that's why I am not 
	//getting the cluster information
	ArrayList<ArrayList>anodes = new ArrayList<ArrayList>(PArchParser.getMachinesCount(cArch));
	for(int r=0;r<PArchParser.getMachinesCount(cArch);++r){
	    GXLNode mnode = PArchParser.getMachineAt(cArch,r);
	    GXLGraph mGraph = PArchParser.getMachineArch(mnode);

	    //mGraph holds the architecture of the machine
	    //This includes the nodes (logicalProcessors) and edges 
	    //(connections between these processors)

	    //Collect all the logicalProcessors in the mGraph
	    ArrayList<GXLNode> nodes = new ArrayList<GXLNode>(PArchParser.getLogicalProcessorCount(mGraph));
	    for(int e=0;e<PArchParser.getLogicalProcessorCount(mGraph);++e)
		nodes.add(PArchParser.getLogicalProcessorAt(mGraph,e));
	    anodes.add(nodes);
	}
	//Now make the Edges and make the connections between nodes
	ArrayList<GXLEdge> list = new ArrayList<GXLEdge>();
	for(int r=0;r<anodes.size();++r){
	    for(int e=0;e<anodes.size();++e){
		if(r != e){
		    ArrayList<GXLNode> n = anodes.get(r);
		    ArrayList<GXLNode> m = anodes.get(e);
		    for(int q=0;q<n.size();++q){
			for(int w=0;w<m.size();++w){
			    GXLEdge edge = new GXLEdge(n.get(q),m.get(w));
			    edge.setAttr("latency_file",new GXLString("Name:"+nFiles.get(r)+":"+"Type:ini:Parser:org.IBM.iniParser"));
			    edge.setAttr("label",new GXLString("Network latency, check file: "+nFiles.get(r)));
			    list.add(edge);
			}
		    }
		}
	    }
	}
	for(int w=0;w<list.size();++w)
	    cArch.add(list.get(w));
	return topGraph;
    }
    private class optionsException extends RuntimeException{
	static final long serialVersionUID = 0;
	public optionsException(){super();}
	public optionsException(String t){super(t);}
    }
}
