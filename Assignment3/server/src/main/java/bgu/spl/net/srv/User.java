package bgu.spl.net.srv;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User<T> {

    private final String username;

    private final String password;

    private int connectionId;

    private boolean isConnected;

    private ConnectionHandler<T> connectionHandler;

    private Map<String , String> channelToSubscriptions;
    private Map<String , String> subscriptionsToChannels;
    

    public User(String username, String password, int connectionId, ConnectionHandler<T> connectionHandler){
        this.username = username;
        this.password = password;
        this.connectionId = connectionId;
        this.connectionHandler = connectionHandler;
        this.isConnected = true;
        this.channelToSubscriptions = new ConcurrentHashMap<String , String>();
        this.subscriptionsToChannels = new ConcurrentHashMap<String , String>(); 
    }


    public Map<String, String> getSubscriptionsToChannels() {
        return subscriptionsToChannels;
    }

    public Map<String , String> getChannelToSubscriptions(){
        return channelToSubscriptions;
    }

    

    public boolean isRegistered(String channel) {
        return (channelToSubscriptions.get(channel) != null);
    }

    /*
     * add subscription only if the user is not already registered to the channel.
     */
    public void addSubsctiption (String channel , String subscriptionId) {
        if (!channelToSubscriptions.containsKey(channel)) {
            channelToSubscriptions.put(channel, subscriptionId);
            subscriptionsToChannels.put(subscriptionId, channel);
            System.out.println(this.username + " Subscribed to channel " + channel + " SubscriptionId: " + subscriptionId);            
        }
    }

    public void removeSubscription (String channel , String subscriptionId) {
        channelToSubscriptions.remove(channel);
        subscriptionsToChannels.remove(subscriptionId);
        System.out.println(this.username + " Has unsubscribed from channel: " + channel + " SubscriptionId: " + subscriptionId);
    }


    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public ConnectionHandler<T> getConnectionHandler() {
        return connectionHandler;
    }

    public void setConnectionHandler(ConnectionHandler<T> connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public void resetSubscriptions () {
        this.channelToSubscriptions = new ConcurrentHashMap<String , String>();
        this.subscriptionsToChannels = new ConcurrentHashMap<String , String>(); 
    }

}
