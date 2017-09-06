import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class BranchTraceLog extends TraceLog {
	
	public BranchTraceLog(String line) {
		
		String[] parts = line.split(SystemConfig.deliminator);
		if(parts[0].startsWith("S"))
			callNumber = Integer.parseInt(parts[0].substring(1));
		else
			callNumber = Integer.parseInt(parts[0]);
		
		params = new HashMap<String, String>();
		
		for(int i = 1; i<parts.length;i+=2)
			params.put(parts[i],parts[i+1]);
	
	}
	
	public static long getCallNumber(String line){
		long callNumber;
		String[] parts = line.split(SystemConfig.deliminator);
		if(parts[0].startsWith("S"))
			callNumber = Integer.parseInt(parts[0].substring(1));
		else
			callNumber = Integer.parseInt(parts[0]); 
		return callNumber;
	}
	
	
}
