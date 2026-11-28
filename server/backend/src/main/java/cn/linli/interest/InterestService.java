package cn.linli.interest;

import cn.linli.auth.AuthenticatedUser;
import cn.linli.auth.ProfileMapper;
import cn.linli.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import static cn.linli.common.DatabaseValues.timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.linli.interest.InterestDtos.*;

@Service
public class InterestService {
    private static final int MAX_MERGED_SLOTS = 20;

    private final InterestMapper mapper;
    private final ProfileMapper profiles;
    private final Clock clock;

    public InterestService(InterestMapper mapper, ProfileMapper profiles, Clock clock) {
        this.mapper = mapper;
        this.profiles = profiles;
        this.clock = clock;
    }

    public List<Topic> topics() {
        return mapper.topics();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Interests interests(AuthenticatedUser user) {
        requireResident(user.id());
        Map<String, Integer> strengths = new LinkedHashMap<>();
        mapper.interests(user.id()).forEach(row -> strengths.put(row.topicCode(), row.strength()));
        return new Interests(strengths);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Interests replaceInterests(AuthenticatedUser user, Interests request) {
        long userId = user.id();
        lockResident(userId);
        Set<String> activeTopics = topics().stream().map(Topic::code).collect(Collectors.toSet());
        if (!activeTopics.containsAll(request.strengths().keySet())) {
            throw invalid("兴趣主题不存在或已停用");
        }
        Instant now = clock.instant();
        mapper.deleteInterests(userId);
        request.strengths().forEach((topic, strength) -> mapper.insertInterest(userId, topic, strength, now));
        return new Interests(Map.copyOf(request.strengths()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Participation participation(AuthenticatedUser user) {
        requireResident(user.id());
        InterestMapper.ParticipationRow row = mapper.participation(user.id());
        if (row == null) {
            return new Participation(Intent.UNKNOWN, null, List.of(), 0, false);
        }
        return participation(row.intent(), row.validUntil(), mapper.slots(user.id()), row.version(), clock.instant());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Participation replaceParticipation(AuthenticatedUser user, ParticipationWrite request) {
        long userId = user.id();
        lockResident(userId);
        InterestMapper.ParticipationRow current = mapper.participation(userId);
        int version = current == null ? 0 : current.version();
        if (request.version() != version) {
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "近期安排已更新，请刷新后重试");
        }

        Instant now = clock.instant();
        Instant validUntil = request.validUntil() == null ? null : timestamp(request.validUntil());
        List<Slot> slots = mergeSlots(request.slots());
        if (request.intent() == Intent.WILLING) {
            if (validUntil == null || !validUntil.isAfter(now)
                || slots.stream().noneMatch(slot -> !slot.startsAt().isBefore(now))
                || slots.stream().anyMatch(slot -> slot.endsAt().isAfter(validUntil))) {
                throw invalid("近期愿意须填写未来的有效期和可参加时段，且时段结束不得超过有效期");
            }
        }
        if (version == Integer.MAX_VALUE) {
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT", "近期安排版本已达上限");
        }
        int nextVersion = version + 1;
        mapper.saveParticipation(userId, request.intent(), validUntil, nextVersion, now);
        mapper.deleteSlots(userId);
        slots.forEach(slot -> mapper.insertSlot(userId, slot));
        return participation(request.intent(), validUntil, slots, nextVersion, now);
    }

    private void requireResident(long id) {
        if (!mapper.activeResident(id)) {
            throw residentRequired();
        }
    }

    private void lockResident(long id) {
        if (profiles.lockActiveUser(id) == null || !mapper.hasResidentProfile(id)) {
            throw residentRequired();
        }
    }

    private static ApiException residentRequired() {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "当前账号没有可用的住户资料");
    }

    private static Participation participation(Intent intent, Instant validUntil, List<Slot> slots, int version,
                                               Instant now) {
        return new Participation(intent, validUntil, slots, version, validUntil != null && !validUntil.isAfter(now));
    }

    private static List<Slot> mergeSlots(List<Slot> input) {
        List<Slot> sorted = new ArrayList<>(input.size());
        for (Slot slot : input) {
            Instant start = timestamp(slot.startsAt());
            Instant end = timestamp(slot.endsAt());
            if (!start.isBefore(end)) {
                throw invalid("空闲时段的结束时间须晚于开始时间");
            }
            sorted.add(new Slot(start, end));
        }
        sorted.sort(Comparator.comparing(Slot::startsAt));
        List<Slot> merged = new ArrayList<>();
        for (Slot slot : sorted) {
            int lastIndex = merged.size() - 1;
            Slot last = lastIndex < 0 ? null : merged.get(lastIndex);
            if (last == null || last.endsAt().isBefore(slot.startsAt())) {
                merged.add(slot);
                if (merged.size() > MAX_MERGED_SLOTS) {
                    throw invalid("合并后最多保留20个空闲时段");
                }
            } else if (slot.endsAt().isAfter(last.endsAt())) {
                merged.set(lastIndex, new Slot(last.startsAt(), slot.endsAt()));
            }
        }
        return List.copyOf(merged);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }
}
