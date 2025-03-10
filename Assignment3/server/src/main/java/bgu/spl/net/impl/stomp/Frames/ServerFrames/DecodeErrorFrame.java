package bgu.spl.net.impl.stomp.Frames.ServerFrames;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.srv.Connections;

public class DecodeErrorFrame extends StompFrame {
    private String reason;

    public DecodeErrorFrame (String reason) {
        super();
        this.setCommand("DECODE ERROR");
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {

        List<ConcurrentHashMap<String, String>> headers = new LinkedList<ConcurrentHashMap<String, String>>();
        headers.add(this.getHeaders());
        ErrorFrame decodeErr = new ErrorFrame(this, headers, reason);
        connections.send(connectionId,decodeErr);
        //optional
        //connections.disconnect(connectionId);      
    }

}
