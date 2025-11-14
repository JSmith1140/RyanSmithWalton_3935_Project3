package tftp;

import java.io.*;

//Handles the TFTP send and recieve flows on top of a ReliableChannel
public class TFTPProtocolFlows {
//TFTP data block size in bytes
    private static final int BLOCK_SIZE = 512;
//Reliable channel used for send and recieve packets
    private final ReliableChannel channel;
//True - This is on the server side
//False - This is on the client side
    private final boolean isServer;

//Constructor that initializes the protocol flows with a reliable channel
    public TFTPProtocolFlows(ReliableChannel channel, boolean isServer) {
        this.channel = channel;
        this.isServer = isServer;
    }




/*
* Receives a file over TFTP
* Called by the client for a get request
* Called by the server for a put request
*/
    public void receiveFile(String filename, FileOutputStream output)
            throws IOException, TFTPErrorException {
//Next block number expected from sender
        short expectedBlock = 1;

        try {
            while (true) {
//Wait for the next DATA packet
                DATAPacket data = channel.receive(DATAPacket.class);

//Case 1: Correct block recieved
                if (data.getBlockNumber() == expectedBlock) {
//Write the recieved data to the output file
                    output.write(data.getData());
                    output.flush();

//Send ACK for this block
                    ACKPacket ack = new ACKPacket(expectedBlock);
                    channel.send(ack);
//Increment expected block number
                    expectedBlock++;

//If this data packet is less than BLOCK_SIZE, its the last block
                    if (data.getData().length < BLOCK_SIZE) {
                        break;
                    }
                }
 
//Case 2: Duplicate block recieved
//Resend ACK for the last received block
                else if (data.getBlockNumber() < expectedBlock) {
//Resend ACK for this block
                    ACKPacket ack = new ACKPacket(data.getBlockNumber());
                    channel.send(ack);
                }
//Case 3: Out-of-order block recieved
                else {
//Something is wrong with ordering or packet loss
                    throw new TFTPErrorException("Out-of-order DATA block received.");
                }
            }
        } catch (ReliableChannel.ProtocolException e) {
//Wrap any protocol exceptions as TFTP errors
            throw new TFTPErrorException("Protocol error while receiving file: " + e.getMessage());
        } finally {
//Close the output file
            output.close();
        }
    }



/*
 * Sends a file over TFTP
 * Called by the client for a put request
 * Called by the server for a get request
*/
    public void sendFile(String filename, FileInputStream input)
            throws IOException, TFTPErrorException {
//first DATA block number is 1
        short blockNumber = 1;
//Scratch buffer for reading file data
        byte[] buffer = new byte[BLOCK_SIZE];
        int bytesRead;

        try {
            while (true) {
//Read up to BLOCK_SIZE bytes from the input file
                bytesRead = input.read(buffer);
//If no more data, treat as zero-length final block
                if (bytesRead == -1) bytesRead = 0;
//Copy only the bytes read into a new array
                byte[] dataToSend = new byte[bytesRead];
                System.arraycopy(buffer, 0, dataToSend, 0, bytesRead);
//Build a DATAPacket with the data
                DATAPacket dataPacket = new DATAPacket(blockNumber, dataToSend);
//Send the DATA packet
                channel.send(dataPacket);
//Wait for ACK for this block
                ACKPacket ack = channel.receive(ACKPacket.class);
//If ACK block number does not match, throw error
                if (ack.getBlockNumber() != blockNumber) {
                    throw new TFTPErrorException("ACK block number mismatch.");
                }

//If we sent less than BLOCK_SIZE bytes, this was the last block
                if (bytesRead < BLOCK_SIZE) {
                    break;
                }
//Otherwise, move to the next block number
                blockNumber++;
            }
        } catch (ReliableChannel.ProtocolException e) {
//Wrap channel protocol exceptions as TFTP errors
            throw new TFTPErrorException("Protocol error while sending file: " + e.getMessage());
        } finally {
//Close the input file
            input.close();
        }
    }
}
