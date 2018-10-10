# About RIA Integrity Checker

RIA Integrity Checker is a Java-based tool that facilitates the integrity check of remote execution. It consists of two parts:

1. Log Inserter: It inserts logging statements to the program so that when the program is executed remotely, it will generate logs on the remote environment.

2. Integrity Checker: After downloading the logs to the trusted environment, it will check the consistency between the program and the logs. Any inconsistency indicates a tampering of the remote computing. The Integrity Check differs from existing solution in that

    * Integrity Check will extract the runtime mathemacial constraints among variables in the program and use it as a baseline to check against the integrity of the logs, which is called *audition*.
    * The audition is performed on a function execution basis, i.e., even though a function will call another function, forming as an embedding sequence, Integrity Checker can perform the audition of function executions in a serialized manner. As a result, auditions on the function execution can be performed in a random manner, thus providing users a trade-off between integrity confidence and overhead.

### The complete introduction of RIA is published in the following papers.

* [1] Y. Wang, Y. Shen and X. Jiang, "Practical Verifiable Computation–A MapReduce Case Study," in IEEE Transactions on Information Forensics and Security, vol. 13, no. 6, pp. 1376-1391, June 2018. [Download here](https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=8241805)
* [2] POSTER: RIA: an Audition-based Method to Protect the Runtime Integrity of MapReduce Applications. In Proceedings of the 2016 ACM SIGSAC Conference on Computer and Communications Security (CCS '16). ACM, New York, NY, USA, 1799-1801.[Download here](http://library.usc.edu.ph/ACM/SIGSAC%202017/ccs/p1799.pdf)

### To show users how to use RIA, we provide several (hadoop) example applications, which showcases how easy to use RIA. 

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




