/**
 * @author Avinash Malik (avimalik@ie.ibm.com)
 * @date 2011-11-08
 */

package org.IBM.stateGraph;

import net.sourceforge.gxl.*;
import java.io.*;
import java.util.*;
import org.xml.sax.*;

public class state extends GXLNode{
    private String id = "";
    private Float myCosts = 0f;
    private Float totalCost = 0f;

    public state(String id){
	super(id);
    }
    public state (String id,Float myCosts, Float cost) {
	super(id);
	this.myCosts = myCosts;
	this.totalCost = myCosts+cost;
    }
    public float getCurrentCost(){
	return totalCost;
    }
    public float getCost(){
	return myCosts;
    }
    public void updateCurrentCost(float c){
	totalCost = myCosts+c;
    }
}
