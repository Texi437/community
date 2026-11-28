package cn.linli.auth;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static cn.linli.auth.ProfileDtos.*;

@RestController
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @PutMapping("/api/users/me")
    public AuthDtos.MyProfile update(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody UpdateProfile data) {
        return profiles.update(user, data);
    }

    @GetMapping("/api/users/me/permissions")
    public Permissions permissions(@AuthenticationPrincipal AuthenticatedUser user) {
        return profiles.permissions(user);
    }

    @GetMapping("/api/residents")
    public ResidentPage residents(@AuthenticationPrincipal AuthenticatedUser user,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return profiles.residents(user, page, size);
    }

    @GetMapping("/api/residents/{userId}")
    public ResidentDetail resident(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable long userId) {
        return profiles.resident(user, userId);
    }
}
