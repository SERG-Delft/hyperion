# encoding: utf-8
require "logstash/outputs/base"
require "json"

# An output plugin for logstash that connects to a hyperion plugin
# manager and queries it for it's place in the pipeline. Will then
# set up 0MQ handlers as appropriate to push any events to the next
# step in the pipeline, effectively acting as an input for hyperion.
class LogStash::Outputs::Hyperion < LogStash::Outputs::Base
  # ZMQ sockets are not thread-safe. Could potentially pool these in the future.
  concurrency :single

  # Hyperion config scope
  config_name "hyperion"

  # Use the JSON codec, as Hyperion only uses JSON.
  default :codec, "json"

  # The ID of us, as registered in the plugin manager.
  config :id, :validate => :string, :required => true

  # The host of the plugin manager, required.
  config :pm_host, :validate => :string, :required => true

  # The port of the plugin manager, required.
  config :pm_port, :validate => :number, :required => true

  public
  def register
    load_zmq
    connect
  end # def register

  public
  def close
    check_zmq_error(@socket.close, "closing ZMQ socket") if @socket
    check_zmq_error(@context.terminate, "terminating ZMQ context") if @context
  rescue RuntimeError => e
    warn e.inspect
    @logger.error "Failed to properly teardown ZeroMQ"
  end

  public
  def multi_receive_encoded(events_and_encoded)
    events_and_encoded.each {|event, encoded| self.publish(event, encoded)}
  end

  private
  def load_zmq
    require "ffi-rzmq"
  end

  public
  def publish(event, payload)
    @logger.debug? && @logger.debug("hyperion: sending", :event => payload)
    check_zmq_error @socket.send_string(payload, ZMQ::DONTWAIT), "sending event to pipeline"
  rescue => e
    warn e.inspect
    @logger.warn "Unable to write event to Hyperion pipeline", :exception => e
  end

  private
  def connect
    raise "Already connected" if @context or @socket

    # Create a context
    @context = ZMQ::Context.new

    # Ask the plugin manager what we are.
    pm_sock = @context.socket(ZMQ::REQ)
    check_zmq_error pm_sock.connect("tcp://#{@pm_host}:#{@pm_port}"), "connect to plugin manager"
    check_zmq_error pm_sock.send_string(JSON.generate(id: @id, type: "push")), "register with plugin manager"

    msg = ""
    check_zmq_error pm_sock.recv_string(msg), "receiving response from plugin manager"
    config = JSON.parse(msg)

    pm_sock.close

    # Create the socket for pushing, using the info we just received.
    @socket = @context.socket(ZMQ::PUSH)
    if config["isBind"] then
      check_zmq_error @socket.bind(config["host"]), "binding to zmq socket"
    else
      check_zmq_error @socket.connect(config["host"]), "connecting to zmq socket"
    end
  rescue => e
    @logger.error "Unable to setup Hyperion output sockets. Is the plugin manager running, accessible, and configured with this plugin?"
    @logger.error "Error: " + e.full_message, { :error => e }
  end

  private
  def check_zmq_error(rc, doing)
    unless ZMQ::Util.resultcode_ok?(rc) || ZMQ::Util.errno == ZMQ::EAGAIN
      @logger.error("ZeroMQ error while #{doing}", { :error_code => rc })
      raise "ZeroMQ error while #{doing}"
    end
  end
end # class LogStash::Outputs::Hyperion
