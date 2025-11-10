package tftp;
import java.nio.ByteBuffer;

public class DATAPacket extends TFTPPacket {
    private short blockNumber;
    private byte[] data;

    public DATAPacket(short blockNumber, byte[] data) {
        super(OP_DATA);
        this.blockNumber = blockNumber;
        this.data = data;
    }

    public DATAPacket(byte[] data) {
        super(OP_DATA);
        ByteBuffer buf = ByteBuffer.wrap(data);
        this.opcode = buf.getShort();      
        this.blockNumber = buf.getShort();  
        this.data = new byte[data.length - 4];
        buf.get(this.data);
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buf = ByteBuffer.allocate(4 + data.length);
        buf.putShort(opcode);
        buf.putShort(blockNumber);
        buf.put(data);
        return buf.array();
    }

public short getBlockNumber() {
    return blockNumber;
}

public byte[] getData() {
    return data;
}



}

