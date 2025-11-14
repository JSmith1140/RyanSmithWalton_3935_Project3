package tftp;// package for this file

import java.io.IOException;// import statments
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class ReliableChannel {
    /*
     * max size of a tftp datagram 4 byte header and 512 byte data and small amount
     * of wiggle room
     */

    private static final int MAX_TFTP_DATAGRAM = 4 + 512 + 4;

    /* default timeout is in milliseconfs for quick response for if nothing is set */
    private static final int DEFAULT_TIMEOUT_MS = 2000;
    /*udp socket we are sending and recieving on */
    private final DatagramSocket socket;
    /* when we lock onto peer all the packes have to come from here */
    private SocketAddress pinnedPeer;

    /*constuctor just save socket and makes sure of it not being null */
    public ReliableChannel(DatagramSocket socket) {
        this.socket = Objects.requireNonNull(socket, "socket");
    }
    /*let other code diles seee who we pinned to */
    public SocketAddress getPinnedPeer() {
        return pinnedPeer;
    }
    /*actually sets the pinned peer */
    public void setPinnedPeer(SocketAddress peer) {
        this.pinnedPeer = peer;
    }
    /*sends tftp packet to speffic peer(only used if we know the address ) */
    public void send(TFTPPacket pkt, SocketAddress peer) throws IOException {
        byte[] data = pkt.serialize();
        DatagramPacket dp = new DatagramPacket(data, data.length, toInetSocketAddress(peer));
        socket.send(dp);
    }
    /* send a tftp packet to peer(used once the peeer locked in) */
    public void send(TFTPPacket pkt) throws IOException {
        if (pinnedPeer == null) {
            throw new IllegalStateException("Pinned peer is not set yet.");
        }
        send(pkt, pinnedPeer);
    }
    /*receive a packet of a certain type with a timeout also pins peer on first pckt and rejects others packets */
    public <T extends TFTPPacket> T receive(Class<T> expected, int timeoutMs)
            throws IOException, ProtocolException {

        int previous = socket.getSoTimeout();
        if (timeoutMs <= 0) timeoutMs = DEFAULT_TIMEOUT_MS;
        socket.setSoTimeout(timeoutMs);

        try {
            for (;;) {
                /*buffer for incoming udp packet */
                byte[] buf = new byte[MAX_TFTP_DATAGRAM];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);

                SocketAddress source = dp.getSocketAddress();

                /*first packet sets pinnedPeer after er only talk to that one 
                 * if another address sends packet error is sent
                */

                if (pinnedPeer == null) {
                    pinnedPeer = source; 
                } else if (!pinnedPeer.equals(source)) {
                    sendUnknownTIDError(dp); 
                    continue;
                }
                /*turns raw bytes into the right tftp packet type */

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
            /*just bubble the timeout up so caller handles it */
            throw e;
        } finally {
            /*put thr olf timeout back */
            try { socket.setSoTimeout(previous); } catch (Exception ignore) {}
        }
    }
    /*recieve with the default timeout */
    public <T extends TFTPPacket> T receive(Class<T> expected) throws IOException, ProtocolException {
        return receive(expected, DEFAULT_TIMEOUT_MS);
    }
    /*close the underlying udp socket */
    public void close() {
        socket.close();
    }
    /*ensures address we got is intetsocketAddress, otherwisw dail */
    private InetSocketAddress toInetSocketAddress(SocketAddress sa) {
        if (sa instanceof InetSocketAddress) return (InetSocketAddress) sa;
        throw new IllegalArgumentException("Unsupported SocketAddress type: " + sa);
    }

    /*look at the opcode and build the right tftp pckt from bytes */
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

    /*if a packet comes from wrong udp port sends it back unknown TIF error this follows 
     * the tftp spec so other side knows its wrong
     */
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
            /*if sending the error fails we ignore it */
        
        }
    }

    /*simple checked exeption for protocol problems */
    public static class ProtocolException extends Exception {
        public ProtocolException(String msg) { super(msg); }
    }
}// end of file
