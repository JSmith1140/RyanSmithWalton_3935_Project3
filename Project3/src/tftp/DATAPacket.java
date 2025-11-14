package tftp;

import java.nio.ByteBuffer;

public class DATAPacket extends TFTPPacket {
    private short blockNumber;
    private byte[] data;

    /**
     * Constructs a new DATAPacket with the given block number and data payload.
     *
     * @param blockNumber the block number of this DATA packet
     * @param data        the payload data for this block (up to 512 bytes)
     */
    public DATAPacket(short blockNumber, byte[] data) {
        super(OP_DATA);
        this.blockNumber = blockNumber;
        this.data = data;
    }

    
    /**
     * Constructs a DATAPacket by parsing a serialized TFTP DATA packet.
     * <p>
     * The provided byte array must contain at least 4 bytes:
     * <pre>
     *     2 bytes: opcode (3)
     *     2 bytes: block number
     *     N bytes: data payload
     * </pre>
     *
     * @param data the raw packet data received over the network
     */
    public DATAPacket(byte[] data) {
        super(OP_DATA);
        ByteBuffer buf = ByteBuffer.wrap(data);
        this.opcode = buf.getShort();      
        this.blockNumber = buf.getShort();  
        this.data = new byte[data.length - 4];
        buf.get(this.data);
    }

    /**
     * Serializes this DATA packet into a byte array suitable for network transmission.
     * <p>
     * The layout is:
     * <pre>
     *     2 bytes: opcode (3)
     *     2 bytes: block number
     *     N bytes: data payload
     * </pre>
     *
     * @return a byte array containing the serialized packet
     */
    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(4 + data.length);
        buf.putShort(opcode);
        buf.putShort(blockNumber);
        buf.put(data);
        return buf.array();
    }

    /**
     * Returns the block number of this DATA packet.
     *
     * @return the block number
     */
    public short getBlockNumber() {
        return blockNumber;
    }

    /**
     * Returns the data payload carried by this DATA packet.
     *
     * @return the data bytes for this block
     */
    public byte[] getData() {
        return data;
    }
}
