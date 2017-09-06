package edu.xidian;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BranchLogger {
	//static HashMap<String, String> signature2File = new HashMap<String, String>();
	Writer writer=null;
	static ArrayList<String> logMem = null;
	static ArrayList<String> spillLogMem = new ArrayList<String>();
	String fileName = null;
	File file = null;
	String deliminator = "\t";
	

	long callNumber = 0;
	boolean outputHash = false;
	final int RANDOM_THRESHOLD = 10;
	
	public BranchLogger(String signature, String type){

		callNumber = InvokeLogger.callCount;
		String writerKey = type+"_"+signature;
	    writer = InvokeLogger.writers.get(writerKey);

		if(writer ==null){
			if(InvokeLogger.fileRandomNumber==10000000){
				InvokeLogger.fileRandomNumber = (int)(Math.random()*100000);
			}
        	//fileName = "/tmp/traceLog/"+type+"_"+System.currentTimeMillis();
        	//fileName = "/tmp/traceLog/"+type+"_"+signature+"_"+callNumber+"_"+System.currentTimeMillis();
			String keySignature = signature;
			if(keySignature.length()>200)
				keySignature = keySignature.substring(keySignature.length()-200);
        	fileName = InvokeLogger.logLocation+type+"_"+keySignature+"_"+InvokeLogger.fileRandomNumber;//(int)(Math.random()*100000);
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
				FileWriter fw = new FileWriter(fileName, true);
				writer = new BufferedWriter(fw); 
        	}catch(Exception e){
        		e.printStackTrace();
        	}
			InvokeLogger.writers.put(writerKey, writer);
		}	
        	//logMem = new ArrayList<String>();
	}
	
	public boolean logCount(){
		try{
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(callNumber)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logTrace(String label, String[]params, String[] values){
		try{
			//if(callNumber%10<RANDOM_THRESHOLD){
				this.writer.append(label);
				for(int i = 0; i<params.length;i++){
					writer.append(deliminator);
					writer.append((String)params[i]);
					writer.append(deliminator);
					writer.append((String)values[i]);
				}
				writer.append("\n");
			//}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logRawString(String str){
		try{
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(str);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(String str){
		try{
			//appendLogMem(str);
			//if(callNumber%10<RANDOM_THRESHOLD){
				if(outputHash)
					this.writer.append(str.hashCode()+deliminator);
				else
					this.writer.append(str+deliminator);
			//}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(byte str){
		try{
			//appendLogMem(String.valueOf(str));
			//if(callNumber%10<RANDOM_THRESHOLD){
				if(outputHash)
					this.writer.append(String.valueOf(str).hashCode()+deliminator);
				else
					this.writer.append(String.valueOf(str)+deliminator);
			//}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(Object obj){
		try{
			//appendLogMem(String.valueOf(obj));
			//if(callNumber%10<RANDOM_THRESHOLD){
				if(outputHash)
					this.writer.append(String.valueOf(obj).hashCode()/*String.valueOf(obj)*/+deliminator);
				else
					this.writer.append(String.valueOf(obj)+deliminator);
			//}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(int i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(double i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(float i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(long i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(short i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(char i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean logString(boolean i){
		try{
			//appendLogMem(String.valueOf(i));
			//if(callNumber%10<RANDOM_THRESHOLD)
				this.writer.append(String.valueOf(i)+deliminator);
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


//	private void spillLogMem() {
//		// TODO Auto-generated method stub
//		synchronized (logMem){ 
//			synchronized(spillLogMem){
//				BranchLogger.spillLogMem = logMem;
//				logMem = new ArrayList<String>();
//			}
//		}
//		SpillMemThread smt = new SpillMemThread(this.writer);
//		smt.start();
//	}

	public void close(){
		if(writer!=null){
			try {
				//spillLogMem();
				//if(callNumber%10<RANDOM_THRESHOLD){
				writer.flush();
				//}
				//writer.close();
			} catch (IOException e) {
//				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//writer = null;
	}

//	public class SpillMemThread extends Thread {
//
//		FileWriter writer=null;
//
//		public SpillMemThread(FileWriter writer){
//			writer = this.writer;
//		}
//	    public void run() {
//	    	try{
//	    		synchronized(BranchLogger.spillLogMem){
//		    		for(String str:BranchLogger.spillLogMem)
//		    			writer.append(str);
//	    		}
//	    		//Transformer.writer.flush();
//			}catch(Exception e){
//				e.printStackTrace();
//			}
//	    }
//	}
}
