package cn.linli.activity;

import cn.linli.auth.AuthenticatedUser;
import cn.linli.common.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import static cn.linli.common.DatabaseValues.timestamp;
import static cn.linli.common.DatabaseValues.pageOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static cn.linli.activity.ActivityDtos.*;
import static cn.linli.activity.ActivityDtos.ActivityStatus.*;

@Service
public class ActivityService {
    private static final ZoneId COMMUNITY_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern ACTIVITY_ID = Pattern.compile(UUID_PATTERN);
    private static final TypeReference<List<ReviewReason>> REASONS_TYPE = new TypeReference<>() {};

    private final ActivityMapper mapper;
    private final Clock clock;
    private final ObjectMapper json;

    public ActivityService(ActivityMapper mapper, Clock clock, ObjectMapper json) {
        this.mapper = mapper;
        this.clock = clock;
        this.json = json;
    }

    public List<Venue> venues() {
        return mapper.venues();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ActivityView> list(AuthenticatedUser user, String scope, int page, int size) {
        switch (scope) {
            case "review" -> requireAdmin(user.role());
            case "discover" -> { }
            default -> throw invalid("活动列表范围不正确");
        }
        return loadPage(user.id(), scope, page, size);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ActivityView> mine(AuthenticatedUser user, String scope, int page, int size) {
        if (!"created".equals(scope) && !"enrolled".equals(scope)) {
            throw invalid("请选择发起或报名活动");
        }
        return loadPage(user.id(), scope, page, size);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ActivityView detail(AuthenticatedUser user, String rawId) {
        ActivityMapper.ActivityRow row = findView(id(rawId), user.id());
        requireVisible(user.id(), user.role(), row.organizerId(), row.status(), row.reviewMethod());
        return view(row, user.id(), clock.instant());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<Participant> participants(AuthenticatedUser user, String rawId, int page, int size) {
        String id = id(rawId);
        long offset = pageOffset(page, size, "页码须为正数，每页须为1至50条");
        ActivityMapper.ActivityRow row = findView(id, user.id());
        requireVisible(user.id(), user.role(), row.organizerId(), row.status(), row.reviewMethod());
        requireManager(user.id(), user.role(), row.organizerId());
        return new Page<>(mapper.participants(id, size, offset), row.enrollmentCount(), page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView create(AuthenticatedUser user, CreateRequest input) {
        long userId = user.id();
        lockActor(userId);
        requireProfile(userId);
        String clientRequestId = id(input.clientRequestId());
        if (mapper.duplicateSubmission(userId, clientRequestId)) {
            throw duplicate();
        }

        var request = new CreateRequest(clientRequestId, input.topicCode(), input.title().strip(),
            input.description().strip(), input.venueId(), timestamp(input.startsAt()), timestamp(input.endsAt()),
            input.capacity(), input.usesSpeaker());
        Venue venue = lockVenue(request.venueId());
        Instant now = clock.instant();
        validatePublishable(venue, request.topicCode(), request.capacity(), request.startsAt(), request.endsAt(), now);
        List<ReviewReason> reasons = exceptions(venue, request.startsAt(), request.endsAt(), request.usesSpeaker());
        boolean auto = reasons.isEmpty();
        String id = UUID.randomUUID().toString();

        try {
            mapper.insert(id, userId, request, auto ? PUBLISHED : PENDING_REVIEW, auto ? ReviewMethod.AUTO : null,
                encodeReasons(reasons), auto ? now : null, now);
        } catch (DuplicateKeyException ex) {
            throw duplicate();
        }
        return view(findView(id, userId), userId, now);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView review(AuthenticatedUser user, String rawId, ReviewRequest request) {
        String id = id(rawId);
        requireAdmin(lockActor(user.id()));
        Venue venue = lockActivityVenue(id);
        ActivityMapper.LockedActivity activity = lockActivity(id);
        requireVersion(activity, request.version());
        if (activity.status() != PENDING_REVIEW) {
            throw invalidStatus("只有待协调活动可以审核");
        }
        Instant now = clock.instant();
        String reasons = null;
        if (request.approved()) {
            validatePublishable(venue, activity.topicCode(), activity.capacity(), activity.startsAt(), activity.endsAt(), now);
            reasons = encodeReasons(exceptions(venue, activity.startsAt(), activity.endsAt(), activity.usesSpeaker()));
        }
        requireUpdated(mapper.review(id, request.approved() ? PUBLISHED : REJECTED, request.version(),
            user.id(), reasons, now));
        return view(findView(id, user.id()), user.id(), now);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView cancel(AuthenticatedUser user, String rawId, int version) {
        return changeState(user, id(rawId), version, CANCELLED);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView end(AuthenticatedUser user, String rawId, int version) {
        return changeState(user, id(rawId), version, ENDED);
    }

    private ActivityView changeState(AuthenticatedUser user, String id, int version, ActivityStatus next) {
        String role = lockActor(user.id());
        lockActivityVenue(id);
        ActivityMapper.LockedActivity activity = lockActivity(id);
        requireVisible(user.id(), role, activity.organizerId(), activity.status(), activity.reviewMethod());
        requireManager(user.id(), role, activity.organizerId());
        requireVersion(activity, version);
        Instant now = clock.instant();
        ActivityStatus current = activity.status();
        if (next == CANCELLED) {
            if (current != PUBLISHED && current != PENDING_REVIEW) {
                throw invalidStatus("只有已发布或待协调活动可以取消");
            }
        } else if (current != PUBLISHED || now.isBefore(activity.endsAt())) {
            throw invalidStatus("活动达到结束时间后才可以标记结束");
        }

        requireUpdated(mapper.updateState(id, current, next, version, now));
        if (next == CANCELLED) {
            mapper.cancelEnrollments(id, now);
        }
        return view(findView(id, user.id()), user.id(), now);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView enroll(AuthenticatedUser user, String rawId) {
        String id = id(rawId);
        long userId = user.id();
        String role = lockActor(userId);
        requireProfile(userId);
        ActivityMapper.LockedActivity activity = lockActivity(id);
        requireVisible(userId, role, activity.organizerId(), activity.status(), activity.reviewMethod());
        Instant now = clock.instant();
        requireRegistration(activity, now);

        EnrollmentStatus current = mapper.lockEnrollment(id, userId);
        if (current == EnrollmentStatus.CONFIRMED) {
            throw conflict("ALREADY_ENROLLED", "已报名本场活动");
        }
        if (current == EnrollmentStatus.CANCELLED) {
            throw invalidStatus("已取消的报名无法恢复");
        }
        if (mapper.enrollmentOverlaps(userId, id, activity.startsAt(), activity.endsAt())) {
            throw conflict("ENROLLMENT_CONFLICT", "已报名的其他活动与本场时间重叠");
        }
        if (mapper.countParticipants(id) >= activity.capacity()) {
            throw conflict("ACTIVITY_FULL", "活动名额已满");
        }

        if (current == null) {
            mapper.insertEnrollment(id, userId, now);
        } else {
            updateEnrollment(id, userId, current, EnrollmentStatus.CONFIRMED, now);
        }
        return view(findView(id, userId), userId, now);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ActivityView withdraw(AuthenticatedUser user, String rawId) {
        String id = id(rawId);
        long userId = user.id();
        String role = lockActor(userId);
        ActivityMapper.LockedActivity activity = lockActivity(id);
        requireVisible(userId, role, activity.organizerId(), activity.status(), activity.reviewMethod());
        Instant now = clock.instant();
        requireRegistration(activity, now);

        if (mapper.lockEnrollment(id, userId) != EnrollmentStatus.CONFIRMED) {
            throw conflict("NOT_ENROLLED", "当前没有有效报名");
        }
        updateEnrollment(id, userId, EnrollmentStatus.CONFIRMED, EnrollmentStatus.WITHDRAWN, now);
        return view(findView(id, userId), userId, now);
    }

    private void updateEnrollment(String id, long userId, EnrollmentStatus current, EnrollmentStatus next, Instant now) {
        if (mapper.updateEnrollment(id, userId, current, next, now) != 1) {
            throw conflict("RETRYABLE_CONFLICT", "报名状态已更新，请刷新后重试");
        }
    }

    private Page<ActivityView> loadPage(long userId, String scope, int page, int size) {
        long offset = pageOffset(page, size, "页码须为正数，每页须为1至50条");
        Instant now = clock.instant();
        List<ActivityView> items = mapper.list(userId, scope, now, size, offset)
            .stream().map(row -> view(row, userId, now)).toList();
        return new Page<>(items, mapper.count(userId, scope, now), page, size);
    }

    private ActivityView view(ActivityMapper.ActivityRow row, long userId, Instant now) {
        return new ActivityView(row.id(), row.topicCode(), row.title(), row.description(), row.venueId(),
            row.venueName(), row.startsAt(), row.endsAt(), row.capacity(), row.usesSpeaker(), row.status(),
            row.version(), row.reviewMethod(), decodeReasons(row.reviewReasons()), row.reviewedAt(),
            row.organizerNickname(), userId == row.organizerId(), row.enrollmentCount(), row.enrollmentStatus(),
            row.status() == PUBLISHED && now.isBefore(row.startsAt()));
    }

    private ActivityMapper.ActivityRow findView(String id, long userId) {
        ActivityMapper.ActivityRow row = mapper.view(id, userId);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    private ActivityMapper.LockedActivity lockActivity(String id) {
        ActivityMapper.LockedActivity row = mapper.lockActivity(id);
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    private Venue lockActivityVenue(String id) {
        // Venue assignment is immutable, so this lookup can precede taking the activity lock.
        Long venueId = mapper.location(id);
        if (venueId == null) {
            throw notFound();
        }
        return lockVenue(venueId);
    }

    private Venue lockVenue(long venueId) {
        Venue venue = mapper.lockVenue(venueId);
        if (venue == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "场地不存在");
        }
        return venue;
    }

    private String lockActor(long userId) {
        String role = mapper.lockActiveRole(userId);
        if (role == null) {
            throw forbidden();
        }
        return role;
    }

    private void requireProfile(long userId) {
        if (!mapper.hasResidentProfile(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号没有可用的住户资料");
        }
    }

    private void validatePublishable(Venue venue, String topic, int capacity, Instant start, Instant end, Instant now) {
        if (!start.isBefore(end) || !now.isBefore(start)) {
            throw invalid("活动开始须晚于当前时间，结束须晚于开始");
        }
        if (!"ACTIVE".equals(venue.status())) {
            throw invalid("场地已停用");
        }
        if (capacity <= 0 || capacity > venue.capacity()) {
            throw invalid("活动人数须为正数且不得超过场地容量");
        }
        if (!mapper.activeTopic(topic)) {
            throw invalid("兴趣主题不存在或已停用");
        }
        if (mapper.venueOccupied(venue.id(), start, end)) {
            throw conflict("VENUE_CONFLICT", "该时段的场地已被活动占用");
        }
    }

    private static List<ReviewReason> exceptions(Venue venue, Instant start, Instant end, boolean usesSpeaker) {
        var from = start.atZone(COMMUNITY_ZONE);
        var to = end.atZone(COMMUNITY_ZONE);
        var reasons = new ArrayList<ReviewReason>(2);
        if (!from.toLocalDate().equals(to.toLocalDate()) || from.toLocalTime().isBefore(venue.opensAt())
            || to.toLocalTime().isAfter(venue.closesAt())) {
            reasons.add(ReviewReason.VENUE_HOURS);
        }
        if (usesSpeaker && !venue.allowsSpeaker()) {
            reasons.add(ReviewReason.SPEAKER);
        }
        return reasons;
    }

    private String encodeReasons(List<ReviewReason> reasons) {
        try {
            return json.writeValueAsString(reasons);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize activity review reasons", ex);
        }
    }

    private List<ReviewReason> decodeReasons(String reasons) {
        try {
            return json.readValue(reasons, REASONS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid stored activity review reasons", ex);
        }
    }

    private static void requireRegistration(ActivityMapper.LockedActivity activity, Instant now) {
        if (activity.status() != PUBLISHED) {
            throw invalidStatus("只有已发布的活动可以报名或退出");
        }
        if (!now.isBefore(activity.startsAt())) {
            throw conflict("REGISTRATION_CLOSED", "活动已开始，报名已截止");
        }
    }

    private static void requireVisible(long userId, String role, long organizerId, ActivityStatus status, ReviewMethod method) {
        boolean privateActivity = status == PENDING_REVIEW || status == REJECTED
            || (status == CANCELLED && method == null);
        if (privateActivity && userId != organizerId && !"ADMIN".equals(role)) {
            throw notFound();
        }
    }

    private static void requireManager(long userId, String role, long organizerId) {
        if (userId != organizerId && !"ADMIN".equals(role)) {
            throw forbidden();
        }
    }

    private static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw forbidden();
        }
    }

    private static void requireVersion(ActivityMapper.LockedActivity activity, int version) {
        if (activity.version() != version || version == Integer.MAX_VALUE) {
            throw versionConflict();
        }
    }

    private static void requireUpdated(int affected) {
        if (affected != 1) {
            throw versionConflict();
        }
    }

    private static String id(String value) {
        if (!ACTIVITY_ID.matcher(value).matches()) {
            throw invalid("编号格式不正确");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    private static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "无权操作该活动");
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "活动不存在");
    }

    private static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private static ApiException versionConflict() {
        return conflict("VERSION_CONFLICT", "活动已更新，请刷新后重试");
    }

    private static ApiException invalidStatus(String message) {
        return conflict("INVALID_STATUS", message);
    }

    private static ApiException duplicate() {
        return conflict("DUPLICATE_SUBMISSION", "活动已提交，请刷新查看");
    }
}
