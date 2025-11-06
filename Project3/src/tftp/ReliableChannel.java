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
    public static class ProtocolException extends Exception {
        public ProtocolException(String msg) { super(msg); }
    }
}
