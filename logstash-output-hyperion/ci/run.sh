sudo apt-get install libzmq3-dev
bundle install
bundle exec rspec spec
bundle exec rspec spec --tag integration
