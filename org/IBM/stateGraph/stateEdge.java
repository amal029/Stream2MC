/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-08
 */
package org.IBM.stateGraph;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;


public class stateEdge extends GXLEdge{
    public stateEdge (state first, state second) {
	super(first.getID(),second.getID());
	this.setDirected(true);
    }
    public stateEdge (String first, String second) {
	super(first,second);
	this.setDirected(true);
    }
}
