package cn.linli.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {
    @Select("SELECT `用户编号` AS id, `用户名` AS username, `密码摘要` AS password_hash, `昵称` AS nickname, `角色` AS role, `状态` AS status FROM `用户账号` WHERE `用户名` = #{username}")
    UserAccount findUserByUsername(String username);

    @Insert("""
        INSERT INTO `用户账号` (`用户名`, `密码摘要`, `昵称`, `角色`, `状态`)
        VALUES (#{username}, #{passwordHash}, #{nickname}, 'RESIDENT', 'ACTIVE')
        """)
    void insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                    @Param("nickname") String nickname);

    @Insert("INSERT INTO `居民资料` (`用户编号`, `楼栋`, `单元`, `门牌`) VALUES (#{id}, #{building}, #{unit}, #{room})")
    void insertResidentProfile(@Param("id") long id, @Param("building") String building,
                       @Param("unit") String unit, @Param("room") String room);

    @Insert("INSERT INTO `登录会话` (`凭证摘要`, `用户编号`, `到期秒数`) VALUES (#{hash}, #{id}, #{expiry})")
    void insertSession(@Param("hash") String hash, @Param("id") long id, @Param("expiry") long expiry);

    @Select("""
        SELECT u.`用户编号` AS id, u.`用户名` AS username, u.`昵称` AS nickname, u.`角色` AS role, s.`凭证摘要` AS token_hash
        FROM `登录会话` s JOIN `用户账号` u ON u.`用户编号` = s.`用户编号`
        WHERE s.`凭证摘要` = #{hash} AND s.`到期秒数` > #{now} AND u.`状态` = 'ACTIVE'
        """)
    AuthenticatedUser findActiveSession(@Param("hash") String hash, @Param("now") long now);

    @Select("""
        SELECT u.`用户编号` AS id, u.`用户名` AS username, u.`昵称` AS nickname, u.`角色` AS role, p.`楼栋` AS building, p.`单元` AS unit, p.`门牌` AS room
        FROM `用户账号` u JOIN `居民资料` p ON p.`用户编号` = u.`用户编号`
        WHERE u.`用户编号` = #{id} AND u.`状态` = 'ACTIVE'
        """)
    AuthDtos.MyProfile findProfileByUserId(long id);

    @Delete("DELETE FROM `登录会话` WHERE `凭证摘要` = #{hash}")
    void deleteSessionByTokenHash(String hash);

    @Delete("DELETE FROM `登录会话` WHERE `用户编号` = #{id} AND `到期秒数` <= #{now}")
    void deleteExpiredSessions(@Param("id") long id, @Param("now") long now);
}
