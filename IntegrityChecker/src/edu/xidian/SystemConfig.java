package edu.xidian;

import java.util.ArrayList;
import java.util.List;

public class SystemConfig {


	final static int considerRate = 100; //maximum 100
	final static int testRatio = 10;      //maximum 1000
	static String traceLogDir = System.getenv("WORK_SPACE").equals("")?
			"/home/ubuntu/Development/transform_space/collectedLogs/":System.getenv("WORK_SPACE")+"/collectedLogs/";
	static String keyDir = System.getenv("WORK_SPACE").equals("")?
			"/home/ubuntu/Development/transform_space/":System.getenv("WORK_SPACE")+"/";
	final static String deliminator = "\t";
	final static int ConstraintCap = 100;
	


}
