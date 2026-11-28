package cn.linli.auth;

// Persistence-only model. Never returned by a controller.
public record UserAccount(long id, String username, String passwordHash,
                         String nickname, String role, String status) { }
