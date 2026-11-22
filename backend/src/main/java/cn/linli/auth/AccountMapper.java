package cn.linli.auth;

import org.apache.ibatis.annotations.*;

@Mapper
public interface AccountMapper {
    @Select("SELECT id, username, password_hash, nickname, role, status FROM sys_user WHERE username = #{username}")
    UserAccount findUserByUsername(String username);

    @Insert("""
        INSERT INTO sys_user (username, password_hash, nickname, role, status)
        VALUES (#{username}, #{passwordHash}, #{nickname}, 'RESIDENT', 'ACTIVE')
        """)
    void insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                    @Param("nickname") String nickname);

    @Insert("INSERT INTO resident_profile (user_id, building, unit, room) VALUES (#{id}, #{building}, #{unit}, #{room})")
    void insertResidentProfile(@Param("id") long id, @Param("building") String building,
                       @Param("unit") String unit, @Param("room") String room);

    @Insert("INSERT INTO auth_session (token_hash, user_id, expires_at_epoch) VALUES (#{hash}, #{id}, #{expiry})")
    void insertSession(@Param("hash") String hash, @Param("id") long id, @Param("expiry") long expiry);

    @Select("""
        SELECT u.id, u.username, u.nickname, u.role, s.token_hash
        FROM auth_session s JOIN sys_user u ON u.id = s.user_id
        WHERE s.token_hash = #{hash} AND s.expires_at_epoch > #{now} AND u.status = 'ACTIVE'
        """)
    AuthenticatedUser findActiveSession(@Param("hash") String hash, @Param("now") long now);

    @Select("""
        SELECT u.id, u.username, u.nickname, u.role, p.building, p.unit, p.room
        FROM sys_user u JOIN resident_profile p ON p.user_id = u.id
        WHERE u.id = #{id} AND u.status = 'ACTIVE'
        """)
    AuthDtos.MyProfile findProfileByUserId(long id);

    @Delete("DELETE FROM auth_session WHERE token_hash = #{hash}")
    void deleteSessionByTokenHash(String hash);

    @Delete("DELETE FROM auth_session WHERE user_id = #{id} AND expires_at_epoch <= #{now}")
    void deleteExpiredSessions(@Param("id") long id, @Param("now") long now);
}
