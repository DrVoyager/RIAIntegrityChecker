WORK_SPACE=/home/joe/Development/git/IntegrityCheck/examples
export WORK_SPACE
SOOT_HOME=$WORK_SPACE/../sootfiles
HADOOP_HOME=$WORK_SPACE/../applications/hadoop-1.0.2

SOURCE_DIR=$WORK_SPACE/hadoop-examples-1.0.2

LOGGER_LIB_DIR=$WORK_SPACE/../LoggerLibrary
CHECKER_DIR=$WORK_SPACE/../IntegrityChecker

cd $LOGGER_LIB_DIR/bin

jar -cfm $WORK_SPACE/LoggerLibrary.jar $LOGGER_LIB_DIR/src/manifest.txt edu

cd $CHECKER_DIR/bin
jar -cfm $WORK_SPACE/Checker.jar $CHECKER_DIR/src/manifest.txt edu


cd $WORK_SPACE

mkdir collectedLogs
rm -rf collectedLogs/*
#copy log files to collectedLogs
scp -r -P 20000 ubuntu@222.25.188.1:~/collectedLogs ./collectedLogs



time java -Xms512M -Xmx1024M -cp .:$SOOT_HOME/jasminclasses-custom.jar:$SOOT_HOME/polyglotclasses-1.3.5.jar:$SOOT_HOME/soot-trunk.jar:$SOOT_HOME/commons-io-2.4-bin/commons-io-2.4/commons-io-2.4.jar:$SOOT_HOME/log4j-1.2.11.jar:$SOOT_HOME/symja-2015-09-26.jar:$WORK_SPACE/LogerLibrary.jar:$WORK_SPACE/Checker.jar  edu.xidian.CheckerMain -cp .:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/jre/lib/jce.jar:$HADOOP_HOME/hadoop-core-1.0.2.jar:$HADOOP_HOME/hadoop-tools-1.0.2.jar:$HADOOP_HOME/hadoop-client-1.0.2.jar:$SOOT_HOME/commons-logging-1.1.3.jar:$SOOT_HOME/commons-cli-1.2.jar:$SOOT_HOME/hsqldb.jar -src-prec c -f c -include-all -process-dir $SOURCE_DIR
