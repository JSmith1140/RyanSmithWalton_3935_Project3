package tftp;

import java.nio.ByteBuffer;

public class ERRORPacket extends TFTPPacket{
    private short errorCode;
    private String errorMsg;

    public static final String[] DEFAULT_MESSAGES = {
        "Not defined, see error message (if any).",          
        "File not found.",         
        "Access violation.",      
        "Disk full or allocation exceeded.",
        "Illegal TFTP operation.", 
        "Unknown transfer ID.",    
        "File already exists.",   
        "No such user."           
    };

    /**
     * Constructs an ERRORPacket using the given error code and message.
     * <p>
     * If {@code errorMsg} is null, a default TFTP message will be used when possible.
     *
     * @param errorCode the TFTP error code that describes the failure
     * @param errorMsg  the human-readable error message, or {@code null} to use the default
     */
    public ERRORPacket(short errorCode, String errorMsg) {
        super(OP_ERROR);
        this.errorCode = errorCode;
        if (errorMsg != null) {
            this.errorMsg = errorMsg;
        } else if (errorCode >= 0 && errorCode < DEFAULT_MESSAGES.length) {
            this.errorMsg = DEFAULT_MESSAGES[errorCode];
        } else {
            this.errorMsg = "Unknown error";
        }
    }

    /**
     * Constructs an ERRORPacket by parsing serialized TFTP error packet data.
     * <p>
     * Expected format:
     * <pre>
     *     2 bytes: opcode (5)
     *     2 bytes: error code
     *     N bytes: error message (null-terminated)
     * </pre>
     *
     * @param data the raw packet bytes received from the network
     */
    public ERRORPacket(byte[] data) {
        super(OP_ERROR);
        ByteBuffer buf = ByteBuffer.wrap(data);

        this.opcode = buf.getShort();

        this.errorCode = buf.getShort();

        int msgLength = data.length - 4;
        if (msgLength > 0) {
            this.errorMsg = new String(data, 4, msgLength - 1); 
        } else {
            this.errorMsg = "";
        }
    }

    /**
     * Serializes this ERROR packet to a byte array suitable for transmission.
     * <p>
     * Layout:
     * <pre>
     *     2 bytes: opcode (5)
     *     2 bytes: error code
     *     N bytes: error message
     *     1 byte : null terminator
     * </pre>
     *
     * @return a byte array representing the encoded ERROR packet
     */
    @Override
    public byte[] serialize() {
        byte[] msgBytes = errorMsg.getBytes();

        ByteBuffer buf = ByteBuffer.allocate(4 + msgBytes.length + 1);
        buf.putShort(opcode);
        buf.putShort(errorCode);    
        buf.put(msgBytes);          
        buf.put((byte) 0);    

        return buf.array();
    }
}
