package edu.xidian;




import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.grimp.Grimp;
import soot.grimp.internal.*;
import soot.jimple.BinopExpr;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NegExpr;


public class SymjaAdaptor {

	public static void main(String[] args){
			try {
				// don't distinguish between lower- and uppercase identifiers
				Config.PARSER_USE_LOWERCASE_SYMBOLS = true;

				ExprEvaluator util = new ExprEvaluator(false, 100);
				
				// Show an expression in the Java form:
				// Note: single character identifiers are case sensistive
				// (the "D()" function input must be written as upper case character)
				String javaForm = util.toJavaForm("D(sin(x)*cos(x),x)");
				// prints: D(Times(Sin(x),Cos(x)),x)
				System.out.println(javaForm.toString());

				// Use the Java form to create an expression with F.* static methods:
				IAST function = D(Times(Sin(x), Cos(x)), x);
				IExpr result = util.evaluate(function);
				// print: Cos(x)^2-Sin(x)^2
				System.out.println(result.toString());

				// evaluate the string directly
				// Note "diff" is an alias for the "D" function
				result = util.evaluate("diff(sin(x)*cos(x),x)");
				// print: Cos(x)^2-Sin(x)^2
				System.out.println(result.toString());

				// evaluate the last result ($ans contains "last answer")
				result = util.evaluate("$ans+cos(x)^2");
				// print: 2*Cos(x)^2-Sin(x)^2
				System.out.println(result.toString());

				// evaluate an Integrate[] expression
				result = util.evaluate("integrate(sin(x)^5,x)");
				// print: 2/3*Cos(x)^3-1/5*Cos(x)^5-Cos(x)
				System.out.println(result.toString());

				// set the value of a variable "a" to 10
				// Note: in server mode the variable name must have a preceding '$' character
				result = util.evaluate("a=10");
				// print: 10
				System.out.println(result.toString());
				
				// do a calculation with variable "a"
				result = util.evaluate("a*3==20");
				// print: 30+b
				System.out.println(result.toString());

				// Do a calculation in "numeric mode" with the N() function
				// Note: single character identifiers are case sensistive
				// (the "N()" function input must be written as upper case character)
				result = util.evaluate("N(sinh(5))");
				// print: 74.20321057778875
				System.out.println(result.toString());

				// define a function with a recursive factorial function definition.
				// Note: fac(0) is the stop condition which must be defined first.
				result = util.evaluate("fac(0)=1;fac(x_IntegerQ):=x*fac(x-1)");
				// now calculate factorial of 10:
				result = util.evaluate("fac(10)");
				// print: 3628800
				System.out.println(result.toString());

			} catch (SyntaxError e) {
				// catch Symja parser errors here
				System.out.println(e.getMessage());
			} catch (MathException me) {
				// catch Symja math errors here
				System.out.println(me.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		
	}
	
	private Value exp;

	private HashMap<String, Value> ExprMap = new HashMap<String, Value>();
	
	public SymjaAdaptor(Value exp) {
		// TODO Auto-generated constructor stub
		this.exp = exp;
	}

	public boolean evalExp(TraceLog curBranchValue) {
		
		Config.PARSER_USE_LOWERCASE_SYMBOLS = true;
		ExprEvaluator util = new ExprEvaluator(false, 100);
		IExpr result;
		
		//add String
		for(String s:curBranchValue.getParams().keySet()){
			result = util.evaluate(filterString(s)+"="+curBranchValue.params.get(s));
		}
		String expString = Exp2String(exp);
		G.v().out.println("original exp: "+exp.toString());

		G.v().out.println("transformed exp: "+expString);
		result = util.evaluate(expString);
		if(result.toString().equals("True"))
			return true;
		else
			return false;
	}
	
//	public Value getTransformedCond(boolean relax, Relation relation) {
//		G.v().out.println("getTransformedCond:left="+left.toString());
//		G.v().out.println("getTransformedCond:right="+right.toString());
//
//		String leftStr = Exp2String(left);
//		String rightStr = Exp2String(right);
//		G.v().out.println("leftStr = "+leftStr);
//		G.v().out.println("rightStr = "+rightStr);
//		
//		F.initSymbols();
//	    EvalUtilities util = new EvalUtilities();
//
//	    IExpr result;
//		FileOutputStream fop = null;
//		File file;
//	    try {
//	      StringBufferWriter buf = new StringBufferWriter();
//	      
//	      result = util.evaluate(leftStr);
//	      OutputFormFactory.get().convert(buf, result);
//	      String leftOutput = buf.toString();
//	      //G.v().out.println("Expanded form for " + leftStr + " is " + leftOutput);
//
//	      buf = new StringBufferWriter();
//	      result = util.evaluate(rightStr);
//	      OutputFormFactory.get().convert(buf, result);
//	      String rightOutput = buf.toString();
//	      //G.v().out.println("Expanded form for " + rightStr + " is " + rightOutput);
//	      
//	      String OPEexpr = leftOutput+"-("+rightOutput+")+"+new Double(this.balancevalue).toString();
//	      String E0 = new Double(this.balancevalue).toString();
//	      
//	      // write <siteNum\tE0\n> to /tmp/OPEindex
//	      String filename = "/tmp/OPEindex";
//	      file = new File(filename);
//			
//			// if file doesn't exists, then create it
//			if (!file.exists()) {
//				file.createNewFile();
//			}
//			FileWriter writer = new FileWriter(filename, true); 
//		    // Writes the content to the file
//		     
//			writer.append(Integer.toString(siteNum));
//			writer.append("\t");
//			writer.append(E0);
//			writer.append("\t");
//			
//			switch(relation){
//			case GT: writer.append("0");break;
//			case GE: writer.append("1");break;
//			default: break;
//			}
//			
//			writer.append("\n");
//		    writer.flush();
//		    writer.close();
//	      
//	      if(relax&&(relation==Relation.GE||relation==Relation.GT)){
//	    	  double relaxbase = Math.random()*5+1; 
//	    	  //double relaxIndex = Math.random()*5+1;
//	    	  OPEexpr +="+" + new Double(relaxbase).toString();
//	    	  //inputIndex +="+" + new Double(relaxIndex).toString();
//	      }
//	      
//	      buf = new StringBufferWriter();
//	      result = util.evaluate(OPEexpr);
//	      OutputFormFactory.get().convert(buf, result);
//	      String outputBase = buf.toString();
//	      
//	      Value OPEValue = String2Exp(outputBase);
//	      
//	      SootClass mathClass = Scene.v().loadClassAndSupport("edu.xidian.CFQStub");
//	      
//	      SootMethod ltoCall = mathClass.getMethodByName("getCFQ");
//
//	      ArrayList<Value> lhsParams = new ArrayList<Value>();
//	      //lhsParams.add(Grimp.v().newCastExpr(OPEValue, DoubleType.v()));
//	      lhsParams.add(OPEValue);
//	      lhsParams.add(IntConstant.v(siteNum));
//	      //lhsParams.add(IntConstant.v(relation.getValue()));
//	      InvokeExpr invokeExp = Grimp.v().newStaticInvokeExpr(ltoCall.makeRef(), lhsParams);
//	      return Grimp.v().newEqExpr(Grimp.v().newCastExpr(invokeExp, IntType.v()), IntConstant.v(1));
//
//	      //	      SootMethod rtoCall = mathClass.getMethodByName("getE0");
////	      
////	      ArrayList<Value> rhsParams = new ArrayList<Value>();
////	      //rhsParams.add(OPEValue);
////	      rhsParams.add(IntConstant.v(siteNum));
////	      rhsParams.add(IntConstant.v(relation.getValue()));
////	      InvokeExpr RHSexpr= Grimp.v().newStaticInvokeExpr(rtoCall.makeRef(), rhsParams);
////	      switch(relation){
////	      case GE:
////	    	  return Grimp.v().newGeExpr(LHSexpr, RHSexpr); 
////	      case GT:
////	    	  return Grimp.v().newGtExpr(LHSexpr, RHSexpr);
//////	      case EQ:
//////	    	  return Grimp.v().newEqExpr(LHSexpr, RHSexpr);
//////	      case NE:
//////	    	  return Grimp.v().newNeExpr(LHSexpr, RHSexpr);
////	      default:	  
////	    	  return null;
////	      }
//	    }
//		catch(Exception e){
//			e.printStackTrace(G.v().out);
//	    } finally {
//	      // Call terminate() only one time at the end of the program  
//	      //ComputerThreads.terminate();
//	    	//G.v().out.println(" in finally");
//	    	//return null;
//	    }
//	    return null;
//	}

//	private Value String2Exp(String output) {
//		try {
//			G.v().out.println("String2Exp "+output);
//            Parser p = new Parser();
//            ASTNode obj = p.parse(output);
//            //G.v().out.println(obj.toString());
//            return processASTNode(obj);
//            //assertEquals(obj.toString(), "Integrate[Plus[Power[Sin[x], 2], Times[3, Power[x, 4]]], x]");
//	    } catch (Exception e) {
//	        e.printStackTrace(G.v().out);
//	        //assertEquals("", e.getMessage());
//	    }
//		return null;
//	}
	
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

	private String Exp2String(Value term) {
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
			return "-1*("+Exp2String(subOp)+")";
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
				return variable;
			}else
				return "("+Exp2String(left)+")"+operator+"("+Exp2String(right)+")";
		}
		else{
			String variable = term.toString();
			variable = filterString(variable);
			ExprMap.put(variable,  term);
			//G.v().out.println("ExprMap put:"+variable+":"+ term.toString());
			return variable;	
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
		if(result.equals("null"))
			result = "0";
		else{
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
			 //TODO:
			 // filter all arithmetic operation when 
			 // the operation is within a function call
			 // e.g.: this.fun1(a*b+c).
			 // 
		}
		return result;
	}


}