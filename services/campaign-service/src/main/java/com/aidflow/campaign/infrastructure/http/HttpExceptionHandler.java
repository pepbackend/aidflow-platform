package com.aidflow.campaign.infrastructure.http;

import com.aidflow.campaign.application.CampaignNotFoundException;
import com.aidflow.campaign.application.ForbiddenCampaignOperationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class HttpExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class, MissingRequestHeaderException.class})
    public ResponseEntity<Map<String, String>> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "bad_request", exception.getMessage());
    }

    @ExceptionHandler(ForbiddenCampaignOperationException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenCampaignOperationException exception) {
        return error(HttpStatus.FORBIDDEN, "forbidden", exception.getMessage());
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(CampaignNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "not_found", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> internalServerError(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error", "Unexpected error");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("error", error, "message", message == null ? status.getReasonPhrase() : message));
    }
}
