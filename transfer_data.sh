#!/bin/bash

mydir=$(dirname $(which $0))
cd $mydir
cd ./data

#go through each folder i.e. survivors
for sur in $(find . -type d -depth 1)
do

#tarFile=$(basename $sur).tar

#echo ./$tarFile

#tar -cf tarFile $sur
echo $sur

scp -r $sur Will@krishna.student.bowdoin.edu:/Users/Will/Documents/workspace/Swarm/data/

done