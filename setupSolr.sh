#!/usr/bin/sh

# export zk ensemble
. /etc/solr/conf/solr-env.sh

export ZOOKEEPER_CONFIG=$SOLR_ZK_ENSEMBLE
export collection_name='eventscollection'
USER="evnttrcd"
SOLR_URL="https://<host>:<port>"

collection_home="/path/solr/$collection_name"
working_dir="/path/hdfs-solr"

delete_collection(){
   #delete collection
   echo "deleting collection..."
   solrctl --zk $ZOOKEEPER_CONFIG collection --delete $collection_name > /dev/null

   #delete config
   solrctl --zk $ZOOKEEPER_CONFIG instancedir --delete $collection_name

   ##Lets clean the old collection dir
   echo "deleting solr instance dir"
   rm -r $collection_home
}

create_instance(){

   echo "deleting solr instance dir"
   rm -r $collection_home

  #create skeleton of solr instance directory
  solrctl --zk $ZOOKEEPER_CONFIG instancedir --generate $collection_home

	if [ -f $working_dir/schema.xml ]; then
	  echo "base $working_dir/schema.xml file found"
	else
	  echo "base $working_dir/schema.xml file does not exists .."
	  exit 1
	fi

  echo "copying file to $collection_home/conf"
  #copy the edited solr config schema.xml to collection before uploading to zookeeper
  cp $working_dir/schema.xml $collection_home/conf
  
  # step for updating solr schema if any
}

upload_config(){
  #upload contents of instance dir to zookeeper
  echo "uploading config ..."
  solrctl --zk $ZOOKEEPER_CONFIG instancedir --create $collection_name $collection_home


}

update_config(){
  #update config
  echo "updating config ..."
  #solrctl --zk $ZOOKEEPER_CONFIG instancedir --update $collection_name $collection_home/conf

  rm -r $working_dir/tmp
  # get current schema
  echo "getting current config..."
  solrctl --zk $ZOOKEEPER_CONFIG instancedir --get $collection_name $working_dir/tmp

  echo "copying schema.xml..."
  cp $working_dir/schema.xml $working_dir/tmp/conf

  # update schema
  echo "uploading configs....."
  solrctl --zk $ZOOKEEPER_CONFIG instancedir --update $collection_name $working_dir/tmp

  echo "update done....."
  rm -r $working_dir/tmp
  #curl --negotiate -u:evnttrcd "$SOLR_URL/solr/admin/collections?action=RELOAD&name=$collection_name"

  echo "committing schema updates..."
  solrctl --zk $ZOOKEEPER_CONFIG collection --reload $collection_name
}

create_collection(){
  #now create collection
  echo "creating solr collection $collection_name..."
  solrctl --zk $ZOOKEEPER_CONFIG collection --create $collection_name -s 7 -m 3 -r 2 -c $collection_name
}

collection_exists(){

  exists=$(solrctl collection --list | grep -c $collection_name)

  if [[ $exists -eq 0 ]]; then
    echo "collection $collection_name does not exists ..."
    return 1
  else
    echo "collection $collection_name already exists .."
    return 0
  fi

}

recreate_collection(){
  delete_collection
  create_instance
  upload_config
  create_collection
}

promptyn(){
        msg="$1 .. enter (y/n)?"
	read CONT?"$msg"
	if [ "$CONT" = "y" ]; then
	  $2 
	else
	  exit 0
	fi
}

###
# Main body of script starts here
###

echo "-------------------------------------------"
echo "------ Setup Solr Collection -------"
echo "-------------------------------------------"

echo "checking status of Collection, please wait.. "

check_options(){
	case "$1" in
	"-u" | "-update")
            promptyn "update Config for $collection_name .." update_config
            echo "config update success !!"
	    ;;
	"-c" | "-create")
	    promptyn "re-creating $collection_name ..." recreate_collection
            if collection_exists; then echo " collection recreate Success !!"; fi
	    ;;
	"-d" | "-del")
	    promptyn "deleting $collection_name ...." delete_collection
            if !collection_exists; then echo " collection delete Success !!"; fi
	    ;;
	*)
	    echo "incorrect options provided... Try again!"
            show_help
	    ;;
	esac
}

show_help(){

 echo "Usage:"
 echo "$0 [options]"
 echo "-u|update : update Config"
 echo "-d|delete : delete Collection"
 echo "-c|create : create Collection"
}


if collection_exists
 then 
  check_options $1
 else
  promptyn "Collection $collection_name does not exist, create ? ..." recreate_collection
  if collection_exists; then echo " Success !!"; fi
fi

