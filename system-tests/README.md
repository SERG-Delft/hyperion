# System Tests

This directory contains various example setups of system tests that have been performed during development of Hyperion. They are a combination of various pipeline plugins and allow us to quickly verify whether the pipeline as a whole is still functional.

The following system test scenarios are currently available:
- [elasticsearch-pipeline](elasticsearch-pipeline): A simple pipeline that receives log information from an ElasticSearch instance, modifies it slightly and then aggregates it. See the README in the folder for more details.

Please note that these system tests are not meant for end-users and are purely for development purposes.
