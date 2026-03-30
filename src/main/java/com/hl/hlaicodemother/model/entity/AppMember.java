package com.hl.hlaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用成员表 实体类。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_member")
public class AppMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用 id
     */
    @Column("appId")
    private Long appId;

    /**
     * 用户 id
     */
    @Column("userId")
    private Long userId;

    /**
     * 成员角色：owner/editor/viewer
     */
    @Column("memberRole")
    private String memberRole;

    /**
     * 成员状态：0-pending 1-active 2-removed 3-rejected
     */
    @Column("memberStatus")
    private Integer memberStatus;

    /**
     * 邀请人用户 id
     */
    @Column("invitedBy")
    private Long invitedBy;

    /**
     * 最后活跃时间
     */
    @Column("lastActiveTime")
    private LocalDateTime lastActiveTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
