# Solr with HBase Lily Indexer Setup
Steps for configuring Indexing HBase records in Solr using Lilly Indexer. It also describes steps to index HBase Columns
with HBase Lily indexer.

## Steps
* Setup Solr Index
* Configure Lily Indexer for HBase Column(s) Indexing
** Configure Morphline configuration
* Create HBase Table
* Test

### Setup Solr Index
Create Solr collection (events-collection) and update config with following/similar schema :

* Edit Solr Schema configuration (Schema.xml)

```xml
    Schema.xml content..

   <field name="row_key" type="string"  indexed="true" stored="true"/>
   <dynamicField name="meta_*" type="string"  indexed="true" stored="true" multiValued="true"/>
    <dynamicField name="ta_*" type="string"  indexed="true" stored="true" multiValued="true"/>
    <dynamicField name="tg_*" type="string"  indexed="true" stored="true" multiValued="true"/>
    <field name="all" type="text_general"  indexed="true" stored="false" multiValued="true"/>

<copyField source="*" dest="all"/>

```
* Upload instancedir to zookeper
    >solrctl --zk $ZOOKEEPER_CONFIG instancedir collection --create events-collection -s 3 -m 3 -r 3 -c events-collection


* Verify Collection *events-data* exists  
    >solrctl collection --list

---
#### Steps for Updating Collection

```shell
--- delete collection
solrctl --zk $ZOOKEEPER_CONFIG collection --delete events-collection

--- delete config
solrctl --zk $ZOOKEEPER_CONFIG instancedir --delete events-collection

--- update config
solrctl --zk $ZOOKEEPER_CONFIG instancedir --update events-collection /data/appId/solr/events-collection/conf

--- create collection
solrctl --zk $ZOOKEEPER_CONFIG collection --create events-collection -s 3 -m 3 -r 3 -c events-data
```

---
#### Upload and Test a Solr Document
>curl --negotiate -u:user 'http://host.rsol.org:8983/solr/events-collection/update?commit=true' --data-binary @test-solr.json -H 'Content-type:application/json'

```json
[
   {
      "row_key":"758c5a21001010ddd56dce1810110181",
      "t_Id":"758c-5a21001010d-dd56dce1810-110181",
      "t_clientIp":"10.197.48.241",
      "t_userId":"user1",
      "t_start":"2014-03-04 02:47:11,264",
      "t_end":"2014-03-04 02:47:11,334",
      "t_someField":"hello"
   },
   {
      "row_key":"5658c5a21001010ddd56dce1810110181",
      "t_transId":"75-8c5a210010-10ddd56dce-1810110181",
      "t_clientIp":"10.197.48.241",
      "t_userId":"user2",
      "t_start":"2014-02-02 02:47:11,200",
      "t_end":"2014-02-02 02:47:12,264",
      "t_someField":"hello"
    }
]

```

#### REST API Test using curl command

```shell
  curl --negotiate -u:user 'http://host.rsol.org:8983/solr/events-collection/select?q=row_key:101001&wt=json'
```

#### Browse collection from HUE Interface
You can also validate and search the data uploaded from the HUE interface
>http://host.rsol..org:8888/search/browse/events-collection


### Configure HBase Lily Indexer - for HBase Column(s) Indexing

#### Create Indexer configuration configuration file
***morphline-hbase-mapper.xml***
```xml
<?xml version="1.0"?>
<indexer table="user:events" unique-key-field="row_key" mapper="com.ngdata.hbaseindexer.morphline.MorphlineResultToSolrMapper">

   <!-- The relative or absolute path on the local file system to the morphline configuration file. -->
   <!-- Use relative path "morphlines.conf" for morphlines managed by Cloudera Manager -->
   <param name="morphlineFile" value="morphlines.conf"/>

   <!-- The optional morphlineId identifies a morphline if there are multiple morphlines in morphlines.conf -->
   <param name="morphlineId" value="eventId"/>

</indexer>

```

#### Create Morphline ETL configuration   
Morphline is a configuration file that allows you to define ETL transformation pipelines. This is part of Cloudera Development Kit.   
***morphline.conf***
```ini
{
    id : txn1
    importCommands : ["org.kitesdk.morphline.**", "com.ngdata.**"]

    commands : [
          {
            # The extractHBaseCells morphline command extracts cells from an HBase Result
            # and transforms the resulting values into a SolrInputDocument.
            extractHBaseCells {
              mappings : [
                               {
                  inputColumn : "t:row_key"
                  outputField : "row_key"
                  type : string
                  source : value
                }


                {
                  inputColumn : "ta:*"
                  outputField : "tg_*"
                  type : string
                  source : value
                }

                {
                  inputColumn : "ta:*"
                  outputField : "tg_*"
                  type : string
                  source : value
                }
              ]
            }
          }

    {
       removeFields {
             blacklist : ["regex:ta_row.*"]
             whitelist : ["literal:row_key"]
         }
    }
    {
     logDebug { format : "output record: {}", args : ["@{}"] }
    }
 ]
}

```
#### Add Indexer
```shell

hbase-indexer add-indexer \
--name eventsIndexer \
--indexer-conf /data/appId/morphline-hbase-mapper.xml \
--connection-param solr.zk=host.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181/solr \
--connection-param solr.collection=events-collection \
--zookeeper host1.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181

```

#### List Indexer

```shell
hbase-indexer list-indexers \
--zookeeper host1.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181

```

#### Update an Indexer

```shell
hbase-indexer update-indexer \
--name eventsIndexer \
--indexer-conf /san-data/appid/morphline-hbase-mapper.xml \
--connection-param solr.zk=host1.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181/solr \
--connection-param solr.collection=events-collection \
--zookeeper host1.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181

```

#### Delete an Indexer

```shell
hbase-indexer delete-indexer --name eventsIndexer \
--zookeeper host1.rsol.org:2181,host2.rsol.org:2181,host3.rsol.org:2181
```

#### Create HBase Table
```
Create 'user:events', 'ta','tg'
```

`Enable replication for NRT (near real time) indexing on Hbase Tables`   
```
disable  'user:events'
alter  'user:events' , {NAME => 'ta', REPLICATION_SCOPE => 1}
alter 'user:events' , {NAME => 'tg', REPLICATION_SCOPE => 1}
enable  'user:events'
```

### Test
Insert data into HBase table and see the results in HUE
