package bgu.spl.net.impl.stomp.Frames.ServerFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.srv.Connections;

public class ReceiptFrame extends StompFrame {


    public ReceiptFrame(String receiptId) {
        super();
        this.setCommand("RECEIPT");
        this.addHeader("receipt-id", receiptId);
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {
        
    }

}
