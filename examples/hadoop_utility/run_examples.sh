if [ $# != 3 ];then
echo "USAGE: ./run_examples.sh {application} {input file path} {output file path}"
echo " e.g.: ./run_examples.sh wordcount WC_Input WC_Output"
exit 1;
fi

time /home/ubuntu/hadoop-1.0.2/bin/hadoop jar /home/ubuntu/taggedExample.jar $1 $2 $3
