#!/usr/bin/sh

USER="evnttrcd"
NAME_NODE="nameservice1"
collection_name="events"
CDH_PARCEL=/opt/cloudera/parcels/CDH
LOG4J_PATH=$CDH_PARCEL/share/doc/search*/examples/solr-nrt/log4j.properties
HDFS_OUT_DIR=hdfs://$NAME_NODE/tmp
input_path=/user/sambos/staging

# export SOLR_ZK_ENSEMBLE=""
. /etc/solr/conf/solr-env.sh

indexEvents(){
	echo $1
	echo $2
	echo ${@:3}

	 HADOOP_OPTS="-Djava.security.auth.login.config=jaasLogin.conf" hadoop \
	 --config /etc/hadoop/conf \
	 jar $CDH_PARCEL/jars/search-mr-*-job.jar \
	 org.apache.solr.hadoop.MapReduceIndexerTool \
	 --log4j $LOG4J_PATH \
	 --morphline-file $2 \
	 --output-dir $HDFS_OUT_DIR \
	 --verbose \
	 --zk-host $SOLR_ZK_ENSEMBLE \
	 --collection $1 \
	 --go-live ${@:3}
}


indexEvents $collection_name morphline-avro.conf $input_path
