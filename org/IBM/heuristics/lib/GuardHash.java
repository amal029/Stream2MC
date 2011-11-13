/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-12
 */

package org.IBM.heuristics.lib;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;

import org.IBM.stateGraph.*;

public class GuardHash<K,V> extends HashMap{

    //Initialize the hashmap
    public GuardHash (int init) {
	super(init);
    }
    
    public GuardHash(){
	super();
    }
    
    public GuardHash(int i, float f){
	super(i,f);
    }

}
