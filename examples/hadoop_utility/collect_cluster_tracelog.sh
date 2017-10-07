mkdir ./collectedLogs
rm -rf ./collectedLogs/*
for i in master slave1 slave2 slave3 slave4; do  scp -r ubuntu@$i:/tmp/traceLog ./collectedLogs/$i/; done
