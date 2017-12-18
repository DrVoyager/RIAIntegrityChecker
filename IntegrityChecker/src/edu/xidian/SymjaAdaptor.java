package edu.xidian;



import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.matheclipse.core.expression.F.*; 

import org.matheclipse.core.basic.Config;
import org.matheclipse.core.eval.EvalUtilities;
import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.expression.F;
import org.matheclipse.core.form.output.OutputFormFactory;
import org.matheclipse.core.interfaces.IAST;
import org.matheclipse.core.interfaces.IExpr;
import org.matheclipse.parser.client.SyntaxError;
import org.matheclipse.parser.client.ast.*;
import org.matheclipse.parser.client.math.MathException;

import soot.DoubleType;
import soot.G;
import soot.IntType;
import soot.RefLikeType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.grimp.Grimp;
import soot.grimp.internal.*;
import soot.jimple.AndExpr;
import soot.jimple.BinopExpr;
import soot.jimple.ConditionExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DoubleConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NegExpr;
import soot.jimple.OrExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.AbstractDefinitionStmt;


public class SymjaAdaptor<N,A> {
	
	private Value exp;
	private Stmt stmt;

	private HashMap<String, Value> ExprMap = new HashMap<String, Value>();
	private String conditionLeft;
	private String conditionRight;
	private String conditionRelation;
	private ForwardFlowAnalysisVerification<N,A> analysis= null;

	ExprEvaluator util;
	IExpr result;
	
	private HashSet<String> stringSet = null;
	

	
	//invoke to evaluate if statement branch
	public SymjaAdaptor(Value exp, ForwardFlowAnalysisVerification<N,A> analysis) {
		// TODO Auto-generated constructor stub
		this.analysis = analysis;
		this.exp = exp;
		Config.PARSER_USE_LOWERCASE_SYMBOLS = true;
		util = new ExprEvaluator(false, 100);
		stringSet = findStrings(exp);
	}

	private HashSet<String> findStrings(Value exp) {
		// TODO Auto-generated method stub
		HashSet<String> stringSet = new HashSet<String>();
		traverseExp(exp, stringSet);
		return stringSet;
	}
	
	public void traverseExp(Value subExp, HashSet<String> stringSet){
		if(subExp instanceof NegExpr){
			Value left = ((NegExpr)subExp).getOp();
			traverseExp(left,stringSet);
		}
		if(subExp instanceof BinopExpr){
			Value left = ((BinopExpr)subExp).getOp1();
			Value right = ((BinopExpr)subExp).getOp2();
			traverseExp(left,stringSet);
			traverseExp(right,stringSet);
		}else{
			if(subExp.getType() instanceof RefLikeType){
				stringSet.add(subExp.toString());
			}
		}
	}

	//invoke for update constriants
	public SymjaAdaptor(Stmt s, ForwardFlowAnalysisVerification<N,A> analysis) {
		this.analysis = analysis;
		this.stmt = s;
		Config.PARSER_USE_LOWERCASE_SYMBOLS = true;
		util = new ExprEvaluator(false, 100);
	}
	
	public SymjaAdaptor(ForwardFlowAnalysisVerification<N,A> analysis){
		this.analysis = analysis;
		Config.PARSER_USE_LOWERCASE_SYMBOLS = true;
		util = new ExprEvaluator(false, 100);
	}
	
	public void addInvokeConstraints(Map<String, String> constraintValues){
		//G.v().out.println("flow through the beginning of a function invocation: left is "+paramValue[0]+" and right is "+paramValue[1]);
		if(constraintValues.size()<SystemConfig.ConstraintCap){
			for(String key: constraintValues.keySet()){
				String runtimeValue = filterString(key)+"symbolicvalue="+filterString(constraintValues.get(key));
				result = util.evaluate(runtimeValue);
			}
		}
//		String right = filterString(paramValue[1]);
//		String left = filterString(paramValue[0]);
//		
//		String runtimeValue = left+"symbolicvalue="+right;
//		//G.v().out.println("constraint after filter is "+left+","+right+","+runtimeValue);
//		result = util.evaluate(runtimeValue);
	}
	
	public boolean checkInvokeConstraints(Map<String, String> constraintValues) {
		//G.v().out.println("flowthrough the end of a function invocation"+postParamValues.toString());
		counter++;
		for(String key:constraintValues.keySet()){
			//G.v().out.println("key is "+key);
			//G.v().out.println("value is "+postParamValues.get(key));

			String runtimeValue = filterString(key)+"verifyvalue"+counter+"="+filterString(constraintValues.get(key));
			result = util.evaluate(runtimeValue);
		}
		for(String constraint: analysis.getConstraints()){
				String verifyConstraint = constraint.replace("symbolicvalue", "verifyvalue"+counter);
				result = util.evaluate(verifyConstraint);
				//G.v().out.println("integrity checking... constraint: "+verifyConstraint);
	
				if(result.toString().equals("False")){
					throw new RuntimeException("Integrity is violated!!! on invoke return statement "+constraintValues.toString()+")...");
				}
			}	
        
		return true;
	}
	
	public boolean evalBranchExp(TraceLog curBranchValue) {
		//G.v().out.println("flowthrough if stmt "+exp.toString());
		//G.v().out.println("before process, current Constraints are "+CheckerMain.constraints.toString());

		//add String
		for(String s:curBranchValue.getParams().keySet()){
			String runtimeValue = filterString(s)+"runtimevalue"+"="+filterTraceLogString(s,curBranchValue);
			//G.v().out.println("Runtime values: "+runtimeValue);
			result = util.evaluate(runtimeValue);	
		}
		if(!checkIntegrity(curBranchValue))
			throw new RuntimeException("Integrity is violated!!! on if statement if("+exp.toString()+")...");
		
		boolean evalResult = evalSubExp(exp);
		//G.v().out.println("after process, current Constraints are "+CheckerMain.constraints.toString());
		return evalResult;
	}
	

	static int counter = 0;
	
	private boolean checkIntegrity(TraceLog curBranchValue) {
		counter++;
		//add String
		for(String s:curBranchValue.getParams().keySet()){
			String runtimeValue = filterString(s)+"verifyvalue"+counter+"="+filterTraceLogString(s,curBranchValue);
			result = util.evaluate(runtimeValue);
			//G.v().out.println("integrity checking... runtimevalues: "+runtimeValue);
		}

		for(String constraint: analysis.getConstraints()){
			String verifyConstraint = constraint.replace("symbolicvalue", "verifyvalue"+counter);
			result = util.evaluate(verifyConstraint);
			//G.v().out.println("integrity checking... constraint: "+verifyConstraint);

			if(result.toString().equals("False"))
				return false;
		}
		return true;
	}
	
	private String filterTraceLogString(String key, TraceLog curBranchValue){
		String result = curBranchValue.params.get(key);
		if(stringSet.contains(key)){
		
			 result = result.replace("$", "dollar");
			 result=result.replace(".", "dot");
			 result=result.replace("<", "LT");
			 result=result.replace(">", "GT");
			 result=result.replace("*", "mul");
			 result=result.replace("-", "MINU");
			 result=result.replace(" ", "SPACE");
			 result=result.replace(":", "COLOMN");
			 result=result.replace("~", "TILDE");
			 result=result.replace("&", "AND");
			 result=result.replace("`", "APOSTROPHE");
			 result=result.replace("{", "CURLYLBR");
			 result=result.replace("}", "CURLYRBR");
			 //result=result.replace("\0", "EMPTY");



			 result=result.replace("(", "LB");
			 result=result.replace(")", "RB");
			 result = result.replace("[", "LSB");
			 result = result.replace("]", "RSB");
			 result = result.replace("%", "REM");
			 result = result.replace("+", "PLUS");
			 result = result.replace("/", "DIV");
			 result = result.replace(",", "COMMA");
			 result = result.replace("_", "UNSCORE");
			 result = result.replace("\"", "DQUOTE");
			 result = result.replace("\'", "QUOTE");
			 result = result.replace("!", "EXCLAMATION");
			 result = result.replace("^", "HAT");
			 result = result.replace("#", "SHARP");
			 result = result.replace("=", "EQUAL");
			 result = result.replace("?", "QUESTION");
			 result = result.replace("\\", "BACKSLASH");
			 result=result.replace(";", "SEMICOLOMN");
			 result=result.replace("|", "OR");

			 result=result.replace("0", "ZERO");
			 result = result.replace("@", "at");
			 //TODO:
			 // filter all arithmetic operation when 
			 // the operation is within a function call
			 // e.g.: this.fun1(a*b+c).
			 // 
		}else{
			//replace "true to 1 and false to 0, as what jimple represents"
			 result = result.replace("true", "1");
			 result = result.replace("false", "0");
		}

		return result;
	}


	private boolean constraintSwitch = true;
	
	public boolean evalSubExp(Value subExp){
		if(subExp instanceof NegExpr){
			Value left = ((NegExpr)subExp).getOp();
			//Value right = ((BinopExpr)term).getOp2();
			//String operator = ((BinopExpr)term).getSymbol().trim();
			return !evalSubExp(left);
		}
		if(subExp instanceof AndExpr){
			Value left = ((BinopExpr)subExp).getOp1();
			Value right = ((BinopExpr)subExp).getOp2();
			return evalSubExp(left)&&evalSubExp(right);
		}
		if(subExp instanceof OrExpr){
			constraintSwitch = false;
			Value left = ((BinopExpr)subExp).getOp1();
			Value right = ((BinopExpr)subExp).getOp2();
			boolean leftValue = evalSubExp(left);
			boolean rightValue = evalSubExp(right);
			constraintSwitch = true;
			return leftValue||rightValue;
		}else{
			String expString = Exp2String(subExp, true);
			//G.v().out.println("original exp: "+exp.toString());

			//G.v().out.println("transformed exp: "+expString);
			result = util.evaluate(expString);
			//G.v().out.println("evaluation result: "+result.toString());

			if(result.toString().equals("True")){
				if(constraintSwitch){
					String addedConstraint =conditionLeft+conditionRelation+conditionRight;
					//G.v().out.println("added constraint 4(if true) "+addedConstraint);
					addConstraint(addedConstraint);
				}
				return true;
			}
			else{
				if(constraintSwitch){
					String addedConstraint = conditionLeft+getOppositeOperator(conditionRelation)+conditionRight;
					//G.v().out.println("added constraint 4(if false) "+addedConstraint);

					addConstraint(addedConstraint);
				}
				return false;
			}
		}
	}

	public  Value processASTNode(ASTNode node){
		//G.v().out.println("entered node is "+node.toString());
		if (node == null) {
		      return null;
		}
		//String returnStr = "";
		
	    if (node instanceof FunctionNode) {
	      final FunctionNode functionNode = (FunctionNode) node;
	      ASTNode fun = (ASTNode) functionNode.get(0);
	      String funName = fun.toString();
	      if(funName.equals("Plus")){
	    	  //G.v().out.println("Plus:"+(ASTNode) functionNode.get(1));
	    	  //G.v().out.println("Plus:"+(ASTNode) functionNode.get(2));
	    	  Value leftOp = processASTNode((ASTNode) functionNode.get(1));
	    	  Value rightOp = processASTNode((ASTNode) functionNode.get(2));
	    	  //G.v().out.println("JAddExpr:"+leftOp.toString());
	    	  //G.v().out.println("JAddExpr:"+rightOp.toString());
	    	  return Grimp.v().newAddExpr(leftOp, rightOp);//new JAddExprExt(leftOp, rightOp);
	      }
	      if(funName.equals("Times")){
	    	  //G.v().out.println("Times:"+(ASTNode) functionNode.get(1));
	    	  //G.v().out.println("Times:"+(ASTNode) functionNode.get(2));
	    	  Value leftOp = processASTNode((ASTNode) functionNode.get(1));
	    	  Value rightOp = processASTNode((ASTNode) functionNode.get(2));
	    	  return Grimp.v().newMulExpr(leftOp, rightOp);//new JMulExpr(leftOp, rightOp);
	      }
	      if(funName.equals("SubtractFrom")){
	    	  Value leftOp = processASTNode((ASTNode) functionNode.get(1));
	    	  Value rightOp = processASTNode((ASTNode) functionNode.get(2));
	    	  return Grimp.v().newSubExpr(leftOp, rightOp);//new JSubExpr(leftOp, rightOp);
	      }
	      if(funName.equals("DivideBy")){
	    	  Value leftOp = processASTNode((ASTNode) functionNode.get(1));
	    	  Value rightOp = processASTNode((ASTNode) functionNode.get(2));
	    	  return Grimp.v().newDivExpr(leftOp, rightOp);//new JDivExpr(leftOp, rightOp);
	      }
	      if(funName.equals("Power")){
		      SootClass mathClass = Scene.v().loadClassAndSupport("java.lang.Math");
		      
		      SootMethod ltoCall = mathClass.getMethodByName("pow");

		      ArrayList<Value> lhsParams = new ArrayList<Value>();
		      //lhsParams.add(Grimp.v().newCastExpr(OPEValue, DoubleType.v()));
		      lhsParams.add(processASTNode((ASTNode) functionNode.get(1)));
		      lhsParams.add(processASTNode((ASTNode) functionNode.get(2)));
		      return Grimp.v().newStaticInvokeExpr(ltoCall.makeRef(), lhsParams);
	      }else{
	    	  return null;
	      }
		}
	    else if (node instanceof SymbolNode) {
	    	//G.v().out.println("SymbolNode:"+node.toString());
	    	String symbol = node.getString();
	        
	    	if(ExprMap.containsKey(symbol)){
	    		Value symbolValue = ExprMap.get(symbol);
	    		if(symbolValue.getType() instanceof  RefType){
	    			SootClass mathClass = Scene.v().loadClassAndSupport("java.lang.System");
	  		      
	  		      SootMethod ltoCall = mathClass.getMethodByName("identityHashCode");
	  		    ArrayList<Value> lhsParams = new ArrayList<Value>();
	  		    lhsParams.add(symbolValue);
	    			Value invokeValue = Grimp.v().newStaticInvokeExpr(ltoCall.makeRef(), lhsParams);
	    			return Grimp.v().newCastExpr(invokeValue,DoubleType.v());
	    		}else
	    			return Grimp.v().newCastExpr(symbolValue,DoubleType.v());
	    	}
	    	else
	    		return null;//node.getString();
	    }
	    //useless
	    else if (node instanceof PatternNode) {
	        PatternNode pn = (PatternNode) node;
		    //G.v().out.println("PatternNode:"+pn.getString());

	        if(ExprMap.containsKey(pn.toString())){
	        	Value patternValue = ExprMap.get(pn.toString());
	        	if(patternValue.getType() instanceof RefType){
	        		SootClass mathClass = Scene.v().loadClassAndSupport("java.lang.System");
		  		      
		  		      SootMethod ltoCall = mathClass.getMethodByName("identityHashCode");
		  		    ArrayList<Value> lhsParams = new ArrayList<Value>();
		  		    lhsParams.add(patternValue);
		    			Value invokeValue = Grimp.v().newStaticInvokeExpr(ltoCall.makeRef(), lhsParams);
		    			return Grimp.v().newCastExpr(invokeValue,DoubleType.v());
	        	}
	    		return Grimp.v().newCastExpr(ExprMap.get(patternValue),DoubleType.v());
	        }else
	    		return null;//node.getString();
	        //if()
		      //G.v().out.println("PatternNode:"+pn.getString());

	        //return pn.getString();
		      //return null;
	    }
	    else if (node instanceof NumberNode) {
	    	//G.v().out.println("NumberNode:"+node.toString());
	        //final IntegerNode integerNode = (IntegerNode) node;
	        String iStr = node.toString();
		      //G.v().out.println("NumberNode:"+iStr);

	        //return iStr;
		      return DoubleConstant.v(Double.parseDouble(iStr));
	    }
	    // not triggered
	    else if (node instanceof StringNode) {
		      //G.v().out.println("StringNode:"+node.getString());

	        return null;
	    }else{
	    	//G.v().out.println("otherNode:"+node.getString());
	    	return null;
	    }
	}

	private String Exp2String(Value term, boolean runTimeEval) {
		if(term instanceof Constant){
			String constant = ((Constant)term).toString();
			constant = filterConstant(constant);
			//constant = filterString(constant);
			//ExprMap.put(constant, IntConstant.v(Integer.parseInt(constant)));
			//G.v().out.println("ExprMap put:"+constant+":"+ term.toString());
			return constant;
		}
		if(term instanceof NegExpr){
			Value subOp = ((NegExpr)term).getOp();
			return "-1*("+Exp2String(subOp,runTimeEval)+")";
		}
		if(term instanceof BinopExpr){
			Value left = ((BinopExpr)term).getOp1();
			Value right = ((BinopExpr)term).getOp2();
			String operator = ((BinopExpr)term).getSymbol().trim();
			//TODO: translate jimple operator into symbolic mathematical operations.
			// So far just treat them as variable
			if(operator.equals(Jimple.CMPL)||
					operator.equals(Jimple.CMPG)||
					operator.equals(Jimple.CMP)||
					operator.equals("%")){
				String variable = term.toString();
				variable = filterString(variable);
				ExprMap.put(variable,  term);
				//G.v().out.println("ExprMap put:"+variable+":"+ term.toString());
				return variable+(runTimeEval?"runtimevalue":"symbolicvalue");
			}else{
				String leftStr = Exp2String(left,runTimeEval);
				String rightStr = Exp2String(right,runTimeEval);
				if(term instanceof ConditionExpr){
					conditionLeft = leftStr;
					conditionRight = rightStr;
					conditionRelation = operator;
				}
				return "("+leftStr+")"+operator+"("+rightStr+")";
			}
		}
		else{
			String variable = term.toString();
			variable = filterString(variable);
			ExprMap.put(variable,  term);
			//G.v().out.println("ExprMap put:"+variable+":"+ term.toString());
			return variable+(runTimeEval?"runtimevalue":"symbolicvalue");	
		}
	}
	
	private String Exp2StringWithRecord(Value term, List<String> variables) {
		if(term instanceof Constant){
			String constant = ((Constant)term).toString();
			constant = filterConstant(constant);
			//constant = filterString(constant);
			//ExprMap.put(constant, IntConstant.v(Integer.parseInt(constant)));
			//G.v().out.println("ExprMap put:"+constant+":"+ term.toString());
			return constant;
		}
		if(term instanceof NegExpr){
			Value subOp = ((NegExpr)term).getOp();
			return "-1*("+Exp2StringWithRecord(subOp, variables)+")";
		}
		if(term instanceof BinopExpr){
			Value left = ((BinopExpr)term).getOp1();
			Value right = ((BinopExpr)term).getOp2();
			String operator = ((BinopExpr)term).getSymbol().trim();
			//TODO: translate jimple operator into symbolic mathematical operations.
			// So far just treat them as variable
			if(operator.equals(Jimple.CMPL)||
					operator.equals(Jimple.CMPG)||
					operator.equals(Jimple.CMP)||
					operator.equals("%")){
				String variable = term.toString();
				variable = filterString(variable);
				ExprMap.put(variable,  term);
				//G.v().out.println("ExprMap put:"+variable+":"+ term.toString());
				return variable+"symbolicvalue";
			}else{
				String leftStr = Exp2StringWithRecord(left,variables);
				String rightStr = Exp2StringWithRecord(right,variables);
				if(term instanceof ConditionExpr){
					conditionLeft = leftStr;
					conditionRight = rightStr;
					conditionRelation = operator;
				}
				return "("+leftStr+")"+operator+"("+rightStr+")";
			}
		}
		else{
			String variable = term.toString();
			variable = filterString(variable);
			ExprMap.put(variable,  term);
			//G.v().out.println("ExprMap put:"+variable+":"+ term.toString());
			variables.add(variable+"symbolicvalue");
			return variable+"symbolicvalue";	
		}
	}
	
	public void updateConstraints(String constraint){
		addConstraint(constraint);
	}
	
	public void updateConstraints() {
		//G.v().out.println("flowthrough stmt "+stmt.toString());
		//G.v().out.println("before process, current Constraints are "+CheckerMain.constraints.toString());

		if(stmt instanceof DefinitionStmt){
			AbstractDefinitionStmt defStmt = (AbstractDefinitionStmt)stmt;
			Value left = defStmt.getLeftOp();
			Value right = defStmt.getRightOp();
			String leftString = Exp2String(left,false);
			//G.v().out.println("leftString "+leftString);
			List<String> usedVariables = new ArrayList<String>();
			String rightString = Exp2StringWithRecord(right, usedVariables);
			
			//G.v().out.println("rightString "+rightString);
			//G.v().out.println("usedVariables x: "+usedVariables);


			List<String> constraintsY = searchConstraint(leftString, true);
			//G.v().out.println("removed constraintsY "+constraintsY);

			
//			Map<String, List<String>> constraintsX = new HashMap<String, List<String>>();
//
//			
//			for(String usedVariable: usedVariables){
//				List<String> searchOutput = searchConstraint(usedVariable, false);
//				if(searchOutput.size()>0)
//					constraintsX.put(usedVariable,searchOutput);
//			}
//			G.v().out.println("found constraintsX "+constraintsX);

			//with probability
			//usedVariables.contains(y) 
			//constraints U= C(Fy-1(*.y))
			if(usedVariables.contains(leftString)){
				String reverseConstraint = reverseStmt(leftString+"_new", rightString, leftString).replace("_new","");
				for(String constraintY:constraintsY){
					String addedConstraint = constraintY.replace(leftString, "("+reverseConstraint+")");
					//G.v().out.println("added constraint y=F(*,y) "+addedConstraint);
					addConstraint(addedConstraint);
				}
			}else{
			
				//constraintsX.contains(xi)
				//constraints U= C(Fx1(-1)(x2, ..., xn, y)
				//...
				//constraints U= C(Fxn(-1)(x1, ..., xn-1, y)
//				if(constraintsX.size()>0){
//					for(String constraintVariableX: constraintsX.keySet()){
//						String reverseConstraint = reverseStmt(leftString, rightString, constraintVariableX);
//						G.v().out.println("added constraint 2 "+constraintVariableX+"=="+reverseConstraint);
//	
//						addConstraint(constraintVariableX+"=="+reverseConstraint);
//					}
//				}
				
				//constraints U= y=F(x1, x2, ..., xn)
				//G.v().out.println("added constraint y=F(x) :"+leftString+"=="+rightString);
				addConstraint(leftString+"=="+rightString);
			}
		}else{
			//func(*,y)
			if(stmt instanceof InvokeStmt){
				InvokeExpr ie = ((InvokeStmt)stmt).getInvokeExpr();
				List<Value> parameters = ie.getArgs();
				for(Value v: parameters){
                    Type type = v.getType();
					if(type instanceof RefLikeType){
						searchConstraint(Exp2String(v,false),true);
					}
				}
			}
		}
		//G.v().out.println("after process, current Constraints are "+CheckerMain.constraints.toString());
	}
	
	private void addConstraint(String equation) {
		
		if(Math.random()*100<SystemConfig.considerRate){
			result = util.evaluate(equation.replace("runtimevalue", "symbolicvalue"));

			analysis.getConstraints().add(result.toString());
	        
		}
		//G.v().out.println("added constraint :"+result.toString());
	}

	private String reverseStmt(String leftString, String rightString, String transformTarget) {
		String runtimeValue = "Solve("+leftString+"=="+rightString+", "+transformTarget+")";
		result = util.evaluate(runtimeValue);
		//G.v().out.println("Solve equation: "+runtimeValue);
		//G.v().out.println("Solve result: "+result.toString());
		String solution = result.toString();
		
		return solution.substring(solution.indexOf("->")+2, solution.length()-2);
	}

	private List<String> searchConstraint(String variable,boolean removeItem) {
		List<String> constraintList = new ArrayList<String>();

		for(String constraint: analysis.getConstraints()){
			//G.v().out.println("search constraint is "+constraint+", variable is "+variable);

			if(extractVariables(constraint).contains(" "+variable+" ")){

				constraintList.add(constraint);
			}
		}
		if(removeItem){
			for(String str:constraintList){
				analysis.getConstraints().remove(str);
				//G.v().out.println("remove constraint 4 "+str);
			}
		}
        
		return constraintList;
	}
	
	private String getOppositeOperator(String operator) {
		if(operator.equals("==")){
			return "!=";
		}else if(operator.equals("!=")){
			return "==";
		}else if(operator.equals(">=")){
			return "<";
		}else if(operator.equals(">")){
			return "<=";
		}else if(operator.equals("<=")){
			return ">";
		}else if(operator.equals("<")){
			return ">=";
		}else{
			G.v().out.println("######unrecogonized operator#######");
			return "#######";
		}
	}

	private String filterConstant(String constant) {
		// TODO Auto-generated method stub
		String result = constant;
		if(result.equals("null"))
			result = "0";
		else if(result.equals("true"))
			result = "1";
		else if(result.equals("false"))
			result = "0";
		return result;
	}

	//replace $ with "dollar".
	private String filterString(String org){
		String result = org;
		if(result.equals(""))
			result = "empty";
		if(result.equals("null"))
			result = "0";
		else{
			result=result.replaceAll("[^\\x20-\\x7e]", "SPECIAL");
			 result = result.replace("$", "dollar");
			 result=result.replace(".", "dot");
			 result=result.replace("<", "LT");
			 result=result.replace(">", "GT");
			 result=result.replace("*", "mul");
			 result=result.replace("-", "MINU");
			 result=result.replace(" ", "SPACE");
			 result=result.replace(":", "COLOMN");
			 result=result.replace("(", "LB");
			 result=result.replace(")", "RB");
			 result = result.replace("[", "LSB");
			 result = result.replace("]", "RSB");
			 result = result.replace("%", "REM");
			 result = result.replace("+", "PLUS");
			 result = result.replace("/", "DIV");
			 result = result.replace(",", "COMMA");
			 result = result.replace("_", "UNSCORE");
			 result = result.replace("\"", "DQUOTE");
			 result = result.replace("\'", "QUOTE");
			 result = result.replace("@", "at");
			 result = result.replace("!", "FACTOR");
			 result = result.replace("?", "QUESTION");
			 result = result.replace(";", "SEMCOLOMN");
			 result = result.replace("&", "AND");
			 result = result.replace("~", "TILDE");
			 result = result.replace("`", "APOSTROPHE");
			 result = result.replace('=', 'E');
			 result = result.replace("{", "leftcurl");
			 result = result.replace("}", "rightcurl");
			 result = result.replace("|", "OR");
			 result = result.replace("^", "hat");

			 result = result.replace(" ", "space");
			 result = result.replace("\t", "tab");



			 
			 //TODO:
			 // filter all arithmetic operation when 
			 // the operation is within a function call
			 // e.g.: this.fun1(a*b+c).
			 // 
		}
		return result;
	}
	
	
	private String extractVariables(String org){
		String result = " "+org+" ";
		
			 result = result.replace("$", " ");
			 result=result.replace(".", " ");
			 result=result.replace("<", " ");
			 result=result.replace(">", " ");
			 result=result.replace("*", " ");
			 result=result.replace("-", " ");
			 result=result.replace(" ", " ");
			 result=result.replace(":", " ");
			 result=result.replace("(", " ");
			 result=result.replace(")", " ");
			 result = result.replace("[", " ");
			 result = result.replace("]", " ");
			 result = result.replace("%", " ");
			 result = result.replace("+", " ");
			 result = result.replace("/", " ");
			 result = result.replace(",", " ");
			 result = result.replace("_", " ");
			 result = result.replace("\"", " ");
			 result = result.replace("\'", " ");
			 result = result.replace("@", " ");
			 result = result.replace("=", " ");
			 result = result.replace("!", " ");
			 

			 //TODO:
			 // filter all arithmetic operation when 
			 // the operation is within a function call
			 // e.g.: this.fun1(a*b+c).
			 // 
		
		return result;
	}


}
