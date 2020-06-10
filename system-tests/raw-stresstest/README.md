# Raw Hyperion Stress-Test

This simple pipeline connects a stresser and rate instance together to measure the raw performance of message sending in the Hyperion pipeline. It requires no additional dependencies.

## 1. Build all jars using docker
```shell script
$ cd system-test/raw-stresstest/
$ docker-compose -f docker-compose.build.yml up
```

Notes:
- The current builder does not cache dependencies
- It mounts the host project in the container to build

## 2. Run the pipeline
```shell script
$ docker-compose up -d
``` 
