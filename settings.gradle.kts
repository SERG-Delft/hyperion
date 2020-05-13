rootProject.name = "Hyperion"

include("aggregator")
include("plugin")
include("pluginmanager")
include("datasource")
include("datasource:common")
include("datasource:plugins:elasticsearch")
include("renamer")
include("extractor")
include("pipeline")
include("pipeline:common")
include("pipeline:plugins:sample")