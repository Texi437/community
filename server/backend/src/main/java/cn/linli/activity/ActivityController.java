package cn.linli.activity;

import cn.linli.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.linli.activity.ActivityDtos.*;

@RestController
@RequestMapping("/api")
public class ActivityController {
    private final ActivityService activities;

    public ActivityController(ActivityService activities) {
        this.activities = activities;
    }

    @GetMapping("/venues")
    public List<Venue> venues() {
        return activities.venues();
    }

    @GetMapping("/activities")
    public Page<ActivityView> activities(@AuthenticationPrincipal AuthenticatedUser user,
                                         @RequestParam(defaultValue = "discover") String scope,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return activities.list(user, scope, page, size);
    }

    @PostMapping("/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityView create(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody CreateRequest request) {
        return activities.create(user, request);
    }

    @GetMapping("/activities/{id}")
    public ActivityView detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return activities.detail(user, id);
    }

    @PostMapping("/activities/{id}/review")
    public ActivityView review(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                               @Valid @RequestBody ReviewRequest request) {
        return activities.review(user, id, request);
    }

    @PostMapping("/activities/{id}/cancel")
    public ActivityView cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                               @Valid @RequestBody VersionRequest request) {
        return activities.cancel(user, id, request.version());
    }

    @PostMapping("/activities/{id}/end")
    public ActivityView end(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                            @Valid @RequestBody VersionRequest request) {
        return activities.end(user, id, request.version());
    }

    @PostMapping("/activities/{id}/enroll")
    public ActivityView enroll(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return activities.enroll(user, id);
    }

    @PostMapping("/activities/{id}/withdraw")
    public ActivityView withdraw(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return activities.withdraw(user, id);
    }

    @GetMapping("/activities/{id}/participants")
    public Page<Participant> participants(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return activities.participants(user, id, page, size);
    }

    @GetMapping("/users/me/activities")
    public Page<ActivityView> myActivities(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam String scope,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return activities.mine(user, scope, page, size);
    }
}
