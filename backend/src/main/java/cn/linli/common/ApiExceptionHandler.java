package cn.linli.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> businessError(ApiException ex) {
        return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidInput(MethodArgumentNotValidException ex) {
        var error = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String message = error.map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("输入不合法");
        return ResponseEntity.badRequest().body(new ApiError("INVALID_INPUT", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> invalidJson() {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_JSON", "JSON 格式或字段不正确"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> databaseUnavailable(DataAccessException ex) {
        // Do not log SQL parameters, password hashes, tokens or resident information.
        log.error("Database request failed: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(503).body(new ApiError("SERVICE_UNAVAILABLE", "服务暂不可用，请稍后重试"));
    }
}
