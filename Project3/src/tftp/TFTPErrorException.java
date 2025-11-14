package tftp;

/*  
 * Exeption used for TFTP protocol errors
 * Lets us distinguish TFTP errors from other IO errors
 */
public class TFTPErrorException extends Exception {


//Construct a TFTP error with a message
    public TFTPErrorException(String message) {
        super(message);
    }
//Construct a TFTP error with a message and cause
    public TFTPErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
