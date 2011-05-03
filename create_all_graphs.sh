#!/bin/bash

mydir=$(dirname $(which $0))
cd $mydir
cd ./data

#go through each folder i.e. survivors
for sur in $(find . -type d -depth 1)
do
    cd $sur
    #iterate through folders of num bots
    for bot in $(find . -type d -depth 1)
    do
	cd $bot

	echo "sur = "  $(basename $sur) " bot = " $(basename $bot)

	#create the set of graphs for each sur/bot combination
	gnuplot ../../../avg_metrics_graphs.plt

	cd ..
	#done with bot loop
    done
    cd ..
    #done with sur loop
done