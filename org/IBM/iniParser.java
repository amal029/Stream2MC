package org.IBM;

import java.io.*;
import java.util.*;


public final class iniParser{
    private static StringBuffer contents=null;
    private HashMap<String,Long> latencyMap=new HashMap<String,Long>();
    public iniParser(){}
    private String fileName=null;
    public iniParser(String filename) throws FileNotFoundException, IOException, Exception{
	File file = new File(filename);
	this.fileName = filename;
	if(file.exists()) {contents = readFile(file);getLatency();}
	else throw new FileNotFoundException(file.toString());
    }
    public String getFileName(){
	return fileName;
    }
    private static StringBuffer readFile(File file) throws FileNotFoundException,IOException{
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = null;

	reader = new BufferedReader(new FileReader(file));
	String text = null;

	// repeat until all lines are read
	while ((text = reader.readLine()) != null) 
	    if(text.startsWith("#")); //Ignore the comments
	    else if(text.matches("^$")); //Ignore the white lines
	    else
		contents.append(text).append(System.getProperty("line.separator"));
	if (reader != null) 
	    reader.close();
        return contents;
    }
    private HashMap<String,String> getValues(int count){
	HashMap<String,String> map = new HashMap<String,String>();
	Scanner scan = new Scanner(contents.toString());
	while(count>0){
	    scan.nextLine();
	    --count;
	}
	while(scan.hasNextLine()){
	    String temp = scan.nextLine();
	    if(temp.startsWith("[")) break;
	    else{
		StringTokenizer token = new StringTokenizer(temp,"=");
		map.put(token.nextToken(),token.nextToken());
	    }
	}
	return map;
    }
    public boolean isByteLatency(String key){
	return isByteLatency(latencyMap,key);
    }
    public boolean isByteLatency(HashMap<String,Long> latencyMap,String key){
	Iterator<String>i = latencyMap.keySet().iterator();
	i = latencyMap.keySet().iterator();
	Long cVal = new Long(0);
	while(i.hasNext()){
	    String k = i.next();
	    if(key.equals(k))
		return false;
	}
	return true;
    }
    public long getLatency(String key){
	return getLatency(latencyMap,key);
    }
    public long getLatency(HashMap<String,Long> latencyMap, String key){
	Long val = new Long(0);
	Long cVal = new Long(0);
	if(key.equals("0")) return 0;
	if((val = latencyMap.get(key))==null)
	    val = latencyMap.get("constant");
	if(val.longValue() != 0) 
	    return val.longValue();
	else
	    return cVal.longValue();
    }
    private void explode(HashMap<String,String> map) throws Exception{
	latencyMap = iExplode(map);
    }
    private HashMap<String,Long> iExplode(HashMap<String,String> map)throws Exception{
	Iterator<String>i = map.keySet().iterator();
	HashMap<String,Long> latencyMap = new HashMap<String,Long>();
	while(i.hasNext()){
	    String key = i.next();
	    if(key.equals("constant")){
		latencyMap.put(key,new Long(map.get(key)));
	    }
	    //This is the explosion...LET IT RIP!!!
	    else{
		String val = map.get(key);
		StringTokenizer token = new StringTokenizer(key,"..");
		//These have to be true, else an exception will be thrown
		long slong = new Long(token.nextToken()).longValue();
		long elong = new Long(token.nextToken()).longValue();
		while(slong<=elong){
		    try{
			latencyMap.put(""+slong,new Long(val));
		    }
		    catch(NumberFormatException e){
			latencyMap.put(""+slong,new Long(new Float(val).longValue()));
		    }
		    ++slong;
		}
	    }
	    // latencyMap.put(key,new Long(map.get(key)));
	}
	return latencyMap;
    }
    public HashMap<String,Long> getLatencyMap(String node) throws Exception{
	HashMap<String,String> map = getV(node);
	return iExplode(map);
    }
    private void getLatency()throws Exception{
	HashMap<String,String> map = getV("[latency]");
	explode(map);
    }
    public HashMap<String,String>getInfo(){
	return getV("[info]");
    }
    private HashMap<String,String> getV(String test){
	HashMap<String,String> map = new HashMap<String,String>();
	Scanner scan = new Scanner(contents.toString());
	int count=0;
	while(scan.hasNextLine()){
	    String temp = scan.nextLine();
	    ++count;
	    if(temp.startsWith(test)){
		map = getValues(count);
		break;
	    }
	}
	return map;
    }
}
