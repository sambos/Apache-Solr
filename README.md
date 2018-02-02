# Apache-Solr
The example here demonstrates how you can index files locally using LocalIndexer.java (see source code). Solr setup scripts can be used to setup a collection in your container and index.sh for indexing files on hadoop cluster using MapReduceIndexerTool. Ooozie workflow configuration can also be used for submitting a job for indexing.


## Oozie workflow
On cloudera cluster, You will need following files in the oozie/lib

```sh
avro-tools-1.7.6-cdh5.11.2.jar  kite-data-core-1.0.0-cdh5.11.2.jar        kite-morphlines-solr-core-1.0.0-cdh5.11.2.jar
httpclient-4.2.5.jar            kite-data-mapreduce-1.0.0-cdh5.11.2.jar   noggit-0.5.jar
httpcore-4.2.5.jar              kite-data-oozie-1.0.0-cdh5.11.2.jar       solr-core-4.10.3-cdh5.11.2.jar
httpmime-4.2.5.jar              kite-morphlines-avro-1.0.0-cdh5.11.2.jar  solr-solrj-4.10.3-cdh5.11.2.jar
jettison-1.3.3.jar              kite-morphlines-core-1.0.0-cdh5.11.2.jar

```

# Cloudera metrics to look for

# Solr Lessons Learned

# Alternatives/Competitors

* Cloudera Search - Clouder search engine based on Solr
* DSE - DataStax Search Engine (based on Cassandar and Solr)
* Elastic Search - Similar to Solr, based one Lucene - uses logstash for data collection and Kibana for visualization (a.k.a ELK)
  * ELK is another alternative to Solr and has a growing user base. Its now certified by Cloudera, HDP and MAPR
