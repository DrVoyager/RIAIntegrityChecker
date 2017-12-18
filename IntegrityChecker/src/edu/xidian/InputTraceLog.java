package edu.xidian;
import java.util.ArrayList;
import java.util.HashMap;


public class InputTraceLog extends TraceLog {

	public InputTraceLog(String line) {
		
		String[] parts = line.split(SystemConfig.deliminator);
		params = new HashMap<String,String>();
		
		if(parts[0].startsWith("S"))
			callNumber = Integer.parseInt(parts[0].substring(1));
		else
			callNumber = Integer.parseInt(parts[0]);
		
		for(int i = 1; i<parts.length;i++){
			//if(parts[i].equals("\t")||parts[i].equals("\n")||parts[i].equals(" ")||parts[i].equals(""))
				//continue;
			params.put("", parts[i]);
		}
	}

}
