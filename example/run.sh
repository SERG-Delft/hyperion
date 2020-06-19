#!/usr/bin/env sh

# Build project.
echo "[+] Building project..."
../gradlew shadowJar

# Run components inside tmux
echo "[+] Launching tmux"
tmux \
  new-session  "java -jar ../pluginmanager/build/pluginmanager-all.jar pluginmanager.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../datasource/plugins/elasticsearch/build/elasticsearch-all.jar run elasticsearch.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../pipeline/plugins/renamer/build/renamer-all.jar renamer.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../pipeline/plugins/pathextractor/build/pathextractor-all.jar pathextractor.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../pipeline/plugins/adder/build/adder-all.jar adder.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../pipeline/plugins/versiontracker/build/versiontracker-all.jar versiontracker.yml; read" \; \
  select-layout tiled \; \
  split-window "sleep 1; java -jar ../aggregator/build/aggregator-all.jar aggregator.yml; read" \; \
  select-layout tiled
