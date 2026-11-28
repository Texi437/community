package cn.linli.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() { }

    public record UpdateProfile(
        @NotBlank @Size(max = 32) String nickname,
        @NotBlank @Size(max = 32) String building,
        @NotBlank @Size(max = 32) String unit,
        @NotBlank @Size(max = 32) String room
    ) { }

    public record Permissions(boolean canQueryResidents) { }
    public record ResidentSummary(long id, String nickname) { }
    public record ResidentDetail(long id, String nickname, String building, String unit, String room) { }
    public record ResidentPage(List<ResidentSummary> items, long total, int page, int size) { }
}
