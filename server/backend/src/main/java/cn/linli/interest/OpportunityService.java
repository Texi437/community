package cn.linli.interest;

import cn.linli.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static cn.linli.interest.InterestDtos.Opportunity;
import static cn.linli.interest.InterestDtos.Slot;
import static cn.linli.interest.InterestMapper.*;

@Service
public class OpportunityService {
    private static final Logger LOG = LoggerFactory.getLogger(OpportunityService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<LocalTime> DAILY_STARTS = List.of(LocalTime.of(9, 0), LocalTime.of(15, 0));
    private static final int HORIZON_DAYS = 14;
    private static final Duration SNAPSHOT_LIFETIME = Duration.ofMinutes(30);
    private static final int PRIVACY_THRESHOLD = 5;
    private static final String SOURCE = "RULE";
    private static final String RULE_VERSION = "rule-v1";
    private static final String LOCK_NAME = "CONCAT('linli:opportunities:', LEFT(SHA2(DATABASE(), 256), 40))";

    private final InterestMapper mapper;
    private final Clock clock;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate refreshTransaction;

    public OpportunityService(InterestMapper mapper, Clock clock, JdbcTemplate jdbc,
                              PlatformTransactionManager transactions) {
        this.mapper = mapper;
        this.clock = clock;
        this.jdbc = jdbc;
        this.refreshTransaction = new TransactionTemplate(transactions);
        this.refreshTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.refreshTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    public List<Opportunity> current(AuthenticatedUser user) {
        return mapper.currentOpportunities(user.id(), clock.instant()).stream()
            .map(OpportunityService::publicView).toList();
    }

    @Scheduled(fixedDelayString = "${linli.opportunities.refresh-interval-ms:900000}")
    public void scheduledRefresh() {
        try {
            refresh();
        } catch (RuntimeException ex) {
            LOG.warn("Activity opportunity refresh failed ({})", ex.getClass().getSimpleName());
        }
    }

    void refresh() {
        refreshTransaction.executeWithoutResult(status -> {
            if (acquireRefreshLock()) {
                refreshSnapshot();
            }
        });
    }

    private boolean acquireRefreshLock() {
        Integer acquired = jdbc.queryForObject("SELECT GET_LOCK(" + LOCK_NAME + ", 0)", Integer.class);
        if (!Integer.valueOf(1).equals(acquired)) {
            return false;
        }
        // JDBC and MyBatis share the transaction-bound connection. Release only after commit or rollback,
        // while that connection is still bound and before it returns to the pool.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    Integer released = jdbc.queryForObject("SELECT RELEASE_LOCK(" + LOCK_NAME + ")", Integer.class);
                    if (!Integer.valueOf(1).equals(released)) {
                        LOG.warn("Activity opportunity refresh lock was unavailable during release");
                    }
                } catch (RuntimeException ex) {
                    LOG.warn("Activity opportunity refresh lock release failed ({})", ex.getClass().getSimpleName());
                }
            }
        });
        return true;
    }

    private void refreshSnapshot() {
        Instant generated = clock.instant().truncatedTo(ChronoUnit.MICROS);
        List<Long> venues = mapper.activeVenueIds();
        Map<Slot, List<DemandRow>> demandBySlot = new LinkedHashMap<>();
        if (!venues.isEmpty()) {
            for (Slot slot : candidates(generated)) {
                List<DemandRow> demand = mapper.demand(slot.startsAt(), slot.endsAt());
                if (!demand.isEmpty()) {
                    demandBySlot.put(slot, demand);
                }
            }
        }

        if (!demandBySlot.isEmpty()) {
            Instant lastEnd = demandBySlot.keySet().stream().map(Slot::endsAt).max(Instant::compareTo).orElseThrow();
            // Acquire all venue locks in ID order before reading the activity index with FOR SHARE.
            // Its gap locks can block inserts for other venues, whose publishers hold venue locks.
            List<VenueRow> lockedVenues = venues.stream().map(mapper::lockOpenVenue)
                .filter(Objects::nonNull).toList();
            for (VenueRow venue : lockedVenues) {
                Instant checkedAt = clock.instant();
                if (!lastEnd.isAfter(checkedAt)) {
                    continue;
                }
                List<OccupiedSlot> occupied = mapper.occupied(venue.id(), checkedAt, lastEnd);
                for (var entry : demandBySlot.entrySet()) {
                    Slot slot = entry.getKey();
                    if (usable(slot, venue, occupied, clock.instant())) {
                        for (DemandRow demand : entry.getValue()) {
                            mapper.upsertOpportunity(snapshot(venue.id(), slot, demand, generated));
                        }
                    }
                }
            }
        }
        mapper.pruneRules(generated);
    }

    static List<Slot> candidates(Instant now) {
        LocalDate today = now.atZone(BUSINESS_ZONE).toLocalDate();
        Instant horizon = now.plus(Duration.ofDays(HORIZON_DAYS));
        List<Slot> result = new ArrayList<>();
        for (int day = 0; day <= HORIZON_DAYS; day++) {
            for (LocalTime time : DAILY_STARTS) {
                Instant startsAt = today.plusDays(day).atTime(time).atZone(BUSINESS_ZONE).toInstant();
                Instant endsAt = startsAt.plus(Duration.ofHours(1));
                if (startsAt.isAfter(now) && !endsAt.isAfter(horizon)) {
                    result.add(new Slot(startsAt, endsAt));
                }
            }
        }
        return result;
    }

    static boolean usable(Slot slot, VenueRow venue, List<OccupiedSlot> occupied, Instant now) {
        return slot.startsAt().isAfter(now)
            && !slot.startsAt().atZone(BUSINESS_ZONE).toLocalTime().isBefore(venue.opensAt())
            && !slot.endsAt().atZone(BUSINESS_ZONE).toLocalTime().isAfter(venue.closesAt())
            && occupied.stream().noneMatch(existing ->
                existing.startsAt().isBefore(slot.endsAt()) && existing.endsAt().isAfter(slot.startsAt()));
    }

    private static OpportunityRow snapshot(long venueId, Slot slot, DemandRow demand, Instant generated) {
        return new OpportunityRow(UUID.randomUUID().toString(), demand.topicCode(), venueId,
            slot.startsAt(), slot.endsAt(), demand.topicName() + "邻里活动", "根据本社区的主题兴趣和近期安排生成",
            demand.potentialCount(), demand.willingCount(), demand.availableCount(), SOURCE, RULE_VERSION,
            generated, generated.plus(SNAPSHOT_LIFETIME));
    }

    static Opportunity publicView(OpportunityRow row) {
        boolean showExactCount = row.potentialCount() >= PRIVACY_THRESHOLD
            && row.willingCount() >= PRIVACY_THRESHOLD && row.availableCount() >= PRIVACY_THRESHOLD;
        return new Opportunity(row.id(), row.topicCode(), row.venueId(), row.startsAt(), row.endsAt(), row.title(),
            row.reason(), showExactCount ? row.potentialCount() : null, showExactCount ? row.willingCount() : null,
            showExactCount ? row.availableCount() : null, showExactCount, row.sourceType(), row.sourceVersion(),
            row.generatedAt(), row.expiresAt());
    }
}
