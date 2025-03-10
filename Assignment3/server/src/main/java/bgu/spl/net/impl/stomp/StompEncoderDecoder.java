package bgu.spl.net.impl.stomp;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.Frames.StompFrame;
import bgu.spl.net.impl.stomp.Frames.StompFrameFactory;
import bgu.spl.net.impl.stomp.Frames.ServerFrames.*;


public class StompEncoderDecoder implements MessageEncoderDecoder<StompFrame> {
    // Define the states of the decoder
    private enum State {
        COMMAND,
        HEADER_KEY,
        HEADER_VALUE,
        BODY,
        END
    }

    private State state = State.COMMAND;    // Initial state
    private StompFrame frame = null;        // Current frame being decoded
    private StringBuilder current = new StringBuilder();  // Buffer for accumulating characters
    private String currentHeaderKey = null; // Current header key being processed
    private byte[] bodyBytes = null;        // Buffer for body bytes when Content-Length is specified
    private int bodyLength = -1;            // Expected body length (-1 if not set)
    private int bodyReceived = 0;           // Number of body bytes received so far

    /**
     * Decodes the next byte in the incoming data stream.
     *
     * @param nextByte the next byte to decode
     * @return a fully decoded StompFrame if the frame is complete, or null otherwise
     */
    @Override
    public StompFrame decodeNextByte(byte nextByte) {
        // If in END state, reset the decoder to start processing a new frame
        if (state == State.END) {
            reset();
        }

        switch (state) {
            case COMMAND:
                if (nextByte == '\n') {
                    String command = current.toString().trim();
                    try {
                        frame = StompFrameFactory.createFrame(command);
                    } catch (IllegalArgumentException e) {
                        // Handle unknown command by creating an ERROR frame
                        frame = new DecodeErrorFrame("sent a message with unknown command");
                        frame.addHeader("message", e.getMessage());
                        frame.setBody("Unknown command: " + command);
                        state = State.END;
                        return frame;
                    }
                    current.setLength(0);
                    state = State.HEADER_KEY;
                } else {
                    current.append((char) nextByte);
                }
                break;

            case HEADER_KEY:
                if (nextByte == ':') {
                    currentHeaderKey = current.toString().trim();
                    current.setLength(0);
                    state = State.HEADER_VALUE;
                } else if (nextByte == '\n') {
                    // Empty line indicates end of headers, start of body
                    state = State.BODY;
                    // Check for Content-Length header
                    String contentLength = frame.getHeaders().get("content-length");
                    if (contentLength != null) {
                        try {
                            bodyLength = Integer.parseInt(contentLength.trim());
                            bodyBytes = new byte[bodyLength];
                        } catch (NumberFormatException e) {
                            // Invalid Content-Length value, treat as error
                            frame = new DecodeErrorFrame("sent a message with invalid content length");
                            frame.addHeader("message", "Invalid Content-Length value: " + contentLength);
                            frame.setBody("Cannot parse Content-Length header.");
                            state = State.END;
                            return frame;
                        }
                    }
                } else {
                    current.append((char) nextByte);
                }
                break;

            case HEADER_VALUE:
                if (nextByte == '\n') {
                    String headerValue = current.toString().trim();
                    frame.addHeader(currentHeaderKey, headerValue);
                    current.setLength(0);
                    state = State.HEADER_KEY;
                } else {
                    current.append((char) nextByte);
                }
                break;

            case BODY:
                if (bodyLength == -1) {
                    // No Content-Length header, read until null byte
                    if (nextByte == 0) { // Null byte indicates end of frame
                        frame.setBody(current.toString());
                        state = State.END;
                        return frame;
                    } else {
                        current.append((char) nextByte);
                    }
                } else {
                    // Content-Length specified, read exact number of bytes
                    bodyBytes[bodyReceived++] = nextByte;
                    if (bodyReceived == bodyLength) {
                        // After reading body, expect a null byte to terminate the frame
                        // The next byte should be null, handled in the next iteration
                    }
                }
                break;

            default:
                // Should not reach here
                break;
        }

        // If Content-Length is set and body is fully received, expect a null byte next
        if (state == State.BODY && bodyLength != -1 && bodyReceived == bodyLength) {
            // The next byte should be a null byte to terminate the frame
            // However, depending on the protocol, the null byte might be included or not
            // Here, we'll handle it in the next iteration
        }

        // After reading body bytes, check if the next byte is null to complete the frame
        if (state == State.BODY && bodyLength != -1 && bodyReceived == bodyLength) {
            // Next byte should be null byte to terminate
            // We will handle it in the next iteration
        }

        // If Content-Length is set and body is fully received, and if the frame expects a null byte,
        // then the frame is complete only after receiving the null byte
        // However, to keep things simple, we'll consider the frame complete after reading the body

        // For the purpose of this implementation, assume that the frame ends after body if Content-Length is set
        if (state == State.BODY && bodyLength != -1 && bodyReceived == bodyLength) {
            frame.setBody(new String(bodyBytes, 0, bodyReceived, StandardCharsets.UTF_8));
            state = State.END;
            return frame;
        }

        return null; // Frame not yet complete
    }

    /**
     * Encodes the given StompFrame into a byte array for transmission.
     *
     * @param message the StompFrame to encode
     * @return the encoded byte array
     */
    @Override
    public byte[] encode(StompFrame message) {
        StringBuilder sb = new StringBuilder();

        // Determine the command based on the frame's class name
        String command = message.getClass().getSimpleName().replace("Frame", "").toUpperCase();
        sb.append(command).append("\n");

        // Append headers
        for (Map.Entry<String, String> header : message.getHeaders().entrySet()) {
            sb.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }

        sb.append("\n"); // Empty line to indicate end of headers

        // Append body if present
        if (message.getBody() != null) {
            sb.append(message.getBody());
        }

        sb.append('\u0000'); // Null byte to terminate the frame

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Resets the decoder's state to be ready for the next frame.
     */
    private void reset() {
        state = State.COMMAND;
        frame = null;
        current.setLength(0);
        currentHeaderKey = null;
        bodyBytes = null;
        bodyLength = -1;
        bodyReceived = 0;
    }
}
