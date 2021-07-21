#!/bin/sh
set -e

# Compiles to Java classes
mvn clean install

#builds a native binary and zip
docker build  -t lambda-v3 .

##copy from the docker container to host
containerId=$(docker create -ti lambda-v3 bash)
docker cp ${containerId}:/tmp/dist/ lambda-v3/graalvm

##build go lambda
#cd ./go-lambda
#GOARCH=amd64 GOOS=linux go build -o ./../lambda-go
#
##build node lambda
#cd ./../node-lambda/
#npm install
#
## Deploy lambdas
#cd ../
#alias sam='sam.cmd'
#
#sam deploy --guided
