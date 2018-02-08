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

* The Solr memory requirement can increase exponentially depending on the search/sort being performed. The memory needed rapidly adds up when
	• Sorting/faceting a large result set
	• Long running queries / Running multiple queries simultaneously
	• Queries with many terms 

* In addition, the memory requirement also depend on the size of the repository as well as the amount of memory you allocate to the Solr caches. Decreasing the Solr cache parameters can dramatically lower the memory requirements, with the drawback of hitting the disk more often. So look for hte cumulative hit ratio, ideally it should be close to <=1. With hit ratio as 0.99 implying 99% of the queries are being served from cache.
* Caching in Solr is unlike ordinary caches in that Solr cached objects will not expire after a certain period of time; rather, cached objects will be valid as long as the Index Searcher is valid.

* Start Solr with "Safe Configuration Parameters"
* JVM heap size should match Solr heap/memory requirement
* * finding the optimal tradeoff between memory usage and performance  - Sweet Spot = finding optimal heap size
* * Note: The more memory Solr has at its disposal, the better it will perform - On the other hand, the more memory given, the more hardware cost and JVM GC overhead
* Use DocValues If workload is heavy in faceting and sorting (for some fields)
* * When used Solr avoids using field-cache and field-value-cache on Heap (greatly reduces memory req on heap and JVM GC)
* * Field Cache is major memory consumer, use docValue to reduce field cache memory footprint. Lucene manages field cahe at JVM level
* * Tradeoff: docValue results in large Disk I/O and impacts performance and requires large direct memory
* Use G1GC and enable GC logging (has trivial overhead) to estimate how jvm handles memory
* Configure HDFS Block Cache and Direct Memory appropriately.
* Text field can cause large memory usage when the field is used for faceting and sorting. Use string field instead of text field in this case. 
* Consider disabling field caching in solrconfig.xml if your application does not need caching - this will ease some heap memory requirements
* Keep the commit values optimal (60 seconds), keeping it more would cause performance impacts.
* Also Oversharding can creat complex interactions and should be monitored. Keep the collections partitioned and manage with virtual pointers to the collections. Depending on your requirement you can possibly create collections by type, date, year or some other criteria suitable for searches.

## Solr Caches
Cache Type | Description | Min Recommended | Max Recommended
------------------------|---------------------------------------------------------------|---|-------
DirectMemory (off-heap) | Caches data read from disk, similar to linux file system cache|8GB|12-16GB
HdfsBlockCache (off-heap) | Caches hdfs blocks <br/> Jvm -XX:MaxDirectMemorySize=20g  (-XX:MaxDirectMemorySize=4294967296) <br/> -Dsolr.hdfs.blockcache.slab.count=1 <br/> -Dsolr.hdfs.blockcache.blocksperbank=16384 <br/> -Dsolr.hdfs.blockcache.direct.memory.allocation=true <br/> -Dsolr.hdfs.blockcache.enabled=true   | |
Document cache | Caches frequently used stored fields, (isn't as performance critical as the other filter/query cache) If you have many stored fields, or large stored values, then you will probably want to keep your document cache relatively small | MB | MB
Field value cache | Similar to field cache but used for faceting and sorting multi-valued fields | 8-12GB | 12-16GB
Field cache | used for faceting, sorting single-valued fields - per node | 4-8GB | 8-12GB
filter cache | 'fq' -> filter query <br/> Caches the results of the frequently used filter query <br/> <br/> Like the query cache, the memory use of filter cache is potentially quite large. Solr represents the document IDs in a filter as a bit-string containing one bit per document in your index. If your index contains one million documents, each filter will require one million bits of memory—around 125KB. For a filter cache sized to hold 1,000 cache entries, that's in the area of 120MB.| 1MB <br/> (512 entries) <br/> 1000 results per entry | 5MB <br/> 10,000 results 
query result cache | Stores the results of frequently used 'q' as array of integer bytes <br/> Entry = Bytes for query string + 8 bytes for each result retrieved | 1MB | 5MB

## Solr Field Compression
By default solr provides Stored Field Compression. LZ4 is default with 4.1 . DEFLATE is an option with 5.0
* Looking at this JIRA for lucene, it appears that compression is by default enabled for stored fields 
https://issues.apache.org/jira/browse/LUCENE-4226
 
* Here is the original post from the lucene commtter referring to the lucene StoredFieldFormat that uses compression.
http://blog.jpountz.net/post/35667727458/stored-fields-compression-in-lucene-41
 
https://www.elastic.co/blog/store-compression-in-lucene-and-elasticsearch


# Additional info on solr sizing/heap estimator
Also take a look at the attached spreadsheets for more calculations

* https://lucidworks.com/2011/09/14/estimating-memory-and-storage-for-lucenesolr/
* https://github.com/apache/lucene-solr/tree/master/dev-tools




# Alternatives/Competitors

* Cloudera Search - Clouder search engine based on Solr
* DSE - DataStax Search Engine (based on Cassandar and Solr)
* Elastic Search - Similar to Solr, based one Lucene - uses logstash for data collection and Kibana for visualization (a.k.a ELK)
  * ELK is another alternative to Solr and has a growing user base. Its now certified by Cloudera, HDP and MAPR
