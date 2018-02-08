# Apache-Solr
The example here demonstrates how you can index files locally using LocalIndexer.java (see source code). Solr setup scripts can be used to setup a collection in your container and index.sh for indexing files on hadoop cluster using MapReduceIndexerTool. Ooozie workflow configuration can also be used for submitting a job for indexing.

For local solr setup (on workstations)
* [localSolrSetup.md](localSolrSetup.md)

For setting up solr in docker, see dockerfile under docker folder.


## Oozie workflow
On cloudera cluster, You will need following files in the oozie/lib

```sh
avro-tools-1.7.6-cdh5.11.2.jar  kite-data-core-1.0.0-cdh5.11.2.jar        kite-morphlines-solr-core-1.0.0-cdh5.11.2.jar
httpclient-4.2.5.jar            kite-data-mapreduce-1.0.0-cdh5.11.2.jar   noggit-0.5.jar
httpcore-4.2.5.jar              kite-data-oozie-1.0.0-cdh5.11.2.jar       solr-core-4.10.3-cdh5.11.2.jar
httpmime-4.2.5.jar              kite-morphlines-avro-1.0.0-cdh5.11.2.jar  solr-solrj-4.10.3-cdh5.11.2.jar
jettison-1.3.3.jar              kite-morphlines-core-1.0.0-cdh5.11.2.jar

```

# Solr Lessons Learned / Few things to remember
* A Solr Collections (logical Index) is made of one or more SolrCore
* A Collection can be partitioned or made of many SolrCore, each partition of logical Index is called Shard. Solr Server has mulitple cores
* Think of Solr Core as a slice of the Index.
* There is an upper document limit = 2^31 limit per shard in Lucene. https://issues.apache.org/jira/browse/LUCENE-5843
* Understand different Solr Caches (fieldCache and fieldValueCache, documentCache, filterCache) is important for analyzing your solr heap requirements.
* Try to aggregate documents when there the number of documents to index is high (order of hundreds of millions).
* Assuming that you would want 500 millions documents per shard, and you have an ingest/index rate at 500 million documents, you would need one shard per day.
* Although sol would perform well if the # of documents per shard are kept under 300 million - again this should be empirically tried out with multiple document sizes.
* Consider breaking down cluster into multiple offline and online collections by type or month range etc. e.g You may consider new collection to handle 3-4 months of data or by partition type if your data is not much skewed.

* Create uniqueness in field values when creating aggregated documents, this will help in overall document size and terms.
* The heap usage per shard is also dependent on the number of documents being indexed. The lower the number of documents the lower the heap usage, but again this is also dependent on the size of each document as well.
* Solr would perform better if the heap size is lower to avoid log GC pauses. Use G1GC for best results.

# Additional info on solr sizing/heap estimator
Also take a look at the attached spreadsheets for more calculations

* https://lucidworks.com/2011/09/14/estimating-memory-and-storage-for-lucenesolr/
* https://github.com/apache/lucene-solr/tree/master/dev-tools




# Alternatives/Competitors

* Cloudera Search - Clouder search engine based on Solr
* DSE - DataStax Search Engine (based on Cassandar and Solr)
* Elastic Search - Similar to Solr, based one Lucene - uses logstash for data collection and Kibana for visualization (a.k.a ELK)
  * ELK is another alternative to Solr and has a growing user base. Its now certified by Cloudera, HDP and MAPR
