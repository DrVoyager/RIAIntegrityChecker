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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

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

/**
 *   Abstract class that provides the fixed point iteration functionality
 *   required by all ForwardFlowAnalyses.
 *  
 */
public abstract class ForwardFlowAnalysisVerification<N,A> extends FlowAnalysis<N,A>
{
	LineIterator branchLogIt;
	LineIterator invokeLogIt;
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
        	if (currFile.isFile()&&currFile.getAbsolutePath().contains(methodSig)&&currFile.getAbsolutePath().contains("branch_")){
             processFile(currFile.getAbsolutePath());
        	}
        }		
	}
    
    /*
     * for each invoke file recording methodSig invocation
     * log file name: invoke_methodSig_randomNo
     * records: callNo 0 <parameter values of methodSig>
     *          callNo 1 <parameter values of methodSig>
     * Simulate the method (see function simulate)
     */
    
    //Analyze execution of function "methodSig"
    protected void processFile(String filePath)
    {

//        File dir = new File(SystemConfig.traceLogDir);
//        File file[] = dir.listFiles();
//		ArrayList<String> fileList = new ArrayList<String>();
//		
//		//record all branch logs with methodSig
//        for (int i = 0; i < file.length; i++) {
//        	File currFile = file[i];
//            if (currFile.isFile()&&currFile.getAbsolutePath().contains(methodSig)&&currFile.getAbsolutePath().contains("branch_"))
//                fileList.add(file[i].getAbsolutePath());
//        }
//        
//        
//        for(String filePath: fileList){
//            G.v().out.println("parsing branch file "+filePath);
            int preInvokeParamNo = 0;
            int postInvokeParamNo = 0;
        	File logFile = new File(filePath);
        	
        	fileSuffix = Integer.parseInt(filePath.substring(filePath.lastIndexOf("_")+1));
        	//find the invoke_ file that records the invocation of function methodSig
        	String invokeFileName = filePath.replace("branch_", "invoke_");
        	//if(methodSig.contains("map(")||methodSig.contains("reduce(")){
        	//	invokeFileName = filePath.replace("branch_", "input_");
        	//}
        	//G.v().out.println("Testing invoke file:"+invokeFileName);
        	File invokeFile = new File(invokeFileName);
        	
        	if(!invokeFile.exists()){
            	G.v().out.println("invoke file does not exist:"+invokeFileName);

        		return;
        	}
            //G.v().out.println("parsing invoke file "+invokeFileName);

            
    		try {
    			branchLogIt = FileUtils.lineIterator(logFile, "UTF-8");
    			invokeLogIt = FileUtils.lineIterator(invokeFile,"UTF-8");
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		
	        //G.v().out.println("start checking...");

	        
	        long currCallNumber = 0;
	        
	        bufTrace = getNextBranchTraceLog();
            //currCallNumber = bufTrace.getCallNumber();

	        while(bufTrace!=null){
	        	//make sure a new invoke
	            assert currCallNumber != bufTrace.getCallNumber();
	            currCallNumber  = bufTrace.getCallNumber();

		            InvokeValues invokeValues = new InvokeValues();
		            String preInvoke="", postInvoke="";
		            //obtain new invoke pair
		        	if(invokeLogIt.hasNext()){
		        		preInvoke = invokeLogIt.next();
		        		if(preInvokeParamNo ==0)
		        			preInvokeParamNo = preInvoke.split(SystemConfig.deliminator).length;

		        		//G.v().out.println("processed string is "+preInvoke);
		        		if(invokeLogIt.hasNext()){
			        		postInvoke = invokeLogIt.next();
			        		if(postInvokeParamNo == 0)
			        			postInvokeParamNo = postInvoke.split(SystemConfig.deliminator).length;
			        		//G.v().out.println("processed string is "+postInvoke);
			        	}else{
			        		//fail safe: to avoid incomplete log file
			        		bufTrace = null;
			        		continue;
			        	}
		        	}else{
		        		//end of invoke log file
		        		bufTrace = null;
		        		continue;
		        	}
		        	

		        	//fail safe: to avoid incomplete invoke log file
		        	if(preInvoke.split(SystemConfig.deliminator).length!=preInvokeParamNo||postInvoke.split(SystemConfig.deliminator).length!=postInvokeParamNo){
		        		bufTrace = null;
		        		continue;
		        	}
		        	
		        	invokeValues.processLine(preInvoke);
		        	invokeValues.processLine(postInvoke);
		        	auditedFunNo++;
		        	
		        	//with 1-testRatio/1000 probability skip simulating the current invocation
		        	if(Math.random()*1000>=SystemConfig.testRatio){
		        		
		        		while(bufTrace.getCallNumber()==invokeValues.getCallNumber()){
		        			bufTrace = getNextBranchTraceLog();
		        		}		        		
		        		continue;
		        	}
        	
		        	//simulate
		        	simulate(invokeValues);
	        }
	        
	        branchLogIt.close();
	        invokeLogIt.close();
        //}
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

        	//Check constraints when returning from the invocation
        	//(Obtain the variable name of return value)       	
        	if(currentNode instanceof RetStmt||currentNode instanceof ReturnStmt||currentNode instanceof ReturnVoidStmt){
            	if(currentNode instanceof ReturnStmt){
            		Value returnId = ((ReturnStmt)currentNode).getOp();
            		invokeValues.setReturnId(returnId.toString());
            	}
        		checkConstraints(invokeValues.getConstraints(InvokeValues.POSTPARAM_TYPE));
            }
        	
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
        	
        	List<N> successors = graph.getSuccsOf(currentNode);
        	//branch statement:
        	// determine the next statement node
        	if(isBranchStmt(currentNode)){ //multiple successors, branch statement
        		curTrace = bufTrace;
        		bufTrace = getNextBranchTraceLog();
        		nextNode = getNextUpdateAndCheckConstraint(currentNode, successors, curTrace);
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
        		updateConstraints(currentNode);
        	}
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
		if(preInvokeStr!=null && postInvokeStr!=null){
			String[] logStrings = {preInvokeStr, postInvokeStr};
			return logStrings;
		}else
			return null;		
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

	private TraceLog getNextBranchTraceLog() {
        
            if (branchLogIt.hasNext()) {
            String line = branchLogIt.nextLine();
            return new BranchTraceLog(line);
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


