# encoding: utf-8
require "logstash/devutils/rspec/spec_helper"
require "logstash/outputs/hyperion"
require "logstash/codecs/json"
require "logstash/event"

require "ffi-rzmq"

describe LogStash::Outputs::Hyperion do
  context "registration and connection" do
    let(:plugin) {
      LogStash::Plugin.lookup("output", "hyperion").new({
        "id" => "Logstash",
        "pm_host" => "localhost",
        "pm_port" => 40191
      })
    }

    after do
      plugin.do_close
    end

    it "should query the plugin manager for identity" do
      pm_thread = start_pm_thread JSON.generate(isBind: true, host: "tcp://*:40192")
      begin
        expect { plugin.register }.to_not raise_error
        expect(pm_thread.value).to eq "{\"id\":\"Logstash\",\"type\":\"push\"}"
      ensure
	pm_thread.exit
      end
    end
  end

  context "sending" do
    let(:plugin) {
      LogStash::Plugin.lookup("output", "hyperion").new({
        "id" => "Logstash",
        "pm_host" => "localhost",
        "pm_port" => 40191
      })
    }

    let(:event) {
      LogStash::Event.new("topic" => "test-topic", "message" => "text")
    }

    it "should publish to ZMQ on events" do
      mock_socket = instance_spy("ZMQ::Socket", :send_string => 0)
      plugin.instance_variable_set(:@socket, mock_socket)

      plugin.publish event, "payload"
      expect(mock_socket).to have_received(:send_string).with("payload", ZMQ::DONTWAIT).ordered
    end

    it "should not throw even if ZMQ returns an error code" do
      mock_socket = instance_spy("ZMQ::Socket", :send_string => 0)
      allow(mock_socket).to receive(:send_string).and_return(-1) # error status code
      plugin.instance_variable_set(:@socket, mock_socket)

      expect { plugin.publish(event, "payload") }.to_not raise_error
    end

    it "should not throw even if ZMQ raises" do
      mock_socket = instance_spy("ZMQ::Socket", :send_string => 0)
      allow(mock_socket).to receive(:send_string).and_raise("boom!")
      plugin.instance_variable_set(:@socket, mock_socket)

      expect { plugin.publish(event, "payload") }.to_not raise_error
    end
  end
end

# Helper that starts a new plugin manager on port 40191 that always returns
# the specified response, regardless of what is sent. The returning thread
# returns the message that it responded to once.
def start_pm_thread(response)
  Thread.new {
    ctx = ZMQ::Context.new
    sock = ctx.socket(ZMQ::REP)
    sock.bind "tcp://*:40191"

    msg = ""
    sock.recv_string msg
    sock.send_string response
    
    sock.close
    ctx.terminate
    
    msg
  }
end
