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
 * 应用版本表 实体类。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app_version")
public class AppVersion implements Serializable {

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
     * 版本号，从 1 开始递增
     */
    private Integer version;

    /**
     * 代码生成类型（html / multi_file）
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 应用初始 prompt 快照
     */
    @Column("initPrompt")
    private String initPrompt;

    /**
     * 本次生成时用户输入的 prompt
     */
    @Column("userPrompt")
    private String userPrompt;

    /**
     * 版本备注
     */
    @Column("versionRemark")
    private String versionRemark;

    /**
     * 是否为当前生效版本：0-否 1-是
     */
    @Column("isCurrent")
    private Integer isCurrent;

    /**
     * 创建该版本的用户 id
     */
    @Column("createUserId")
    private Long createUserId;

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
