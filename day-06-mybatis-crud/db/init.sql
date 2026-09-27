-- 只操作本课独立库；重复执行不会删除或清空数据。
create database if not exists kb_learning_day06 character set utf8mb4 collate utf8mb4_unicode_ci;

use kb_learning_day06;

create table if not exists learning_document
(
    id      bigint       not null auto_increment comment '数据库生成的主键',
    title   varchar(100) not null comment '文档标题',
    content mediumtext   not null comment '文档内容',
    primary key (id)
) engine = innodb
  default charset = utf8mb4
  collate = utf8mb4_unicode_ci comment ='文档表';