/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-13
 */

package org.IBM.heuristics.lib;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.IBM.*;
import org.IBM.stateGraph.state;
import org.IBM.stateGraph.stateEdge;
import org.IBM.stateGraph.stateGraph;
import org.IBM.heuristics.lib.*;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;


//This class is used in the simulator to simulate guarded transitions in
//the state space

public class Guard {
    private String name;
    private boolean value;
    public Guard (String name) {
	this.name = name;
    }
    
    protected void setGuardValue(boolean value){
	this.value = value;
    }
    
    protected boolean getGuardValue(){
	return this.value;
    }
}
