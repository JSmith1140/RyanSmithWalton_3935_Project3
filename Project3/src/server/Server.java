package server;

import merrimackutil.json.parser.JSONParser;
import merrimackutil.json.parser.ast.SyntaxTree;
import merrimackutil.json.types.JSONObject;
import merrimackutil.net.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;


public class Server {

    private static Log logger;
    private static ExecutorService threadPool;
    private static Path dataDir;
    private static int port;

    public static void main(String[] args) {
        String configFile = "config-2.json";


        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help") || args[i].equals("-h")) {
                printHelp();
                return;
            } else if (args[i].equals("--config") || args[i].equals("-c")) {
                if (i + 1 < args.length) {
                    configFile = args[i + 1];
                    i++;
                } else {
                    System.err.println("Missing value for --config");
                    return;
                }
            }
        }

        startServer(configFile);
    }

    private static void startServer(String configFile) {
    try {
        JSONObject config = loadConfig(configFile);

        dataDir = Paths.get(config.getString("data-dir"));
        port = config.getInt("port");
        Integer poolSizeObj = config.getInt("pool-size");
        int poolSize = (poolSizeObj != null) ? poolSizeObj : 10;
        String logFile = config.getString("log");

        logger = new Log(logFile, "tftpd"); 
        logger.loggingOn();
        logger.log("tftpd starting...");
        logger.log("Port: " + port);
        logger.log("Data directory: " + dataDir.toAbsolutePath());
        logger.log("Thread pool size: " + poolSize);

        threadPool = Executors.newFixedThreadPool(poolSize);

        runServerLoop();

    } catch (IOException e) {
        System.err.println("Error reading config file: " + e.getMessage());
        if (logger != null) logger.log("Error reading config file: " + e.getMessage());
    } catch (Exception e) {
        System.err.println("Server startup failed: " + e.getMessage());
        if (logger != null) logger.log("Server startup failed: " + e.getMessage());
    }
}

private static JSONObject loadConfig(String configFile) throws IOException {
    File file = new File(configFile);
    try {
        JSONParser parser = new JSONParser(file); 
        SyntaxTree tree = parser.parse();       

        if (parser.hasError()) {
            throw new IOException("JSON parse errors: " + parser.getErrorLog());
        }


        Object obj = tree.evaluate();
        if (obj instanceof JSONObject) {
            return (JSONObject) obj;
        } else {
            throw new IOException("Config is not a JSON object");
        }

    } catch (FileNotFoundException e) {
        throw new IOException("Config file not found: " + e.getMessage(), e);
    }
}

    private static void runServerLoop() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            logger.log("Server listening on port " + port);
            byte[] buffer = new byte[516];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                short opcode = (short)(((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF));

                if (opcode == 1 || opcode == 2) {
                    threadPool.submit(new TftpRequestHandler(packet, dataDir, logger));
                } else {
                    logger.log("Ignoring invalid opcode: " + opcode);
                }
            }
        } catch (IOException e) {
            logger.log("Server socket error: " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("""
                usage:
                tftpd
                tftpd --config <config>
                tftpd --help
                options:
                  -c, --config   Config file to use.
                  -h, --help     Display this help message.
                """);
    }
}
