create database if not exists hl_ai_code_mother;

use hl_ai_code_mother;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table if not exists app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               not null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey),
    INDEX idx_appName (appName),
    INDEX idx_userId (userId)
) comment '应用' collate = utf8mb4_unicode_ci;

--  应用表增加“当前版本”信息
alter table app
    add column currentVersion int default 0 not null comment '当前版本号' after deployedTime,
    add column currentVersionId bigint null comment '当前生效的版本记录 id' after currentVersion;

create index idx_currentVersion on app(currentVersion);
create index idx_currentVersionId on app(currentVersionId);

--  新增应用版本表：保存每次生成出来的历史版本
create table if not exists app_version
(
    id                 bigint auto_increment comment 'id' primary key,
    appId              bigint                                not null comment '应用 id',
    version            int          default 1                not null comment '版本号，从 1 开始递增',
    codeGenType        varchar(64)                           not null comment '代码生成类型（html / multi_file）',
    initPrompt         text                                  null comment '应用初始 prompt 快照',
    userPrompt         text                                  null comment '本次生成时用户输入的 prompt',
    versionRemark      varchar(512)                          null comment '版本备注',
    isCurrent          tinyint      default 0                not null comment '是否为当前生效版本：0-否 1-是',
    createUserId       bigint                                not null comment '创建该版本的用户 id',
    createTime         datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime         datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete           tinyint      default 0                not null comment '是否删除',

    unique key uk_appId_versionNumber (appId, version),
    index idx_appId (appId),
    index idx_current (appId, isCurrent),
    index idx_createUserId (createUserId)
) comment '应用版本表' collate = utf8mb4_unicode_ci;

-- 增加部署标识
alter table app_version
    add column deployKey varchar(64) null comment '部署标识' after versionRemark;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

