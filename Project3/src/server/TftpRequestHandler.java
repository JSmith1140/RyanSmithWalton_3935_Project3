package server;

import merrimackutil.net.Log;


import java.io.*;
import java.net.*;
import java.nio.file.*;


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
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress clientAddr = request.getAddress();
            int clientPort = request.getPort();
            byte[] data = request.getData();

            short opcode = (short)(((data[0] & 0xFF) << 8) | (data[1] & 0xFF));

            String filename = extractFilename(data);
            Path filePath = dataDir.resolve(filename).normalize();

            if (!filePath.startsWith(dataDir)) {
                throw new TftpErrorException("Access violation");
            }

            if (opcode == 1) {
                lg.log("RRQ from " + clientAddr + ":" + clientPort + " -> " + filename);
                sendFile(socket, filePath, clientAddr, clientPort);
            } else if (opcode == 2) {
                lg.log("WRQ from " + clientAddr + ":" + clientPort + " -> " + filename);
                receiveFile(socket, filePath, clientAddr, clientPort);
            } else {
                lg.log("Invalid opcode received: " + opcode);
            }

        } catch (TftpErrorException e) {
            lg.log("TFTP Error: " + e.getMessage());

        } catch (IOException e) {
            lg.log("IO Error: " + e.getMessage());
        } catch (Exception e) {
            lg.log("Unexpected Handler error: " + e.getMessage());
        }
    }

    private void sendFile(DatagramSocket socket, Path file, InetAddress addr, int port) throws IOException, TftpErrorException {
        if (!Files.exists(file)) {
            throw new TftpErrorException("File not found");
        }

        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[512];
            short blockNum = 1;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(0);
                baos.write(3);
                baos.write((blockNum >> 8) & 0xFF);
                baos.write(blockNum & 0xFF);
                baos.write(buffer, 0, bytesRead);

                DatagramPacket dataPacket = new DatagramPacket(baos.toByteArray(), baos.size(), addr, port);
                socket.send(dataPacket);

                if (bytesRead < 512) break;
                blockNum++;
            }
        }
    }

    private void receiveFile(DatagramSocket socket, Path file, InetAddress addr, int port) throws IOException, TftpErrorException {

        try (OutputStream out = Files.newOutputStream(file)) {
            short blockNum = 0;

            while (true) {
                blockNum++;
  
                byte[] ack = {0, 4, (byte)(blockNum >> 8), (byte)(blockNum & 0xFF)};
                socket.send(new DatagramPacket(ack, ack.length, addr, port));

                byte[] buffer = new byte[516];
                DatagramPacket dataPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(dataPacket);

                int len = dataPacket.getLength();
                if (len < 4) break;

                int dataLen = len - 4;
                out.write(buffer, 4, dataLen);

                if (dataLen < 512) break;
            }
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
