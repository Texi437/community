package cn.linli.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;

public final class DatabaseValues {
    private static final Instant MIN_TIMESTAMP = Instant.ofEpochSecond(1);
    private static final Instant MAX_TIMESTAMP = Instant.ofEpochSecond(Integer.MAX_VALUE, 999_999_000);

    private DatabaseValues() {}

    public static Instant timestamp(Instant value) {
        Instant normalized = value.truncatedTo(ChronoUnit.MICROS);
        if (normalized.isBefore(MIN_TIMESTAMP) || normalized.isAfter(MAX_TIMESTAMP)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "时间超出可保存范围");
        }
        return normalized;
    }

    public static long pageOffset(int page, int size, String errorMessage) {
        if (page < 1 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", errorMessage);
        }
        return ((long) page - 1) * size;
    }
}
