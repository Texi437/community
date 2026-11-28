package cn.linli.workorder;

import org.apache.ibatis.annotations.*;
import java.util.List;
import static cn.linli.workorder.WorkOrderDtos.*;

@Mapper
public interface WorkOrderMapper {
    String SELECT_ORDER = """
        SELECT w.`工单编号` AS id, w.`上报人` AS reporter_id, r.`昵称` reporter_nickname, w.`处理人` AS assignee_id,
        a.`昵称` assignee_nickname, w.`标题` AS title, w.`分类` AS category, w.`位置` AS location, w.`描述` AS description,
        w.`状态` AS status, w.`版本` AS version, w.`结果` AS result, w.`创建时间` AS created_at, w.`更新时间` AS updated_at
        FROM `诉求工单` w JOIN `用户账号` r ON r.`用户编号`=w.`上报人` LEFT JOIN `用户账号` a ON a.`用户编号`=w.`处理人`
        """;
    String SCOPE = """
        <where>
          (#{role}='ADMIN' OR (#{role}='RESIDENT' AND w.`上报人`=#{userId})
          OR (#{role}='STAFF' AND w.`处理人`=#{userId}))
          <if test='status != null'>AND w.`状态`=#{status}</if>
          <if test='category != null'>AND w.`分类`=#{category}</if>
        </where>
        """;

    @Insert("""
        INSERT INTO `诉求工单`(`工单编号`,`上报人`,`请求编号`,`标题`,`分类`,`位置`,`描述`)
        VALUES(#{id},#{reporterId},#{requestId},#{data.title},#{data.category},#{data.location},#{data.description})
        """)
    void insert(@Param("id") String id, @Param("reporterId") long reporterId,
                @Param("requestId") String requestId, @Param("data") Create data);

    @Select(SELECT_ORDER + " WHERE w.`工单编号`=#{id}")
    Order find(String id);

    @Select("SELECT `工单编号` AS id FROM `诉求工单` WHERE `工单编号`=#{id} FOR UPDATE")
    String lock(String id);

    @Select("<script>" + SELECT_ORDER + SCOPE + " ORDER BY w.`创建时间` DESC,w.`工单编号` DESC LIMIT #{size} OFFSET #{offset}</script>")
    List<Order> list(@Param("role") String role, @Param("userId") long userId,
                     @Param("status") Status status, @Param("category") Category category,
                     @Param("size") int size, @Param("offset") long offset);

    @Select("<script>SELECT COUNT(*) FROM `诉求工单` w " + SCOPE + "</script>")
    long count(@Param("role") String role, @Param("userId") long userId,
               @Param("status") Status status, @Param("category") Category category);

    @Update("""
        UPDATE `诉求工单` SET `状态`=#{next},`处理人`=#{assigneeId},`结果`=#{result},
        `版本`=`版本`+1,`更新时间`=CURRENT_TIMESTAMP(6)
        WHERE `工单编号`=#{id} AND `版本`=#{version} AND `状态`=#{previous}
        """)
    int transition(@Param("id") String id, @Param("version") int version, @Param("previous") Status previous,
                   @Param("next") Status next, @Param("assigneeId") Long assigneeId, @Param("result") String result);

    @Insert("""
        INSERT INTO `工单日志`(`工单编号`,`操作人`,`操作角色`,`操作`,`原状态`,`新状态`,`备注`)
        VALUES(#{id},#{actorId},#{role},#{action},#{previous},#{next},#{note})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "key.id", keyColumn = "日志编号")
    void insertLog(@Param("id") String id, @Param("actorId") long actorId, @Param("role") String role,
             @Param("action") String action, @Param("previous") Status previous,
             @Param("next") Status next, @Param("note") String note, @Param("key") LogKey key);

    default long log(String id, long actorId, String role, String action, Status previous, Status next, String note) {
        var key = new LogKey();
        insertLog(id, actorId, role, action, previous, next, note, key);
        return key.id;
    }

    class LogKey {
        private long id;

        public void setId(long id) { this.id = id; }
    }

    @Select("""
        SELECT l.`日志编号` AS id,l.`操作人` AS actor_id,u.`昵称` actor_nickname,l.`操作角色` AS actor_role,l.`操作` AS action,l.`原状态` AS from_status,l.`新状态` AS to_status,l.`备注` AS note,l.`创建时间` AS created_at
        FROM `工单日志` l JOIN `用户账号` u ON u.`用户编号`=l.`操作人` WHERE l.`工单编号`=#{id} ORDER BY l.`日志编号`
        """)
    List<Log> logs(String id);

    @Select("SELECT `用户编号` AS id,`昵称` AS nickname FROM `用户账号` WHERE `角色`='STAFF' AND `状态`='ACTIVE' ORDER BY `用户编号`")
    List<Staff> staff();

    @Select("SELECT COUNT(*) FROM `用户账号` WHERE `用户编号`=#{id} AND `角色`='STAFF' AND `状态`='ACTIVE'")
    int activeStaff(long id);

    @Insert("INSERT INTO `文件附件`(`附件编号`,`工单编号`,`存储键`,`文件类型`,`字节数`) VALUES(#{id},#{workOrderId},#{storageKey},#{contentType},#{byteSize})")
    void insertAsset(FileAsset asset);

    @Insert("""
        INSERT INTO `办结附件` (`附件编号`, `日志编号`)
        SELECT a.`附件编号` AS id, l.`日志编号` AS id FROM `文件附件` a
        JOIN `工单日志` l ON l.`工单编号` = a.`工单编号`
        WHERE a.`附件编号` = #{assetId} AND l.`日志编号` = #{logId} AND l.`操作` = 'COMPLETE'
        """)
    int linkCompletionAsset(@Param("assetId") String assetId, @Param("logId") long logId);

    @Select("""
        SELECT m.`附件编号` AS assetId, m.`日志编号` AS logId
        FROM `办结附件` m JOIN `工单日志` l ON l.`日志编号` = m.`日志编号`
        JOIN `文件附件` a ON a.`附件编号` = m.`附件编号` AND a.`工单编号` = l.`工单编号`
        WHERE l.`工单编号` = #{id} AND l.`操作` = 'COMPLETE'
        ORDER BY l.`日志编号`, a.`创建时间`, a.`附件编号`
        """)
    List<CompletionAttachment> completionAttachments(String id);

    @Select("SELECT `附件编号` AS id,`工单编号` AS work_order_id,`存储键` AS storage_key,`文件类型` AS content_type,`字节数` AS byte_size FROM `文件附件` WHERE `工单编号`=#{id} ORDER BY `创建时间`,`附件编号`")
    List<FileAsset> assets(String id);

    @Select("SELECT `附件编号` AS id,`工单编号` AS work_order_id,`存储键` AS storage_key,`文件类型` AS content_type,`字节数` AS byte_size FROM `文件附件` WHERE `附件编号`=#{assetId} AND `工单编号`=#{id}")
    FileAsset asset(@Param("id") String id, @Param("assetId") String assetId);
}
