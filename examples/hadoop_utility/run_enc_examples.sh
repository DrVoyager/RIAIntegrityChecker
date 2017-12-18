if [ $# != 3 ];then
echo "USAGE: ./run_enc_examples.sh {application} {input file path} {output file path}"
echo " e.g.: ./run_enc_examples.sh wordcount WC_Input WC_Output"
exit 1;
fi
WORK_SPACE=/home/ubuntu

cd $WORK_SPACE
$WORK_SPACE/hadoop-1.0.2/bin/hadoop fs -put key key

LIBJARS=/home/ubuntu/hadoop-1.0.2/lib/javallier_2.10-0.6.0.jar,/home/ubuntu/hadoop-1.0.2/lib/jope.jar,/home/ubuntu/hadoop-1.0.2/lib/jetty-util-6.1.26.jar,/home/ubuntu/hadoop-1.0.2/lib/thep.jar,/home/ubuntu/hadoop-1.0.2/lib/soot-inference.jar,/home/ubuntu/hadoop-1.0.2/lib/encryption.jar,/home/ubuntu/hadoop-1.0.2/lib/jackson-databind-2.9.0.jar,/home/ubuntu/hadoop-1.0.2/lib/javallier_2.10-0.6.0.jar,/home/ubuntu/hadoop-1.0.2/lib/thep.jar

time /home/ubuntu/hadoop-1.0.2/bin/hadoop jar /home/ubuntu/taggedExampleEnc.jar $1 -libjars ${LIBJARS} $2 $3
