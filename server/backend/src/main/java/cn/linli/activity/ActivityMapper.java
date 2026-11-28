package cn.linli.activity;

import org.apache.ibatis.annotations.*;

import java.time.Instant;
import java.util.List;

import static cn.linli.activity.ActivityDtos.*;

@Mapper
public interface ActivityMapper {
    record LockedActivity(long organizerId, String topicCode, long venueId, Instant startsAt,
                          Instant endsAt, int capacity, boolean usesSpeaker, ActivityStatus status,
                          int version, ReviewMethod reviewMethod) {}

    record ActivityRow(String id, String topicCode, String title, String description,
                       long venueId, String venueName, Instant startsAt, Instant endsAt, int capacity,
                       boolean usesSpeaker, ActivityStatus status, int version, ReviewMethod reviewMethod,
                       String reviewReasons, Instant reviewedAt, long organizerId, String organizerNickname,
                       long enrollmentCount, EnrollmentStatus enrollmentStatus) {}

    String VENUE_COLUMNS = "SELECT `场地编号` AS id, `名称` AS name, `容量` AS capacity, `开放时间` AS opens_at, `关闭时间` AS closes_at, `允许音响` AS allows_speaker, `规则` AS rule_text, `状态` AS status FROM `场地`";
    String VIEW_COLUMNS = """
        SELECT a.`活动编号` AS id, a.`主题代码` AS topic_code, a.`标题` AS title, a.`描述` AS description, a.`场地编号` AS venue_id, v.`名称` AS venue_name,
               a.`开始时间` AS starts_at, a.`结束时间` AS ends_at, a.`容量` AS capacity, a.`使用音响` AS uses_speaker, a.`状态` AS status, a.`版本` AS version,
               a.`审核方式` AS review_method, a.`审核原因` AS review_reasons, a.`审核时间` AS reviewed_at, a.`发起人` AS organizer_id, u.`昵称` AS organizer_nickname,
               (SELECT COUNT(*) FROM `活动报名` e
                WHERE e.`活动编号` = a.`活动编号` AND e.`状态` = 'CONFIRMED') AS enrollment_count,
               own.`状态` AS enrollment_status
        """;
    String VIEW_JOINS = """
        FROM `社区活动` a
        JOIN `场地` v ON v.`场地编号` = a.`场地编号`
        JOIN `用户账号` u ON u.`用户编号` = a.`发起人`
        LEFT JOIN `活动报名` own ON own.`活动编号` = a.`活动编号` AND own.`用户编号` = #{userId}
        """;
    String PAGE_FILTER = """
        <choose>
            <when test="scope == 'review'">WHERE a.`状态` = 'PENDING_REVIEW'</when>
            <when test="scope == 'created'">WHERE a.`发起人` = #{userId}</when>
            <when test="scope == 'enrolled'">WHERE own.`用户编号` = #{userId}</when>
            <otherwise>WHERE a.`状态` = 'PUBLISHED' AND a.`开始时间` &gt; #{now}</otherwise>
        </choose>
        """;

    @Select(VENUE_COLUMNS + " WHERE `状态` = 'ACTIVE' ORDER BY `场地编号`")
    List<Venue> venues();

    @Select(VENUE_COLUMNS + " WHERE `场地编号` = #{id} FOR UPDATE")
    Venue lockVenue(long id);

    @Select("SELECT `角色` AS role FROM `用户账号` WHERE `用户编号` = #{id} AND `状态` = 'ACTIVE' FOR UPDATE")
    String lockActiveRole(long id);

    @Select("SELECT EXISTS (SELECT 1 FROM `居民资料` WHERE `用户编号` = #{id})")
    boolean hasResidentProfile(long id);

    @Select("SELECT EXISTS (SELECT 1 FROM `兴趣主题` WHERE `主题代码` = #{code} AND `启用` = TRUE)")
    boolean activeTopic(String code);

    @Select("""
        SELECT EXISTS (SELECT 1 FROM `社区活动`
                       WHERE `发起人` = #{organizerId} AND `请求编号` = #{clientRequestId})
        """)
    boolean duplicateSubmission(@Param("organizerId") long organizerId, @Param("clientRequestId") String clientRequestId);

    @Insert("""
        INSERT INTO `社区活动` (
            `活动编号`, `发起人`, `请求编号`, `主题代码`, `标题`, `描述`, `场地编号`, `开始时间`,
            `结束时间`, `容量`, `使用音响`, `状态`, `审核方式`, `审核原因`, `审核时间`, `创建时间`, `更新时间`
        ) VALUES (
            #{id}, #{organizerId}, #{request.clientRequestId}, #{request.topicCode}, #{request.title},
            #{request.description}, #{request.venueId}, #{request.startsAt}, #{request.endsAt}, #{request.capacity},
            #{request.usesSpeaker}, #{status}, #{method,jdbcType=VARCHAR}, #{reasons}, #{reviewedAt,jdbcType=TIMESTAMP},
            #{now}, #{now}
        )
        """)
    void insert(@Param("id") String id, @Param("organizerId") long organizerId,
                @Param("request") CreateRequest request, @Param("status") ActivityStatus status,
                @Param("method") ReviewMethod method, @Param("reasons") String reasons,
                @Param("reviewedAt") Instant reviewedAt, @Param("now") Instant now);

    @Select("SELECT `场地编号` AS venue_id FROM `社区活动` WHERE `活动编号` = #{id}")
    Long location(String id);

    @Select("""
        SELECT `发起人` AS organizer_id, `主题代码` AS topic_code, `场地编号` AS venue_id, `开始时间` AS starts_at, `结束时间` AS ends_at, `容量` AS capacity,
               `使用音响` AS uses_speaker, `状态` AS status, `版本` AS version, `审核方式` AS review_method
        FROM `社区活动` WHERE `活动编号` = #{id} FOR UPDATE
        """)
    LockedActivity lockActivity(String id);

    @Select("""
        SELECT EXISTS (SELECT 1 FROM `社区活动` WHERE `场地编号` = #{venueId} AND `状态` = 'PUBLISHED'
                       AND `开始时间` < #{endsAt} AND `结束时间` > #{startsAt})
        """)
    boolean venueOccupied(@Param("venueId") long venueId, @Param("startsAt") Instant startsAt,
                          @Param("endsAt") Instant endsAt);

    @Update("""
        UPDATE `社区活动` SET `状态` = #{status}, `版本` = `版本` + 1, `审核人` = #{reviewerId},
            `审核方式` = 'MANUAL', `审核原因` = COALESCE(#{reasons,jdbcType=VARCHAR}, `审核原因`),
            `审核时间` = #{now}, `更新时间` = #{now}
        WHERE `活动编号` = #{id} AND `状态` = 'PENDING_REVIEW' AND `版本` = #{version}
        """)
    int review(@Param("id") String id, @Param("status") ActivityStatus status, @Param("version") int version,
               @Param("reviewerId") long reviewerId, @Param("reasons") String reasons, @Param("now") Instant now);

    @Update("""
        UPDATE `社区活动` SET `状态` = #{next}, `版本` = `版本` + 1, `更新时间` = #{now}
        WHERE `活动编号` = #{id} AND `状态` = #{current} AND `版本` = #{version}
        """)
    int updateState(@Param("id") String id, @Param("current") ActivityStatus current,
                    @Param("next") ActivityStatus next, @Param("version") int version, @Param("now") Instant now);

    @Select(VIEW_COLUMNS + VIEW_JOINS + " WHERE a.`活动编号` = #{id}")
    ActivityRow view(@Param("id") String id, @Param("userId") long userId);

    @Select("<script>" + VIEW_COLUMNS + VIEW_JOINS + PAGE_FILTER
        + " ORDER BY a.`开始时间`, a.`活动编号` LIMIT #{size} OFFSET #{offset}</script>")
    List<ActivityRow> list(@Param("userId") long userId, @Param("scope") String scope, @Param("now") Instant now,
                           @Param("size") int size, @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) " + VIEW_JOINS + PAGE_FILTER + "</script>")
    long count(@Param("userId") long userId, @Param("scope") String scope, @Param("now") Instant now);

    @Select("""
        SELECT `状态` AS status FROM `活动报名` WHERE `活动编号` = #{id} AND `用户编号` = #{userId} FOR UPDATE
        """)
    EnrollmentStatus lockEnrollment(@Param("id") String id, @Param("userId") long userId);

    @Select("""
        SELECT EXISTS (
            SELECT 1 FROM `活动报名` e JOIN `社区活动` a ON a.`活动编号` = e.`活动编号`
            WHERE e.`用户编号` = #{userId} AND e.`状态` = 'CONFIRMED' AND e.`活动编号` <> #{id}
            AND a.`状态` = 'PUBLISHED' AND a.`开始时间` < #{endsAt} AND a.`结束时间` > #{startsAt}
        )
        """)
    boolean enrollmentOverlaps(@Param("userId") long userId, @Param("id") String id,
                               @Param("startsAt") Instant startsAt, @Param("endsAt") Instant endsAt);

    @Select("SELECT COUNT(*) FROM `活动报名` WHERE `活动编号` = #{id} AND `状态` = 'CONFIRMED'")
    long countParticipants(String id);

    @Insert("""
        INSERT INTO `活动报名` (`活动编号`, `用户编号`, `状态`, `创建时间`, `更新时间`)
        VALUES (#{id}, #{userId}, 'CONFIRMED', #{now}, #{now})
        """)
    void insertEnrollment(@Param("id") String id, @Param("userId") long userId, @Param("now") Instant now);

    @Update("""
        UPDATE `活动报名` SET `状态` = #{next}, `更新时间` = #{now}
        WHERE `活动编号` = #{id} AND `用户编号` = #{userId} AND `状态` = #{current}
        """)
    int updateEnrollment(@Param("id") String id, @Param("userId") long userId, @Param("current") EnrollmentStatus current,
                         @Param("next") EnrollmentStatus next, @Param("now") Instant now);

    @Update("""
        UPDATE `活动报名` SET `状态` = 'CANCELLED', `更新时间` = #{now}
        WHERE `活动编号` = #{id} AND `状态` = 'CONFIRMED'
        """)
    void cancelEnrollments(@Param("id") String id, @Param("now") Instant now);

    @Select("""
        SELECT u.`昵称` AS nickname FROM `活动报名` e JOIN `用户账号` u ON u.`用户编号` = e.`用户编号`
        WHERE e.`活动编号` = #{id} AND e.`状态` = 'CONFIRMED' ORDER BY e.`用户编号`
        LIMIT #{size} OFFSET #{offset}
        """)
    List<Participant> participants(@Param("id") String id, @Param("size") int size, @Param("offset") long offset);
}
