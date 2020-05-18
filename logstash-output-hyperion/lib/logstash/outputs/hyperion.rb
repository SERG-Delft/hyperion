# encoding: utf-8
require "logstash/outputs/base"

# An hyperion output that does nothing.
class LogStash::Outputs::Hyperion < LogStash::Outputs::Base
  config_name "hyperion"

  public
  def register
  end # def register

  public
  def receive(event)
    return "Event received"
  end # def event
end # class LogStash::Outputs::Hyperion
