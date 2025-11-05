package tftp.protocol;

import java.io.*;
import java.net.*;
import tftp.channel.*;
import tftp.packets.*;


public class TFTPProtocolFlows {

    private static final int BLOCK_SIZE = 512;

    private ReliableChannel channel;
    private boolean isServer;

    public TFTPProtocolFlows(ReliableChannel channel, boolean isServer) {
        this.channel = channel;
        this.isServer = isServer;
    }

    public void receiveFile(String filename, FileOutputStream output) throws IOException, TFTPErrorException {
        int expectedBlock = 1;

        while (true) {
            TFTPPacket packet = channel.receive();

            if (packet instanceof ErrorPacket) {
                throw new TFTPErrorException(((ErrorPacket) packet).getErrorMessage());
            }

            if (!(packet instanceof DataPacket)) {
                throw new TFTPErrorException("Unexpected packet type during file receive.");
            }

            DataPacket data = (DataPacket) packet;


            if (data.getBlockNumber() != expectedBlock) {
                channel.send(new AckPacket((short) (expectedBlock - 1)));
                continue;
            }

            byte[] dataBytes = data.getData();
            output.write(dataBytes);


            channel.send(new AckPacket((short) expectedBlock));

            if (dataBytes.length < BLOCK_SIZE) {

                break;
            }

            expectedBlock++;
        }
    }


    public void sendFile(String filename, FileInputStream input) throws IOException, TFTPErrorException {
        int blockNumber = 1;
        byte[] buffer = new byte[BLOCK_SIZE];
        int bytesRead;

        while ((bytesRead = input.read(buffer)) != -1) {
            byte[] dataToSend = (bytesRead == BLOCK_SIZE) ? buffer : java.util.Arrays.copyOf(buffer, bytesRead);
            DataPacket dataPacket = new DataPacket((short) blockNumber, dataToSend);

            channel.send(dataPacket);

            TFTPPacket response = channel.receive();

            if (response instanceof ErrorPacket) {
                throw new TFTPErrorException(((ErrorPacket) response).getErrorMessage());
            }

            if (!(response instanceof AckPacket)) {
                throw new TFTPErrorException("Expected ACK, received unexpected packet.");
            }

            AckPacket ack = (AckPacket) response;
            if (ack.getBlockNumber() != blockNumber) {
                throw new TFTPErrorException("ACK block mismatch.");
            }

            if (bytesRead < BLOCK_SIZE) {
                break;
            }

            blockNumber++;
        }
    }
}
