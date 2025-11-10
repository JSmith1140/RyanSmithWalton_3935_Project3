package tftp;
import java.io.*;

/**
 * Implements the two main TFTP data subprotocol flows:
 *  - receiveFile(): for handling RRQ (receiving a file)
 *  - sendFile(): for handling WRQ (sending a file)
 *
 * Built on top of the ReliableChannel abstraction.
 */
public class TFTPProtocolFlows {

    private static final int BLOCK_SIZE = 512;

    private final ReliableChannel channel;
    private final boolean isServer;

    public TFTPProtocolFlows(ReliableChannel channel, boolean isServer) {
        this.channel = channel;
        this.isServer = isServer;
    }

    /**
     * Receives a file (used when processing a Read Request).
     * The remote side sends DATA packets; this side sends ACKs.
     */
    public void receiveFile(String filename, FileOutputStream output)
            throws IOException, TFTPErrorException {

        short expectedBlock = 1;

        try {
            while (true) {
                DATAPacket data = channel.receive(DATAPacket.class);

                // Correct block
                if (data.getBlockNumber() == expectedBlock) {
                    output.write(data.getData());
                    output.flush();

                    // Send ACK
                    ACKPacket ack = new ACKPacket(expectedBlock);
                    channel.send(ack);

                    expectedBlock++;

                    // End of transfer (last block < 512 bytes)
                    if (data.getData().length < BLOCK_SIZE) {
                        break;
                    }
                }
                // Duplicate block (ACK again)
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

    /**
     * Sends a file (used when processing a Write Request).
     * This side sends DATA packets; the remote side sends ACKs.
     */
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

                // Wait for ACK
                ACKPacket ack = channel.receive(ACKPacket.class);

                if (ack.getBlockNumber() != blockNumber) {
                    throw new TFTPErrorException("ACK block number mismatch.");
                }

                // End of file (short block)
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
