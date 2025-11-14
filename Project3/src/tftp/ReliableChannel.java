package tftp;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class ReliableChannel {

    private static final int MAX_TFTP_DATAGRAM = 4 + 512 + 4;

    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final DatagramSocket socket;
    private SocketAddress pinnedPeer;

    public ReliableChannel(DatagramSocket socket) {
        this.socket = Objects.requireNonNull(socket, "socket");
    }

    public SocketAddress getPinnedPeer() {
        return pinnedPeer;
    }

    public void setPinnedPeer(SocketAddress peer) {
        this.pinnedPeer = peer;
    }

    public void send(TFTPPacket pkt, SocketAddress peer) throws IOException {
        byte[] data = pkt.serialize();
        DatagramPacket dp = new DatagramPacket(data, data.length, toInetSocketAddress(peer));
        socket.send(dp);
    }

    public void send(TFTPPacket pkt) throws IOException {
        if (pinnedPeer == null) {
            throw new IllegalStateException("Pinned peer is not set yet.");
        }
        send(pkt, pinnedPeer);
    }

    public <T extends TFTPPacket> T receive(Class<T> expected, int timeoutMs)
            throws IOException, ProtocolException {

        int previous = socket.getSoTimeout();
        if (timeoutMs <= 0) timeoutMs = DEFAULT_TIMEOUT_MS;
        socket.setSoTimeout(timeoutMs);

        try {
            for (;;) {
                byte[] buf = new byte[MAX_TFTP_DATAGRAM];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);

                SocketAddress source = dp.getSocketAddress();

                if (pinnedPeer == null) {
                    pinnedPeer = source; 
                } else if (!pinnedPeer.equals(source)) {
                    sendUnknownTIDError(dp); 
                    continue;
                }

                TFTPPacket packet = parse(dp.getData(), dp.getLength());
                if (!expected.isInstance(packet)) {
                    throw new ProtocolException(
                        "Unexpected packet type. Expected " + expected.getSimpleName()
                        + " but got " + packet.getClass().getSimpleName());
                }

                @SuppressWarnings("unchecked")
                T typed = (T) packet;
                return typed;
            }
        } catch (SocketTimeoutException e) {
            throw e;
        } finally {
            try { socket.setSoTimeout(previous); } catch (Exception ignore) {}
        }
    }

    public <T extends TFTPPacket> T receive(Class<T> expected) throws IOException, ProtocolException {
        return receive(expected, DEFAULT_TIMEOUT_MS);
    }

    public void close() {
        socket.close();
    }

    private InetSocketAddress toInetSocketAddress(SocketAddress sa) {
        if (sa instanceof InetSocketAddress) return (InetSocketAddress) sa;
        throw new IllegalArgumentException("Unsupported SocketAddress type: " + sa);
    }

    private TFTPPacket parse(byte[] data, int length) throws ProtocolException {
        if (length < 2) {
            throw new ProtocolException("Datagram too short for opcode.");
        }
        short opcode = ByteBuffer.wrap(data, 0, 2).getShort();
        byte[] exact = Arrays.copyOf(data, length);

        switch (opcode) {
            case TFTPPacket.OP_RRQ:   return new RRQPacket(exact);
            case TFTPPacket.OP_WRQ:   return new WRQPacket(exact);
            case TFTPPacket.OP_DATA:  return new DATAPacket(exact);
            case TFTPPacket.OP_ACK:   return new ACKPacket(exact);
            case TFTPPacket.OP_ERROR: return new ERRORPacket(exact);
            default:
                throw new ProtocolException("Illegal TFTP operation. Unknown opcode: " + opcode);
        }
    }

    private void sendUnknownTIDError(DatagramPacket stray) {
        try {
      
            byte[] msg = "Unknown transfer ID.".getBytes();
            ByteBuffer buf = ByteBuffer.allocate(4 + msg.length + 1);
            buf.putShort((short) 5); 
            buf.putShort((short) 5);  
            buf.put(msg);
            buf.put((byte) 0);
            byte[] payload = buf.array();

            DatagramPacket dp = new DatagramPacket(
                payload,
                payload.length,
                stray.getAddress(),
                stray.getPort()
            );
            socket.send(dp);
        } catch (Exception ignore) {
        
        }
    }


    public static class ProtocolException extends Exception {
        public ProtocolException(String msg) { super(msg); }
    }
}

