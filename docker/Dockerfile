FROM docker-repot-host:5000/java:latest
MAINTAINER Majeed
ENV SOLR_DIR solr-4.10.3
ENV SOLR_DIST Apache-solr-4.10.3.tgz
ENV SOLR_INDXR_TOOL solr-indexer-tool.tgz

WORKDIR /home
RUN mkdir -p /home
COPY dist/$SOLR_DIST /home
RUN tar -C /home --extract --file /home/$SOLR_DIST
# copying the contents of root dir to /home
RUN cp -r /home/$SOLR_DIR/* /home \
    && rm -rf /home/$SOLR_DIR

# lsof is needed as the bin/solr scripts use them
RUN yum -y install lsof \
 && yum -y install elfutils 
COPY dist/$SOLR_INDXR_TOOL solrSetup.sh schema.xml /home/

#extract indexer tool and override configs
RUN tar -xvzf /home/$SOLR_INDXR_TOOL
COPY runSolrIndexer.sh log4j.properties *.conf /home/indexer/

# copy data to be indexed
RUN mkdir -p /home/indexer/data
COPY data /home/indexer/data

# run morphline and indexer
RUN chmod 777 /home/solrSetup.sh
ENTRYPOINT ["/home/solrSetup.sh"]

EXPOSE 8983
EXPOSE 8984
EXPOSE 1281
EXPOSE 9983
