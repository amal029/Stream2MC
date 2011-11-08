/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-08
 */
package org.IBM.stateGraph;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class stateGraph extends GXLGraph {
    private state root = null;
    public stateGraph (String id,state root) {
	super(id);
	this.root = root;
    }
    
    public state getSourceNode(){
	return root;
    }
}
