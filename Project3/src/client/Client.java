package client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import tftp.ReliableChannel;
import tftp.TFTPProtocolFlows;
import tftp.RRQPacket;
import tftp.WRQPacket;
import tftp.TFTPErrorException;

public class Client {
    private static final int DEFAULT_PORT = 69;

    private static void usage() {
        System.out.println("Usage:\n" +
            "  tftp --get <filename> --server <addr>[:port]\n" +
            "  tftp --put <filename> --server <addr>[:port]\n" +
            "  tftp --help\n");
    }

    public static void main(String[] args) {
        if (args.length == 0 || contains(args, "--help") || contains(args, "-h")) {
            usage();
            return;
        }

        boolean isGet = contains(args, "--get") || contains(args, "-g");
        boolean isPut = contains(args, "--put") || contains(args, "-p");
        String filename = valueAfter(args, isGet ? "--get" : "--put");
        if (filename == null) filename = valueAfter(args, isGet ? "-g" : "-p");
        String serverSpec = valueAfter(args, "--server");
        if (serverSpec == null) serverSpec = valueAfter(args, "-s");

        if ((isGet == isPut) || filename == null || serverSpec == null) {
            usage();
            return;
        }

        try {
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

            InetAddress addr = InetAddress.getByName(host);
            SocketAddress server = new InetSocketAddress(addr, port);

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(2000);

            ReliableChannel channel = new ReliableChannel(socket);


            TFTPProtocolFlows flows = new TFTPProtocolFlows(channel, /*isServer=*/false);

            if (isGet) {
                RRQPacket rrq = new RRQPacket(filename);
                channel.send(rrq, server);
                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    flows.receiveFile(filename, fos);
                }
                System.out.println("Downloaded: " + filename);
            } else {
                try (FileInputStream fis = new FileInputStream(filename)) {
                    WRQPacket wrq = new WRQPacket(filename);
                    channel.send(wrq, server);
                    flows.sendFile(filename, fis);
                }
                System.out.println("Uploaded: " + filename);
            }

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

    private static boolean contains(String[] a, String flag) {
        for (String s : a) if (s.equalsIgnoreCase(flag)) return true;
        return false;
    }

    private static String valueAfter(String[] a, String flag) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i].equalsIgnoreCase(flag)) return a[i + 1];
        }
        return null;
    }
}
