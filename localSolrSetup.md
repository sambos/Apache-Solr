# Setup for Local Environment
This helps in indexing data to the local Solr instance with minimal setup and uses the same morphline configuration file used with cloudera or hortonworks. Please follow 
the installation and configuration steps below before running the IndexerDriver tool.

## Setup steps
* Install Solr
* Create Solr collection
* Configure Solr 
* Index data to local Solr instance

### Install Solr

Download and Install Solr 4.10.3 to you local machine.   
There may be different steps needed for running Solr 5.x and Solr 6.x.    
Cloudera CDH 5.11.x uses 4.10 solr but they backport many of features from Solr 5.x. 

### Create Solr collection

Make sure that your JAVA_HOME points to 64bit version of JDK 1.7    

The default scripts provided under solrHome\bin work well with unix environment. But for windows you will need to update the solr.cmd for it to work.
update solr.cmd file and replace   
@echo Using Java: %JAVA%  with   
@echo "Using Java: %JAVA%"


#### Step by Step guide for creating solr collection

Execute the following command from the Solr home directory and follow the step by step prompts.    
``` 
bin/solr.cmd -e cloud 
```

You may choose your clollection name as ```eventtrc-txns``` or something else.
* Enter # nodes as 1 or 2   
* Enter # shards as 1 or 2   
* Enter # replica as 1 or 2  
* Choose a default config for now, which can be updated later.
* Enter port for node1 as 8983   
* Enter port for node2 (if selected) as 8984   

The setup will try to start both the instances once they are created. You will then be able to load the Solr home page at http://localhost:8983/solr.
If it does not start, please stop all the nodes first and restart them again.  You will need to select different ports if there is a conflict.

##### Stop nodes
Stop the Solr instances with the following command   
``` 
bin\solr.cmd stop -all 
```

##### Restart nodes   
```
bin\solr restart -c -p 8983 -d node1    
bin\solr restart -c -p 8984 -z localhost:9983 -d node2    
```

Once Solr instance is up and running with default configuration, you will need to update zookeeper with eventtrc configuration.

#### Configure Solr
##### Update Solr schema
In this step we will create a new solr configuration and link it to the new collection we just created.

* create a directory under Solr.home as conifgs/events-config
* download the default config from the local solr instance to this directory using command:
```
# you just need to run once from node - $solr.home\node1 directory
scripts\cloud-scripts\zkcli -zkhost localhost:9983 -cmd downconfig -confdir ..\configs\events-config -confname default
```
* update the schema.xml with the following contents

``` xml

   <dynamicField name="e_*" type="text_general" indexed="true" stored="true" multiValued="true" />
   <dynamicField name="*" type="text_general" indexed="true" stored="true" multiValued="true" />
   

```
* upload to zookeepr instance with following command and we name it as 'eventtrc-config'

```
scripts\cloud-scripts\zkcli -zkhost 127.0.0.1:9983 -cmd upconfig -confdir ..\configs\events-config -confname events-config
```
* Now you can link the new config to events-collection collection and restart Solr

```
scripts\cloud-scripts\zkcli -zkhost localhost:9983 -cmd linkconfig -collection events-collection -confname events-config
```

#### Index data to local Solr instance
All the above stes are pre-requisites for working on this section.   

clone solr-indexer project, build and execute the ```runSolrIndexer.sh``` shell
* you will need to specify the location of morphline file, you can download a copy from [workflows\solr](https://sfgitlab.opr.statefarm.org/StormWatch/workflows/tree/master/solr) or from solr-indexer workspace (under src/main/resources)
* second argument to program is your data file with graph transactions (output of hbase-loader)

```
Usage: 
java <optional-args> -jar jar-file <morphline.conf> <dataFile1> ... <dataFileN>
<optional-args> as system properties :
-Doutput=file-path  - write morphline output to a file
-Dpost=true - index results using Http POST command


"$JAVA_HOME/bin/java" -Doutput=./out.json -jar target/local-indexer-0.0.1-SNAPSHOT.jar src/main/resources/morphline.conf src/main/resources/document.json
```

Once you were able to successfully run the script 'runIndex.sh', you will either need to commit using the following url or restart the Solr to commit the data.

```
curl http://localhost:8983/solr/events-collection/update?commit=true
```

done :)
