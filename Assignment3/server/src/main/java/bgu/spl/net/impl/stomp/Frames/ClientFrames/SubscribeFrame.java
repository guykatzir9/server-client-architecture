package bgu.spl.net.impl.stomp.Frames.ClientFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.Connections;

public class SubscribeFrame extends StompFrame {

    public SubscribeFrame(){
        super();
        this.setCommand("SUBSCRIBE");
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {

        if (!hasReceipt()) {
            frameFunctions.handleNoReceiprErr(this , connectionId , connections);
            return;
        }

        String receiptId = this.getHeaders().get("receipt");
        String validation = isValid();
        if(!validation.equals("true")){
            frameFunctions.handleSyntaxErr(this , validation, receiptId , connectionId , connections);
            return;
        }

        if (!connections.existConnectedUser(connectionId)) {
            frameFunctions.handleNotConnectedUser(this , "no connected user" , receiptId, connectionId , connections);
            return;
        }
        String destination = this.getHeaders().get("destination");
        System.out.println(destination);
        if (connections.isRegisterd(connectionId , destination)) {
            frameFunctions.handleAlreadyRegisteredErr(this , connectionId , receiptId , connections);
            return;
        }
        //subscribe to the channel
        String channelId = this.getHeaders().get("id");
        connections.subscribe(connectionId, channelId , destination);
        frameFunctions.sendReceiptFrame(receiptId, connectionId, connections);   
    }

    
    public String isValid() {
        if (!getHeaders().containsKey("id")) {
            return "Missing id header";
        }

        if (!getHeaders().containsKey("destination")) {
            return "Missing destination header";
        }

        if (!getBody().equals("")) {
            return "SUBSCRIBE frame should not have a body"; 
        }

        return "true"; // Frame is valid
    }

    public boolean hasReceipt() {
        return getHeaders().containsKey("receipt");
    }

    
}
