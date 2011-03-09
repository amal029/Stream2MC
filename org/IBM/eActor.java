package org.IBM;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class eActor extends Actor{
    private String execFileName;
    private String execFilePath;
    public eActor(String id){super(id);}
    public eActor(GXLNode e){super(e);}
    public long getExecTime(String attr){
	return getVals(attr);
    }
}
