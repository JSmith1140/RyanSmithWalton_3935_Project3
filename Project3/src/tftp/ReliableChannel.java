
package tftp;

import java.io.*;
import java.net.*;
import tftp.TftpCore;  

public class ReliableChannel {
    private final DatagramSocket socket;
    private final boolean isServer;

    public ReliableChannel(DatagramSocket socket, boolean isServer) {
        this.socket = socket;
        this.isServer = isServer;
    }

    public void send(TftpCore.Packet pkt, SocketAddress peer) throws IOException {
        byte[] data = pkt.serialize();
        DatagramPacket dp = new DatagramPacket(data, data.length, peer);
        socket.send(dp);
    }

    public <T extends TftpCore.Packet> T receive(Class<T> expected, int timeoutMs)
            throws IOException, TftpCore.TftpException {
        socket.setSoTimeout(timeoutMs);
        int retries = 0;
        while (true) {
            try {
                byte[] buf = new byte[516]; 
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                socket.receive(dp);
                TftpCore.Packet pkt = TftpCore.Packet.parse(dp);
                if (expected.isInstance(pkt))
                    return expected.cast(pkt);
            } catch (SocketTimeoutException e) {
                if (++retries >= 3)
                    throw new TftpCore.TftpErrorException("Timeout: giving up after 3 retries.");
            }
        }
    }
}

