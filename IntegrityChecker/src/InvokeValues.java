import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class InvokeValues {

	String returnValue = null;//return variable value
	String returnId=null; //return variable name

	
	ArrayList<String> paramPreValues = new ArrayList<String>(); //values of pre invoke parameters
	ArrayList<String> paramPostValues = new ArrayList<String>();// values of post invoke prameters
	String[] paramId; //variable name of parameters
	
	static int PREPARAM_TYPE = 0;
	static int POSTPARAM_TYPE=1;
	static int RETURN_TYPE=2;
	
	public ArrayList<String> getParamPreValues() {
		return paramPreValues;
	}

	public void setParamPreValues(ArrayList<String> paramPreValues) {
		this.paramPreValues = paramPreValues;
	}

	public ArrayList<String> getParamPostValues() {
		return paramPostValues;
	}

	public void setParamPostValues(ArrayList<String> paramPostValues) {
		this.paramPostValues = paramPostValues;
	}

	public String[] getParamId() {
		return paramId;
	}

	public void setParamId(String[] paramId) {
		this.paramId = paramId;
	}

	public String getReturnId() {
		return returnId;
	}

	public void setReturnId(String returnId) {
		this.returnId = returnId;
	}

	public void setReturnValue(String returnValue) {
		this.returnValue = returnValue;
	}

	public void setCallNumber(long callNumber) {
		this.callNumber = callNumber;
	}

	
	public String getReturnValue() {
		return returnValue;
	}

	public ArrayList<String> getPreParamValues() {
		return paramPreValues;
	}

	public long getCallNumber() {
		return callNumber;
	}


	
	public void setParamId(int index, String id){

		paramId[index] = id;
	}

	long callNumber;
	
	public InvokeValues(){
		
	}
	
	public void processLine(String line){
		String[] parts = line.split(SystemConfig.deliminator);
		if(parts[0].startsWith("S"))
			callNumber = Integer.parseInt(parts[0].substring(1));
		else
			callNumber = Integer.parseInt(parts[0]);			
			
		for(int i = 2; i<parts.length;i++){
			if(parts[1].equals("0"))
				paramPreValues.add(parts[i]);
			if(parts[1].equals("1")){
				if(i-2<paramPreValues.size())
					paramPostValues.add(parts[i]);
				else
					returnValue = parts[i];
			}
		}
		if(parts[1].equals("0"))
			paramId = new String[paramPreValues.size()];
	}


	public Map<String, String> getConstraint(int index,
			int type) {
		Map<String, String> constraints = new HashMap<String, String>();
		if(type == PREPARAM_TYPE){
			constraints.put(paramId[index], paramPreValues.get(index));
		}else if(type == POSTPARAM_TYPE){
			constraints.put(paramId[index], paramPostValues.get(index));
		}else{
			constraints.put(returnId, returnValue);
		}
		return constraints;
	}
	
	
	// if type is POSTPARAM_TYPE, return both the post paramvalue and return value
	public Map<String, String> getConstraints(int type){
		Map<String, String> constraints = new HashMap<String, String>();
		for(int i = 0; i< paramId.length; i++){
			if(type == PREPARAM_TYPE){
				constraints.put(paramId[i], paramPreValues.get(i));
			}
			if(type == POSTPARAM_TYPE){
				constraints.put(paramId[i], paramPostValues.get(i));
			}
		}
		if(type == POSTPARAM_TYPE || type== this.RETURN_TYPE){
			if(returnValue!=null)
				constraints.put(returnId, returnValue);
		}
		return constraints;
	}
}
