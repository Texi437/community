package cn.linli.auth;

public record AuthenticatedUser(long id, String username, String nickname, String role, String tokenHash) { }
