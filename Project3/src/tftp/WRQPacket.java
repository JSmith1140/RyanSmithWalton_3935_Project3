package tftp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class WRQPacket extends TFTPPacket {
    private String filename;
    private String mode = "octet";

    /**
     * Constructs a new WRQPacket requesting permission to write a file.
     *
     * @param filename the name of the file being uploaded to the server
     */
    public WRQPacket(String filename) {
        super(OP_WRQ);
        this.filename = filename;
    }

    /**
     * Constructs a WRQPacket by parsing raw serialized packet data.
     * <p>
     * Expected format:
     * <pre>
     *     2 bytes: opcode (2)
     *     string : filename (null-terminated)
     *     string : mode (null-terminated)
     * </pre>
     *
     * @param data the raw WRQ packet bytes received from the network
     */
    public WRQPacket(byte[] data) {
        super(OP_WRQ);
        this.opcode = ByteBuffer.wrap(data, 0, 2).getShort();
        int start = 2;
        int end = findZero(data, start);
        this.filename = new String(data, start, end - start);
    }

    /**
     * Serializes this WRQ packet into a TFTP-compliant byte array.
     * <p>
     * Format:
     * <pre>
     *     2 bytes: opcode (2)
     *     bytes : filename
     *     1 byte: 0 terminator
     *     bytes : mode (usually "octet")
     *     1 byte: 0 terminator
     * </pre>
     *
     * @return the encoded WRQ packet as a byte array
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
     * Finds the index of the next zero (null terminator) byte in the data array.
     *
     * @param data  the byte array to scan
     * @param start the index at which to begin scanning
     * @return the index of the next zero byte, or -1 if no terminator is found
     */
    private int findZero(byte[] data, int start) {
        for (int i = start; i < data.length; i++)
            if (data[i] == 0) return i;
        return -1;
    }
}
