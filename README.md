# HOW TO APPLY RIA ON HADOOP EXAMPLE APPLICATIONS

##Environments & Prerequisites
* This example applies RIA into Hadoop 1.0.2 example applications, so we need 

>* a hadoop cluster
* Hadoop 1.0.2 & hadoop-examples-1.0.2.jar
* java 1.8
* ant 1.9.3

* Set up the environment path 

>In the transform_example.sh and the check_example.sh, set the path, including WORK_SPACE (the current example directory), JAVA_HOME (ensure the jre/lib/rt.jar and jre/lib/jce.jar can be found under $JAVA_HOME), and the address of the Hadoop server and the log collection server.

##Run this example
* Under the example directory

>Run transform_example.sh to transform the example source and pack it into taggedExamples.jar then upload to the hadoop server. 

* On the hadoop server

>Run the transformed applications, including Wordcount, Pi, Terasort use the run_examples.sh
Use the collect_cluster_tracelog.sh to collect logs from hadoop clusters.

* Under the example directory

>Run the check_example.sh to check the integrity of the execution. It will automatically collect the logs from the hadoop server and check the integrity based on the log.




