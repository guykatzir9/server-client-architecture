package bgu.spl.net.impl.stomp.Frames.ServerFrames;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.Connections;

public class ErrorFrame extends StompFrame {

    public ErrorFrame(StompFrame badFrame, List<ConcurrentHashMap<String, String>> headers, String errorDetails){
        super();
        this.setCommand("ERROR");
        for(ConcurrentHashMap<String, String> header : headers){
            for(String key : header.keySet()){
                this.addHeader(key, header.get(key));
            }
        }
        this.setBody(frameFunctions.makeErrorBody(badFrame, errorDetails));
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {
    }
}
