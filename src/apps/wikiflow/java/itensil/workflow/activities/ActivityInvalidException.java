package itensil.workflow.activities;

public class ActivityInvalidException extends Exception {

	public ActivityInvalidException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActivityInvalidException(String message) {
		super(message);
	}

}
