# Example Pipeline Configuration

This folder contains an example pipeline configuration as described in the [setup tutorial](/docs/hyperion-setup.md
). Please refer to that document for the full tutorial.

## Running

To run the example here, clone the entire repository and run `./run.sh`. Note that you need Java and tmux installed
 for the example to run. You will also need a PostgreSQL database for the aggregator. If you have Docker installed
 , you can start a simple database using the following command:
 
 ```shell script
$ docker run --name some-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```

Please remember to update `aggregator.yml` if your database credentials differ.
