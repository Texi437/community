package cn.linli.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

public class BearerTokenFilter extends OncePerRequestFilter {
    private final AuthService auth;
    private final ObjectMapper json;

    public BearerTokenFilter(AuthService auth, ObjectMapper json) {
        this.auth = auth;
        this.json = json;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null) { chain.doFilter(request, response); return; }
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            SecurityErrors.write(json, response, 401, "INVALID_TOKEN", "登录凭证不正确");
            return;
        }
        AuthenticatedUser user;
        try { user = auth.authenticateToken(header.substring(7)); }
        catch (DataAccessException ex) {
            SecurityErrors.write(json, response, 503, "SERVICE_UNAVAILABLE", "服务暂不可用，请稍后重试");
            return;
        }
        if (user == null) {
            SecurityErrors.write(json, response, 401, "INVALID_TOKEN", "登录已过期或凭证无效，请重新登录");
            return;
        }
        var authentication = new UsernamePasswordAuthenticationToken(user, null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try { chain.doFilter(request, response); }
        finally { SecurityContextHolder.clearContext(); }
    }
}
