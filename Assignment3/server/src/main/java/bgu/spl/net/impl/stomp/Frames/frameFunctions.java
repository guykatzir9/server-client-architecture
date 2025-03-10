package bgu.spl.net.impl.stomp.Frames;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.stomp.Frames.ServerFrames.ConnectedFrame;
import bgu.spl.net.impl.stomp.Frames.ServerFrames.ErrorFrame;
import bgu.spl.net.impl.stomp.Frames.ServerFrames.MessageFrame;
import bgu.spl.net.impl.stomp.Frames.ServerFrames.ReceiptFrame;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class frameFunctions {

    public static void sendMessageFrame (String subscription , String messageId , String destination , String body, int connectionId , Connections<StompFrame> connections) {
        MessageFrame message = new MessageFrame(subscription, messageId, destination, body);
        System.out.println(message.toString());
        connections.send(connectionId, message);
    }

    public static void sendErrorFrame(StompFrame badFrame, List<ConcurrentHashMap<String, String>> headers, String errorDetails, int connectionId, Connections<StompFrame> connections ){
        ErrorFrame errorFrame = new ErrorFrame(badFrame, headers, errorDetails);
        connections.send(connectionId, errorFrame);
        connections.disconnect(connectionId);
        System.out.println(errorFrame.toString());
    }

    public static void sendReceiptFrame (String receiptId, int connectionId , Connections<StompFrame> connections) {
        connections.send(connectionId, new ReceiptFrame(receiptId));
    }

    public static String makeErrorBody(StompFrame badFrame, String errorDetails){
        String body = "";
        body = body + "The message: \\n";
        body = body + "----- \\n";
        body = body + badFrame.toString();
        body = body + "----- \\n";
        body = body + errorDetails;
        return body;
    }


    public static List<ConcurrentHashMap<String, String>> createErrorHeaders(String errMessage , String receiptId){
        List<ConcurrentHashMap<String, String>> headers = new ArrayList<>();
        ConcurrentHashMap<String,String> receipt = new ConcurrentHashMap<>();
        ConcurrentHashMap<String,String> message = new ConcurrentHashMap<>();
        receipt.put("receipt-id", receiptId);
        message.put("message", errMessage);
        headers.add(receipt);
        headers.add(message);
        return headers;
    }

    public static void handleSyntaxErr(StompFrame msg , String errbody, String reciptId , int connectionId , Connections<StompFrame> connections) {
        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("malformed syntax received" , reciptId);
        frameFunctions.sendErrorFrame(msg, headers, errbody, connectionId, connections);
    }

    
    public static void handleNoReceiprErr(StompFrame msg , int connectionId , Connections<StompFrame> connections) {
        String reciptId = "null";   
        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("receipt not received" , reciptId);
        String errbody = "this message must contain receipt id to be send successfully";
        frameFunctions.sendErrorFrame(msg, headers, errbody, connectionId, connections);
    }

    public static void handleExisitingUserErr(String username , StompFrame msg , String reciptId , int connectionId , Connections<StompFrame> connections) {
       
        // if client is already logged there is no need to send error and disconnect
         if (connectionId == connections.getConnections().get(username).getConnectionId()) {
            System.out.println("client is already logged in");
            return;
        }         
        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("already connected" , reciptId);
        String bodyError = "this user is already connected to the server from other client";
        frameFunctions.sendErrorFrame(msg , headers , bodyError, connectionId, connections);
          
    }

    public static void handleWrongPasswordErr(StompFrame msg , int connectionId , String reciptId , Connections<StompFrame> connections) {

        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("wrong password" , reciptId);
        String bodyError = "received wrong password for this username";
        frameFunctions.sendErrorFrame(msg , headers , bodyError, connectionId, connections);

    }

    public static void connectExistingUser(String username , int connectionId , Connections<StompFrame> connections) {
        User<StompFrame> user = connections.getConnections().get(username);
        ConnectionHandler<StompFrame> handler = connections.getConnectionHandlers().get(connectionId);
        user.setConnected(true);
        user.setConnectionHandler(handler);
        user.setConnectionId(connectionId);
        handler.setUser(user);
        frameFunctions.sendConnectedFrame(connectionId, connections);
    }

    public static void sendConnectedFrame(int connectionId , Connections<StompFrame> connections) {
        connections.send(connectionId, new ConnectedFrame());
    }

    public static void connectNewUser(String username , String password , int connectionId , Connections<StompFrame> connections) {
        ConnectionHandler<StompFrame> handler = connections.getConnectionHandler(connectionId);
        User<StompFrame> user = new User<>(username, password, connectionId , handler);
        handler.setUser(user);
        connections.getConnections().put(username, user);
        frameFunctions.sendConnectedFrame(connectionId, connections);

    }

    public static void handleNotConnectedUser (StompFrame msg , String errMessage , String receiptId , int connectionId , Connections<StompFrame> connections) {

        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders(errMessage , receiptId);
        String bodyError = "Attempted to send a message other than 'CONNECT' before establishing a connection to the server.";
        frameFunctions.sendErrorFrame(msg , headers , bodyError, connectionId, connections);
    }


    public static void handleAlreadyRegisteredErr(StompFrame msg , int connectionId , String reciptId , Connections<StompFrame> connections) {

        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("already registerd" , reciptId);
        String bodyError = "The user attempted to subscribe to a channel they are already registered to";
        frameFunctions.sendErrorFrame(msg , headers , bodyError, connectionId, connections);
    }

    public static void handleWrongUnsubscribeErr(StompFrame msg , int connectionId , String reciptId , Connections<StompFrame> connections) {

        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("wrong unsubscribe" , reciptId);
        String bodyError = "The user attempted to unsubscribe to a channel they are not registered to";
        frameFunctions.sendErrorFrame(msg , headers , bodyError, connectionId, connections);
    }

    public static void handleNoSuchChannelError(String channel ,StompFrame msg, int connectionId, String receiptId, Connections<StompFrame> connections){
        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("No such channel" , receiptId);
        String bodyError = "The channel " + channel + " does not exist";
        frameFunctions.sendErrorFrame(msg, headers, bodyError, connectionId, connections);
    }
    
    public static void handleNotCurrentlySubscribedError(String channel ,StompFrame msg, int connectionId, String receiptId, Connections<StompFrame> connections){
        List<ConcurrentHashMap<String, String>> headers = createErrorHeaders("Sent to channel not subscribed to" , receiptId);
        String bodyError = "The user cannot send a message to channel " + channel + " Since he is not subscribed";
        frameFunctions.sendErrorFrame(msg, headers, bodyError, connectionId, connections);
    }
    
    

}






 
