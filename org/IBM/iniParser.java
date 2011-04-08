package org.IBM;

import java.io.*;
import java.util.*;


public final class iniParser{
    private static StringBuffer contents=null;
    private static HashMap<String,Long> latencyMap=new HashMap<String,Long>();
    public iniParser(){}
    public iniParser(String filename) throws FileNotFoundException, IOException, Exception{
	File file = new File(filename);
	if(file.exists()) {contents = readFile(file);getLatency();}
	else throw new FileNotFoundException();
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
    public static void main(String args[]){
    	if(args.length < 1){
    	    System.out.println("Usage: iniParser <file.ini>");
    	    System.exit(1);
    	}
    	try{
    	    iniParser parser = new iniParser(args[0]);
    	    HashMap<String,String>map = parser.getInfo();
    	    Iterator<String> i = map.keySet().iterator();
    	    System.out.println("getInfo()");
    	    while(i.hasNext()){
    		String key = i.next();
    		System.out.println(key+"="+map.get(key));
    	    }
    	    System.out.println("\n\n\n");
    	    i = latencyMap.keySet().iterator();
    	    while(i.hasNext()){
    		String key = i.next();
    		// System.out.println(key+"="+latencyMap.get(key));
    	    }
    	    //Now the main test...
    	    if(!parser.isByteLatency("0")){
    	    	System.out.println("This is an exact value for sending 0 bytes of data: "+parser.getLatency("0"));
    	    }
    	    if(!parser.isByteLatency("300")){
    	    	System.out.println("This is an exact value for sending 300 bytes of data: "+parser.getLatency("300"));
    	    }
    	    if(!parser.isByteLatency("90")){
    	    	System.out.println("This is an exact value for sending 90 bytes of data: "+parser.getLatency("90"));
    	    }
    	    if(!parser.isByteLatency("700")){
    	    	System.out.println("This is an exact value for sending 700 bytes of data: "+parser.getLatency("700"));
    	    }else System.out.println("This is value for sending 1 byte of data: "+parser.getLatency("700")+" and hence 700 bytes will take: "+(parser.getLatency("700")*700));
    	    if(!parser.isByteLatency("80000")){
    	    	System.out.println("This is an exact value for sending 800 bytes of data: "+parser.getLatency("80000"));
    	    }else System.out.println("This is value for sending 1 byte of data: "+parser.getLatency("80000")+" and hence 80000 bytes will take: "+(parser.getLatency("80000")*80000));
    	    // /*
    	    //   Testing core [0..1] latency values
    	    //  */
    	    // HashMap<String,Long> coreMap = parser.getLatencyMap("[0..1]");
    	    // if(!parser.isByteLatency(coreMap,"0")){
    	    // 	System.out.println("This is an exact value for sending 0 bytes of data: "+parser.getLatency(coreMap,"0"));
    	    // }
    	    // if(!parser.isByteLatency(coreMap,"300")){
    	    // 	System.out.println("This is an exact value for sending 300 bytes of data: "+parser.getLatency(coreMap,"300"));
    	    // }
    	    // if(!parser.isByteLatency(coreMap,"90")){
    	    // 	System.out.println("This is an exact value for sending 90 bytes of data: "+parser.getLatency(coreMap,"90"));
    	    // }
    	    // if(!parser.isByteLatency(coreMap,"700")){
    	    // 	System.out.println("This is an exact value for sending 700 bytes of data: "+parser.getLatency(coreMap,"700"));
    	    // }else System.out.println("This is value for sending 1 byte of data: "+parser.getLatency(coreMap,"700")+" and hence 700 bytes will take: "+(parser.getLatency(coreMap,"700")*700));
    	    // if(!parser.isByteLatency(coreMap,"800")){
    	    // 	System.out.println("This is an exact value for sending 800 bytes of data: "+parser.getLatency(coreMap,"800"));
    	    // }else System.out.println("This is value for sending 1 byte of data: "+parser.getLatency(coreMap,"800")+" and hence 800 bytes will take: "+(parser.getLatency(coreMap,"800")*800));
    	}
    	// catch(IOException e){e.printStackTrace();}
    	catch(Exception ae){ae.printStackTrace();}
    }
}
