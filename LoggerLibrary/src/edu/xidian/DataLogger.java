package edu.xidian;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DataLogger {
	//static HashMap<String, FileWriter> writers=new HashMap<String, FileWriter>();
	static ArrayList<String> logMem = null;
	static ArrayList<String> spillLogMem = new ArrayList<String>();
	String fileName = null;
	File file = null;
	boolean isNewCall = true;
	Writer writer = null;
	long callNumber = 0;
	boolean outputHash = false;
	
	String deliminator = "\t";


	public DataLogger(String signature, String type){
			callNumber = InvokeLogger.callCount;
			String writerKey = type+"_"+signature;
		    writer = InvokeLogger.writers.get(writerKey);
			if(writer ==null){
				if(InvokeLogger.fileRandomNumber==10000000){
					InvokeLogger.fileRandomNumber = (int)(Math.random()*100000);
				}
	        	fileName = InvokeLogger.logLocation+type+"_"+signature+"_"+InvokeLogger.fileRandomNumber;//(int)(Math.random()*100000);
	        	try{
	        		File folder = new File(InvokeLogger.logLocation);
	        		if (!folder.exists()) {
	        			folder.mkdir();
	        		}
		        	file = new File(fileName);
		        	if(file.exists()){
						file.delete();
					}
					file.createNewFile();
					writer = new BufferedWriter(new FileWriter(fileName, true)); 
	        	}catch(Exception e){
	        		e.printStackTrace();
	        	}
	        	InvokeLogger.writers.put(writerKey, writer);
	        	//logMem = new ArrayList<String>();
			}
			try{
				this.writer.append(String.valueOf("S"+callNumber+deliminator));
			}catch(Exception e){
				e.printStackTrace();
			}
	}
	
	
	public boolean logTrace(String label, String[]params, String[] values){
		try{
			this.writer.append(label);
			for(int i = 0; i<params.length;i++){
				writer.append(deliminator);
				writer.append((String)params[i]);
				writer.append(deliminator);
				writer.append((String)values[i]);
			}
			writer.append("\n");
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logRawString(String str){
		try{
			this.writer.append(str);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	
	public boolean logString(String str){
		try{
			//appendLogMem(str);
			if(outputHash)
				this.writer.append(str.hashCode()+deliminator);
			else
				this.writer.append(str+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(byte str){
		try{
			//appendLogMem(String.valueOf(str));
			if(outputHash)
				this.writer.append(String.valueOf(str).hashCode()+deliminator);
			else
				this.writer.append(String.valueOf(str)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logEncryptString(Object obj){
		String outputStr = null;
		try{
			outputStr = String.valueOf(obj);
		}catch(NullPointerException ne){
			outputStr = new String("");
		}
		try{
			this.writer.append(outputStr+deliminator);
		}
		catch(Exception e){
			//System.err.println("logString obj "+obj.toString());
			//System.err.println(String.valueOf(obj));
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(Object obj){
		try{
			//appendLogMem(String.valueOf(obj));
//			if(obj instanceof Iterable){
//			      for (Object each : (Iterable) obj) {
//			          this.writer.append(String.valueOf(each)+deliminator);
//			        }
//			}else
			if(outputHash)
				this.writer.append(String.valueOf(obj).hashCode()+deliminator);
			else
				this.writer.append(String.valueOf(obj)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
//	private String getString(Object obj){
//		if(obj instanceof org.apache.hadoop.io.Text){
//			return ((org.apache.hadoop.io.Text)obj).toString();
//		}
//	}
	
	public boolean logString(int i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(double i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(float i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(long i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(short i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(char i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(boolean i){
		try{
			//appendLogMem(String.valueOf(i));
			this.writer.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	


	public void close(){
//		if(writer!=null){
//			try {
//				//spillLogMem();
//				writer.flush();
//				//writer.close();
//			} catch (IOException e) {
////				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		writer = null;
	}

}
