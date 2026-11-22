package cn.linli.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{3,31}", message = "需为字母开头的4至32位字母、数字或下划线") String username,
        @NotBlank @Size(min = 8, max = 64, message = "长度需为8至64个字符") String password,
        @NotBlank @Size(max = 32) String nickname,
        @NotBlank @Size(max = 32) String building,
        @NotBlank @Size(max = 32) String unit,
        @NotBlank @Size(max = 32) String room
    ) { }

    public record LoginRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{3,31}") String username,
        @NotBlank @Size(min = 8, max = 64) String password
    ) { }

    public record UserSummary(long id, String username, String nickname, String role) { }
    public record LoginResponse(String accessToken, String tokenType, long expiresIn,
                                Instant expiresAt, UserSummary user) { }
    public record MyProfile(long id, String username, String nickname, String role,
                            String building, String unit, String room) { }
}
