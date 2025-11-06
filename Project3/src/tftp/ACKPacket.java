import java.nio.ByteBuffer;

public class ACKPacket extends TFTPPacket {
    private short blockNumber;

    public ACKPacket(short blockNumber){
        super(OP_ACK);
        this.blockNumber = blockNumber;
    }

    public ACKPacket(byte[] data) {
        super(OP_ACK);
        this.opcode = ByteBuffer.wrap(data, 0, 2).getShort();
        this.blockNumber = ByteBuffer.wrap(data, 2, 2).getShort();
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putShort(opcode);
        buf.putShort(blockNumber);
        return buf.array();
    }
}
