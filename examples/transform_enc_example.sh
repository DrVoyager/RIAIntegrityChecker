# set up your JAVA_HOME 

# set up the current example directory
WORK_SPACE=/home/joe/Development/git/IntegrityCheck/examples

SOOT_HOME=$WORK_SPACE/../sootfiles
HADOOP_HOME=$WORK_SPACE/../applications/hadoop-1.0.2


LOGGER_LIB_DIR=$WORK_SPACE/../LoggerLibrary
INSERTER_DIR=$WORK_SPACE/../LogInserterFullJTP
CHECKER_DIR=$WORK_SPACE/../IntegrityChecker
SOURCE_DIR=$WORK_SPACE/hadoop-examples-1.0.4-enc
DEST_DIR=$WORK_SPACE/hadoop-examples-1.0.4-enc-out
PACKAGE_DIR=$WORK_SPACE/hadoop-examples-1.0.4-enc-package

cd $LOGGER_LIB_DIR/bin

jar -cfm $WORK_SPACE/LoggerLibrary.jar $LOGGER_LIB_DIR/src/manifest.txt edu 

cd $INSERTER_DIR/bin
jar -cfm $WORK_SPACE/LogInserter.jar $INSERTER_DIR/src/manifest.txt edu

cd $WORK_SPACE

mkdir $SOURCE_DIR
mkdir $DEST_DIR
mkdir $PACKAGE_DIR

rm -rf $SOURCE_DIR/*
cp $HADOOP_HOME/../hadoop-examples-1.0.4-Encryption.jar ./
unzip  hadoop-examples-1.0.4-Encryption.jar -d $SOURCE_DIR


rm -rf $DEST_DIR/*

cp -r $LOGGER_LIB_DIR/bin/edu $SOURCE_DIR/


# JTP
java -Xms512M -Xmx1024M -cp .:$SOOT_HOME/jasminclasses-custom.jar:$SOOT_HOME/polyglotclasses-1.3.5.jar:$SOOT_HOME/soot-trunk.jar:$SOOT_HOME/commons-io-2.4-bin/commons-io-2.4/commons-io-2.4.jar:$SOOT_HOME/log4j-1.2.11.jar:$SOOT_HOME/symja-2015-09-26.jar:$WORK_SPACE/LoggerLibrary.jar:$WORK_SPACE/LogInserter.jar  edu.xidian.LoggerMain -cp .:$JAVA_HOME/jre/lib/rt.jar:$JAVA_HOME/jre/lib/jce.jar:$HADOOP_HOME/hadoop-core-1.0.2.jar:$HADOOP_HOME/hadoop-tools-1.0.2.jar:$HADOOP_HOME/hadoop-client-1.0.2.jar:$SOOT_HOME/commons-logging-1.1.3.jar:$SOOT_HOME/commons-cli-1.2.jar:$WORK_SPACE/lib/thep.jar:$WORK_SPACE/lib/encryption.jar:$WORK_SPACE/lib/jope.jar:$WORK_SPACE/lib/javallier_2.10-0.6.0.jar:$WORK_SPACE/lib/jetty-util-6.1.26.jar:$WORK_SPACE/lib/jackson-databind-2.9.0.jar:$SOOT_HOME/hsqldb.jar:$WORK_SPACE/LoggerLibrary.jar  -src-prec c -f c -include-all -process-dir $SOURCE_DIR -output-dir $DEST_DIR

rm -rf $PACKAGE_DIR/*
cp -r $SOURCE_DIR/org $PACKAGE_DIR/
cp $SOURCE_DIR/META-INF/MANIFEST.MF $PACKAGE_DIR/manifest.txt
cp -r $LOGGER_LIB_DIR/bin/edu $PACKAGE_DIR/


cp $DEST_DIR/org/apache/hadoop/examples/WordCount* $PACKAGE_DIR/org/apache/hadoop/examples/
cp $DEST_DIR/org/apache/hadoop/examples/PiEstimator* $PACKAGE_DIR/org/apache/hadoop/examples/
rm -rf $PACKAGE_DIR/org/apache/hadoop/examples/terasort
cp -r $DEST_DIR/org/apache/hadoop/examples/terasort $PACKAGE_DIR/org/apache/hadoop/examples/

cd $PACKAGE_DIR

rm ../taggedExampleEnc.jar
jar -cfm ../taggedExampleEnc.jar manifest.txt org edu

cd ..
scp -P 20000 ./taggedExampleEnc.jar ubuntu@222.25.188.1:./
#scp -P TestHostPort ./taggedExample.jar ubuntu@TestHostIP:./
