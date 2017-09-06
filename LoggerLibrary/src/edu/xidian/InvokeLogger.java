package edu.xidian;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import java.util.Stack;

/*
 * invoke log format
 * invoke log name format: invoke_<Function Signature>_randomNum
 * format in each invoke: 
 *     <callNumber> 0 <parameters>
 *     <callNumber> 1 <parameters and return value>
 *     
 * relation log name format: relation_<Function Signature>_randomNum
 * format in each relation:
 *     <callerNumber> <calleeNumber>
 */
public class InvokeLogger {
	static HashMap<String, Writer> writers=new HashMap<String, Writer>();
	static ArrayList<String> logMem = null;
	static ArrayList<String> spillLogMem = new ArrayList<String>();
	String invokeFileName = null;
	String relationFileName = null;
	File file = null;
	boolean isNewCall = true;
	Writer invokeWriter = null;
	Writer relationWriter = null;
	//static int functionIndex = 0;
	static long callCount = 0;
	static int fileRandomNumber = 10000000;
	long currCount = 0;
	final String invokeType = "invoke";
	final String relationType = "relation";

	String callerSignature;
	boolean outputHash = true;
	private long oldCount;
	String deliminator = "\t";
	final static String logLocation = "/tmp/traceLog/";
	int logStatus = 0; // 0 for before invoke; 1 for after invoke 
	
	public InvokeLogger(String signature, String type){
		callerSignature = signature;
	}
	
	private void getWriter(String invokedMethodSignature){
		String keySignature = invokedMethodSignature;
		if(keySignature.length()>200)
			keySignature = keySignature.substring(keySignature.length()-200);
		String invokeWriterKey = invokeType+"_"+keySignature;
	    invokeWriter = writers.get(invokeWriterKey);
		String relationWriterKey = relationType+"_"+keySignature;
	    relationWriter = writers.get(relationWriterKey);
		if(invokeWriter ==null && relationWriter == null){
			if(InvokeLogger.fileRandomNumber==10000000){
				InvokeLogger.fileRandomNumber = (int)(Math.random()*100000);
			}
			invokeFileName = logLocation+invokeWriterKey+"_"+fileRandomNumber;
			relationFileName = logLocation+relationWriterKey+"_"+fileRandomNumber;
			try{
        		File folder = new File(logLocation);
        		if (!folder.exists()) {
        			folder.mkdir();
        		}
	        	file = new File(invokeFileName);
	        	if(!file.exists()){
	        		file.createNewFile();
	        	}
				
				invokeWriter = new BufferedWriter(new FileWriter(invokeFileName, true)); 
	        	
				file = new File(relationFileName);
	        	if(!file.exists()){
	        		file.createNewFile();
	        	}
				
				relationWriter = new BufferedWriter(new FileWriter(relationFileName, true)); 
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        	writers.put(invokeWriterKey, invokeWriter);
        	writers.put(relationWriterKey, relationWriter);
		}
	}
	
	
	public boolean logBeforeInvoke(String invokedMethodSignature){
		try{
			getWriter(invokedMethodSignature);
			callCount++;
			oldCount = currCount;
			currCount = callCount;

			this.invokeWriter.append(String.valueOf(currCount+deliminator+"0"+deliminator/*+invokedMethod+deliminator*/));
			this.relationWriter.append(oldCount+deliminator+currCount+"\n");
			this.logStatus = 0; //start to receive pre-invoke parameter
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logAfterInvoke(String invokedMethod){
		try{

				this.invokeWriter.append(String.valueOf(currCount+deliminator+"1"+deliminator/*+invokedMethod+deliminator*/));
				this.logStatus = 1; // start to receive after-invoke parameter
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return true;
	}
	
	public boolean logTrace(String label, String[]params, String[] values){
		try{
				this.invokeWriter.append(label);
				for(int i = 0; i<params.length;i++){
					invokeWriter.append(deliminator);
					invokeWriter.append((String)params[i]);
					invokeWriter.append(deliminator);
					invokeWriter.append((String)values[i]);
				}
				invokeWriter.append("\n");
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logRawString(String str){
		try{
			
				this.invokeWriter.append(str);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(String str){
		try{

			if(str==null)
				str = new String();
				if(outputHash)
					this.invokeWriter.append(str.hashCode()+deliminator);
				else
					this.invokeWriter.append(str+deliminator);
				
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(byte str){
		try{
			//appendLogMem(String.valueOf(str));
			//if(currCount%10==11){
			
				if(outputHash)
					this.invokeWriter.append(String.valueOf(str).hashCode()+deliminator);
				else
					this.invokeWriter.append(String.valueOf(str)+deliminator);
			//}
		}catch(Exception e){
			//System.err.println("logString byte "+str);
			//System.err.println(String.valueOf(str));
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(Object obj){
		String outputStr = null;
		try{
			//appendLogMem(String.valueOf(obj));
//			if(obj instanceof Iterable){
//				this.writer.append("[");
//			      for (Object each : (Iterable) obj) {
//			    	  this.writer.append(String.valueOf(each)+deliminator);
//			        }
//			      this.writer.append("]");
//			}else
			//if(currCount%10==11){
//			String objStr = String.valueOf(obj);

			//System.err.println("logString obj "+obj.toString());
			outputStr = String.valueOf(obj);
		}catch(NullPointerException ne){
			outputStr = new String("");
		}
		try{
			if(outputHash)
				this.invokeWriter.append(outputStr.hashCode()/*String.valueOf(obj)*/+deliminator);
			else
				this.invokeWriter.append(outputStr+deliminator);
		}
		catch(Exception e){
			//System.err.println("logString obj "+obj.toString());
			//System.err.println(String.valueOf(obj));
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
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(double i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(float i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(long i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(short i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(char i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(boolean i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(currCount%10==11)
				this.invokeWriter.append(i+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
//	private void appendLogMem(String str){
//		synchronized(logMem){
//			logMem.add(str);
//		}
//		//synchronized(logMem){
//			if(logMem.size()>1000000){
//				spillLogMem();
//			}
//		//}
//	}
//
//	
//	private void spillLogMem() {
//		// TODO Auto-generated method stub
//		synchronized (logMem){ 
//			synchronized(spillLogMem){
//				InputLogger.spillLogMem = logMem;
//				logMem = new ArrayList<String>();
//			}
//		}
//		SpillMemThread smt = new SpillMemThread(this.writer);
//		smt.start();
//	}

	public void close(){

		//callNumStack.pop();
		//writer = null;
		if(invokeWriter!=null){
			try {
				//spillLogMem();
				//if(callNumber%10<RANDOM_THRESHOLD){
				invokeWriter.flush();
				//}
				//writer.close();
			} catch (IOException e) {
//				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(relationWriter!=null){
			try {
				//spillLogMem();
				//if(callNumber%10<RANDOM_THRESHOLD){
				relationWriter.flush();
				//}
				//writer.close();
			} catch (IOException e) {
//				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
//	
//	public class SpillMemThread extends Thread {
//
//		FileWriter writer=null;
//
//		public SpillMemThread(FileWriter writer){
//			writer = this.writer;
//		}
//	    public void run() {
//	    	try{
//	    		synchronized(InputLogger.spillLogMem){
//		    		for(String str:InputLogger.spillLogMem)
//		    			writer.append(str);
//	    		}
//	    		//Transformer.writer.flush();
//			}catch(Exception e){
//				e.printStackTrace();
//			}
//	    }
//	}
}
