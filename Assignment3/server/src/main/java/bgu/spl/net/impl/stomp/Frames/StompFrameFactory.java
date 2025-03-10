package bgu.spl.net.impl.stomp.Frames;

import bgu.spl.net.impl.stomp.Frames.ClientFrames.ConnectFrame;
import bgu.spl.net.impl.stomp.Frames.ClientFrames.DisconnectFrame;
import bgu.spl.net.impl.stomp.Frames.ClientFrames.SendFrame;
import bgu.spl.net.impl.stomp.Frames.ClientFrames.SubscribeFrame;
import bgu.spl.net.impl.stomp.Frames.ClientFrames.UnsubscribeFrame;

public class StompFrameFactory {
    public static StompFrame createFrame(String command) {
        switch (command.toUpperCase()) {
            // Client Frames
            case "CONNECT":
                return new ConnectFrame();
            case "SEND":
                return new SendFrame();
            case "SUBSCRIBE":
                return new SubscribeFrame();
            case "UNSUBSCRIBE":
                return new UnsubscribeFrame();
            case "DISCONNECT":
                return new DisconnectFrame();

            default:
                throw new IllegalArgumentException("Unknown STOMP command: " + command);
        }
    }
}
