package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final MessagingProtocol<T> protocol;

    private final MessageEncoderDecoder<T> encdec;

    private final Socket sock;

    private BufferedInputStream in;

    private BufferedOutputStream out;

    private volatile boolean connected = true;

    private User<T> user = null;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            this.in = new BufferedInputStream(sock.getInputStream());
            this.out = new BufferedOutputStream(sock.getOutputStream());
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    this.protocol.process(nextMessage);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        System.out.println("close has been called in the server side");
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try{
            this.out.write(this.encdec.encode(msg));
            this.out.flush();
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

    public User<T> getUser(){
        return this.user;
    }

    public void setUser(User<T> user){
        this.user = user;
    }
    
    @Override
    public MessagingProtocol<T> getProtocol() {
        return this.protocol;
    }
}
