package com.example.demo.exception;

/**
 * Custom exception for redline-specific errors
 */
public class RedlineException extends RuntimeException {
    
    public enum ErrorCode {
        PARAGRAPH_NOT_FOUND("The specified paragraph could not be found"),
        ANCHOR_NOT_FOUND("The anchor paragraph could not be found"),
        DOCUMENT_PROCESSING_ERROR("Failed to process document"),
        INPUT_VALIDATION_ERROR("Invalid input parameters");
        
        private final String defaultMessage;
        
        ErrorCode(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }
        
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
    
    private final ErrorCode errorCode;
    
    public RedlineException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
    
    public RedlineException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RedlineException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public RedlineException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}