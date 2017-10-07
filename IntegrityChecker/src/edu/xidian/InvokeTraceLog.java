package edu.xidian;
import java.util.ArrayList;
import java.util.HashMap;


public class InvokeTraceLog extends TraceLog {
	boolean hasReturn = false;
	String returnValue = "";
	public InvokeTraceLog(String line) {
		
		String[] parts = line.split(SystemConfig.deliminator);
		params = new HashMap<String,String>();
		
		if(parts[0].startsWith("S"))
			callNumber = Integer.parseInt(parts[0].substring(1));
		else
			callNumber = Integer.parseInt(parts[0]);
			for(int i = 1; i<parts.length;i+=2){
				//if(parts[i].equals("\t")||parts[i].equals("\n")||parts[i].equals(" ")||parts[i].equals(""))
					//continue;
				if(i+1==parts.length){ // with return value
					returnValue = parts[i];
					hasReturn = true;
				}else
					params.put("", parts[i+1]);
			}
	}
}
