package bgu.spl.net.impl.stomp.Frames.ServerFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.srv.Connections;

public class ConnectedFrame extends StompFrame {

    public ConnectedFrame () {
        super();
        this.setCommand("CONNECTED");
        this.addHeader("version", "1.2");
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }
        


}
