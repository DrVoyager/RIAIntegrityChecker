WORK_SPACE=/home/joe/Development/git/IntegrityCheck/examples
export WORK_SPACE
SOOT_HOME=$WORK_SPACE/../sootfiles
HADOOP_HOME=$WORK_SPACE/../applications/hadoop-1.0.2

SOURCE_DIR=$WORK_SPACE/hadoop-examples-1.0.2

LOGGER_LIB_DIR=$WORK_SPACE/../LoggerLibrary
CHECKER_DIR=$WORK_SPACE/../IntegrityChecker

cd $CHECKER_DIR
ant
cp dist/Checker.jar $WORK_SPACE/Checker.jar
#jar -cfm $WORK_SPACE/Checker.jar $CHECKER_DIR/src/manifest.txt edu

scp -P 50119 $WORK_SPACE/Checker.jar ubuntu@222.25.188.1:~/Development/git/integrity_check/examples/

