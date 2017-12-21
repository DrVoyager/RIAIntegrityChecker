rm *.jar
rm -rf hadoop-*
rm -rf collectedLogs
rm -rf sootOutput

WORK_SPACE=/home/joe/Development/git/IntegrityCheck/examples
LOGGER_LIB_DIR=$WORK_SPACE/../LoggerLibrary
INSERTER_DIR=$WORK_SPACE/../LogInserterFullJTP
CHECKER_DIR=$WORK_SPACE/../IntegrityChecker

cd $LOGGER_LIB_DIR
ant clean

cd $INSERTER_DI
ant clean

cd $CHECKER_DIR
ant clean

