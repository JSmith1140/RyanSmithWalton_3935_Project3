package tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

public abstract class TFTPPacket {
    // opcodes for each of the packets per the RFC
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DATA = 3;
    public static final short OP_ACK = 4;
    public static final short OP_ERROR = 5;

    protected short opcode;

    /**
     * Constructs a new TFTP packet with the specified opcode.
     *
     * @param opcode the TFTP opcode corresponding to the packet type
     */
    public TFTPPacket(short opcode) {
        this.opcode = opcode;
    }

    /**
     * Returns the opcode of this TFTP packet.
     *
     * @return the packet opcode
     */
    public short getOpcode() {
        return opcode;
    }

    /**
     * Returns whether this packet is a Read Request (RRQ).
     *
     * @return {@code true} if the packet opcode is RRQ; {@code false} otherwise
     */
    public boolean isRRQ()   { 
        return opcode == OP_RRQ; 
    }

    /**
     * Returns whether this packet is a Write Request (WRQ).
     *
     * @return {@code true} if the packet opcode is WRQ; {@code false} otherwise
     */
    public boolean isWRQ()   { 
        return opcode == OP_WRQ; 
    }

    /**
     * Returns whether this packet is a Data packet.
     *
     * @return {@code true} if the packet opcode is DATA; {@code false} otherwise
     */
    public boolean isDATA()  { 
        return opcode == OP_DATA;
    }

    /**
     * Returns whether this packet is an Acknowledgment (ACK) packet.
     *
     * @return {@code true} if the packet opcode is ACK; {@code false} otherwise
     */
    public boolean isACK()   { 
        return opcode == OP_ACK; 
    }

    /**
     * Returns whether this packet is an Error packet.
     *
     * @return {@code true} if the packet opcode is ERROR; {@code false} otherwise
     */
    public boolean isERROR() { 
        return opcode == OP_ERROR; 
    }

    /**
     * Serializes this TFTP packet into a byte array for network transmission.
     * <p>
     * This method must be implemented by concrete subclasses to provide the
     * exact byte-level representation defined by the TFTP specification.
     *
     * @return a byte array containing the serialized packet data
     */
    public abstract byte[] serialize();

    /**
     * Converts this packet into a {@link DatagramPacket} for sending over UDP.
     *
     * @param address the destination IP address
     * @param port    the destination port number
     * @return a {@code DatagramPacket} containing the serialized packet data
     */
    public DatagramPacket toDatagramPacket(InetAddress address, int port) {
        byte[] data = serialize();
        return new DatagramPacket(data, data.length, address, port);
    }  
}
