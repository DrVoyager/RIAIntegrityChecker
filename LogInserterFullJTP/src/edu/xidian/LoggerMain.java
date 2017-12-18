package edu.xidian;


/* Soot - a J*va Optimization Framework
 * Copyright (C) 2008 Eric Bodden
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.G;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootMethod;
import soot.Transform;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.grimp.Grimp;
import soot.grimp.NewInvokeExpr;
import soot.jimple.*;
import soot.jimple.internal.AbstractBinopExpr;
import soot.jimple.internal.AbstractInterfaceInvokeExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.util.Chain;

public class LoggerMain {

	//static int counter = 0;
	public static void main(String[] args) {
//		PackManager.v().getPack("jtp").add(
//				new Transform("jtp.myTransform", new BodyTransformer() {
//
//					protected void internalTransform(Body body, String phase, Map options) {
//						new MyAnalysis(new ExceptionalUnitGraph(body));
//						// use G.v().out instead of System.out so that Soot can
//						// redirect this output to the Eclipse console
//						G.v().out.println(body.getMethod());
//					}
//					
//				}));
//		
//		soot.Main.main(args);
		
		PackManager.v().getPack( "jtp" ).add( 
				new Transform("jtp.LogInserter", new BodyTransformer() {
				      protected void internalTransform(Body body, String phase, Map options) {
				        new LogInserter(body);
				      }
				    }) );
		//Scene.v().addBasicClass("java.lang.Math",SootClass.SIGNATURES);
		soot.Main.main(args);
	}
	
	
	
	public static class LogInserter{
		static List<String> systemMethods = null;
		
		public LogInserter(Body aBody) {
			String declaredClass = aBody.getMethod().getDeclaringClass().toString();
			String declaredFunction = aBody.getMethod().toString();
			G.v().out.println("start insertting at class ..."+declaredClass+"function..."+declaredFunction);
			if(declaredClass.contains("edu.xidian.")){
				G.v().out.println("Encounters the Transformer class ...skip...");
				return;
			}
			
			if(systemMethods==null){
				systemMethods = new ArrayList<String>();
				systemMethods.add("encryptUtil.EncryptUtil: java.lang.String getAH(int)");
				systemMethods.add("encryptUtil.EncryptUtil: java.lang.String addAH(java.lang.String,java.lang.String)");
				
			}
			
			
	          PatchingChain units = aBody.getUnits();
	          Iterator<Unit> staticScanIt = null;
	          Unit currStmt = null, firstStmt = null, preStmt, entryStmt;

	          Map<Unit, Unit> ifStmtMap = new HashMap<Unit, Unit>();

	          Local branchLoggerLocal = Grimp.v().newLocal("branchLogger", RefType.v("edu.xidian.BranchLogger"));
	          Local inputLoggerLocal = Grimp.v().newLocal("inputLogger", RefType.v("edu.xidian.DataLogger"));
	          Local outputLoggerLocal = Grimp.v().newLocal("outputLogger", RefType.v("edu.xidian.DataLogger"));
	          Local invokeLoggerLocal = Grimp.v().newLocal("invokeLogger", RefType.v("edu.xidian.InvokeLogger"));
          	
	          aBody.getLocals().add(branchLoggerLocal); 
	          aBody.getLocals().add(invokeLoggerLocal);
	          staticScanIt = units.snapshotIterator();
	          boolean initBranchLogger = false;
	          boolean initDataLogger = false;
	          boolean closeBranchLogger = false;
	          ArrayList<Value> paramList = new ArrayList<Value>();
	          int loggedParamNo = 0;
	          Value iteratorValue = null;
	          char funcType = getFunctionType(declaredFunction);
	          
	          while (staticScanIt.hasNext()) {
	        	    
		            currStmt = (Unit)staticScanIt.next();
		            G.v().out.println("current statement is "+currStmt.toString());
		            //skip non-exception identity Stmt
		            if (currStmt instanceof IdentityStmt){
		            	Value value = ((IdentityStmt)currStmt).getRightOp();
		            	if(!(value instanceof CaughtExceptionRef)){
		            		IdentityStmt is = (IdentityStmt) currStmt;
							Value rightOp = is.getRightOp();
							if (rightOp instanceof ParameterRef && loggedParamNo<2){
								//G.v().out.println("Identity Stmt rightOp is "+ rightOp.toString());
								paramList.add(is.getLeftOp());
								if(loggedParamNo==1&&funcType == 'r'){
									iteratorValue = is.getLeftOp();
								}
								loggedParamNo++;
							}
			            	continue;
		            	}
		            }
		            if(!initBranchLogger){
		            	//init BranchLogger
		            	if(isMapReduceFunction(declaredFunction)){
		            	  aBody.getLocals().add(inputLoggerLocal); 
		            	  initLogger(inputLoggerLocal, units, currStmt, "edu.xidian.DataLogger", "input", declaredFunction);
		            	  aBody.getLocals().add(outputLoggerLocal); 
		            	  initLogger(outputLoggerLocal, units, currStmt, "edu.xidian.DataLogger", "output", declaredFunction);
		            	  initDataLogger = true;
		            	  int paramIndex = 0;
		            	  if(funcType == 'm'){
			            	  for(Value param: paramList){
									logDataBefore(param, inputLoggerLocal, units, currStmt);
							  	paramIndex++;
			            	  }
		            	  }
		            	  if(funcType == 'r'){ //in reduce function, the input 
								logDataBefore(paramList.get(0), inputLoggerLocal, units, currStmt);
		            	  }
		            	}
		            	initLogger(branchLoggerLocal, units, currStmt, "edu.xidian.BranchLogger", "branch", declaredFunction);
		            	initLogger(invokeLoggerLocal, units, currStmt, "edu.xidian.InvokeLogger", "invoke", declaredFunction);
		            	initBranchLogger = true;
		            }
		            if(funcType == 'r'){
						if(currStmt instanceof AssignStmt){
							AssignStmt as = (AssignStmt)currStmt;
							if(as.getRightOp() instanceof AbstractInterfaceInvokeExpr){
								AbstractInterfaceInvokeExpr aiie = (AbstractInterfaceInvokeExpr)as.getRightOp();
								if(aiie.getBase().toString().equals(iteratorValue.toString())&&aiie.getMethod().getSignature().contains("java.lang.Iterable: java.util.Iterator iterator()")){
									iteratorValue = as.getLeftOp();
								}
								if(aiie.getBase().toString().equals(iteratorValue.toString())&&aiie.getMethod().getSignature().contains("java.util.Iterator: java.lang.Object next()")){
									iteratorValue = as.getLeftOp();
								}
							}
							if(as.getRightOp() instanceof CastExpr){
								CastExpr ce = (CastExpr)as.getRightOp();
								if(ce.getOp().toString().equals(iteratorValue.toString())){
									iteratorValue = as.getLeftOp();
									logDataAfter(iteratorValue, inputLoggerLocal, units, currStmt, 1, declaredFunction);
								}
							}
						}
		            }
		            
//		            if (currStmt instanceof DefinitionStmt) {
//						DefinitionStmt ds = (DefinitionStmt) currStmt;
//						Value rightOp = ds.getRightOp();
//						if (rightOp instanceof ParameterRef){
//							G.v().out.println("Definition Stmt rightOp is "+ rightOp.toString());
//							int paramIndex = ((ParameterRef)rightOp).getIndex();
//							if(initDataLogger && paramIndex<2){
//								//Value leftOp = ds.getLeftOp();				
//								logData(rightOp, inputLoggerLocal,units, currStmt, paramIndex);
//							}
//							
//						}
//					}
		            
		            if(currStmt instanceof InvokeStmt){
		            	SootMethod method = ((InvokeStmt)currStmt).getInvokeExpr().getMethod();	
		            	String methodName = method.toString();
		            	//G.v().out.println("invoke stmt method is "+method.toString());
		        		if(method.toString().equals("<java.lang.System: void exit(int)>")){
			            	insertCloseLogger(branchLoggerLocal, "edu.xidian.BranchLogger", units, currStmt);
			            	if(initDataLogger){
			            		insertEnter(inputLoggerLocal, units, currStmt);
				            	insertCloseLogger(inputLoggerLocal, "edu.xidian.DataLogger", units, currStmt);
			            		insertEnter(outputLoggerLocal, units, currStmt);
				            	insertCloseLogger(outputLoggerLocal, "edu.xidian.DataLogger", units, currStmt);
				            	insertCloseLogger(invokeLoggerLocal, "edu.xidian.InvokeLogger", units, currStmt);

			            	}
		        		}else
		        		if(methodName.contains("Context: void write(")&&
		        				isMapReduceFunction(declaredFunction)){ //TODO: temporary solution
		        			InvokeExpr ie = ((InvokeStmt)currStmt).getInvokeExpr();
		    				List<Value> parameters = ie.getArgs();
		    				if(parameters.size()==2){
		    					int paramIndex=0;
			    				for(Value v: parameters){
				        			logDataBefore(v, outputLoggerLocal,units, currStmt);
				        			paramIndex++;
			    				}
		    				}
		        		}else{// normal function invoke
		        			InvokeExpr ie = ((InvokeStmt)currStmt).getInvokeExpr();
		    				List<Value> parameters = ie.getArgs();
		    				logInvokeBefore(parameters, invokeLoggerLocal,units, currStmt,ie.getMethod().getSignature());
		        			logInvokeAfter(new ArrayList<Value>(), invokeLoggerLocal, units, currStmt, ie.getMethod().getSignature());
		    			}
		            }
					if(currStmt instanceof AssignStmt){
						AssignStmt as = (AssignStmt)currStmt;
						if(as.getRightOp() instanceof InvokeExpr){
							InvokeExpr ie = (InvokeExpr)(as.getRightOp());
							Value returnVal = as.getLeftOp();
							
		    				List<Value> parameters = ie.getArgs();
		    					logInvokeBefore(parameters, invokeLoggerLocal,units, currStmt,ie.getMethod().getSignature());
			        			ArrayList<Value> returnList = new ArrayList<Value>();
			        			returnList.add(returnVal);
		    					logInvokeAfter(returnList, invokeLoggerLocal, units, currStmt,ie.getMethod().getSignature());			        			
						}
					}
		            if(currStmt instanceof RetStmt||currStmt instanceof ReturnStmt||currStmt instanceof ReturnVoidStmt){
			            	//G.v().out.println("return stmt is "+currStmt.toString());
			            	insertCloseLogger(branchLoggerLocal, "edu.xidian.BranchLogger",units, currStmt);
			            	if(initDataLogger){
			            		insertEnter(inputLoggerLocal, units, currStmt);
				            	insertCloseLogger(inputLoggerLocal, "edu.xidian.DataLogger", units, currStmt);
				            	insertEnter(outputLoggerLocal, units, currStmt);
				            	insertCloseLogger(outputLoggerLocal, "edu.xidian.DataLogger", units, currStmt);
				            	insertCloseLogger(invokeLoggerLocal, "edu.xidian.InvokeLogger", units, currStmt);
			            	}
		            }

		            //G.v().out.println("currStmt is "+ currStmt.toString());
					//G.v().out.println("currStmt is  "+currStmt.toString()+",hash is "+currStmt.hashCode());

		            if(currStmt instanceof IfStmt){
		            		if(!ifStmtMap.containsKey(currStmt)){ //insert logger
			            		Value orgIfCondition = ((IfStmt) currStmt).getCondition();
			            		//G.v().out.println("IfCondition is "+currStmt.toString());
			            		Unit firstInsertedStmt = logBranch(orgIfCondition,branchLoggerLocal,units, currStmt);
			            		//units.insertBefore(invokeStmt, currStmt);
			            		ifStmtMap.put(currStmt, firstInsertedStmt);
		            		}
		            		// for a if stmt that points to original target, should check if the original target has added
		            		// the logger, if true, update the target.
		            		Unit originalTarget = ((IfStmt)currStmt).getTarget();
		            		if(ifStmtMap.containsKey(originalTarget)){
		            			((IfStmt)currStmt).setTarget(ifStmtMap.get(originalTarget));
		            		}
		            }
		            	
		            // for any stmt go to original if stmt, should 
//		            // redirect to new stmt according to the ifStmtMap
		            if(currStmt instanceof GotoStmt){
		            		Unit originalTarget = ((GotoStmt)currStmt).getTarget();
		            		if(ifStmtMap.containsKey(originalTarget)){
		            			((GotoStmt)currStmt).setTarget(ifStmtMap.get(originalTarget));
		            		}
		            }
	          }
	          
	          //adjust trap start position to make sure loggers are declared at any time, including exception handler.
	          Map<Unit, Integer> unitOrder = new HashMap<Unit, Integer>();// to record the order of each unit, in order to adjust the trap start position.
              int unitIndex = 0;
	          Unit startPosition = null;
	          PatchingChain newUnits = aBody.getUnits();
	          
	          Iterator<Unit> newIt = newUnits.iterator();
	          boolean detectInvokeInitFlag = false;
	          while (newIt.hasNext()) {
		            currStmt = (Unit)newIt.next();
		            G.v().out.println("new body statement is "+currStmt.toString());
		            if(detectInvokeInitFlag)
		            {
		            	startPosition = currStmt;
		            	detectInvokeInitFlag = false;
		            }
		            if(currStmt instanceof InvokeStmt){
		            	SootMethod method = ((InvokeStmt)currStmt).getInvokeExpr().getMethod();	
		        	    if(method.toString().contains("edu.xidian.InvokeLogger: void <init>(java.lang.String,java.lang.String)>"))
		        			  detectInvokeInitFlag = true;
		            }
		            unitIndex++;
		            unitOrder.put(currStmt, unitIndex);
	          }
	          
	          Chain traps = aBody.getTraps();
		      Iterator<Unit> staticScantrap = traps.snapshotIterator();
	            while (staticScantrap.hasNext())
	            {
	                Trap trap = (Trap) staticScantrap.next();
	                Unit tep = trap.getBeginUnit();
	                
	                G.v().out.println("trap.getBeginUnit-Say:"+(tep).toString());
	                G.v().out.println("trap.getBeginUnit-Set to :"+startPosition.toString());
	                if(unitOrder.get(tep)<unitOrder.get(startPosition))
	                	trap.setBeginUnit(startPosition);
	            }
		}

		private boolean isMapReduceFunction(String declaredFunction){
			char functionType = getFunctionType(declaredFunction);
			if(functionType=='m'||functionType =='r')
				return true;
			else
				return false;
		}
		
		private char getFunctionType(String declaredFunction){
			if(declaredFunction.contains("void map("))
				return 'm';
			if(declaredFunction.contains("void reduce("))
				return 'r';
			else
				return 'f';			
		}

		private void insertCloseLogger(Local loggerLocal,String loggerClassSig,
				PatchingChain units, Unit currStmt) {
			SootMethod toCall = Scene.v().getMethod
				      ("<"+loggerClassSig+": void close()>");
			Stmt invokeStmt = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(
					loggerLocal, toCall.makeRef(),new ArrayList()));
			units.insertBefore(invokeStmt,currStmt);
		}

//		private void initBranchLogger(Local transLocal, PatchingChain units,
//				Unit currStmt, String functionSignature) {
//			
//		    soot.jimple.NewExpr sootNew = soot.jimple.Jimple.v().newNewExpr(RefType.v("edu.xidian.BranchLogger"));
//
//		    soot.jimple.AssignStmt stmt = soot.jimple.Jimple.v().newAssignStmt(transLocal, sootNew);
//			//Expr rhs = Jimple.v().newInstanceOfExpr(transLocal, RefType.v("java.util.ArrayList"));
//			units.insertBefore(stmt, currStmt);
//			SpecialInvokeExpr newTrans = Jimple.v().newSpecialInvokeExpr(transLocal,
//					Scene.v().getMethod("<edu.xidian.BranchLogger: void <init>(java.lang.String)>").makeRef(),
//					Arrays.asList(StringConstant.v(functionSignature)));
//			soot.jimple.Stmt invokeStmt = soot.jimple.Jimple.v().newInvokeStmt(newTrans);
//			units.insertBefore(invokeStmt, currStmt);			
//		}
		
		private void initLogger(Local loggerLocal, PatchingChain units,
				Unit currStmt, String className, String type, String declaredFunction) {
			
		    soot.jimple.NewExpr sootNew = soot.jimple.Jimple.v().newNewExpr(RefType.v(className));

		    soot.jimple.AssignStmt stmt = soot.jimple.Jimple.v().newAssignStmt(loggerLocal, sootNew);
			//Expr rhs = Jimple.v().newInstanceOfExpr(transLocal, RefType.v("java.util.ArrayList"));
			units.insertBefore(stmt, currStmt);
			SpecialInvokeExpr newTrans = Jimple.v().newSpecialInvokeExpr(loggerLocal,
					Scene.v().getMethod("<"+className+": void <init>(java.lang.String,java.lang.String)>").makeRef(),
					Arrays.asList(StringConstant.v(declaredFunction), StringConstant.v(type)));
			soot.jimple.Stmt invokeStmt = soot.jimple.Jimple.v().newInvokeStmt(newTrans);
			units.insertBefore(invokeStmt, currStmt);			
		}
		
		private InvokeStmt prepareInsertStmt(Value loggedValue, Local loggerLocal, String className, boolean raw){
			SootMethod toCall = Scene.v().getMethod
				      ("<"+className+": boolean logString(java.lang.String)>");

			Type vType = loggedValue.getType();
			if(vType instanceof IntType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(int)>");
			}else if(vType instanceof LongType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(long)>");
			}else if(vType instanceof ShortType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(short)>");
			}else if(vType instanceof CharType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(char)>");
			}else if(vType instanceof DoubleType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(double)>");
			}else if(vType instanceof FloatType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(float)>");
			}else if(vType instanceof BooleanType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(boolean)>");
			}else if(vType instanceof ByteType){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(byte)>");
			}else if(vType instanceof RefLikeType && !raw){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logString(java.lang.Object)>");
			}else if(vType instanceof RefLikeType && raw){
				toCall = Scene.v().getMethod
					      ("<"+className+": boolean logEncryptString(java.lang.Object)>");
			}
			//inserted code: loggerLocal.logString(values[i]);
			 InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (loggerLocal, toCall.makeRef(), Arrays.asList(loggedValue)));
			 return newInvokeStmt;
		}
		
		private Unit insertEnter(Local dataLoggerLocal, PatchingChain units, Unit currStmt){
			SootMethod toCall = Scene.v().getMethod
		      ("<edu.xidian.DataLogger: boolean logRawString(java.lang.String)>");
			//inserted code: loggerLocal.logString("\n");
			Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
			Jimple.v().newVirtualInvokeExpr
	           (dataLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v("\n"))));
			units.insertBefore(newInvokeStmt, currStmt);
			return newInvokeStmt;
		}
		
		private Unit logDataBefore(Value loggedValue, Local dataLoggerLocal, PatchingChain units, Unit currStmt) {
			Stmt insertedStmt = prepareInsertStmt(loggedValue, dataLoggerLocal,"edu.xidian.DataLogger", false);
			units.insertBefore(insertedStmt, currStmt);
//			toCall = Scene.v().getMethod
//				      ("<edu.xidian.DataLogger: boolean logString(java.lang.String)>");
			//inserted code: loggerLocal.logString("\n");
//			String deliminator = "\t";
//			if(paramIndex == 1){
//				deliminator = methodName+"\n";
//			}
//			Stmt newInvokeStmt2 = Jimple.v().newInvokeStmt(
//					Jimple.v().newVirtualInvokeExpr
//			           (dataLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v(deliminator))));
			//units.insertBefore(newInvokeStmt, currStmt);
			return insertedStmt;
		}
		

		private Unit logDataAfter(Value loggedValue, Local dataLoggerLocal, PatchingChain units, Unit currStmt, int paramIndex, String methodName) {
			Stmt insertedStmt = prepareInsertStmt(loggedValue, dataLoggerLocal, "edu.xidian.DataLogger", false);
			
			units.insertAfter(insertedStmt, currStmt);
			return insertedStmt;
		}
		
		private void logInvokeAfter(List<Value> parameters, Local invokeLoggerLocal,
				PatchingChain units, Unit currStmt, String invokeMethod) {
			boolean isRaw= false;
			for(String med: systemMethods){
				if(invokeMethod.contains(med)){
					isRaw= true;
					break;
				}
			}
			Stmt inFuncCurStmt = (Stmt)currStmt;
			SootMethod toCall = Scene.v().getMethod
				      ("<edu.xidian.InvokeLogger: boolean logAfterInvoke(java.lang.String)>");
			InvokeStmt insertedStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (invokeLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v(invokeMethod))));
			units.insertAfter(insertedStmt, inFuncCurStmt);
			inFuncCurStmt = insertedStmt;
			for(Value v: parameters){
				insertedStmt = prepareInsertStmt(v, invokeLoggerLocal, "edu.xidian.InvokeLogger", isRaw);
				units.insertAfter(insertedStmt, inFuncCurStmt);
				inFuncCurStmt = insertedStmt;
			}		
			toCall = Scene.v().getMethod
				      ("<edu.xidian.InvokeLogger: boolean logRawString(java.lang.String)>");
					//inserted code: loggerLocal.logString("\n");
					Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (invokeLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v("\n"))));
					units.insertAfter(newInvokeStmt, inFuncCurStmt);
		}


		private void logInvokeBefore(List<Value> parameters, Local invokeLoggerLocal,
				PatchingChain units, Unit currStmt, String invokeMethod) {
			boolean isRaw= false;
			for(String med: systemMethods){
				if(invokeMethod.contains(med)){
					isRaw= true;
					break;
				}
			}
			SootMethod toCall = Scene.v().getMethod
				      ("<edu.xidian.InvokeLogger: boolean logBeforeInvoke(java.lang.String)>");
			InvokeStmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (invokeLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v(invokeMethod))));
			units.insertBefore(newInvokeStmt, currStmt);
			for(Value v: parameters){
				Stmt insertedStmt = prepareInsertStmt(v, invokeLoggerLocal, "edu.xidian.InvokeLogger", isRaw);
				units.insertBefore(insertedStmt, currStmt);
			}
			toCall = Scene.v().getMethod
				      ("<edu.xidian.InvokeLogger: boolean logRawString(java.lang.String)>");
					//inserted code: loggerLocal.logString("\n");
					Stmt newInvokeStmt2 = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (invokeLoggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v("\n"))));
					units.insertBefore(newInvokeStmt2, currStmt);
		}
		
		private Unit logBranch(Value orgIfCondition, Local loggerLocal, PatchingChain units, Unit currStmt) {
			
			//String[] params = {"i0","i1"};
			
			//Value[] values = {new JimpleLocal("i0",IntType.v()), new JimpleLocal("i1",IntType.v())};
			
			ArrayList<String> params= new ArrayList<String>();
			ArrayList<Value> values = new ArrayList<Value>();
			
			analyzeCondition(orgIfCondition,params, values);
			
			//ArrayType paramType = ArrayType.v(RefType.v("java.lang.String"), params.length);
			//ArrayList newArgList = new ArrayList();
//			newArgList.add(StringConstant.v("S1"));
//			
//			newArgList.add(ArrayType.v(RefType.v("java.lang.String"), 1));
//			newArgList.add(ArrayType.v(RefType.v("java.lang.String"), 1));
			//newArgList.add();
            //for (int i = 0; i < 3; i++)
            //    newArgList.add(newExpr(v.getArg(i)));
			//counter++;
			//inserted code: loggerLocal.logString("S<counter>\t");
			SootMethod toCall = Scene.v().getMethod
				      ("<edu.xidian.BranchLogger: boolean logCount()>");
			Stmt newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (loggerLocal, toCall.makeRef(), Arrays.asList()));
			Unit newCurrStmt = newInvokeStmt;
			units.insertBefore(newInvokeStmt, currStmt);
			G.v().out.println("insert new invoke statement logCount()");
			for(int i = 0; i< params.size(); i++){
				toCall = Scene.v().getMethod
					      ("<edu.xidian.BranchLogger: boolean logString(java.lang.String)>");
				//inserted code: loggerLocal.logString("\tparams[i]\t");
				newInvokeStmt = Jimple.v().newInvokeStmt(
						Jimple.v().newVirtualInvokeExpr
				           (loggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v(params.get(i)))));
				units.insertBefore(newInvokeStmt, currStmt);
				
				newInvokeStmt = prepareInsertStmt(values.get(i),loggerLocal, "edu.xidian.BranchLogger", false);
				units.insertBefore(newInvokeStmt, currStmt);
			}
			toCall = Scene.v().getMethod
				      ("<edu.xidian.BranchLogger: boolean logRawString(java.lang.String)>");
			//inserted code: loggerLocal.logString("\n");
			newInvokeStmt = Jimple.v().newInvokeStmt(
					Jimple.v().newVirtualInvokeExpr
			           (loggerLocal, toCall.makeRef(), Arrays.asList(StringConstant.v("\n"))));
			units.insertBefore(newInvokeStmt, currStmt);
			//return the first inserted stmt
			return newCurrStmt;
		}

		private void analyzeCondition(Value exp,
				ArrayList<String> params, ArrayList<Value> values) {
			if(exp instanceof AbstractBinopExpr){
				for (Iterator i = exp.getUseBoxes().iterator(); i.hasNext(); ) {
					ValueBox box = (ValueBox) i.next();
					analyzeCondition(box.getValue(), params, values);
				}
			}else if(exp instanceof BinopExpr){
				analyzeCondition(((BinopExpr)exp).getOp1(),params, values);
				analyzeCondition(((BinopExpr)exp).getOp2(),params, values);
			}else if(exp instanceof CastExpr){
				analyzeCondition(((BinopExpr)exp).getOp1(),params, values);
			}else{
				if(exp instanceof Local){
					params.add(((Local)exp).getName());
					values.add(exp);
				}
			}
		}
	}




//	public static class MyAnalysis extends ForwardFlowAnalysisVerification/*extends ForwardFlowAnalysis */ {
//
//		public MyAnalysis(ExceptionalUnitGraph graph) {
//			super(graph);
//
//			Body aBody = graph.getBody();
//			doAnalysis();
//		}
		
		

//		@Override
//		protected void flowThrough(Object in, Object d, Object out) {
//	        FlowSet inSet = (FlowSet) in;
//	        FlowSet outSet = (FlowSet) out;
//	        Unit s = (Unit) d;
//	        G.v().out.println("flow through "+d.toString());
//		}
//
//		@Override
//		protected Object newInitialFlow() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		protected Object entryInitialFlow() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		protected void merge(Object in1, Object in2, Object out) {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		protected void copy(Object source, Object dest) {
//			// TODO Auto-generated method stub
//			
//		}
//
//	}

}