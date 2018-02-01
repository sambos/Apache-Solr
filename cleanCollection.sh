#!/usr/bin/bash

export collection_name="eventscollection"
USER=sambos
SOLR_URL="https://<host>:<port>/solr/$collection_name/update?commit=true"

function deleteAll(){
 echo "..... deleting from $SOLR_URL"
 curl --negotiate -u:$USER $SOLR_URL --data-binary '<delete><query>*:*</query></delete>' -H 'Content-type:text/xml'
}

read -p "Continue delete All data in Solr (y/n)?" CONT
if [ "$CONT" = "y" ]; then
  deleteAll
else
  exit 0
fi
