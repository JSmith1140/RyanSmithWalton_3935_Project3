import java.nio.ByteBuffer;

public class ACKPacket extends TFTPPacket {
    private short blockNumber;

    public ACKPacket(short blockNumber) {
        super(OP_ACK);
        this.blockNumber = blockNumber;
    }

    public ACKPacket(byte[] data) {
        super(OP_ACK);
        ByteBuffer buf = ByteBuffer.wrap(data);
        this.opcode = buf.getShort();
        this.blockNumber = buf.getShort();
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putShort(opcode);
        buf.putShort(blockNumber);
        return buf.array();
    }

    public short getBlockNumber() {
        return blockNumber;
    }
}
