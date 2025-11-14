package server;

import merrimackutil.net.Log;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import tftp.ReliableChannel;
import tftp.TFTPProtocolFlows;
import tftp.ACKPacket;
import tftp.TFTPErrorException;

public class TftpRequestHandler implements Runnable {

    private final DatagramPacket request;
    private final Path dataDir;
    private final Log lg;

    public TftpRequestHandler(DatagramPacket request, Path dataDir, Log lg) {
        this.request = request;
        this.dataDir = dataDir;
        this.lg = lg;
    }

    @Override
    public void run() {
        InetAddress clientAddr = request.getAddress();
        int clientPort = request.getPort();
        byte[] data = request.getData();

        short opcode = (short)(((data[0] & 0xFF) << 8) | (data[1] & 0xFF));
        String filename = extractFilename(data);
        Path filePath = dataDir.resolve(filename).normalize();

        System.out.println("[Handler] New request from " + clientAddr + ":" + clientPort +
                           " opcode=" + opcode + " file=" + filename);

        try (DatagramSocket dataSocket = new DatagramSocket()) {

            // Stop ../../ style paths
            if (!filePath.startsWith(dataDir)) {
                throw new server.TftpErrorException("Access violation");
            }

            SocketAddress peer = new InetSocketAddress(clientAddr, clientPort);
            ReliableChannel channel = new ReliableChannel(dataSocket);
            channel.setPinnedPeer(peer);

            TFTPProtocolFlows flows = new TFTPProtocolFlows(channel, /*isServer=*/true);

            if (opcode == 1) {
                // RRQ – DOWNLOAD
                lg.log("RRQ from " + clientAddr + ":" + clientPort + " -> " + filename);

                if (!Files.exists(filePath)) {
                    lg.log("(TFTP Error) File " + filePath.getFileName() + " not found");
                    throw new server.TftpErrorException("File not found");
                }

                try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                    flows.sendFile(filename, fis);
                }

                lg.log("transmitted file " + filePath.getFileName()
                       + " to " + clientAddr.getHostName());

            } else if (opcode == 2) {
                // WRQ – UPLOAD
                lg.log("WRQ from " + clientAddr + ":" + clientPort + " -> " + filename);

                if (Files.exists(filePath)) {
                    lg.log("(TFTP Error) File " + filePath.getFileName() + " already exists");
                    throw new server.TftpErrorException("File already exists");
                }

                // Send ACK(0) from this new TID
                System.out.println("[Handler] Sending ACK(0) from port " +
                                   dataSocket.getLocalPort());
                ACKPacket ack0 = new ACKPacket((short)0);
                channel.send(ack0);

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    flows.receiveFile(filename, fos);
                }

                lg.log("received file " + filePath.getFileName()
                       + " from " + clientAddr.getHostName());
            } else {
                lg.log("Invalid opcode received: " + opcode);
            }

            channel.close();

        } catch (server.TftpErrorException e) {
            lg.log("TFTP Error: " + e.getMessage());
        } catch (TFTPErrorException e) {
            lg.log("TFTP protocol Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (IOException e) {
            lg.log("IO Error: " + e.getMessage());
            e.printStackTrace(System.err);
        } catch (Exception e) {
            lg.log("Unexpected Handler error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private String extractFilename(byte[] data) {
        int i = 2;
        StringBuilder sb = new StringBuilder();
        while (i < data.length && data[i] != 0) {
            sb.append((char) data[i++]);
        }
        return sb.toString();
    }
}
