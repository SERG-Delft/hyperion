# System tests
This directory contains end-to-end system tests.  
The end-to-end test spawns separate containers for each plugin in the pipeline.

## Build all jars using docker
```shell script
$ cd src/
$ docker-compose -f docker-compose.build.yml up gradle
```

Notes:
- The current builder does not cache dependencies
- It mounts the host project in the container to build

## Run the project
```shell script
docker-compose up -d
``` 