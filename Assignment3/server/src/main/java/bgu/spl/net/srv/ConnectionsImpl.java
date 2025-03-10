package bgu.spl.net.srv;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections<T> {

    private final Map <Integer , ConnectionHandler<T>> connectionHandlers;

    private final Map<String, User<T>> connections; 

    private final Map<String, List<Integer>> channels;

    AtomicInteger messageCounter = new AtomicInteger(1);

    public ConnectionsImpl(){
        // key = connectionId   Value = corresponding ConnectionHandler
        this.connectionHandlers = new ConcurrentHashMap<Integer , ConnectionHandler<T>>();
        // Key = user name      Value = corresponding user
        this.connections = new ConcurrentHashMap<String, User<T>>(); 
        // Key = Channel        Value = list of connectionId subscribed to this channel
        this.channels = new ConcurrentHashMap<String,List<Integer>>(); 
    }


    public Map<Integer, ConnectionHandler<T>> getConnectionHandlers() {
        return connectionHandlers;
    }

    public Map<String, User<T>> getConnections() {
        return connections;
    }

    public Map<String, List<Integer>> getChannels() {
        return channels;
    }

    public AtomicInteger getMessageCounter() {
        return messageCounter;
    }

    public boolean existConnectedUser (int connectionId) {
        User<T> user = connectionHandlers.get(connectionId).getUser();
        return (user != null && user.isConnected() );
    }

    // assuming we call this method only for valid connectionId
    public Boolean isRegisterd (int connectionId , String channel) { 

        User<T> currUser = connectionHandlers.get(connectionId).getUser();
        return currUser.isRegistered(channel);
    }

    public boolean send(int connectionId, T msg){

        //If connectionId isn't exist or the user is not connected we won't send the msg.
        ConnectionHandler<T> currHandler = connectionHandlers.get(connectionId);
        if( currHandler == null) {
            System.out.println("No active user found for given connectionID");
            return false;
        }
        //else send the msg to the client using the ConnectionHandler        
        currHandler.send(msg);
        return true;
    }

    public void send(String channel, T msg){
     
    }    
    
    /*
     * disconnect the user corresponding for this connection id. assuming legal id here
     * closing the connection handler and reset the user subscriptions.
     * 
     */
    public void disconnect(int connectionId){      
        //Close the client's connection handler and remove from list
            ConnectionHandler<T> connectionHandler = connectionHandlers.remove(connectionId);
            User<T> user = connectionHandler.getUser();
            if (user != null) {
                for (String channel : user.getChannelToSubscriptions().keySet()) {
                    channels.get(channel).remove((Integer)connectionId);
                }
                user.resetSubscriptions();
                user.setConnected(false);
                user.setConnectionHandler(null);
                user.setConnectionId(-1);
                connectionHandler.getProtocol().setShouldTerminate(true);
            } 
    }

    /**
     * Subscribes a client to a specific channel.
     * If the server does not contain any subscriptions to this channel, a new subscription list is created for the channel.
     * 
     * @param connectionId the ID of the client connection
     * @param channelId the ID of the channel to subscribe to
     * @param channel the name of the channel to subscribe to
     */
    public void subscribe(int connectionId,String channelId, String channel) {

        User<T> currUser = connectionHandlers.get(connectionId).getUser();
        currUser.addSubsctiption(channel, channelId);

        //if channel does'nt exist , create new set for it.
        if (!channels.containsKey(channel)) {
            channels.put(channel, new LinkedList<Integer>());    
        }
        channels.get(channel).add(connectionId);       
    }    

    /**
     * Unsubscribes a client from a specific channel.
     *
     * @param connectionId the ID of the connection to unsubscribe
     * @param channel the name of the channel to unsubscribe from
     *
     * This method removes the subscription of the client identified by the given connectionId
     * from the specified channel if the subscription exists. If the client has no more subscriptions
     * after the removal, the client is removed from the clientSubscriptions map.
     */

     public void unsubscribe(int connectionId, String channel){

        User<T> currUser = connectionHandlers.get(connectionId).getUser();
        String subscriptionId = currUser.getChannelToSubscriptions().get(channel);
        currUser.removeSubscription(channel, subscriptionId);
        channels.get(channel).remove((Integer)connectionId);

        synchronized (channels) {
            if (channels.get(channel).isEmpty()) {
                channels.remove(channel);
            }
        }
    }

    public boolean isLoggedIn(String username){
        if(connections.containsKey(username) && connections.get(username).isConnected())
            return true;
        return false;
    }

    public boolean existingUser (String username) {
        return connections.containsKey(username);
    }

    public boolean checkChannel(String channel){
        return this.channels.containsKey(channel);
    }

    public void addConnectionHandler(int conneciotnId, ConnectionHandler<T> handler){
        this.connectionHandlers.put(conneciotnId, handler);
    }

    public ConnectionHandler<T> getConnectionHandler(int conneciotnId){
        return connectionHandlers.get(conneciotnId);
    }

}





