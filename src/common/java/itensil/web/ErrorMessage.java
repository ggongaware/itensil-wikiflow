package itensil.web;

/**
 * Use this to send an error message to the Web UI
 * 
 * @author grantg
 *
 */
public class ErrorMessage extends Exception {
	
    /**
     * @param message
     */
    public ErrorMessage(String message) {
        super(message);
    }
    
}
