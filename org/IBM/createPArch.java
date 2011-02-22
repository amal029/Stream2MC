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
        if(cpid > 1)ret = 0x1; //It is NUMA, i.e., it has multiple CPUs will have to look at /sys/devices/system/node
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
    private static ArrayList<GXLNode> makeProcessorNodes(ArrayList<String> nodes,String machineName,ArrayList<Integer>pID,ArrayList<Integer>cID){
        ArrayList<GXLNode> list = new ArrayList<GXLNode>(10);
        for(int e=0;e<nodes.size();++e){
            StringTokenizer token = new StringTokenizer(nodes.get(e),":");
            token.nextToken();
            String nodeNum = token.nextToken();
            GXLNode node = new GXLNode(machineName+":"+"logicalProcessor:"+new StringTokenizer(nodeNum," ").nextToken());
            if(pID.size() > e && cID.size()>e)
                node.setAttr("label",new GXLString(machineName+":"+"logicalProcessor:"+
						   new StringTokenizer(nodeNum," ").nextToken()+"\\nCPU:"+pID.get(e)+"\\nCore:"+cID.get(e)));
            list.add(node);
        }
        return list;
    }
    private static ArrayList<GXLEdge> makeEdges(ArrayList<GXLNode> nodes, ArrayList<Integer> pID, ArrayList<Integer>cID,
						int[][]mem_access_times){
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
                edge.setAttr("label",new GXLString(comm_time+""));
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
        private static GXLGraph completeMachineGraph(GXLGraph machine, String arg,String machineName){
	StringBuffer procFileBuffer = readViaSSH(arg,"/proc/cpuinfo","/tmp/cpuinfo");
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
		if(new File("/tmp/distance"+r).exists()){
		    StringBuffer temp = readFile("/tmp/distance"+r);
		    Scanner scan = new Scanner(temp.toString());
		    while(scan.hasNextLine()){
			String stemp = scan.nextLine();
			StringTokenizer token = new StringTokenizer(stemp," ");
			for(int y=0;y<cpids.get(0).intValue();++y)
			    mem_access_times[r][y] = new Integer(token.nextToken()).intValue();
		    }
		}
		else
		    throw new RuntimeException("I am confused: Don't know if machine "+machineName+" is NUMA or not\n HELP!!!!");
	    }
	}
	if((ret&0x0001)==1){
	    System.out.println("Machine "+machineName+" is a multi-core processor, which is currently not supported");
	}
	for(int e=0;e<nodes.size();++e)
	    machine.add(nodes.get(e));
	//Make edges between nodes
	/**
	 * FIXME: Later on this will also take in a URI
	 * which will tell the time between cores
	 * that file will be a .ini file, just like the network
	 * times
	 */
	ArrayList<GXLEdge> edges = makeEdges(nodes,pID,cID,mem_access_times);
	for(int e=0;e<edges.size();++e)
	    machine.add(edges.get(e));
	return machine;
    }
    public static void main(String args[]){
        if(args.length < 1)
            System.out.println("Usage: createPArch <user1@machine-name1 user2@machine-name2 ....>");
        try{
            //Make the top-level single gxl graph and node
            GXLDocument topDoc = new GXLDocument();
            GXLGraph topGraph = new GXLGraph("topGraph");
            topGraph.setEdgeMode("directed");
            topGraph.setEdgeIDs(true);
            GXLNode topNode = new GXLNode("topNode");
            topGraph.add(topNode);
            GXLGraph topNodeg = new GXLGraph("pArch");
            topNodeg.setAttr("label",new GXLString("pArch"));
            topNodeg.getAttr("label").setKind("graph");
            topNode.add(topNodeg);
            //Now make i args.length number of graphs
            for(int i=0;i<args.length;++i){
                StringTokenizer token = new StringTokenizer(args[i],"@");
                token.nextToken();
                String machineName = token.nextToken();
                GXLNode node = new GXLNode("machine_"+machineName+"_node");
                node.setAttr("label",new GXLString("machine_"+machineName+"_node"));
                GXLGraph gr = new GXLGraph("machine_"+machineName+"_graph");
                gr.setAttr("label",new GXLString("machine_"+machineName+"_graph"));
                gr.getAttr("label").setKind("graph");
                gr.setEdgeMode("directed");
                gr.setEdgeIDs(true);
                gr = completeMachineGraph(gr,args[i],machineName);
                node.add(gr);
                topNodeg.add(node);
            }
            topDoc.getDocumentElement().add(topGraph);
            topDoc.write(new File("pArch.gxl"));
        }
        catch(IOException e){e.printStackTrace();}
        catch(NoSuchElementException es){es.printStackTrace();}
    }
}
