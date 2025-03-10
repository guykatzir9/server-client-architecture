package bgu.spl.net.impl.stomp.Frames.ClientFrames;

import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.frameFunctions;
import bgu.spl.net.srv.Connections;

public class ConnectFrame extends StompFrame {

    private static final String VALID_ACCEPT_VERSIONS = "1.2";
    private static final String DEFAULT_HOST = "stomp.cs.bgu.ac.il";

    public ConnectFrame(){
        super();
        this.setCommand("Connect");
    }


    @Override
    public void process(int connectionId, Connections<StompFrame> connections) {

        // if (!hasReceipt()) {
        //     frameFunctions.handleNoReceiprErr(this , connectionId , connections);
        //     return;
        // }

        String receiptId = this.getHeaders().get("receipt");
        if(receiptId == null){
            receiptId = "-1";
        }
        String validation = isValid();
        if(!validation.equals("true")){
            frameFunctions.handleSyntaxErr(this , validation, receiptId , connectionId , connections);
            return;
        }

        String username = this.getHeaders().get("login");
        String password = this.getHeaders().get("passcode");
        if (connections.existingUser(username)) {

            if (!connections.getConnections().get(username).getPassword().equals(password)) {
                frameFunctions.handleWrongPasswordErr(this , connectionId , receiptId ,connections);
                return;
            }

            if(connections.isLoggedIn(username)){
                frameFunctions.handleExisitingUserErr(username , this , receiptId ,connectionId , connections);
                return;
            }

            else { // activate the user and create a connection
                frameFunctions.connectExistingUser(username, connectionId, connections);
                //frameFunctions.sendReceiptFrame(receiptId, connectionId, connections);
                return;
            }
        }
        else { // create and activate new user and add it 
            frameFunctions.connectNewUser(username, password, connectionId, connections);
            //frameFunctions.sendReceiptFrame(receiptId, connectionId, connections);
            return;
        }
    }

    public boolean hasReceipt() {
        return getHeaders().containsKey("receipt");
    }

    public String isValid() {
        String acceptVersion = getHeaders().get("accept-version");
        String host = getHeaders().get("host");

        if (acceptVersion == null || !VALID_ACCEPT_VERSIONS.equals(acceptVersion)) {
            return "Missing or invalid accept-version (valid accept-version is 1.2))";  
        }

        if (host == null || !host.equals(DEFAULT_HOST)) {
            return "Missing or invalid host"; 
        }

        if (!getHeaders().containsKey("login")) {
            return "Missing login"; 
        }

        if (!getHeaders().containsKey("passcode")) {
            return "Missing passcode"; 
        }

        if (!getBody().equals("")) {
            return "CONNECT frame should not have a body";  
        }

        return "true"; // Frame is valid
    }
    
}
