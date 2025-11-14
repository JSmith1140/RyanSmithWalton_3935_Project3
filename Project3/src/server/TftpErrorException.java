package server;

public class TftpErrorException extends Exception {
    /**
     * Constructs a new {@code TftpErrorException} with the specified detail message.
     *
     * @param msg the detail message explaining the cause of the TFTP error
     */
    public TftpErrorException(String msg) {
        super(msg);
    }
}
