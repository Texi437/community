package cn.linli.auth;

import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

import static cn.linli.auth.ProfileDtos.*;

@Mapper
public interface ProfileMapper {
    String ACTIVE_RESIDENTS = """
        FROM `用户账号` u JOIN `居民资料` p ON p.`用户编号` = u.`用户编号`
        WHERE u.`状态` = 'ACTIVE'
        """;

    @Select("""
        SELECT EXISTS (
            SELECT 1 FROM `资料授权` p JOIN `用户账号` u ON u.`用户编号` = p.`用户编号`
            WHERE u.`用户编号` = #{id} AND u.`状态` = 'ACTIVE' AND u.`角色` IN ('STAFF', 'ADMIN')
            AND p.`权限代码` = 'RESIDENT_PROFILE_READ'
            AND (p.`到期时间` IS NULL OR p.`到期时间` > #{now})
        )
        """)
    boolean canQueryResidents(@Param("id") long id, @Param("now") Instant now);

    @Select("SELECT `用户编号` AS id FROM `用户账号` WHERE `用户编号` = #{id} AND `状态` = 'ACTIVE' FOR UPDATE")
    Long lockActiveUser(long id);

    @Update("UPDATE `用户账号` SET `昵称` = #{nickname} WHERE `用户编号` = #{id}")
    void updateNickname(@Param("id") long id, @Param("nickname") String nickname);

    @Update("UPDATE `居民资料` SET `楼栋` = #{building}, `单元` = #{unit}, `门牌` = #{room} WHERE `用户编号` = #{id}")
    void updateAddress(@Param("id") long id, @Param("building") String building,
                       @Param("unit") String unit, @Param("room") String room);

    @Select("SELECT u.`用户编号` AS id, u.`昵称` AS nickname " + ACTIVE_RESIDENTS + " ORDER BY u.`用户编号` LIMIT #{size} OFFSET #{offset}")
    List<ResidentSummary> listResidents(@Param("size") int size, @Param("offset") long offset);

    @Select("SELECT COUNT(*) " + ACTIVE_RESIDENTS)
    long countResidents();

    @Select("SELECT u.`用户编号` AS id, u.`昵称` AS nickname, p.`楼栋` AS building, p.`单元` AS unit, p.`门牌` AS room " + ACTIVE_RESIDENTS + " AND u.`用户编号` = #{id}")
    ResidentDetail findResident(long id);
}
