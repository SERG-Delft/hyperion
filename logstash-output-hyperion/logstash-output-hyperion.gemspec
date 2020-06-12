Gem::Specification.new do |s|
  s.name          = 'logstash-output-hyperion'
  s.version       = '0.1.0'
  s.licenses      = ['Apache-2.0']
  s.summary       = 'Integrates logstash with the Hyperion pipeline.'
  s.description   = 'Enables logstash to write events to plugins in the Hyperion pipeline. This plugin should be prefered over the ElasticSearch input plugin, when possible.'
  s.homepage      = 'https://github.com/serg-delft/hyperion'
  s.authors       = ['Hyperion Authors']
  s.require_paths = ['lib']

  # Files
  s.files = Dir['lib/**/*','spec/**/*','vendor/**/*','*.gemspec','*.md','CONTRIBUTORS','Gemfile','LICENSE','NOTICE.TXT']
   # Tests
  s.test_files = s.files.grep(%r{^(test|spec|features)/})

  # Special flag to let us know this is actually a logstash plugin
  s.metadata = { "logstash_plugin" => "true", "logstash_group" => "output" }

  # Gem dependencies
  s.add_runtime_dependency "ffi-rzmq", "~> 2.0.7"
  s.add_runtime_dependency "logstash-core-plugin-api", "~> 2.0"
  s.add_runtime_dependency "logstash-codec-json"
  s.add_development_dependency "logstash-devutils"
end
