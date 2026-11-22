package cn.linli.auth;

import cn.linli.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class SecurityErrors {
    private SecurityErrors() { }

    public static void write(ObjectMapper json, HttpServletResponse response, int status,
                             String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (status == 401) { response.setHeader("WWW-Authenticate", "Bearer"); }
        json.writeValue(response.getOutputStream(), new ApiError(code, message));
    }
}
