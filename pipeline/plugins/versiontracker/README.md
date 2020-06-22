# Hyperion - Version Tracker Plugin

![Gradle check VersionTracker plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20VersionTracker%20plugin/badge.svg)

This package provides a version tracker plugin that is able to automatically attach Git commit hashes to incoming log messages based on the most recent commit in a configurable git repository. If you have a deploy system that deploys based on the most recent commit in a certain branch, you can use this plugin to automatically attach the appropriate version hash to the log.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The version tracker plugin is able to pull the most recent commit hash on a specified remote git repository. Simply configure the plugin with which repository to use and which branch to use. The plugin will periodically pull the latest version (as specified in the `update-interval` setting) and attach it to all incoming messages.

For example, take the following partial config:

```yaml
projects:
  backend:
    repository: https://github.com/Jane/backend.git
    branch: refs/heads/production
    update-interval: 60
    authentication:
      type: https
      username: jane
      password: supersecretpassword123
```

This configuration will poll the latest commit on the `production` branch of the specified repository every minute. If a log message is received whose `project` field is set to `"backend"`, it will attach the hash as appropriate.

For example, given that the HEAD of `production` currently points to `48a7361cbaddbc4976e3afd6664724859c380a92`, the following input:

```json
{
  "project": "backend",
  "message": "Foo"
}
```

Will result in the following output:

```json
{
  "project": "backend",
  "message": "Foo",
  "version": "48a7361cbaddbc4976e3afd6664724859c380a92"
}
```

All messages with a different or missing project field will be passed on without modification.

Note that due to the polling behavior of this plugin, the version hash attached to log messages may be wrong for the first few logs that happen after the most recent deploy. If this is a problem for your use case, consider [making your own plugin](/docs/writing-java-kotlin-plugin.md).

## Building & Running

To build the library, run `gradle pipeline:versiontracker:shadowJar`. The result will be located in `build/versiontracker-all.jar`.

To execute the tests and linting, run `gradle pipeline:versiontracker:check`.

To run a compiled version of the version tracker plugin, simply launch it using Java:

```shell script
java -jar build/versiontracker-all.jar [path to config]
```

## Docker
The versiontracker plugin can be easily build and run using [Docker](https://www.docker.com/). 

### Running the pre-built docker image
A pre-built image is available at the [docker hub repository](https://hub.docker.com/r/sergdelft/hyperion).
The tag to use is `sergdelft/hyperion:pipeline-plugins-versiontracker-0.1.0`, for the latest version please check the repository.

To run this image with `versiontracker_config.yml` as its configuration execute:
```shell script
docker run -it -rm -v ${PWD}/versiontracker_config.yml:/root/config.yml sergdelft/hyperion:pipeline-plugins-versiontracker-0.1.0
```

### Building the docker image yourself
The included Dockerfile compiles the versiontracker into a fat jar and copies it to a new image which runs the plugin with the given config.
To build and run the plugin, execute the following command from the _project root_. 

```shell script
docker build . -f pipeline/plugins/versiontracker/Dockerfile -t hyperion-versiontracker:latest
```

after building is complete you can run the versiontracker.
Please note that the docker container will load the configuration file from `/root/config.yml` in its container.

```shell script
docker run -it -rm -v ${PWD}/versiontracker_config.yml:/root/config.yml hyperion-versiontracker:latest
```

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The map of projects that this plugin is able to version and their relevant
# git repository details. Any incoming log message that has a project not listed
# in this map will be passed on as-is.
projects:
    # The project name, as passed in the "project" field of the input.
    backend:
        # The URL to the repository. This can be the HTTP URL (if using https authentication)
        # or the SSH URL (if using SSH authentication).
        repository: https://github.com/Jane/backend.git
        # The branch to use as reference for the latest version. This is usually
        # refs/heads/<name of your production branch>.
        branch: refs/heads/production
        # The amount of time in seconds between each refresh of the current version. Lowering
        # this will increase networking traffic but decrease the amount of logs that may get
        # tagged with the wrong version after a deploy. Defaults to 360 seconds (6 minutes).
        update-interval: 60
        # The authentication used to access the git repository. If using an HTTP URL
        # that is publicly accessible, this field is not required. For all other operations, it is.
        authentication:
            # The type of authentication to use. Either 'https' or 'ssh'.
            # Note: https authentication using username and password is insecure
            # and not recommended for production. The version tracker will warn for this.
            type: https

            # If type == https, the username to use for authentication.
            username: jane
            # If type == https, the password to use for authentication.
            password: supersecretpassword123
    
            # If type == ssh, the path to the private SSH key to use for authentication.
            # Note: the SSH key must be in a PEM format. See the notes section of the README.
            keyPath: ./path/to/key/id_rsa

# Various settings needed for the plugin to interact with the pipeline,
# such as it's unique ID and the hostname and port of the Hyperion plugin manager.
# 
# Please note that the plugin must also be able to talk to any of its previous
# and next steps in the pipeline. As such, it is recommended that all of the 
# plugins are contained on a single networking setup.
pipeline:
    # The host and port pair that can be used to contact the Hyperion plugin manager.
    # Please note that this machine must be able to talk over TCP to the manager and
    # that the manager must be aware of this plugin/aggregator.
    manager-host: "manager:8000"
  
    # The unique ID of this pipeline step that matches the configuration of the plugin
    # manager. Used to identify which plugins are inputs/outputs of this step. Please
    # note that the plugin will crash at launch if the plugin manager does not recognize
    # this plugin ID.
    plugin-id: "VersionTracker"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Input Format

This plugin accepts any type of JSON value as input. If the input is not valid JSON, or if it is not a JSON object, it will be passed to the next stage of the pipeline unaffected. If the input is a JSON object but it does not have a `project` key, it will be passed on unmodified as well.

```json
{
    "project": "backend",
    "log_line": 10,
    "log_file": "MyFile.java"
}
```

## Output Format

This plugin will resolve the version of the project according to the configuration and output a new JSON object that contains an extra `version` field.

```json
{
    "project": "backend",
    "log_line": 10,
    "log_file": "MyFile.java",
    "version": "48a7361cbaddbc4976e3afd6664724859c380a92"
}
```

## Notes
Private key authentication currently only supports PEM style keys due to limitations in JGit. You can check whether your private key is in PEM format by checking if the file starts with `-----BEGIN RSA PRIVATE KEY-----`.

A workaround for converting an existing key from openSSH to PEM exists by changing the passphrase and rewriting it in a different format. This can be done by executing:

```shell script
$ ssh-keygen -p -N "new_passphrase" -m pem -f /path/to/key
```

Note that this will update the file in place. Consider saving a copy elsewhere beforehand.
