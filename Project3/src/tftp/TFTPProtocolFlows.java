package tftp;

import java.io.*;


public class TFTPProtocolFlows {

    private static final int BLOCK_SIZE = 512;

    private final ReliableChannel channel;
    private final boolean isServer;

    public TFTPProtocolFlows(ReliableChannel channel, boolean isServer) {
        this.channel = channel;
        this.isServer = isServer;
    }


    public void receiveFile(String filename, FileOutputStream output)
            throws IOException, TFTPErrorException {

        short expectedBlock = 1;

        try {
            while (true) {
                DATAPacket data = channel.receive(DATAPacket.class);

    
                if (data.getBlockNumber() == expectedBlock) {
                    output.write(data.getData());
                    output.flush();

   
                    ACKPacket ack = new ACKPacket(expectedBlock);
                    channel.send(ack);

                    expectedBlock++;


                    if (data.getData().length < BLOCK_SIZE) {
                        break;
                    }
                }
 
                else if (data.getBlockNumber() < expectedBlock) {
                    ACKPacket ack = new ACKPacket(data.getBlockNumber());
                    channel.send(ack);
                }
                else {
                    throw new TFTPErrorException("Out-of-order DATA block received.");
                }
            }
        } catch (ReliableChannel.ProtocolException e) {
            throw new TFTPErrorException("Protocol error while receiving file: " + e.getMessage());
        } finally {
            output.close();
        }
    }


    public void sendFile(String filename, FileInputStream input)
            throws IOException, TFTPErrorException {

        short blockNumber = 1;
        byte[] buffer = new byte[BLOCK_SIZE];
        int bytesRead;

        try {
            while (true) {
                bytesRead = input.read(buffer);
                if (bytesRead == -1) bytesRead = 0;

                byte[] dataToSend = new byte[bytesRead];
                System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);

                DATAPacket dataPacket = new DATAPacket(blockNumber, dataToSend);
                channel.send(dataPacket);

                ACKPacket ack = channel.receive(ACKPacket.class);

                if (ack.getBlockNumber() != blockNumber) {
                    throw new TFTPErrorException("ACK block number mismatch.");
                }


                if (bytesRead < BLOCK_SIZE) {
                    break;
                }

                blockNumber++;
            }
        } catch (ReliableChannel.ProtocolException e) {
            throw new TFTPErrorException("Protocol error while sending file: " + e.getMessage());
        } finally {
            input.close();
        }
    }
}
