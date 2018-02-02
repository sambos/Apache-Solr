export JAVA_HOME="/usr/lib/jvm/java-1.8.0-openjdk"
"$JAVA_HOME/bin/java" \
 -Xms1g -Xmx5g \
 -Dlog4j.configuration=file:log4j.properties \
 -Drm=.*part-.* \
 -Drange=10 \
 -Dskip="test" \
 -DSTEP_LIMIT=100000 \
 -jar lib/local-indexer-0.0.1-SNAPSHOT.jar $1 ${@:2}

# command line options
#-Dskip="^(?:(?!118).)*$"

