#!/bin/bash

mydir=$(dirname $(which $0))
cd $mydir
./COMPILE.sh
java -Djava.awt.headless=true -Xmx16G -cp ./bin/:./lib/jgrapht-0.8.2/jgrapht-jdk1.6.jar simulation.World $1 1>./stdout.txt 2>err.txt
