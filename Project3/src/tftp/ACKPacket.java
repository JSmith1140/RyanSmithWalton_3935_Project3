 package tftp;

 import java.nio.ByteBuffer;

/*
 * Represents a TFTP ACK packet 
 * 
 * ACK packets confirm successful receipt of DATA blocks
 * during file transfers.
 * 
 * Format:
 * 2 bytes - Opcode (4 for ACK)
 * 2 bytes - Block number being acknowledged
 * 
 */


public class ACKPacket extends TFTPPacket {
//Block number being acknowledged
    private short blockNumber;
//Constructor used when sending an ACK
    public ACKPacket(short blockNumber) {
        super(OP_ACK);
        this.blockNumber = blockNumber;
    }
//Constructor used to parse an incoming ACK packet
//Extracts block number from the packet
    public ACKPacket(byte[] data) {
        super(OP_ACK);
        ByteBuffer buf = ByteBuffer.wrap(data);
//Should equal OP_ACK
        this.opcode = buf.getShort();
//Block number acknowledged
        this.blockNumber = buf.getShort();
    }


//Convert this ACK packet to a byte array for sending
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putShort(opcode);
        buf.putShort(blockNumber);
        return buf.array();
    }
//Returns the block number being acknowledged
    public short getBlockNumber() {
        return blockNumber;
    }
}
