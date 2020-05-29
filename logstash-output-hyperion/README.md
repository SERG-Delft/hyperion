# Hyperion - Logstash Output Plugin

![Ruby spec logstash plugin](https://github.com/SERG-Delft/monitoring-aware-ides/workflows/Ruby%20spec%20logstash%20plugin/badge.svg)

This is an output plugin for [Logstash](https://github.com/elastic/logstash), allowing it to send data to Hyperion. If you have a typical ELK logging stack, this output can be used to push data to the Hyperion pipeline as it arrives in LogStash.

It is fully free and fully open source. The license is Apache 2.0, meaning you are pretty much free to use it however you want in whatever way.

## Dependencies & Installation

Using the Logstash output plugin requires you to have libzmq installed. On Debian-based distributions, this can be done using

```shell script
$ sudo apt install libzmq3-dev
```

After installing dependencies, the output plugin can be installed into Logstash using the `bin/logstash-plugin` binary bundled with Logstash as follows:

```shell script
$ bin/logstash-plugin install <path to .gem file>
```

The `.gem` file needed for install can be found in the releases or by manually building the plugin, as per the next section.

## Usage and Documentation

For a full documentation, please see the [asciidoc](docs/index.asciidoc). This document also discusses all the configuration options for the plugin.

In general, simply installing the plugin and adding the following section to your Logstash configuration should work:

```
output {
    hyperion {
        id => "<id of this plugin within the pipeline>"
        pm_host => "<hostname of the plugin manager>"
        pm_port => 12345
    }
}
```

## Building & Testing

To get started, you'll need JRuby with the Bundler gem installed. The JRuby version used for development is `9.1.12.0`. We recommend using [RVM](https://rvm.io/) to manage your Ruby versions.

Install all required dependencies using the following command:

```shell script
$ bundle install
```

The tests can be ran using the following command:

```shell script
$ bundle exec rspec
```

---

If you want to test your plugin in a local Logstash clone, follow the following instructions:

- Edit Logstash `Gemfile` and add the local plugin path, for example:
```ruby
gem "logstash-output-hyperion", :path => "/your/local/logstash-output-hyperion"
```

- Install plugin
```sh
bin/logstash-plugin install --no-verify
```

- Run Logstash with the plugin
```sh
bin/logstash -e 'output { hyperion { ... } }'
```

At this point any modifications to the plugin code will be applied to this local Logstash setup. After modifying the plugin, simply rerun Logstash.

---

If you want to build/install the plugin in an installed Logstash, follow the following instructions:

- Build your plugin gem
```sh
gem build logstash-output-hyperion.gemspec
```

- Install the plugin from the Logstash home
```sh
bin/logstash-plugin install /your/local/plugin/logstash-output-hyperion.gem
```

- Start Logstash and proceed to test the plugin
