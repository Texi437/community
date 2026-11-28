package cn.linli.auth;

import cn.linli.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class AuthService {
    private static final long SESSION_SECONDS = 7 * 24 * 60 * 60;
    private final AccountMapper accounts;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final String dummyPasswordHash;

    public AuthService(AccountMapper accounts, PasswordEncoder passwords, Clock clock) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.clock = clock;
        this.dummyPasswordHash = passwords.encode("not-an-account-" + random.nextLong());
    }

    @Transactional
    public AuthDtos.UserSummary register(AuthDtos.RegisterRequest request) {
        validatePasswordBytes(request.password());
        String username = request.username().toLowerCase(Locale.ROOT);
        try {
            accounts.insertUser(username, passwords.encode(request.password()), request.nickname().strip());
        } catch (DuplicateKeyException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN", "该用户名已被注册");
        }
        UserAccount account = accounts.findUserByUsername(username);
        accounts.insertResidentProfile(account.id(), request.building().strip(), request.unit().strip(), request.room().strip());
        return toUserSummary(account);
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        validatePasswordBytes(request.password());
        UserAccount account = accounts.findUserByUsername(request.username().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(request.password(), account == null ? dummyPasswordHash : account.passwordHash());
        if (!matches || account == null || !"ACTIVE".equals(account.status())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "用户名或密码错误");
        }
        long now = clock.instant().getEpochSecond();
        long expiry = now + SESSION_SECONDS;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        accounts.deleteExpiredSessions(account.id(), now);
        accounts.insertSession(hashToken(token), account.id(), expiry);
        return new AuthDtos.LoginResponse(token, "Bearer", SESSION_SECONDS, Instant.ofEpochSecond(expiry), toUserSummary(account));
    }

    public AuthenticatedUser authenticateToken(String token) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) { return null; }
        return accounts.findActiveSession(hashToken(token), clock.instant().getEpochSecond());
    }

    public AuthDtos.MyProfile getCurrentUserProfile(AuthenticatedUser user) {
        var profile = accounts.findProfileByUserId(user.id());
        if (profile == null) { throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请重新登录"); }
        return profile;
    }

    public void logout(AuthenticatedUser user) { accounts.deleteSessionByTokenHash(user.tokenHash()); }

    private static AuthDtos.UserSummary toUserSummary(UserAccount account) {
        return new AuthDtos.UserSummary(account.id(), account.username(), account.nickname(), account.role());
    }

    private static void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "密码的 UTF-8 编码不得超过72字节");
        }
    }

    static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
