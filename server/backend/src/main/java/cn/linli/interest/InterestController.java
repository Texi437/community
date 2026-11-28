package cn.linli.interest;

import cn.linli.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.linli.interest.InterestDtos.*;

@RestController
@RequestMapping("/api")
public class InterestController {
    private final InterestService interests;
    private final OpportunityService opportunities;

    public InterestController(InterestService interests, OpportunityService opportunities) {
        this.interests = interests;
        this.opportunities = opportunities;
    }

    @GetMapping("/interests/topics")
    public List<Topic> topics() {
        return interests.topics();
    }

    @GetMapping("/users/me/interests")
    public Interests interests(@AuthenticationPrincipal AuthenticatedUser user) {
        return interests.interests(user);
    }

    @PutMapping("/users/me/interests")
    public Interests replaceInterests(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody Interests body) {
        return interests.replaceInterests(user, body);
    }

    @GetMapping("/users/me/participation")
    public Participation participation(@AuthenticationPrincipal AuthenticatedUser user) {
        return interests.participation(user);
    }

    @PutMapping("/users/me/participation")
    public Participation replaceParticipation(@AuthenticationPrincipal AuthenticatedUser user,
                                              @Valid @RequestBody ParticipationWrite body) {
        return interests.replaceParticipation(user, body);
    }

    @GetMapping("/activity-opportunities")
    public List<Opportunity> opportunities(@AuthenticationPrincipal AuthenticatedUser user) {
        return opportunities.current(user);
    }
}
