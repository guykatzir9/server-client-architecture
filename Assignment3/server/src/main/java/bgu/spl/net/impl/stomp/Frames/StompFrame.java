package bgu.spl.net.impl.stomp.Frames;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.Connections;

public abstract class StompFrame {
    private String command;
    private ConcurrentHashMap<String,String> headers;
    private String body;

    public StompFrame() {
        headers = new ConcurrentHashMap<String,String>();
    }

    // Getters and Setters
    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public ConcurrentHashMap<String,String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    // Abstract process methods to be implemented by subclasses
    public abstract void process(int connectionId, Connections<StompFrame> connections);

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(command).append("\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        sb.append("\n").append(body);
        return sb.toString();
    }

}
