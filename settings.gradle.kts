rootProject.name = "Hyperion"

include("aggregator")
include("datasource")
include("datasource:common")
include("datasource:plugins:elasticsearch")
include("plugin")
include("pluginmanager")
include("pipeline")
include("pipeline:common")
include("pipeline:plugins:extractor")
include("pipeline:plugins:pathextractor")
include("pipeline:plugins:renamer")
