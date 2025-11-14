package client;// package for this file

import java.io.FileInputStream;// impoorts
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import tftp.ReliableChannel; // imports for tftp
import tftp.TFTPProtocolFlows;
import tftp.RRQPacket;
import tftp.WRQPacket;
import tftp.TFTPErrorException;
import tftp.ACKPacket;

public class Client {// start of client
    /*default port to talk to tftp server */
    private static final int DEFAULT_PORT = 5000;
    /*prints how to run the program and what flags mean */
    private static void usage() {
        System.out.println("""
            Usage:
              tftp --get <filename> --server <addr>[:port]
              tftp --put <filename> --server <addr>[:port]
              tftp --help
            options:
              -g, --get       Gets the named file from the server.
              -p, --put       Put the named file onto the server.
              -s, --server    The server to perform the operation on.
              -h, --help      Show this message.
            """);
    }

    public static void main(String[] args) {
        /*if no arg or user asks for assistence just show usage, stop */
        if (args.length == 0 || contains(args, "--help") || contains(args, "-h")) {
            usage();
            return;
        }
        /*determines if doing get or put */
        boolean isGet = contains(args, "--get") || contains(args, "-g");
        boolean isPut = contains(args, "--put") || contains(args, "-p");
        /*pull out the filename based on flags used  */
        String filename = valueAfter(args, isGet ? "--get" : "--put");
        if (filename == null) filename = valueAfter(args, isGet ? "-g" : "-p");
        /* finds the server address */
        String serverSpec = valueAfter(args, "--server");
        if (serverSpec == null) serverSpec = valueAfter(args, "-s");
        /*check on flags and required values */
        if ((isGet == isPut) || filename == null || serverSpec == null) {
            usage();
            return;
        }

        try {
            /*splits server string, host & port */
            String host;
            int port;
            int colon = serverSpec.lastIndexOf(":");
            if (colon >= 0) {
                host = serverSpec.substring(0, colon);
                port = Integer.parseInt(serverSpec.substring(colon + 1));
            } else {
                host = serverSpec;
                port = DEFAULT_PORT;
            }
            /*build socket server address */
            InetAddress addr = InetAddress.getByName(host);
            SocketAddress server = new InetSocketAddress(addr, port);
            /*udp socket set a timeout so user dosent wait for infinity */
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(2000);
            /*wrap the socket in reliable channel and helps flow */
            ReliableChannel channel = new ReliableChannel(socket);
            TFTPProtocolFlows flows = new TFTPProtocolFlows(channel, false);
            /* handles the RRQ download */
            if (isGet) {
                /*send read request */
                RRQPacket rrq = new RRQPacket(filename);
                channel.send(rrq, server);
                /*write every data block to local file that comes */
                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    flows.receiveFile(filename, fos);
                }
                System.out.println("Downloaded: " + filename);
            } else {
                /*else handle upload wrq */
                try (FileInputStream fis = new FileInputStream(filename)) {
                    WRQPacket wrq = new WRQPacket(filename);

                    System.out.println("[Client] Sending WRQ to " + server);
                    channel.send(wrq, server);
                    /*wait for first ack(O) before sending data*/
                    System.out.println("[Client] Waiting for ACK(0)...");
                    ACKPacket ack0 = channel.receive(ACKPacket.class);
                    System.out.println("[Client] Got ACK(" + ack0.getBlockNumber() + ")");
                    /* if the first ack is not block 0 something is off */
                    if (ack0.getBlockNumber() != 0) {
                        throw new TFTPErrorException(
                            "Expected ACK(0) from server, got block " + ack0.getBlockNumber());
                    }
                    /* stream file to server in data blocks */
                    flows.sendFile(filename, fis);
                }
                System.out.println("Uploaded: " + filename);
            }
            /* chanel is dont talking, close */
            channel.close();
        } catch (TFTPErrorException te) {
            System.err.println("TFTP Error: " + te.getMessage());
        } catch (java.io.FileNotFoundException fnf) {
            System.err.println("IO Error: File does not exist.");
        } catch (java.net.SocketTimeoutException ste) {
            System.err.println("Timeout: " + ste.getMessage());
        } catch (java.io.IOException ioe) {
            System.err.println("IO Error: " + ioe.getMessage());
        } catch (Exception e) {
            System.err.println("Fatal: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
    /* check if a flag in args */
    private static boolean contains(String[] a, String flag) {
        for (String s : a) if (s.equalsIgnoreCase(flag)) return true;
        return false;
    }
    /*get value tha comes  after flag, if flag */
    private static String valueAfter(String[] a, String flag) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i].equalsIgnoreCase(flag)) return a[i + 1];
        }
        return null;
    }
}// end of file
