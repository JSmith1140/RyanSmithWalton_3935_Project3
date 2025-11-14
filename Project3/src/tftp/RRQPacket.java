package tftp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class RRQPacket extends TFTPPacket {
    private String filename;
    private String mode = "octet";

    /**
     * Constructs a new RRQPacket requesting the specified filename.
     *
     * @param filename the name of the file being requested
     */
    public RRQPacket(String filename) {
        super(OP_RRQ);
        this.filename = filename;
    }

    /**
     * Constructs an RRQPacket by parsing a serialized RRQ packet.
     * <p>
     * Expected format:
     * <pre>
     *     2 bytes: opcode (1)
     *     string : filename (null-terminated)
     *     string : mode (null-terminated)
     * </pre>
     *
     * @param data the raw byte data of the incoming RRQ packet
     */
    public RRQPacket(byte[] data) {
        super(OP_RRQ);
        this.opcode = ByteBuffer.wrap(data, 0, 2).getShort();
        int start = 2;
        int end = findZero(data, start);
        this.filename = new String(data, start, end - start);
    }

    /**
     * Serializes this RRQ packet into the correct byte format for TFTP.
     * <p>
     * Format:
     * <pre>
     *     2 bytes: opcode (1)
     *     bytes : filename
     *     1 byte: 0 terminator
     *     bytes : mode (usually "octet")
     *     1 byte: 0 terminator
     * </pre>
     *
     * @return the byte array representation of this RRQ packet
     */
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

    /**
     * Finds the position of the next zero byte in the given array, starting at {@code start}.
     *
     * @param data  the array to scan
     * @param start the index at which scanning begins
     * @return the index of the next zero byte, or -1 if none exists
     */
    private int findZero(byte[] data, int start) {
        for (int i = start; i < data.length; i++)
            if (data[i] == 0) return i;
        return -1;
    }
}
