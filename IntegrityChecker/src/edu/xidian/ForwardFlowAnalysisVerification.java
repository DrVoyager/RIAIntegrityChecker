package edu.xidian;
/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */




import soot.*;
import soot.toolkits.graph.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import encryptUtil.EncryptUtil;
import soot.jimple.AssignStmt;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.ParameterRef;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.options.*;
import soot.toolkits.graph.interaction.*;
import soot.toolkits.scalar.FlowAnalysis;
import thep.paillier.exceptions.BigIntegerClassNotValid;

/**
 *   Abstract class that provides the fixed point iteration functionality
 *   required by all ForwardFlowAnalyses.
 *  
 */
public abstract class ForwardFlowAnalysisVerification<N,A> extends FlowAnalysis<N,A>
{
	LineIterator branchLogIt;
	LineIterator invokeLogIt;
	File branchFile;
	TraceLog bufTrace;
	int fileSuffix = -1;
	static long auditedFunNo = 0;
	String path = null;
	static HashMap<String, LineIterator> logReaders=new HashMap<String, LineIterator>();
	HashSet<String> constraints = new HashSet<String>();

	private LineIterator getReader(String readerKey){

	    LineIterator relationLogIt = logReaders.get(readerKey);
		if(relationLogIt == null){
			String relationFileName = path+readerKey+"_"+fileSuffix;
			//G.v().out.println("relation file: "+relationFileName);
    		File relationFile = new File(relationFileName);
    		
    		try {
    			relationLogIt = FileUtils.lineIterator(relationFile, "UTF-8");
				logReaders.put(readerKey, relationLogIt);
			} catch (IOException e) {
				//e.printStackTrace();
				G.v().out.println("Log file not found: "+relationFile.toString());
				return null;
			}
		}
		return relationLogIt;
	}
	/** Construct the analysis from a DirectedGraph representation of a Body.
     */
    public ForwardFlowAnalysisVerification(DirectedGraph<N> graph)
    {
        super(graph);

        //branchLogIt = getBranchIterator(logFile);
    }

    protected boolean isForward()
    {
        return true;
    }

    protected void doAnalysis(){
    	//do nothing
    }
    
    
    /*
     * check all the logs related to function methodSig
     */
    protected void doAnalysis(String methodSig){
    	G.v().out.println("analysis method "+ methodSig);

    	iterateFolders(methodSig, new File(SystemConfig.traceLogDir));
    }
    
    private void iterateFolders(String methodSig, File currFile) {

        //File currFile = new File(filePath);
        if (currFile.isDirectory()){
        	G.v().out.println("analysis folder "+ currFile.toString());

        	File files[] = currFile.listFiles();
        	for(File f: files){
        		path = f.getParent()+File.separatorChar;
        		iterateFolders(methodSig, f);
        	}
        }
        else {
        	if (currFile.isFile()&&currFile.getAbsolutePath().contains(methodSig)&&currFile.getAbsolutePath().contains("invoke_")){
             try{
            	G.v().out.println("analysis currFile "+ currFile.getAbsolutePath());
        		fastProcessFile(currFile.getAbsolutePath(), methodSig);
             }catch(Exception e){
            	 e.printStackTrace();
             }
        	}
        }		
	}
    
    /*
     * for each invoke file recording methodSig invocation
     * log file name: invoke_methodSig_randomNo
     * records: callNo 0 <parameter values of methodSig>
     *          callNo 1 <parameter values of methodSig>
     * Simulate the method (see function simulate)
     * 
     * branch file name: branch_methodSig_randomNo
     * file contents: for each execution of methodSig with "callNo"
     * records: callNo variable1 value1 variable2 value 2
     *          callNo variable1 value 1 variable2 value 2
     *          ......
     * 
     * TODO: need to process recursion call
     */
    
    protected void fastProcessFile(String invokeFileName, String methodSig)
    {
        	
        	fileSuffix = Integer.parseInt(invokeFileName.substring(invokeFileName.lastIndexOf("_")+1));
        	//find the invoke_ file that records the invocation of function methodSig
        	String branchFileName = invokeFileName.replace("invoke_", "branch_");
        	branchFile = new File(branchFileName);

        	File invokeFile = new File(invokeFileName);
        	
        	if(!branchFile.exists()){
            	//G.v().out.println("branch file does not exist:"+invokeFileName);

        		return;
        	}
            
    		try {
    			branchLogIt = FileUtils.lineIterator(branchFile, "UTF-8");
    			invokeLogIt = FileUtils.lineIterator(invokeFile,"UTF-8");

		        long currCallNumber = -1; //current call number looking for the other part
		        long nextCallNumber = -1; //next call number to be found the other part when the current call number is done
		        
		        //boolean setNextCallNo = false; //whether has set the next call number. Related to the above variable.
		        String nextPreInvokeLine = "";
		        String preInvokeLine = "";
		        String postInvokeLine = "";
		        int readStatus = 0; //0: not identify pre-invoke CallNo; 1: identify pre-invoke CallNo, but not post-invokeCallNo
	        	String invokeLine="";
	    		long tempCallNumber;
	    		
	    		while(invokeLogIt.hasNext()){
	    			invokeLine = invokeLogIt.next();
	    			try{
	    				tempCallNumber = InvokeValues.getCallNumber(invokeLine);
	    			}catch (Exception e){
	    				break;
	    			}
	
					if(currCallNumber == -1){ //preinvoke
						currCallNumber = tempCallNumber;
						preInvokeLine = invokeLine;
					}else if(-1*tempCallNumber == currCallNumber){ //find the matching postinvoke
			        	//with 1-testRatio/1000 probability skip simulating the current invocation
			        	if(Math.random()*1000>=SystemConfig.testRatio){	        		
	        		
	        				currCallNumber = -1;
	        				continue;
			        	}
	    				postInvokeLine = invokeLine;
	    				processInvoke(preInvokeLine, postInvokeLine, methodSig);
	    				currCallNumber = -1;
					}else{ //find the preinvoke not find the machine post invoke, look the next
						continue; //TODO: this implementation skips the recursive function call.
					}
	    		}
    		}catch (Exception e) {
    			e.printStackTrace();
    		}finally{
    	        
    	        LineIterator.closeQuietly(branchLogIt);
    	        LineIterator.closeQuietly(invokeLogIt);
    	        for(String key: logReaders.keySet())
    	        	logReaders.get(key).close();
    	    }
    	   
    }
    
//    //Analyze execution of function "methodSig"
//    protected void processFile(String filePath) throws Exception
//    {
//
//        	branchFile = new File(filePath);        	
//        	fileSuffix = Integer.parseInt(filePath.substring(filePath.lastIndexOf("_")+1));
//        	//find the invoke_ file that records the invocation of function methodSig
//        	String invokeFileName = filePath.replace("branch_", "invoke_");
//
//        	File invokeFile = new File(invokeFileName);
//        	
//        	if(!invokeFile.exists()){
//            	G.v().out.println("invoke file does not exist:"+invokeFileName);
//        		return;
//        	}
//            
//    		try {
//    			branchLogIt = FileUtils.lineIterator(branchFile, "UTF-8");
//    			invokeLogIt = FileUtils.lineIterator(invokeFile,"UTF-8");
//    		
//
//	        
//	        long currCallNumber = -1;
//	        long nextCallNumber = -1;
//	        String preInvokeLine = "";
//	        String postInvokeLine = "";
//	        int readStatus = 0; //0: not identify pre-invoke CallNo; 1: identify pre-invoke CallNo, but not post-invokeCallNo
//	        boolean setNextCallNo = false;
//	        while(true){
//	        	String invokeLine="";
//	        	if(invokeLogIt.hasNext()){
//	        		invokeLine = invokeLogIt.next();
//	        		long tempCallNumber;
//	        		try{
//	        			tempCallNumber = InvokeValues.getCallNumber(invokeLine);
//	        		}catch(Exception e){
//	        			break;
//	        		}
//	        		if(readStatus==0){
//	        			if(tempCallNumber == nextCallNumber //find the next call
//	        					|| nextCallNumber == -1     // begin iterating the invoke file
//	        					||tempCallNumber>currCallNumber //no next call, just find the next bigger call
//	        					){ 
//	        				currCallNumber = tempCallNumber;
//	        				preInvokeLine = invokeLine;
//	        				readStatus = 1;
//	        				setNextCallNo = false;
//	        			}
//	        		}else if(readStatus == 1){
//	        			
//	        			if(-1*tempCallNumber == currCallNumber){
//	        				
//	    		        	//with 1-testRatio/1000 probability skip simulating the current invocation
//	    		        	if(Math.random()*1000>=SystemConfig.testRatio)	        		
//	    		        		continue;
//	    		        	
//	        				postInvokeLine = invokeLine;
//	        				processInvoke(preInvokeLine, postInvokeLine);
//	        				readStatus = 0;
//	        			}else if(!setNextCallNo && nextCallNumber<tempCallNumber ){
//	        				nextCallNumber = tempCallNumber;
//	        				setNextCallNo = true;
//	        			}
//	        		}
//	        	}else{
//	        		if(currCallNumber >= nextCallNumber)
//	        			break;
//	        		else{
//	        			if(invokeLogIt!=null)
//	        				invokeLogIt.close();
//	        			invokeLogIt = FileUtils.lineIterator(invokeFile,"UTF-8");
//	        		}
//	        	}
//	        }
//	        
//    		}catch (IOException e) {
//    			e.printStackTrace();
//    		}finally{
//    	        
//    	        LineIterator.closeQuietly(branchLogIt);
//    	        LineIterator.closeQuietly(invokeLogIt);
//    	        for(String key: logReaders.keySet()){
//    	        	logReaders.get(key).close();
//    	        }
//    	    }
//    }

    static List<String> systemMethods = null;
    private static boolean isEncryptionMethod(String methodName){
    	if(systemMethods==null){
    		systemMethods = new ArrayList<String>();

        	systemMethods.add("encryptUtil.EncryptUtil: java.lang.String getAH(int)");
    		systemMethods.add("encryptUtil.EncryptUtil: java.lang.String add(java.lang.String,java.lang.String)");
    		
    	}
    	
		for(String med: systemMethods){
			if(methodName.contains(med)){
				return true;
			}
		}
		return false;
		
    }
    
    private void processInvoke(String preInvokeLine, String postInvokeLine, String methodSig) {

    	
    	InvokeValues invokeValues = new InvokeValues();
        invokeValues.processLine(preInvokeLine);
        invokeValues.processLine(postInvokeLine);
        
        if(isEncryptionMethod(methodSig)){
        	try{
        		if(EncryptSimulate(invokeValues, methodSig))
        			throw new Exception("Encryption Simulation Failed");
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        }
        
//        if(!branchLogIt.hasNext()){
//			try {
//				branchLogIt = FileUtils.lineIterator(branchFile, "UTF-8");
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//        }
        bufTrace = getNextBranchTraceLog(invokeValues.getCallNumber());
        if(bufTrace==null){
        	G.v().out.println("$$$$$$$$$$$$$$$$$$$$$WARNING: NO BRANCH LOG FOUND$$$$$$$$$$$$$$$$$$$$$$$$"+invokeValues.getCallNumber());
        	return;
        }
        auditedFunNo++;
        simulate(invokeValues);
        
	}
    
    static EncryptUtil encryptUtil = null;
		
	private boolean EncryptSimulate(InvokeValues invokeValues, String methodSig) throws NumberFormatException, BigIntegerClassNotValid, IOException, ClassNotFoundException{
    	if(encryptUtil==null){
	       	 encryptUtil = new EncryptUtil();
	     	FileInputStream in = new FileInputStream(SystemConfig.keyDir+"/encryptutil.object");
	     	ObjectInputStream oi = new ObjectInputStream(in);
	 		encryptUtil = (EncryptUtil) oi.readObject();
	 	    oi.close();
	 	    in.close();
    	}

		String returnValue =  invokeValues.getReturnValue();
		ArrayList<String> paramPreValues = invokeValues.getParamPreValues();
		String simulatReturnValue = new String();
		if (methodSig.contains("encryptUtil.EncryptUtil: java.lang.String getAH(int)")) 
			simulatReturnValue = encryptUtil.getAH(Integer.parseInt(paramPreValues.get(0)));
		else if(methodSig.contains("encryptUtil.EncryptUtil: java.lang.String add(java.lang.String,java.lang.String)")) 
			simulatReturnValue = encryptUtil.add(paramPreValues.get(0), paramPreValues.get(1));
		else
			return false;
		
		if (simulatReturnValue.equalsIgnoreCase(returnValue))
			return true;
		return false;
    }
	
	private void simulate(InvokeValues invokeValues) {
        List<N> heads = graph.getHeads();
        constraints.clear();
        
        TraceLog curTrace = null;
        
        N headNode = null;
        //start from head
        {
            Iterator<N> it = heads.iterator();
            
            while (it.hasNext()) {
                headNode = it.next();
            }
        }

        N currentNode = headNode;
        N nextNode = null;
        
        while(currentNode!=null){
        	//G.v().out.println("current Node is "+ currentNode.toString());
        	
        	//Fill in the constraint when entering the invocation
        	//(Obtain the variable name of invocation parameters) 
        	//G.v().out.println("SPEED Test 1"+System.currentTimeMillis());
        	if (currentNode instanceof IdentityStmt){
        		IdentityStmt is = (IdentityStmt) currentNode;
				Value rightOp = is.getRightOp();
				if (rightOp instanceof ParameterRef){
					//obtain the variable name of the invocation parameter
					ParameterRef pr = (ParameterRef)rightOp;
					invokeValues.setParamId(pr.getIndex(), is.getLeftOp().toString());
					insertConstraints(invokeValues.getConstraint(pr.getIndex(), InvokeValues.PREPARAM_TYPE));
				}
            }
        	//G.v().out.println("SPEED Test 2");

        	//Check constraints when returning from the invocation
        	//(Obtain the variable name of return value)       	
        	if(currentNode instanceof RetStmt||currentNode instanceof ReturnStmt||currentNode instanceof ReturnVoidStmt){
            	if(currentNode instanceof ReturnStmt){
            		Value returnId = ((ReturnStmt)currentNode).getOp();
            		invokeValues.setReturnId(returnId.toString());
            	}
        		checkConstraints(invokeValues.getConstraints(InvokeValues.POSTPARAM_TYPE));
            }
        	//G.v().out.println("SPEED Test 3");

          	// assignment with an invoke statement:
			// obtain the invoke parameters from callee's invoke and relation log
			// check pre invoke log aginst constraints
			// insert post invoke log to the constraints
			if(currentNode instanceof AssignStmt){
				AssignStmt as = (AssignStmt)currentNode;
				if(as.getRightOp() instanceof InvokeExpr){
					InvokeExpr ie = (InvokeExpr)(as.getRightOp());
					//invoke file record
					// callNo 0 <parameter values>
	        		String[] logStrings = obtainInvokeeParams(ie,invokeValues.getCallNumber());
		    		if(logStrings!=null){
        				InvokeValues calleeInvokeValues = new InvokeValues();
        				calleeInvokeValues.processLine(logStrings[0]);
        	        	calleeInvokeValues.processLine(logStrings[1]);        	        	
        	        	List<Value> values = ie.getArgs();
        	        	int i = 0;
        	        	for(Value v: values){
        	        		invokeValues.setParamId(i, v.toString());
        	        		//G.v().out.println("callee: [" +i+","+v.toString()+"]");
        	        		i++;
        	        	}
    					Value returnVal = as.getLeftOp();
        	        	invokeValues.setReturnId(returnVal.toString());
        	        	checkConstraints(invokeValues.getConstraints(InvokeValues.PREPARAM_TYPE));
    		        	insertConstraints(invokeValues.getConstraints(InvokeValues.POSTPARAM_TYPE));
	        		}
				}
			}
        	//G.v().out.println("SPEED Test 4");

        	// invoke statement:
			// obtain the invoke parameters from callee's invoke and relation log
			// check pre invoke log aginst constraints
			// insert post invoke log to the constraints
        	if(currentNode instanceof InvokeStmt){
	    		InvokeStmt stmt = (InvokeStmt)currentNode;
	    		InvokeExpr ie = stmt.getInvokeExpr();
        		String[] logStrings = obtainInvokeeParams(ie,invokeValues.getCallNumber());
	    		if(logStrings!=null){
    				InvokeValues calleeInvokeValues = new InvokeValues();
    				calleeInvokeValues.processLine(logStrings[0]);
    	        	calleeInvokeValues.processLine(logStrings[1]);        	        	
    	        	List<Value> values = ie.getArgs();
    	        	int i = 0;
    	        	for(Value v: values){
    	        		invokeValues.setParamId(i, v.toString());
    	        		//G.v().out.println("callee: [" +i+","+v.toString()+"]");
    	        		i++;
    	        	}
    	        	checkConstraints(invokeValues.getConstraints(InvokeValues.PREPARAM_TYPE));
    	        	insertConstraints(invokeValues.getConstraints(InvokeValues.POSTPARAM_TYPE));
        		}
        	}
        	//long previousTime=System.currentTimeMillis();
        	//long currentTime = System.currentTimeMillis();
        	
        	//G.v().out.println("SPEED Test 5" + (currentTime-previousTime));
        	//previousTime = currentTime;

        	List<N> successors = graph.getSuccsOf(currentNode);
        	//branch statement:
        	// determine the next statement node
        	if(isBranchStmt(currentNode)){ //multiple successors, branch statement
        		curTrace = bufTrace;
        		
        		//currentTime = System.currentTimeMillis();
            	//G.v().out.println("SPEED Test 6" + (currentTime-previousTime));
            	//previousTime = currentTime;

        		bufTrace = getNextBranchTraceLog(curTrace.getCallNumber());
        		
        		//currentTime = System.currentTimeMillis();
            	//G.v().out.println("SPEED Test 7" + (currentTime-previousTime));
            	//previousTime = currentTime;

        		nextNode = getNextUpdateAndCheckConstraint(currentNode, successors, curTrace);
        		//currentTime = System.currentTimeMillis();
            	//G.v().out.println("SPEED Test 8" + (currentTime-previousTime));
            	//previousTime = currentTime;

            	//G.v().out.println("   current Node is a branch, the next Node is "+nextNode.toString());
        	}else{
        		if(successors==null||successors.size()==0){ //end of graph
            		nextNode = null;
            	}else if(successors.size()==1){ //normal statement, without exception
            		nextNode = successors.get(0);
            	}else if (!isBranchStmt(currentNode)){ //exception
            		for(N n:successors){
            			if(!(((Stmt)n).toString().contains("@caughtexception")))
            				nextNode = n;
            			//G.v().out.println("exception related successor "+((Stmt)n).toString());
            		}
            	}
        		//Normal statement or branch statement, update constraint
        		//currentTime = System.currentTimeMillis();
            	//G.v().out.println("SPEED Test 9" + (currentTime-previousTime));
            	//previousTime = currentTime;
            	
        		updateConstraints(currentNode);
        		//currentTime = System.currentTimeMillis();
            	//G.v().out.println("SPEED Test 10" + (currentTime-previousTime));
            	//previousTime = currentTime;
        	}
        	
    		//currentTime = System.currentTimeMillis();
        	//G.v().out.println("SPEED Test 11" + (currentTime-previousTime));
        	//previousTime = currentTime;
        	currentNode = nextNode;
        }		
	}

    
    /*
     * find pre-invoke and post-invoke records in invoke_callee_randomNum that 
     * records the invocation of callee function (ie) from callsite curCallNo
     * 
     * log records are as follows
     * invoke_caller_randomNum:
     * callerNo 0 <parameter values of caller() at callerNo>
     * callerNo 1 <parameter and return values of caller() at callerNo>
     * 
     * relation_callee_randomNum:
     * callerNo calleeNo
     * 
     * invoke_callee_randomNum:
     * calleeNo 0 <parameter values of callee() at calleeNo>
     * calleeNo 1 <parameter and return values of callee() at calleeNo>
     * 
     * scenario: function caller call function callee
     * 
     * start from invoke_caller_randomNum
     * record: callerNo 0 <parameter values of caller() at callerNo>
     * 
     * get relation_callee_randomNum
     * format: callerNo    calleeNo
     * 
     * find calleeNumber
     * search on invoke_callee_randomNum
     * find pre invoke and post invoke records that start with calleeNo
     * 
     */
    private String[] obtainInvokeeParams(InvokeExpr ie, long curCallNo){
		String preInvokeStr = null;
		String postInvokeStr = null;

		LineIterator relationIt = getReader("relation_"+ie.getMethod().getSignature());
		if(relationIt==null)
			return null;
		Long calleeNumber = -1L;
		//G.v().out.println("START SEARCH FOR RELATION");
		while(relationIt.hasNext())
		{
			String[] relationMap = relationIt.next().split(SystemConfig.deliminator);
			if(relationMap.length!=2)
				break;
			Long callerNumber = Long.parseLong(relationMap[0]);
			if(callerNumber>curCallNo)
				break;
			if(callerNumber == curCallNo){
				calleeNumber = Long.parseLong(relationMap[1]);
				break;
			}
		}
		//G.v().out.println("FINISH SEARCH FOR RELATION");

		if(calleeNumber != -1L){
			LineIterator invokeIt = getReader("invoke_"+ie.getMethod().getSignature());
			while(invokeIt.hasNext()){
				Long currCallNumber = Long.parseLong(invokeIt.next().split(SystemConfig.deliminator)[0]);
				if(currCallNumber>calleeNumber){
					break;
				}
				if(currCallNumber == calleeNumber){
					 preInvokeStr = invokeIt.next();
					if(invokeIt.hasNext())
						postInvokeStr = invokeIt.next();
					break;
				}
				if(currCallNumber < calleeNumber){
					continue;
				}
			}
		}
		//G.v().out.println("FINISH SEARCH FOR INVOKE");

		if(preInvokeStr!=null && postInvokeStr!=null){
			//G.v().out.println("FOUND  INVOKE");

			String[] logStrings = {preInvokeStr, postInvokeStr};
			return logStrings;
		}else{
			//G.v().out.println("NOT FOUND  INVOKE "+ie.getMethod().getSignature());
			return null;		
		}
    }
    
	private boolean obtainInvokeeParams(InvokeValues invokeeValues) {
		// TODO Auto-generated method stub
		return false;
	}
	private void checkConstraints(Map<String, String> paramValues) {
		SymjaAdaptor symjaAdaptor = new SymjaAdaptor(this);
		symjaAdaptor.checkInvokeConstraints(paramValues);
	}

	private void updateConstraints(N currentNode) {
		Stmt s = (Stmt)currentNode;
		SymjaAdaptor symjaAdaptor = new SymjaAdaptor(s,this);
    	symjaAdaptor.updateConstraints();
	}
	
	private void insertConstraints(Map<String, String> paramValues){
		SymjaAdaptor symjaAdaptor = new SymjaAdaptor(this);
		symjaAdaptor.addInvokeConstraints(paramValues);
	}

	private TraceLog getNextBranchTraceLog(long callNo) {
		//boolean newIT=false;
//		if(!branchLogIt.hasNext()){
//	        //newIT = true;
//			try{
//	        	//G.v().out.println("#######WARNING, LOOP AROUND BRANCH##########"+branchFile+" CALLNO "+callNo);
//	        	branchLogIt.close();
//				branchLogIt = FileUtils.lineIterator(branchFile, "UTF-8");
//	
//	    	}catch(IOException e){
//	    		e.printStackTrace();
//	    	}
//		}
        while (branchLogIt.hasNext()) {
	        String line = branchLogIt.nextLine();
	        BranchTraceLog btl = new BranchTraceLog(line);
	        if (btl.getCallNumber()==callNo){
	        	//if(newIT)
		        	//G.v().out.println("#######FOUND BRANCH##########"+branchFile+" CALLNO "+callNo);

	        	return btl;
	        }else if(btl.getCallNumber()>callNo){
	        	return null;
	        }
        }
        return null;
	}

	private boolean isBranchStmt(N currentNode) {
		Stmt s = (Stmt)currentNode;
		if(s instanceof IfStmt)
			return true;
		else
			return false;
	}

	private N getNextUpdateAndCheckConstraint(N currentNode, List<N> successors, TraceLog curBranchValue) {
	
		Stmt s = (Stmt)currentNode;
		if(!(s instanceof IfStmt))
			return null;
		//if statement
		IfStmt ifStmt = (IfStmt) s;
		Value condition = ifStmt.getCondition();

		boolean expValue = evalExpValue(condition, curBranchValue);
		N ifTarget = (N)(ifStmt.getTarget());
		if(expValue)
			return ifTarget;//successors.get(0);
		else
			return ifTarget==successors.get(0)?successors.get(1):successors.get(0);			
	}

	private boolean evalExpValue(Value condition, TraceLog curBranchValue) {
		SymjaAdaptor symjaAdaptor = new SymjaAdaptor(condition, this);
    	return symjaAdaptor.evalBranchExp(curBranchValue);
	}

	protected Collection<N> constructWorklist(final Map<N, Integer> numbers) {
		return new TreeSet<N>( new Comparator<N>() {
            public int compare(N o1, N o2) {
                Integer i1 = numbers.get(o1);
                Integer i2 = numbers.get(o2);
                return (i1.intValue() - i2.intValue());
            }
        } );
	}
	public HashSet<String> getConstraints() {
		// TODO Auto-generated method stub
		return this.constraints;
	}

}


