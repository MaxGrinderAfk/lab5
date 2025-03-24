package idespring.lab5.exceptions;

public class SubjectNotAssignedException extends RuntimeException {
    public SubjectNotAssignedException(String message) {
        super(message);
    }
}
