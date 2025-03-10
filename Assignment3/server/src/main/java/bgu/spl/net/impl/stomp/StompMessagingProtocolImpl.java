package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.srv.Connections;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<StompFrame>{

    private int connectionId;

    private Connections<StompFrame> connections;

    private Boolean shouldTerminate = false;
    

    public void setShouldTerminate(Boolean shouldTerminate) {
        this.shouldTerminate = shouldTerminate;
    }

    public void start(int connectionId, Connections<StompFrame> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    public void process(StompFrame msg) {
        msg.process(connectionId, connections);
    }

    
 
 
    @Override
    public boolean shouldTerminate() {
        return this.shouldTerminate;
    }
}
