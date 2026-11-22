package com.mwcm.reconciliation.institution;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mwcm.reconciliation.source.DuplicateIntegrationSourceNameException;
import com.mwcm.reconciliation.source.IntegrationSourceNotFoundException;
import com.mwcm.reconciliation.transaction.TransactionNotFoundException;

@RestControllerAdvice 
public class ApiExceptionHandler {
    
    @ExceptionHandler(DuplicateInstitutionCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateInstitutionCodeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }
    
    @ExceptionHandler(InstitutionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InstitutionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exception.getMessage()));
    }
    
    @ExceptionHandler(DuplicateIntegrationSourceNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateIntegrationSourceNameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IntegrationSourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationSourceNotFound(
            IntegrationSourceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }
}
