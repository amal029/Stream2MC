package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class cActor extends Actor{
    public cActor(String id){super(id);}
    public cActor(GXLNode e){super(e);}
    public long getRate(){
	return getVals("rate");
    }
    public long getRep(){
	long ret = super.getRep();
	if(ret != 1) throw new RuntimeException("Communication Actor "+this.getID()+" has invalid repition value: "+ret);
	else return ret;
    }
    public boolean checkcActor(){
	boolean ret = true;
	for(int e=0;e<getConnectionCount();++e){
	    if(getConnectionAt(e).getDirection().equals(GXL.IN)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getTarget();
		if(node.getIsMergeNode()){ ret = false; break;}
	    }
	    if(getConnectionAt(e).getDirection().equals(GXL.OUT)){
		GXLEdge le = (GXLEdge)getConnectionAt(e).getLocalConnection();
		Actor node = (Actor)le.getSource();
		if(node.getIsSplitNode()){ ret = false; break;}
	    }
	}
	return ret;
    }
}
