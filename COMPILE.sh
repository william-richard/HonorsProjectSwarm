#!/bin/bash

mydir=$(dirname $(which $0))
cd $mydir
javac -classpath ./lib/jgrapht-0.8.2/jgrapht-jdk1.6.jar -sourcepath ./src/ -d ./bin/ ./src/simulation/World.java
