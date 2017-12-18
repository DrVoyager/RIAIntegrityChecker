package edu.xidian;

import java.util.HashMap;
import java.util.Map;


public class TraceLog {
	
	public TraceLog(String line) {
		
		String[] parts = line.split("\t");
		params = new HashMap<String, String>();
		
		for(int i = 1; i<parts.length;i+=2)
			params.put(parts[i],parts[i+1]);
	}

	Map<String, String> params;
	
	public Map<String, String> getParams(){
		return params;
	}

}