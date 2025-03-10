package bgu.spl.net.impl.stomp.Frames.ClientFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.Connections;

public class UnsubscribeFrame extends StompFrame {

    public UnsubscribeFrame(){
        super();
        this.setCommand("UNSUBSCRIBE");
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
            frameFunctions.handleNotConnectedUser(this , "no connected user" ,receiptId, connectionId , connections);
            return;
        }

        String subscriptionId = this.getHeaders().get("id");
        if (subscriptionId == null) {
            System.out.println("subscriptionId is null");
        }
        if (subscriptionId.equals("")) {
            System.out.println("subscriptionId is empty");
        }
        System.out.println(subscriptionId);
        System.out.println("printing has been happenning");
        String channel = connections.getConnectionHandler(connectionId).getUser().getSubscriptionsToChannels().get(subscriptionId);
        if (channel == null){
            System.out.println("channel is null");
            return;
        }
        if(!connections.isRegisterd(connectionId, channel)){
            frameFunctions.handleWrongUnsubscribeErr(this, connectionId, receiptId, connections);
            return;
        }
        //unsubscribe to this channel.    
        connections.unsubscribe(connectionId, channel);
        frameFunctions.sendReceiptFrame(receiptId, connectionId, connections);        
    }
    
    public String isValid() {
        if (!getHeaders().containsKey("id")) {
            return "Missing id header"; 
        }

        if (!getBody().equals("")) {
            return "UNSUBSCRIBE frame should not have a body"; 
        }

        return "true"; // Frame is valid
    }

    public boolean hasReceipt() {
        return getHeaders().containsKey("receipt");
    }



}
