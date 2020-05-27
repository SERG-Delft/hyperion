# Git version tracker plugin

Hyperion plugin that tracks the version of a git repository branch and adds
it as a field to the input.

It does this by periodically polling the commit hashes of all HEADs, 
and then checking if the given reference exists. The reference's object id 
is subsequently added in a "version" field in the JSON formatted log.

The plugin can be run by passing in a `.yml` config.

Fetching from private repositories requires authentication however,
the plugin supports username/password or ssh key authentication. 
Username/password should preferably not be used, as that
would require storing a password in plain text. It is recommended to
only use it when in development or testing.

The following shows a configuration for tracking multiple projects using
different authentication schemes. The `updateInterval` field is optional
and defines the amount of seconds to wait between each fetch.

```yaml
projects:
  sap:
    repository: https://github.com/John/sap.git
    branch: refs/heads/production
    updateInterval: 60
  backend:
    repository: https://github.com/Jane/backend.git
    branch: refs/heads/production
    authentication:
      type: https
      username: jane
      password: supersecretpassword123
  datastore:
    repository: git@github.com:Richard/datastore.git
    branch: refs/heads/production
    authentication:
      type: ssh
      keyPath: ./path/to/key/id_rsa

zmq:
  id: VersionTracker
  pluginManager: "tcp://localhost:5555"
```

## Example of plugin

current hash of HEAD on `production` on `https://github.com/John/sap.git`:
`48a7361cbaddbc4976e3afd6664724859c380a92`


Input:
```json
{
  "message": "[May 27 09:27:13] ERROR com.sap.Main:23 - Test",
  "project": "sap"
}
```

Output:
```json
{
  "message": "[May 27 09:27:13] ERROR com.sap.Main:23 - Test",
  "project": "sap",
  "version": "48a7361cbaddbc4976e3afd6664724859c380a92"
}
```

## Notes
Private key authentication currently only supports PEM style keys due to JGit.
You can verify that it is in PEM if the header starts with `-----BEGIN RSA PRIVATE KEY-----`.

A workaround for converting an existing key from openSSH to PEM exists by changing the
passphrase and rewriting it in a different format. This can be done by executing:

```shell script
$ ssh-keygen -p -N "new_passphrase" -m pem -f /path/to/key
```

This will change the file in place! So save it to somewhere else if necessary.


