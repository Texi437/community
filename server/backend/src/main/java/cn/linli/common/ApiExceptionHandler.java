package cn.linli.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    @ExceptionHandler(ConcurrencyFailureException.class)
    ResponseEntity<ApiError> transactionConflict() {
        return ResponseEntity.status(409).body(new ApiError("RETRYABLE_CONFLICT", "数据正在更新，请刷新后重试"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ApiError> duplicateRecord() {
        return ResponseEntity.status(409).body(new ApiError("DUPLICATE_SUBMISSION", "该记录已提交，请刷新查看"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> databaseUnavailable(DataAccessException ex) {
        // Do not log SQL parameters, password hashes, tokens or resident information.
        log.error("Database request failed: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(503).body(new ApiError("SERVICE_UNAVAILABLE", "服务暂不可用，请稍后重试"));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestPartException.class})
    ResponseEntity<ApiError> invalidParameter() {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_INPUT", "请求参数不正确或缺少必填部分"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge() {
        return ResponseEntity.status(413).body(new ApiError("UPLOAD_TOO_LARGE", "图片不得超过5MiB，请求总大小不得超过16MiB"));
    }
}
