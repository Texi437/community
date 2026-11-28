package cn.linli.interest;

import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static cn.linli.interest.InterestDtos.*;

@Mapper
public interface InterestMapper {
    @Select("""
        SELECT `主题代码` AS code, `名称` AS name, `排序` AS sortOrder
        FROM `兴趣主题` WHERE `启用` = TRUE ORDER BY `排序`, `主题代码`
        """)
    List<Topic> topics();

    @Select("""
        SELECT EXISTS(
            SELECT 1 FROM `用户账号` u JOIN `居民资料` p ON p.`用户编号` = u.`用户编号`
            WHERE u.`用户编号` = #{userId} AND u.`状态` = 'ACTIVE'
        )
        """)
    boolean activeResident(long userId);

    @Select("SELECT EXISTS(SELECT 1 FROM `居民资料` WHERE `用户编号` = #{userId})")
    boolean hasResidentProfile(long userId);

    @Select("""
        SELECT `主题代码` AS topicCode, `强度` AS strength
        FROM `用户兴趣` WHERE `用户编号` = #{userId} ORDER BY `主题代码`
        """)
    List<InterestRow> interests(long userId);

    @Delete("DELETE FROM `用户兴趣` WHERE `用户编号` = #{userId}")
    void deleteInterests(long userId);

    @Insert("""
        INSERT INTO `用户兴趣` (`用户编号`, `主题代码`, `强度`, `更新时间`)
        VALUES (#{userId}, #{topicCode}, #{strength}, #{now})
        """)
    void insertInterest(@Param("userId") long userId, @Param("topicCode") String topicCode,
                        @Param("strength") int strength, @Param("now") Instant now);

    @Select("""
        SELECT `意愿` AS intent, `有效期` AS validUntil, `版本` AS version
        FROM `参与意愿` WHERE `用户编号` = #{userId}
        """)
    ParticipationRow participation(long userId);

    @Insert("""
        INSERT INTO `参与意愿` (`用户编号`, `意愿`, `有效期`, `版本`, `更新时间`)
        VALUES (#{userId}, #{intent}, #{validUntil,jdbcType=TIMESTAMP}, #{version}, #{now}) AS incoming
        ON DUPLICATE KEY UPDATE `意愿` = incoming.`意愿`, `有效期` = incoming.`有效期`,
            `版本` = incoming.`版本`, `更新时间` = incoming.`更新时间`
        """)
    void saveParticipation(@Param("userId") long userId, @Param("intent") Intent intent,
                           @Param("validUntil") Instant validUntil, @Param("version") int version,
                           @Param("now") Instant now);

    @Select("""
        SELECT `开始时间` AS startsAt, `结束时间` AS endsAt
        FROM `空闲时段` WHERE `用户编号` = #{userId} ORDER BY `开始时间`, `结束时间`
        """)
    List<Slot> slots(long userId);

    @Delete("DELETE FROM `空闲时段` WHERE `用户编号` = #{userId}")
    void deleteSlots(long userId);

    @Insert("""
        INSERT INTO `空闲时段` (`用户编号`, `开始时间`, `结束时间`)
        VALUES (#{userId}, #{slot.startsAt}, #{slot.endsAt})
        """)
    void insertSlot(@Param("userId") long userId, @Param("slot") Slot slot);

    @Select("""
        SELECT `场地编号` AS id
        FROM `场地` WHERE `状态` = 'ACTIVE' ORDER BY `场地编号`
        """)
    List<Long> activeVenueIds();

    // Locking reads see the current venue and published activities after the repeatable-read calculation.
    @Select("""
        SELECT `场地编号` AS id, `开放时间` AS opensAt, `关闭时间` AS closesAt
        FROM `场地` WHERE `场地编号` = #{id} AND `状态` = 'ACTIVE' FOR SHARE
        """)
    VenueRow lockOpenVenue(long id);

    @Select("""
        SELECT `场地编号` AS venueId, `开始时间` AS startsAt, `结束时间` AS endsAt
        FROM `社区活动`
        WHERE `场地编号` = #{venueId} AND `状态` = 'PUBLISHED'
          AND `开始时间` < #{end} AND `结束时间` > #{start}
        FOR SHARE
        """)
    List<OccupiedSlot> occupied(@Param("venueId") long venueId, @Param("start") Instant start,
                                @Param("end") Instant end);

    @Select("""
        SELECT t.`主题代码` AS topicCode, t.`名称` AS topicName,
            COUNT(*) AS potentialCount,
            COUNT(CASE WHEN p.`意愿` = 'WILLING' AND p.`有效期` >= #{end} THEN 1 END) AS willingCount,
            COUNT(CASE WHEN p.`意愿` = 'WILLING' AND p.`有效期` >= #{end} AND EXISTS (
                SELECT 1 FROM `空闲时段` s
                WHERE s.`用户编号` = u.`用户编号` AND s.`开始时间` <= #{start} AND s.`结束时间` >= #{end}
            ) THEN 1 END) AS availableCount
        FROM `兴趣主题` t
        JOIN `用户兴趣` i ON i.`主题代码` = t.`主题代码` AND i.`强度` > 0
        JOIN `用户账号` u ON u.`用户编号` = i.`用户编号` AND u.`状态` = 'ACTIVE'
        JOIN `居民资料` r ON r.`用户编号` = u.`用户编号`
        LEFT JOIN `参与意愿` p ON p.`用户编号` = u.`用户编号`
        WHERE t.`启用` = TRUE
        GROUP BY t.`主题代码`, t.`名称`
        ORDER BY t.`主题代码`
        """)
    List<DemandRow> demand(@Param("start") Instant start, @Param("end") Instant end);

    @Insert("""
        INSERT INTO `活动机会` (
            `机会编号`, `主题代码`, `场地编号`, `开始时间`, `结束时间`, `标题`, `推荐依据`, `兴趣人数`, `意愿人数`,
            `可用人数`, `来源类型`, `规则版本`, `生成时间`, `到期时间`
        )
        VALUES (
            #{id}, #{topicCode}, #{venueId}, #{startsAt}, #{endsAt}, #{title}, #{reason}, #{potentialCount},
            #{willingCount}, #{availableCount}, #{sourceType}, #{sourceVersion}, #{generatedAt}, #{expiresAt}
        ) AS incoming
        ON DUPLICATE KEY UPDATE `标题` = incoming.`标题`, `推荐依据` = incoming.`推荐依据`,
            `兴趣人数` = incoming.`兴趣人数`, `意愿人数` = incoming.`意愿人数`,
            `可用人数` = incoming.`可用人数`, `规则版本` = incoming.`规则版本`,
            `生成时间` = incoming.`生成时间`, `到期时间` = incoming.`到期时间`
        """)
    void upsertOpportunity(OpportunityRow opportunity);

    @Delete("DELETE FROM `活动机会` WHERE `来源类型` = 'RULE' AND `生成时间` < #{generatedAt}")
    void pruneRules(Instant generatedAt);

    @Select("""
        SELECT o.`机会编号` AS id, o.`主题代码` AS topicCode, o.`场地编号` AS venueId,
            o.`开始时间` AS startsAt, o.`结束时间` AS endsAt, o.`标题` AS title, o.`推荐依据` AS reason,
            o.`兴趣人数` AS potentialCount, o.`意愿人数` AS willingCount, o.`可用人数` AS availableCount,
            o.`来源类型` AS sourceType, o.`规则版本` AS sourceVersion,
            o.`生成时间` AS generatedAt, o.`到期时间` AS expiresAt
        FROM `活动机会` o
        JOIN `兴趣主题` t ON t.`主题代码` = o.`主题代码` AND t.`启用` = TRUE
        JOIN `场地` v ON v.`场地编号` = o.`场地编号` AND v.`状态` = 'ACTIVE'
        WHERE o.`到期时间` > #{now} AND o.`开始时间` > #{now}
            AND DATE(o.`开始时间`) = DATE(o.`结束时间`)
            AND TIME(o.`开始时间`) >= v.`开放时间` AND TIME(o.`结束时间`) <= v.`关闭时间`
            AND NOT EXISTS (
                SELECT 1 FROM `社区活动` a
                WHERE a.`场地编号` = o.`场地编号` AND a.`状态` = 'PUBLISHED'
                    AND a.`开始时间` < o.`结束时间` AND a.`结束时间` > o.`开始时间`
            )
            AND NOT EXISTS (
                SELECT 1 FROM `用户兴趣` i
                WHERE i.`用户编号` = #{userId} AND i.`主题代码` = o.`主题代码` AND i.`强度` = 0
            )
        ORDER BY o.`开始时间`, o.`主题代码`, o.`场地编号`, o.`机会编号`
        """)
    List<OpportunityRow> currentOpportunities(@Param("userId") long userId, @Param("now") Instant now);

    record InterestRow(String topicCode, int strength) {}
    record ParticipationRow(Intent intent, Instant validUntil, int version) {}
    record VenueRow(long id, LocalTime opensAt, LocalTime closesAt) {}
    record OccupiedSlot(long venueId, Instant startsAt, Instant endsAt) {}
    record DemandRow(String topicCode, String topicName, int potentialCount, int willingCount, int availableCount) {}
    record OpportunityRow(String id, String topicCode, long venueId, Instant startsAt, Instant endsAt,
                          String title, String reason, int potentialCount, int willingCount, int availableCount,
                          String sourceType, String sourceVersion, Instant generatedAt, Instant expiresAt) {}
}

