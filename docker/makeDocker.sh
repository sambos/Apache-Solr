#!/usr/bin/sh

cleanImage() {
	 imageId=$(sudo docker images | grep -w ^$1 | awk -F " " '{print $3}')

	 if [[ ! -z $imageId ]];then
	  echo "found image $imageId ..."
	  echo "deleting imageId: $imageId"
	  sudo docker rmi $imageId

	  echo "deleting any untagged images ..."
	  [ $(sudo docker images -q -f dangling=true | wc -l) -gt 0 ] && sudo docker rmi $(sudo docker images -q -f dangling=true) || echo "no untagged images found ..."
	 else
	  echo "No image found matching name: $1 ... !!"
	 fi
}

tailLogs() {
	 #sudo docker ps -a
	 containerId=$(sudo docker ps -a | grep $1 | awk -F " " '{print $1}')
	 echo "displaying logs for image $containerId --> $1"
	 sudo docker logs -f $containerId
}

buildImage(){
        imageName=$1:$2

	cleanImage $1
	echo "building image name $imageName .."
	sudo docker build -t $imageName .
}

# ----------------
## -- main --
# ----------------

if [ $# -eq 0 ]; then

	echo "Usage $0 [-e] [-l]"
        exit 1

elif [ "$1" == "-e" ];then

        name="logevents"
        tag="v1"
        imageName=$name:$tag
	rm data
  	ln -s dataset1 data
        buildImage $name $tag
	sudo docker run -d -t -i -p8983:8983 -p8984:8984 -p2181:2181 -p9983:9983 $imageName collection collection-config morphline.conf "data/*"
	tailLogs $name

elif [ "$1" == "-l" ];then 

        name="levents"
        tag="v1"
        imageName=$name:$tag
	rm data
	ln -s dataset2 data
        buildImage $name $tag
	sudo docker run -d -t -i -p6983:8983 -p6984:8984 -p5181:2181 -p5983:9983 $imageName collection2 collection2-config -g morphline1.conf morphline2.conf
	tailLogs $name

else
	echo "wrong args.."
        exit 1
fi

echo "Done !!"
