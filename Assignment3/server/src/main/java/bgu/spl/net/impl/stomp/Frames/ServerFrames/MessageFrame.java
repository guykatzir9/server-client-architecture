package bgu.spl.net.impl.stomp.Frames.ServerFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.srv.Connections;

public class MessageFrame extends StompFrame {

    public MessageFrame(String subscription , String messageId, String destination , String body){

        super();
        this.setCommand("MESSAGE");
        this.addHeader("subscription", subscription);
        this.addHeader("messageId", messageId);
        this.addHeader("destination", destination);
        this.setBody(body);
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {

    }



}
