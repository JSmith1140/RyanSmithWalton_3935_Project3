package client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class TestTftpHarness {


    private static final short OP_RRQ   = 1;
    private static final short OP_WRQ   = 2;
    private static final short OP_DATA  = 3;
    private static final short OP_ACK   = 4;
    private static final short OP_ERROR = 5;

    public static void main(String[] args) throws Exception {

        MiniTftpStub stub = new MiniTftpStub();
        Thread t = new Thread(stub, "MiniTftpStub");
        t.start();

   
        while (stub.getPort() == 0) Thread.sleep(10);


        File tmpDir = new File("out/test_tmp");
        tmpDir.mkdirs();
        File putFile = new File(tmpDir, "put_sample.txt");
        try (FileWriter w = new FileWriter(putFile)) {
            w.write("Hello from client put!\nLine 2.\n");
        }


        System.out.println("=== PUT test ===");
        client.Client.main(new String[]{
            "--put", putFile.getAbsolutePath(),
            "--server", "127.0.0.1:" + stub.getPort()
        });


        System.out.println("\n=== GET test ===");
        File gotFile = new File(tmpDir, "got_sample.txt");
        if (gotFile.exists()) gotFile.delete();
        client.Client.main(new String[]{
            "--get", gotFile.getAbsolutePath(),
            "--server", "127.0.0.1:" + stub.getPort()
        });


        System.out.println("\n=== Verify ===");
        if (stub.getLastUploaded() != null && stub.getLastUploaded().exists()) {
            System.out.println("PUT saved to stub: " + stub.getLastUploaded().getAbsolutePath() +
                               " (" + stub.getLastUploaded().length() + " bytes)");
        } else {
            System.out.println("PUT: no file captured by stub.");
        }

        if (gotFile.exists()) {
            System.out.println("GET downloaded: " + gotFile.getAbsolutePath() +
                               " (" + gotFile.length() + " bytes)");
            System.out.println("GET preview:\n" + readAll(gotFile));
        } else {
            System.out.println("GET file missing: " + gotFile.getAbsolutePath());
        }

        stub.close();
        t.join(500);
        System.out.println("\nDone.");
    }

    private static String readAll(File f) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }


    private static class MiniTftpStub implements Runnable, Closeable {
        private final DatagramSocket sock;
        private volatile boolean running = true;
        private volatile File lastUploaded;

        MiniTftpStub() throws SocketException {
            this.sock = new DatagramSocket(0, InetAddress.getLoopbackAddress());
            this.sock.setSoTimeout(2000);
        }

        int getPort() { return sock.getLocalPort(); }
        File getLastUploaded() { return lastUploaded; }

        @Override public void run() {
            byte[] buf = new byte[2048];
            while (running) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    sock.receive(p);

                    byte[] in = Arrays.copyOfRange(p.getData(), 0, p.getLength());
                    short op = getShort(in, 0);
                    if (op == OP_RRQ) {

                        byte[] content = ("Hello from MiniTftpStub!\nThis is a GET response.\n").getBytes(StandardCharsets.US_ASCII);
  
                        byte[] data = buildData(1, content);
                        DatagramPacket dp = new DatagramPacket(data, data.length, p.getSocketAddress());
                        sock.send(dp);
              
                    } else if (op == OP_WRQ) {
               
                        byte[] ack0 = buildAck(0);
                        sock.send(new DatagramPacket(ack0, ack0.length, p.getSocketAddress()));

       
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int expectedBlock = 1;
                        while (true) {
                            DatagramPacket pd = new DatagramPacket(new byte[2048], 2048);
                            sock.receive(pd);
                            byte[] din = Arrays.copyOfRange(pd.getData(), 0, pd.getLength());
                            short op2 = getShort(din, 0);
                            if (op2 != OP_DATA) break;
                            int blk = getShort(din, 2) & 0xFFFF;
                            byte[] chunk = Arrays.copyOfRange(din, 4, din.length);
                            if (blk == expectedBlock) {
                                out.write(chunk);
                                expectedBlock++;
                            }
         
                            byte[] ack = buildAck(blk);
                            sock.send(new DatagramPacket(ack, ack.length, pd.getSocketAddress()));
                            if (chunk.length < 512) {
                       
                                break;
                            }
                        }
  
                        try {
                            File tmpDir = new File("out/test_tmp");
                            tmpDir.mkdirs();
                            lastUploaded = new File(tmpDir, "stub_received.bin");
                            try (FileOutputStream fos = new FileOutputStream(lastUploaded)) {
                                out.writeTo(fos);
                            }
                        } catch (IOException ignored) {}
                    } else {
       
                        byte[] err = buildError(0, "Illegal operation");
                        sock.send(new DatagramPacket(err, err.length, p.getSocketAddress()));
                    }
                } catch (SocketTimeoutException ste) {
       
                } catch (IOException ioe) {
                    if (running) ioe.printStackTrace();
                }
            }
        }

        @Override public void close() {
            running = false;
            try { sock.close(); } catch (Exception ignored) {}
        }

   

        private static byte[] buildData(int block, byte[] payload) {
            byte[] out = new byte[4 + payload.length];
            putShort(out, 0, OP_DATA);
            putShort(out, 2, (short) block);
            System.arraycopy(payload, 0, out, 4, payload.length);
            return out;
        }

        private static byte[] buildAck(int block) {
            byte[] out = new byte[4];
            putShort(out, 0, OP_ACK);
            putShort(out, 2, (short) block);
            return out;
        }

        private static byte[] buildError(int code, String msg) {
            byte[] m = msg.getBytes(StandardCharsets.US_ASCII);
            byte[] out = new byte[4 + m.length + 1];
            putShort(out, 0, OP_ERROR);
            putShort(out, 2, (short) code);
            System.arraycopy(m, 0, out, 4, m.length);
            out[out.length - 1] = 0;
            return out;
        }

        private static short getShort(byte[] a, int i) {
            return (short) (((a[i] & 0xFF) << 8) | (a[i + 1] & 0xFF));
        }

        private static void putShort(byte[] a, int i, short v) {
            a[i] = (byte) ((v >> 8) & 0xFF);
            a[i + 1] = (byte) (v & 0xFF);
        }
    }
}
