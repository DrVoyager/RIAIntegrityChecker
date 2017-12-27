WORK_SPACE=/home/ubuntu/Development/git/integrity_check/examples
export WORK_SPACE
SOOT_HOME=$WORK_SPACE/../sootfiles
HADOOP_HOME=$WORK_SPACE/../applications/hadoop-1.0.2
SOURCE_DIR=$WORK_SPACE/hadoop-examples-1.0.4-enc
CHECKER_DIR=$WORK_SPACE/../IntegrityChecker

cd $CHECKER_DIR
ant
cp dist/Checker.jar $WORK_SPACE/Checker.jar
#jar -cfm $WORK_SPACE/Checker.jar $CHECKER_DIR/src/manifest.txt edu

cd $WORK_SPACE

rm -rf $SOURCE_DIR/*
cp $HADOOP_HOME/../hadoop-examples-1.0.4-Encryption.jar ./
unzip  hadoop-examples-1.0.4-Encryption.jar -d $SOURCE_DIR
unzip  ./lib/encryption.jar -d $SOURCE_DIR

rm -rf collectedLogs
mkdir collectedLogs
#copy log files to collectedLogs
scp -r -P 20000 ubuntu@222.25.188.1:~/collectedLogs ./collectedLogs


time java -Xms512M -Xmx1024M -cp .:$WORK_SPACE/lib/thep.jar:$WORK_SPACE/lib/encryption.jar:$WORK_SPACE/lib/jope.jar:$WORK_SPACE/lib/javallier_2.10-0.6.0.jar:$SOOT_HOME/jasminclasses-custom.jar:$SOOT_HOME/polyglotclasses-1.3.5.jar:$SOOT_HOME/soot-trunk.jar:$SOOT_HOME/commons-io-2.4-bin/commons-io-2.4/commons-io-2.4.jar:$SOOT_HOME/log4j-1.2.11.jar:$SOOT_HOME/symja-2015-09-26.jar:$WORK_SPACE/LogerLibrary.jar:$WORK_SPACE/Checker.jar  edu.xidian.CheckerMain -cp .:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/jre/lib/jce.jar:$HADOOP_HOME/hadoop-core-1.0.2.jar:$HADOOP_HOME/hadoop-tools-1.0.2.jar:$HADOOP_HOME/hadoop-client-1.0.2.jar:$SOOT_HOME/commons-logging-1.1.3.jar:$SOOT_HOME/commons-cli-1.2.jar:$WORK_SPACE/lib/thep.jar:$WORK_SPACE/lib/encryption.jar:$WORK_SPACE/lib/jope.jar:$WORK_SPACE/lib/javallier_2.10-0.6.0.jar:$WORK_SPACE/lib/jetty-util-6.1.26.jar:$WORK_SPACE/lib/jackson-databind-2.9.0.jar:$SOOT_HOME/hsqldb.jar -src-prec c -f c -include-all -process-dir $SOURCE_DIR
