package edu.xidian;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class TraceLog {

	public TraceLog() {
		// TODO Auto-generated constructor stub
	}
	HashMap<String,String> params;
	
	long callNumber;
	
	public HashMap<String, String> getParams(){
		return params;
	}
	
	public long getCallNumber(){
		return callNumber;
	}
	
	public String toString(){
		String str = "";
		for(String key:params.keySet()){
			str+=key+":"+params.get(key)+"      ";
		}
		return str;
	}
}
