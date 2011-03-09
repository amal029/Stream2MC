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
}
