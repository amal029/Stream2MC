package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;

public final class PArchParser{
    public PArchParser(){}
    
    public static int getClusterCount(GXLGraph graph){
	return graph.getGraphElementCount();
    }
    public static GXLNode getClusterAt(GXLGraph graph,int i){
	return (GXLNode)graph.getGraphElementAt(i);
    }
    public static GXLGraph getClusterArch(GXLNode node){
	return node.getGraphAt(0);
    }
    //This this needs this loop, because the network edges are attached
    //to the cArch rather than the mArch
    public static int getMachinesCount(GXLGraph cArch){
	int count = 0;
	for(int y=0;y<cArch.getGraphElementCount();++y){
	    if(cArch.getGraphElementAt(y) instanceof GXLNode) ++count;
	}
	return count;
    }
    public static GXLEdge getNetworkConnectionAt(GXLGraph cArch, int i){
	return (GXLEdge)cArch.getGraphElementAt(getMachinesCount(cArch)+i);
    }
    public static int getNetworkConnectionCount(GXLGraph cArch){
	int count =0;
	for(int y=0;y<cArch.getGraphElementCount();++y){
	    if(cArch.getGraphElementAt(y) instanceof GXLEdge) ++count;
	}
	return count;
    }
    public static GXLNode getMachineAt(GXLGraph graph, int i){
	return (GXLNode)graph.getGraphElementAt(i);
    }
    public static GXLGraph getMachineArch(GXLNode node){
	return node.getGraphAt(0);
    }
    public static int getLogicalProcessorConnectionCount(GXLGraph e){
	int all = e.getGraphElementCount();
	int lcount =0;
	for(int r=0;r<all;++r){
	    if(e.getGraphElementAt(r) instanceof GXLEdge) ++lcount;
	}
	return lcount;
    }
    public static int getLogicalProcessorCount(GXLGraph e){
	int all = e.getGraphElementCount();
	int lcount =0;
	for(int r=0;r<all;++r){
	    if(e.getGraphElementAt(r) instanceof GXLNode) ++lcount;
	}
	return lcount;
    }
    public static GXLEdge getLogicalProcessorConnectionAt(GXLGraph e, int y){
	int all = e.getGraphElementCount();
	int lcount =0;
	for(int r=0;r<all;++r){
	    if(e.getGraphElementAt(r) instanceof GXLNode) ++lcount;
	}
	return (GXLEdge)e.getGraphElementAt(lcount+y);
    }
    public static GXLNode getLogicalProcessorAt(GXLGraph e, int y){
	return (GXLNode)e.getGraphElementAt(y);
    }
    //All the edge connection functions are still remaining
}
