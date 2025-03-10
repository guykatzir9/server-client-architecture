package bgu.spl.net.srv;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void send(String channel, T msg);

    void disconnect(int connectionId);

    public void subscribe(int connectionId,String channelId, String channel);

    public void unsubscribe(int connectionId, String channel);

    public Boolean isRegisterd (int connectionId , String channel);

    public Map<String, List<Integer>> getChannels();

    public Map<String, User<T>> getConnections();

    public Map<Integer, ConnectionHandler<T>> getConnectionHandlers();

    public AtomicInteger getMessageCounter();

    public boolean isLoggedIn(String username);

    public boolean existingUser (String username);

    public boolean existConnectedUser (int connectionId);

    public boolean checkChannel(String channel);

    public void addConnectionHandler(int conneciotnId, ConnectionHandler<T> handler);

    public ConnectionHandler<T> getConnectionHandler(int conneciotnId);



}
