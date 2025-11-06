import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class WRQPacket extends TFTPPacket {
    private String filename;
    private String mode = "octet";

    public WRQPacket(String filename) {
        super(OP_WRQ);
        this.filename = filename;
    }

    public WRQPacket(byte[] data) {
        super(OP_WRQ);
        this.opcode = ByteBuffer.wrap(data, 0, 2).getShort();
        int start = 2;
        int end = findZero(data, start);
        this.filename = new String(data, start, end - start);
    }

    @Override
    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(opcode);
        out.writeBytes(buf.array());
        out.writeBytes(filename.getBytes());
        out.write(0);
        out.writeBytes(mode.getBytes());
        out.write(0);
        return out.toByteArray();
    }

    private int findZero(byte[] data, int start) {
        for (int i = start; i < data.length; i++)
            if (data[i] == 0) return i;
        return -1;
    }
}
