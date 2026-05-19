package com.javeme.duobao.exception;
import com.javeme.duobao.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * This method catches all the RuntimeExceptions we explicitly threw
     * (e.g., "Invalid password", "User is banned", "Out of stock")
     * @param ex
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRunTimeException(RuntimeException ex) {

        log.error("Business rule violation: {}", ex.getMessage(), ex);

        //Create a response object
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );

        //return error 400 and error response
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * This method acts as the final fallback. It catches unexpected system crashes,
     * bugs or unhandled errors (like database disconnects or NullPointerExceptions
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {

        log.error("Fatal system error occurred", ex);

        //Create a response object
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "a system error occurred. Please try again later.",
                System.currentTimeMillis()

        );

        //return error 500 and error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
