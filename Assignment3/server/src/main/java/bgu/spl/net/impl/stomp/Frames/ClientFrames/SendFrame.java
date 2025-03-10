package bgu.spl.net.impl.stomp.Frames.ClientFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class SendFrame extends StompFrame {

    public SendFrame(){
        super();
        this.setCommand("SEND");
    }

    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {

        //If connectionId isn't exist or the user is not connected we won't send the msg.
        ConnectionHandler<StompFrame> handler = connections.getConnectionHandler(connectionId);
        if( handler == null || !handler.getUser().isConnected()){
            System.out.println("No active user found for given connectionID");
            return;
        }

        // if (!hasReceipt()) {
        //     frameFunctions.handleNoReceiprErr(this , connectionId , connections);
        //     connections.disconnect(connectionId);
        //     return;
        // }

        String receipt = getHeaders().get("receipt");
        if (receipt == null) {
            receipt = "-1";
        }

        String validation = isValid();
        if (!validation.equals("true")) { //If the frame is malformed in syntax
            frameFunctions.handleSyntaxErr(this, validation, receipt, connectionId, connections);
            connections.disconnect(connectionId);
            return;
        }

        String channel = this.getHeaders().get("destination");

        if (!checkDetination(connections, channel)){
            // send error and close - channel doesnt exist
            frameFunctions.handleNoSuchChannelError(channel, this, connectionId, receipt, connections);
            return;
        }

        if (connections.isRegisterd(connectionId , channel)) {
            for (Integer Id : connections.getChannels().get(channel)) {
               // create unique msg for this user
               User<StompFrame> currUser = connections.getConnectionHandlers().get(Id).getUser();
               String subscriptionId = currUser.getChannelToSubscriptions().get(channel);
               String messageId = connections.getMessageCounter().toString();
               String body = this.getBody();
               frameFunctions.sendMessageFrame(subscriptionId, messageId, channel, body, Id , connections);
               //frameFunctions.sendReceiptFrame(receipt, connectionId, connections);
           }   
       }else {
                //send error - client can't send message to channel not subscribed to 
                frameFunctions.handleNotCurrentlySubscribedError(channel, this, connectionId, receipt, connections);
                return;
            }
    }

    private boolean checkDetination(Connections<StompFrame> connections, String channel){
        return connections.checkChannel(channel);
    }
    
    public String isValid() {
        if (!getHeaders().containsKey("destination")) {
            return "Headers did not contain a detination header";
        }

        if (getBody().equals("")) {
            return "SEND frame must have a body"; 
        }

        return "true"; // Frame is valid
    }

    public boolean hasReceipt() {
        return getHeaders().containsKey("receipt");
    }

}
