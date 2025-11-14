package tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;

public abstract class TFTPPacket {
    public static final short OP_RRQ = 1;
    public static final short OP_WRQ = 2;
    public static final short OP_DATA = 3;
    public static final short OP_ACK = 4;
    public static final short OP_ERROR = 5;

    protected short opcode;

    public TFTPPacket(short opcode) {
        this.opcode = opcode;
    }

    public short getOpcode() {
        return opcode;
    }

    public boolean isRRQ()   { 
        return opcode == OP_RRQ; 
    }

    public boolean isWRQ()   { 
        return opcode == OP_WRQ; 
    }

    public boolean isDATA()  { 
        return opcode == OP_DATA;
    }

    public boolean isACK()   { 
        return opcode == OP_ACK; 
    }
    public boolean isERROR() { 
        return opcode == OP_ERROR; 
    }

    public abstract byte[] serialize();

    public DatagramPacket toDatagramPacket(InetAddress address, int port) {
        byte[] data = serialize();
        return new DatagramPacket(data, data.length, address, port);
    }  
}
