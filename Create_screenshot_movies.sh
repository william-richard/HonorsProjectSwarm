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
	#iterate through folders with individual runs
	for run in $(find . -type d -depth 1)
	do
	    cd $run

	    #see if the screenshot folder exists
	    if [ -d "screenshots" ]; then
		#if the replay file does not already exist, create it
		if [ ! -f "replay.mp4" ]; then
		    #if it does, create the movie
		    ffmpeg -f image2 -r 2 -i ./screenshots/%d.jpeg -b 600k ./replay.mp4
		fi
		#remove the screenshots folder
		rm -r ./screenshots	    
	    fi

	    cd ..
	    #done with run loop
	done
	cd ..
	#done with bot loop
    done
    cd ..
    #done with sur loop
done