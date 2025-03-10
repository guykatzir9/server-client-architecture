package bgu.spl.net.impl.stomp.Frames.ClientFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.Connections;

public class DisconnectFrame extends StompFrame {

    public DisconnectFrame(){
        super();
        this.setCommand("DISCONNECT");
    }

    /*
     *Disconnect the client from the server and return a receipt with the id given 
     in the Disconnect Frame to the client. 
     */
    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {
        System.out.println("disconnect frame has received by the server");

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
        frameFunctions.sendReceiptFrame(receiptId , connectionId , connections);
        System.out.println("receipt has been sent fron the server to the client");
        connections.disconnect(connectionId);
        
    }

    
    public String isValid() {

        if (!getBody().equals("")) {
            return "DISCONNECT frame should not have a body";
        }
        return "true"; // Frame is valid
    }

    public boolean hasReceipt() {
        return getHeaders().containsKey("receipt");
    }

}
