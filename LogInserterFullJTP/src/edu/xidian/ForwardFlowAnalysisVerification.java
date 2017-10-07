package edu.xidian;


/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */




import soot.*;
import soot.toolkits.graph.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import soot.jimple.IfStmt;
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
	
	/** Construct the analysis from a DirectedGraph representation of a Body.
     */
    public ForwardFlowAnalysisVerification(DirectedGraph<N> graph)
    {
        super(graph);
        String filePath = "/tmp/traceLog";
        File logFile = new File(filePath);
        
        //branchLogIt = getBranchIterator(logFile);
        
		try {
			branchLogIt = FileUtils.lineIterator(logFile, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    protected boolean isForward()
    {
        return true;
    }

    protected void doAnalysis()
    {
		G.v().out.println("test RangeAnalysis doAnalysis");
    	final Map<N, Integer> numbers = new HashMap<N, Integer>();
//        Timers.v().orderComputation = new soot.Timer();
//        Timers.v().orderComputation.start();
        List<N> orderedUnits = constructOrderer().newList(graph,false);
//        Timers.v().orderComputation.end();
        int i = 1;

        for( Iterator<N> uIt = orderedUnits.iterator(); uIt.hasNext(); ) {
            final N u = uIt.next();
            numbers.put(u, new Integer(i));
            i++;
        }

        Collection<N> changedUnits = constructWorklist(numbers);

        List<N> heads = graph.getHeads();
        int numNodes = graph.size();
        int numComputations = 0;
        
        HashSet<N> unVisitedNode = new HashSet<N>();
        
        // Set initial values and nodes to visit.
        {
            Iterator<N> it = graph.iterator();

            while(it.hasNext())
            {
                N s = it.next();

                changedUnits.add(s);
				G.v().out.println("currentNode is  "+s.toString());
				unVisitedNode.add(s);
				//G.v().out.println("range is "+ai.toString());

                unitToBeforeFlow.put(s, newInitialFlow());
                unitToAfterFlow.put(s, newInitialFlow());
            }
        }
        
        G.v().out.println("start checking...");
        
        A beforeFlow = newInitialFlow();
        A afterFlow = newInitialFlow();
        TraceLog curBranchValue = null;
        
        N headNode = null;
        //start from head
        {
            Iterator<N> it = heads.iterator();
            
            while (it.hasNext()) {
                headNode = it.next();
                // this is a forward flow analysis
                G.v().out.println("headNode is  "+headNode.toString());
                //unitToBeforeFlow.put(s, entryInitialFlow());
            }
        }
        
        N currentNode = headNode;
        N nextNode = null;
        
//        //HashSet<N> potentialUnvisitedNodes = new HashSet<N>();
//        Queue<N> potentialUnvisitedNodes = new LinkedList<N>();
//        
//        //iterate nodes
//        while(currentNode!=null){
//        	//update conditions
//        	//if currentNode is a branch stmt, determine next node in determineSuccessor
//        	G.v().out.println("current Node is "+ currentNode.toString());
//        	
//        	List<N> successors = graph.getSuccsOf(currentNode);
//        	Iterator<N> succIt = successors.iterator();
//        	G.v().out.println("   its successors are ");
//
//            while(succIt.hasNext())
//            {
//                N succ = succIt.next();
//            	G.v().out.println("   successor:     "+succ.toString());   
//            	if(unVisitedNode.contains(succ)){
//            		potentialUnvisitedNodes.offer(succ);
//            	}
//            }
//            unVisitedNode.remove(currentNode);
//            currentNode = potentialUnvisitedNodes.poll();
//        }
        
        while(currentNode!=null){
        	//update conditions
        	//if currentNode is a branch stmt, determine next node in determineSuccessor
        	G.v().out.println("current Node is "+ currentNode.toString());

        	if(isBranchStmt(currentNode)){
        		//update Branch Values
        		curBranchValue = getNextTraceLog();
        	}
        	List<N> successors = graph.getSuccsOf(currentNode);
        	if(successors==null||successors.size()==0){
        		nextNode = null;
        	}else if(successors.size()==1){
        		nextNode = successors.get(0);
        	}else{ //multiple successors, branch statement
            	G.v().out.println("    current Node is a branch");
        		nextNode = determineSuccessor(currentNode, successors, curBranchValue);
        	}
        	flowThrough(beforeFlow, currentNode, afterFlow); 
        	beforeFlow = afterFlow;
        	currentNode = nextNode;
        }

    /*
        // Feng Qian: March 07, 2002
        // Set initial values for entry points
        {
            Iterator<N> it = heads.iterator();
            
            while (it.hasNext()) {
                N s = it.next();
                // this is a forward flow analysis
                unitToBeforeFlow.put(s, entryInitialFlow());
            }
        }
        
        // Perform fixed point flow analysis
        {
            A previousAfterFlow = newInitialFlow();

            while(!changedUnits.isEmpty())
            {
                A beforeFlow;
                A afterFlow;

                //get the first object
                N s = changedUnits.iterator().next();
                changedUnits.remove(s);
                boolean isHead = heads.contains(s);

                copy(unitToAfterFlow.get(s), previousAfterFlow);

                // Compute and store beforeFlow
                {
                    List<N> preds = graph.getPredsOf(s);

                    beforeFlow = unitToBeforeFlow.get(s);
                    
                    if(preds.size() == 1)
                        copy(unitToAfterFlow.get(preds.get(0)), beforeFlow);
                    else if(preds.size() != 0)
                    {
                        Iterator<N> predIt = preds.iterator();

                        copy(unitToAfterFlow.get(predIt.next()), beforeFlow);

                        while(predIt.hasNext())
                        {
                            A otherBranchFlow = unitToAfterFlow.get(predIt.next());
                            mergeInto(s, beforeFlow, otherBranchFlow);
                        }
                    }

                    if(isHead && preds.size() != 0)
                    		mergeInto(s, beforeFlow, entryInitialFlow());
                    	}
                
                {
                    // Compute afterFlow and store it.
                    afterFlow = unitToAfterFlow.get(s);
                    if (Options.v().interactive_mode()){
                        
                        A savedInfo = newInitialFlow();
                        if (filterUnitToBeforeFlow != null){
                            savedInfo = filterUnitToBeforeFlow.get(s);
                            copy(filterUnitToBeforeFlow.get(s), savedInfo);
                        }
                        else {
                            copy(beforeFlow, savedInfo);
                        }
                        FlowInfo fi = new FlowInfo(savedInfo, s, true);
                        if (InteractionHandler.v().getStopUnitList() != null && InteractionHandler.v().getStopUnitList().contains(s)){
                            InteractionHandler.v().handleStopAtNodeEvent(s);
                        }
                        InteractionHandler.v().handleBeforeAnalysisEvent(fi);
                    }
                    flowThrough(beforeFlow, s, afterFlow);
                    if (Options.v().interactive_mode()){
                        A aSavedInfo = newInitialFlow();
                        if (filterUnitToAfterFlow != null){
                            aSavedInfo = filterUnitToAfterFlow.get(s);
                            copy(filterUnitToAfterFlow.get(s), aSavedInfo);
                        }
                        else {
                            copy(afterFlow, aSavedInfo);
                        }
                        FlowInfo fi = new FlowInfo(aSavedInfo, s, false);
                        InteractionHandler.v().handleAfterAnalysisEvent(fi);
                    }
                    numComputations++;
                }

                // Update queue appropriately
                    if(!afterFlow.equals(previousAfterFlow))
                    {
                        Iterator<N> succIt = graph.getSuccsOf(s).iterator();

                        while(succIt.hasNext())
                        {
                            N succ = succIt.next();
                            
                            changedUnits.add(succ);
                        }
                    }
                }
            }
        
        // G.v().out.println(graph.getBody().getMethod().getSignature() + " numNodes: " + numNodes + 
        //    " numComputations: " + numComputations + " avg: " + Main.truncatedOf((double) numComputations / numNodes, 2));
        
        Timers.v().totalFlowNodes += numNodes;
        Timers.v().totalFlowComputations += numComputations;
        
        */
        branchLogIt.close();
        
    }
    
	private TraceLog getNextTraceLog() {
        
            if (branchLogIt.hasNext()) {
            String line = branchLogIt.nextLine();
            return new TraceLog(line);
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

	private N determineSuccessor(N currentNode, List<N> successors, TraceLog curBranchValue) {
	
		Stmt s = (Stmt)currentNode;
		if(!(s instanceof IfStmt))
			return null;
		//if statement
		IfStmt ifStmt = (IfStmt) s;
		Value condition = ifStmt.getCondition();
		G.v().out.println("        determineSuccessor");
		G.v().out.println("        condition is "+condition.toString());
		G.v().out.println("        curBranchValue is "+curBranchValue.getParams().toString());

		boolean expValue = evalExpValue(condition, curBranchValue);
		if(expValue)
			return successors.get(0);
		else
			return successors.get(1);			
	}

	private boolean evalExpValue(Value condition, TraceLog curBranchValue) {
		SymjaAdaptor symjaAdaptor = new SymjaAdaptor(condition);
    	return symjaAdaptor.evalExp(curBranchValue);
	}

	private Iterator<TraceLog> getBranchIterator(File logFile) {
		// TODO Auto-generated method stub
		return null;
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

}

