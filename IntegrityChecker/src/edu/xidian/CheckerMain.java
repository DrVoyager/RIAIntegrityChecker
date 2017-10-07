package edu.xidian;

/* Integrity Checker
 * Based on Soot - a J*va Optimization Framework
 * Copyright (C) 2016 Yongzhi Wang
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
import java.util.HashSet;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.PackManager;
import soot.Transform;
import soot.Unit;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.FlowSet;

public class CheckerMain {

	
	public static void main(String[] args) {
		PackManager.v().getPack("jtp").add(
				new Transform("jtp.myTransform", new BodyTransformer() {

					protected void internalTransform(Body body, String phase, Map options) {
						new MyAnalysis(new ExceptionalUnitGraph(body));
						// use G.v().out instead of System.out so that Soot can
						// redirect this output to the Eclipse console
						G.v().out.println(body.getMethod());
					}
				}));
		
		soot.Main.main(args);
		G.v().out.println("########################Total Function No is "+ForwardFlowAnalysisVerification.auditedFunNo+"#########################");
	}

	public static class MyAnalysis extends ForwardFlowAnalysisVerification {

		public MyAnalysis(ExceptionalUnitGraph graph) {
			super(graph);
			Body aBody = graph.getBody();
			String declearedClass = aBody.getMethod().getDeclaringClass().toString();
			String declaredMethod = aBody.getMethod().toString();
			
			G.v().out.println("start checking method ..."+aBody.getMethod().toString());
			if(declearedClass.contains("edu.xidian.Transformer")){
				G.v().out.println("Encounter the Transformer class ...skip...");
			}else
			doAnalysis(declaredMethod);
		}

		@Override
		protected void flowThrough(Object in, Object d, Object out) {
	        FlowSet inSet = (FlowSet) in;
	        FlowSet outSet = (FlowSet) out;
	        Unit s = (Unit) d;
	        //G.v().out.println("flow through "+d.toString());
		}

		@Override
		protected Object newInitialFlow() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Object entryInitialFlow() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void merge(Object in1, Object in2, Object out) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void copy(Object source, Object dest) {
			// TODO Auto-generated method stub
			
		}

	}

}