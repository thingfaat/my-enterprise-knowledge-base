# Day 06：SQL、MyBatis 与持久化分层

目标：亲手完成文档增删改查，并通过数据库查询和应用重启证明数据已经持久化；能够解释 Controller → Service → Mapper → SQL 的完整链路。

使用 JDK 17、Boot 3.2.0、MyBatis Starter 3.0.3、现有 MySQL 8.0 容器。预计 4 小时：回顾 15 分钟，SQL/原理 45 分钟，环境与源码 60 分钟，动手 90 分钟，验收 30 分钟。首次数据库配置超过预算时可拆为 Day 06A 环境/SQL、06B CRUD，两个部分都在同一当天模块，不跳过验收。

核验边界：Day 05 源码和 middleware 配置已读；Docker 实测 mysql8 已停止，未登录数据库、未创建库/用户、未运行本课示例。备课时已在临时目录使用本地缓存依赖完成七个 Java 文件的 javac --release 17 编译检查；这不是 Maven 依赖解析、Spring 启动或数据库验收。下文所有运行结果均为待验证预期。

## 原理

### 数据真正保存在哪里

Day 04 的 Map 属于 Java 进程，停止应用会丢失；今天数据写进 MySQL 的表，应用退出不删除数据库行。已有 Docker 挂载让 MySQL 数据文件保存在 middleware/data/mysql，但这不等于已验证容器重建或磁盘故障恢复；本课只验证 Java 应用重启后数据仍在。

表是一组列定义和多行记录。主键 id 唯一标识一行，由 AUTO_INCREMENT 生成；不要假设自增 ID 永远连续或下一条一定为 1。title 为 VARCHAR(100)，content 为 MEDIUMTEXT，NOT NULL 防止 SQL NULL；空字符串与 NULL 不同。utf8mb4 支持 emoji 等字符。

| SQL 操作 | 今天的作用 | 必须理解的返回值 |
|---|---|---|
| INSERT INTO ... VALUES ... | 创建一行 | 行数与生成主键是两回事 |
| SELECT ... WHERE id = ... | 按主键查询 | 找不到是无结果，不是数据库连接失败 |
| UPDATE ... WHERE id = ... | 替换指定行内容 | 本课设置返回匹配行数；同值更新仍成功 |
| DELETE ... WHERE id = ... | 物理删除指定行 | 0 表示本次未删到；没有 WHERE 会扩大影响范围 |

执行前先读 SQL 的 WHERE。只有明确知道目标学习库、表和 ID 才运行写入；本课脚本不包含 DROP/TRUNCATE，也不初始化参考业务表。

### MyBatis 没有替你设计 SQL

Mapper 是接口，MyBatis 为它创建代理；调用方法时找到对应 SQL，绑定参数、经 JDBC 执行，再把结果映射到 Entity。连接来自 DataSource（本例默认连接池），不是每个 Controller 手动维护数据库连接。

今天固定 Starter 3.0.3，其发布说明明确切换到 Boot 3.2.x 基线；不要复制官网当前 4.x 安装示例到本课。[3.0.3 发布说明](https://github.com/mybatis/spring-boot-starter/releases/tag/mybatis-spring-boot-3.0.3)。Starter 支持根据 DataSource 建立 MyBatis 基础组件并扫描 Mapper。[官方 Starter 说明](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)。

XML 的 namespace 对应 Mapper 全限定名，statement id 对应方法名，mapper-locations 决定从哪找 XML。`#{...}` 生成参数占位绑定；`${...}` 是文本替换，不能拼接不可信输入。生成键用 useGeneratedKeys/keyProperty 回填到对象。[MyBatis XML 说明](https://mybatis.org/mybatis-3/sqlmap-xml.html)。

本例直接使用原生 MyBatis，参考工程使用 MyBatis-Plus 的 BaseMapper 和自定义 XML。先理解 SQL 与映射，再学增强封装；不要同时添加两个 Starter。本课每次写操作只有一条修改 SQL，没有实现多语句事务；MyBatis-Spring 在 Spring 事务之外的 Mapper 数据操作会提交。多步骤原子性和回滚留到 Day 08。[MyBatis-Spring 事务说明](https://mybatis.org/spring/transactions.html)。

### DTO、Entity、响应对象各自负责什么

DocumentWriteRequest 表示 HTTP 输入，DocumentEntity 表示数据库行，DocumentResponse 表示输出。即使今天字段相似也明确转换，未来数据库增加内部字段不自动暴露给客户端。ID 由数据库生成；更新目标 ID 来自 URL。

GET 是读取，POST 创建返回 201/Location，PUT 整体替换返回 204，DELETE 成功返回 204。本例 PUT 缺正文表示替换为空，不是局部更新；不存在返回 404。数据库故障不能伪装成空列表或 404。

## 现有数据流

### 自己的项目

Day 05 当前实现是 YAML/Profile/环境变量 → PreviewProperties → PreviewService → 预览响应，带 requestId 日志。源码的绑定、范围检查、码点截断和日志主体与教案一致，尚未加入独立 uppercase 配置；学习记录为空，本次不把运行验收和掌握状态标为通过。

Day 04 是 Controller → Service → Map 的内存文档接口；上一轮 Location 与正文行为反馈继续保留，本课不修改旧模块。Day 06 尚不存在，没有现成 DataSource、Mapper 或数据库表可以假定使用。

### 参考实现与差异

参考根：`/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend/kb-document`。

- `src/main/java/com/knowledge/base/document/controller/CategoryController.java`：创建/查询入口委托 CategoryService，输入 CategoryDTO、输出 Result 包装。
- `src/main/java/com/knowledge/base/document/service/impl/CategoryServiceImpl.java`：创建时检查名称/父分类，构造 Category，再调用 Mapper insert；查询后转换 CategoryVO。
- `src/main/java/com/knowledge/base/document/mapper/CategoryMapper.java`：继承 BaseMapper，并声明自定义方法。
- `src/main/java/com/knowledge/base/document/entity/Category.java`：对应 kb_category，存在 Java 属性与数据库列名映射。
- `src/main/resources/mapper/CategoryMapper.xml`：namespace 对应接口；resultMap 映射列；selectByParentId 等 SQL 使用参数绑定，部分查询筛选 deleted=0。

以上是源码证据，未启动参考服务。本课采用独立简表、自增 ID、物理删除，无分类树/逻辑删除/权限，不宣称已实现参考文档或分类模块。

### 已检查的 MySQL 环境

`/Users/hingfaattam/projects/trae_projects/middleware/docker-compose.yml` 的 mysql 服务声明 mysql:8.0、容器名 mysql8、3306:3306、./data/mysql:/var/lib/mysql。只读 docker ps 未发现运行容器；docker ps -a 查到 mysql8 为 Exited (0)。尚未核验精确服务端版本、账号权限、已有学习库或 SQL 模式。

今天先由你启动现有容器并实际登录确认。不要重装 MySQL，不复制参考工程密码，不把 middleware/.env 的内容写入本仓库。

## 本次需要改动的数据流

启动：环境变量提供账号 → Boot 创建 DataSource → MyBatis 注册 Mapper 代理/加载 XML → Service 构造器注入 Mapper。

创建：HTTP JSON → 输入 DTO → Service 检查/转换 Entity → Mapper.insert → 参数化 INSERT → MySQL 写行并生成 ID → 回填 entity.id → 响应 DTO → 201/Location。

查询：路径/查询参数 → Service → Mapper SELECT → resultMap 生成 Entity → 输出 DTO；单行不存在返回 404，列表无匹配返回 []。搜索使用 LOCATE，空关键词匹配全部；大小写比较受 MySQL 排序规则影响，不再沿用 Java contains 的大小写承诺。最多前 100 条不是完整分页。

更新/删除：只修改指定 ID，依据单条语句匹配/删除行数判断存在性；更新成功和删除成功返回 204。禁止以“先 SELECT 再 UPDATE”假装解决并发；本课也未提供版本锁，覆盖更新的并发控制后续再学。

不涉及登录、权限、缓存、消息、前端或跨服务调用。真正的持久化新增在 Mapper/SQL/MySQL 这一段；没有把 Day 04 的 Map 搬进新模块。

## 文件位置（复用/新增/修改）

仓库根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`。当天模块根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud`。

整个仓库规划（Day 01–05 已存在，Day 06 今天由你创建）：

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── learning/
├── day-01-spring-boot-hello/
├── day-02-spring-di/
├── day-03-boot-auto-config/
├── day-04-spring-mvc/
├── day-05-config-logging/
├── day-06-mybatis-crud/
├── day-11-cloud-lab/             # 后续连续实验
│   ├── user-service/
│   ├── document-service/
│   └── gateway-service/
├── knowledge-base-backend/       # 后续正式后端
└── knowledge-base-frontend/      # 后续正式前端
```

今天的完整结构：

```text
my-enterprise-knowledge-base/
└── day-06-mybatis-crud/
    ├── pom.xml
    ├── db/init.sql
    └── src/main/
        ├── resources/
        │   ├── application.yml
        │   └── mapper/DocumentMapper.xml
        └── java/com/thingfaat/knowledge/
            ├── Day06MybatisCrudApplication.java
            ├── controller/DocumentController.java
            ├── service/DocumentService.java
            ├── mapper/DocumentMapper.java
            ├── entity/DocumentEntity.java
            └── dto/
                ├── DocumentWriteRequest.java
                └── DocumentResponse.java
```

独立 Maven 应用；无跨日应用 JAR 依赖。只复用前几课的骨架/分层思路，以下十一文件全部新增。运行依赖是外部现有 MySQL 和专属 kb_learning_day06 库。根目录不创建 POM/src；旧课程、参考工程和 middleware 配置本次均不修改。

| 绝对保存位置 | 操作/所属 | 职责 |
|---|---|---|
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/pom.xml` | 新增 / Day 06 | Web/MyBatis/驱动依赖 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/resources/application.yml` | 新增 / Day 06 | 连接/Mapper 配置 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/db/init.sql` | 新增 / Day 06 | 独立学习库和表 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/Day06MybatisCrudApplication.java` | 新增 / Day 06 | 启动扫描 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/entity/DocumentEntity.java` | 新增 / Day 06 | 数据库行与主键回填 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentWriteRequest.java` | 新增 / Day 06 | HTTP 写入输入 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java` | 新增 / Day 06 | HTTP 输出 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/mapper/DocumentMapper.java` | 新增 / Day 06 | Mapper 代理接口 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/resources/mapper/DocumentMapper.xml` | 新增 / Day 06 | SQL 与结果映射 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/service/DocumentService.java` | 新增 / Day 06 | 校验、数据库调用与转换 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java` | 新增 / Day 06 | HTTP 路由与状态码 |

## 基于现有代码的完整增量代码

先自己画出创建与查询链，再对照完整代码。先保存 SQL，其后按第六节启动现有容器、确认数据库和建表，再运行应用。

```bash
mkdir -p day-06-mybatis-crud/db
mkdir -p day-06-mybatis-crud/src/main/resources/mapper
mkdir -p day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/{controller,service,mapper,entity,dto}
```

### day-06-mybatis-crud/pom.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- parent 提供版本管理和构建默认值；实际依赖仍由 dependencies 声明。 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.thingfaat</groupId>
    <artifactId>day-06-mybatis-crud</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <!-- 编译目标为 17；实际运行 Maven 的 JDK 用 mvn -version 查看。 -->
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 本阶段通过 Starter 选择依赖集合，具体配置仍需满足运行条件。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- 本课学习原生 MyBatis；不要同时引入 MyBatis-Plus Starter。 -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>3.0.3</version>
        </dependency>
        <!-- 驱动版本由 Boot 3.2.0 管理；运行时需要。 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 将应用及其依赖打包为 java -jar 可启动的归档。 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### day-06-mybatis-crud/src/main/resources/application.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: day-06-mybatis-crud
  datasource:
    # 仅用于已确认的本机学习实例；正式环境不能照搬关闭 TLS 的设置。
    # useAffectedRows=false：UPDATE 返回匹配行数，同值更新也按“存在”处理。
    url: "jdbc:mysql://127.0.0.1:3306/kb_learning_day06?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useAffectedRows=false"
    username: "${KB_DB_USERNAME}"
    password: "${KB_DB_PASSWORD}"
    hikari:
      maximum-pool-size: 3
      connection-timeout: 5000
  sql:
    init:
      # 建表由你显式执行 db/init.sql，不让启动过程隐式改变数据库。
      mode: never
server:
  address: 127.0.0.1
  port: 18088
mybatis:
  mapper-locations: classpath:/mapper/*.xml
logging:
  level:
    root: INFO
    com.thingfaat.knowledge: INFO
    # SQL DEBUG 可能输出参数和正文；本课通过数据库 SELECT 交叉验证。
```

### day-06-mybatis-crud/db/init.sql

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/db/init.sql`

```sql
-- 只操作本课独立库；重复执行不会删除或清空数据。
CREATE DATABASE IF NOT EXISTS kb_learning_day06
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE kb_learning_day06;
CREATE TABLE IF NOT EXISTS learning_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据库生成的主键',
    title VARCHAR(100) NOT NULL COMMENT '文档标题',
    content MEDIUMTEXT NOT NULL COMMENT '文档正文',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- IF NOT EXISTS 不会修复已有表的结构；必须另行核对 SHOW CREATE TABLE。
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/Day06MybatisCrudApplication.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/Day06MybatisCrudApplication.java`

```java
package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 当天独立应用；Starter 在扫描范围内发现带 @Mapper 的接口。 */
@SpringBootApplication
public class Day06MybatisCrudApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day06MybatisCrudApplication.class, args);
    }
}
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/entity/DocumentEntity.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/entity/DocumentEntity.java`

```java
package com.thingfaat.knowledge.entity;

/**
 * 数据库行的 Java 表示。可变 ID 用于接收数据库生成的主键回填。
 * 显式提供无参构造器和访问器，避免此时引入 Lombok 或构造器映射知识。
 */
public class DocumentEntity {
    private Long id;
    private String title;
    private String content;

    public DocumentEntity() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentWriteRequest.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentWriteRequest.java`

```java
package com.thingfaat.knowledge.dto;

/** 创建/整体更新的输入；ID 来自数据库或 URL，不由 JSON 决定。 */
public record DocumentWriteRequest(String title, String content) {
}
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java`

```java
package com.thingfaat.knowledge.dto;

/** HTTP 输出，与持久化 Entity 分开；以后表增加内部字段不自动暴露给客户端。 */
public record DocumentResponse(Long id, String title, String content) {
}
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/mapper/DocumentMapper.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/mapper/DocumentMapper.java`

```java
package com.thingfaat.knowledge.mapper;

import com.thingfaat.knowledge.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 接口由 MyBatis 生成代理；方法名与 XML statement id 一一对应。 */
@Mapper
public interface DocumentMapper {
    // 返回插入行数；新 ID 由 XML 的 keyProperty 回填到 entity.id。
    int insert(DocumentEntity entity);
    DocumentEntity findById(@Param("id") Long id);
    List<DocumentEntity> search(@Param("keyword") String keyword);
    int update(DocumentEntity entity);
    int deleteById(@Param("id") Long id);
}
```

### day-06-mybatis-crud/src/main/resources/mapper/DocumentMapper.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/resources/mapper/DocumentMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.thingfaat.knowledge.mapper.DocumentMapper">
    <!-- 显式列出字段映射，便于解释行如何变成 Entity。 -->
    <resultMap id="documentMap" type="com.thingfaat.knowledge.entity.DocumentEntity">
        <id column="id" property="id"/>
        <result column="title" property="title"/>
        <result column="content" property="content"/>
    </resultMap>

    <!-- #{...} 绑定参数值，不做 SQL 文本拼接。 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
        INSERT INTO learning_document (title, content) VALUES (#{title}, #{content})
    </insert>
    <select id="findById" resultMap="documentMap">
        SELECT id, title, content FROM learning_document WHERE id = #{id}
    </select>
    <select id="search" resultMap="documentMap">
        <!-- LOCATE 做包含查询，避免把用户输入中的 %/_ 当成 LIKE 通配符。
             空关键词返回全部；只返回前 100 条，完整分页留到 Day 09。 -->
        SELECT id, title, content FROM learning_document
        WHERE LOCATE(#{keyword}, title) > 0
        ORDER BY id ASC LIMIT 100
    </select>
    <update id="update">
        UPDATE learning_document SET title = #{title}, content = #{content} WHERE id = #{id}
    </update>
    <delete id="deleteById">
        DELETE FROM learning_document WHERE id = #{id}
    </delete>
</mapper>
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/service/DocumentService.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/service/DocumentService.java`

```java
package com.thingfaat.knowledge.service;

import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.dto.DocumentWriteRequest;
import com.thingfaat.knowledge.entity.DocumentEntity;
import com.thingfaat.knowledge.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 组织输入检查、Mapper 调用与结果转换；不再持有 Map 或 AtomicLong。 */
@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentMapper mapper;

    public DocumentService(DocumentMapper mapper) { this.mapper = mapper; }

    /** 数据库生成 ID 并回填；insert 的返回值不是 ID。 */
    public DocumentResponse create(DocumentWriteRequest request) {
        DocumentEntity entity = prepare(request);
        if (mapper.insert(entity) != 1 || entity.getId() == null) {
            throw new IllegalStateException("Insert did not return expected row/key");
        }
        log.info("document created id={}", entity.getId());
        return toResponse(entity);
    }

    /** 单行查询无结果时，MyBatis 返回 null。 */
    public DocumentResponse detail(Long id) {
        DocumentEntity entity = mapper.findById(id);
        if (entity == null) { throw notFound(); }
        return toResponse(entity);
    }

    /** 仅实现标题包含查询，排序和最多 100 条限制位于 SQL。 */
    public List<DocumentResponse> search(String keyword) {
        return mapper.search(keyword.strip()).stream().map(this::toResponse).toList();
    }

    /** PUT 整体替换标题/正文；缺正文表示替换为空字符串，不是“保持原值”。 */
    public void update(Long id, DocumentWriteRequest request) {
        DocumentEntity entity = prepare(request);
        entity.setId(id);
        // 依赖本课 useAffectedRows=false 的匹配行数语义；同值更新仍应成功。
        if (mapper.update(entity) == 0) { throw notFound(); }
        log.info("document updated id={}", id);
    }

    /** 物理删除只作用于指定 ID；不先查再删，直接判断语句结果。 */
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) { throw notFound(); }
        log.info("document deleted id={}", id);
    }

    private DocumentEntity prepare(DocumentWriteRequest request) {
        String title = request.title() == null ? "" : request.title().strip();
        String content = request.content() == null ? "" : request.content();
        // 用码点数限制输入，正文保留空白；详细注解校验留到 Day 07。
        if (title.isBlank() || title.codePointCount(0, title.length()) > 100
                || content.codePointCount(0, content.length()) > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title or content length");
        }
        DocumentEntity entity = new DocumentEntity();
        entity.setTitle(title);
        entity.setContent(content);
        return entity;
    }

    private DocumentResponse toResponse(DocumentEntity entity) {
        return new DocumentResponse(entity.getId(), entity.getTitle(), entity.getContent());
    }

    private ResponseStatusException notFound() {
        // 暂保留上一课 HTTP 异常写法；Day 07 再统一拆分异常边界。
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
}
```

### day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java`

```java
package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.dto.DocumentWriteRequest;
import com.thingfaat.knowledge.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** HTTP 适配层，不在 Controller 写 SQL；复习 Day 04 参数绑定。 */
@RestController
@RequestMapping(value = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentController {
    private final DocumentService service;
    public DocumentController(DocumentService service) { this.service = service; }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> create(@RequestBody DocumentWriteRequest request) {
        DocumentResponse response = service.create(request);
        // Location 包含正确的路径分隔符，可直接用于后续详情查询。
        return ResponseEntity.created(URI.create("/api/documents/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public DocumentResponse detail(@PathVariable("id") Long id) { return service.detail(id); }

    @GetMapping
    public List<DocumentResponse> search(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return service.search(keyword);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody DocumentWriteRequest request) {
        service.update(id, request);
        // 写入成功返回 204，无响应正文；用 GET 再核对数据库结果。
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 测试验收

### 1. 启动并确认现有实例

先创建本课文件，再执行。这里只启动 mysql 服务，不重建、不清空数据目录，不启动整套中间件：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
docker compose -f /Users/hingfaattam/projects/trae_projects/middleware/docker-compose.yml up -d mysql
docker ps --filter name=mysql8 --format '{{.Names}} {{.Status}} {{.Ports}}'
docker exec -it mysql8 mysql -uroot -p
```

密码在交互提示中输入现有管理员密码；不要在命令行后拼接密码。管理员账号只用于本节建库/授权，应用使用专用账号。若不知道密码，先确认自己已有的 middleware 管理配置，不猜测或重置数据库。

在 MySQL 控制台执行以下只读核对（交互 SQL，不保存文件）：

```sql
SELECT VERSION(), @@port, @@sql_mode;
SHOW DATABASES LIKE 'kb_learning_day06';
```

预期 MySQL 8.0，服务端内部端口 3306；严格模式包含 STRICT_TRANS_TABLES 或 STRICT_ALL_TABLES。Docker 发布端口单独由 docker ps 核对。若该库已有其他用途，先停止初始化并确认归属，不混用；本课约定这是你的专属练习库。服务端版本、账号或端口不符时先解决环境，不把猜测写进验收结果。

退出控制台，复制已保存的 SQL（不会自动执行），再登录执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
docker cp /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud/db/init.sql mysql8:/tmp/day06-init.sql
docker exec -it mysql8 mysql -uroot -p
```

MySQL 控制台执行：

```sql
SOURCE /tmp/day06-init.sql;
SHOW CREATE TABLE kb_learning_day06.learning_document;
```

核对 id/title/content、主键、字符集和表引擎；IF NOT EXISTS 不会迁移旧表结构。不要把“执行没报错”当成表结构一定正确。

### 2. 建立专用账号并配置进程环境

以下仅在管理员交互控制台执行，**不要保存真实密码到脚本**。先检查账号是否已经存在：

```sql
SELECT User, Host FROM mysql.user WHERE User = 'kb_day06';
```

新建时将下面占位符换成你自己管理的密码；如果已有该账号，先核对用途和权限，不覆盖密码、不再 CREATE。`%` 用于宿主机经 Docker 网络连接，权限仅限本课库；这不是正式部署的通用授权模板。

```sql
CREATE USER 'kb_day06'@'%' IDENTIFIED BY 'REPLACE_WITH_YOUR_OWN_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON kb_learning_day06.* TO 'kb_day06'@'%';
SHOW GRANTS FOR 'kb_day06'@'%';
```

应用账号无建表权限，建表保持显式管理。退出后用专用账号登录，确认能 SELECT：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
docker exec -it mysql8 mysql -ukb_day06 -p kb_learning_day06
```

在控制台执行 SELECT COUNT(*) FROM learning_document;，然后退出。容器内登录不等于宿主机 JDBC 已通过，后续 HTTP 才验证完整连接链。

当前环境是 zsh，以下交互隐藏密码输入；变量只存在当前终端进程环境，不写入 Git。不要开启 shell 调试跟踪或输出变量值：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
export KB_DB_USERNAME=kb_day06
read -s "KB_DB_PASSWORD?请输入 Day 06 数据库密码: "
export KB_DB_PASSWORD
```

IDEA 启动时需要在该运行配置中设置同名环境变量，不默认继承另一终端的 export。不要把包含真实值的共享 Run Configuration 提交到 Git。

### 3. 构建并启动

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
java -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
java -jar target/day-06-mybatis-crud-0.0.1-SNAPSHOT.jar
```

预期使用 JDK 17、BUILD SUCCESS、监听 18088。构建无数据库验收测试；某些连接错误可能到首次 SQL 才暴露，不能只看 Tomcat 启动行。启动成功后另开终端进行以下操作。

### 4. 创建 → 查询 → 修改 → 重启 → 删除

所有 curl 都只操作本课应用与自己创建的测试行。用实际响应 ID，不假定一定为 1：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
curl -i -X POST http://127.0.0.1:18088/api/documents   -H 'Content-Type: application/json'   -d '{"title":"MyBatis lesson","content":"数据库持久化 😀"}'
# 根据上一条响应填写；这里只设置普通测试 ID，不是密码。
read "DAY06_DOC_ID?请输入刚创建的文档 ID: "
curl -i "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"
curl -i --get http://127.0.0.1:18088/api/documents --data-urlencode 'keyword=MyBatis'
curl -i -X PUT "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"   -H 'Content-Type: application/json' -d '{"title":"Updated lesson","content":"更新后的正文"}'
curl -i -X PUT "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"   -H 'Content-Type: application/json' -d '{"title":"Updated lesson","content":"更新后的正文"}'
curl -i "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"
```

预期 POST 201 和正确 Location；详情/搜索 200；两次 PUT 均 204，即使第二次写相同值。读取后 title/content 已更新。若同值更新 404，先查 useAffectedRows、实际 JDBC URL/驱动语义，不以数据没变化等同不存在。

通过专用 MySQL 账号执行 `SELECT id, title, content FROM learning_document ORDER BY id;`，找到实际 ID，与 HTTP 对照。设置断点观察 insert 返回行数与 entity.id 回填；两者可能恰好都为 1，但含义不同。

只停止 Java 应用，保持 MySQL 运行；用同一环境变量重启同一 JAR，再 GET 此 ID，预期仍为 200 和更新后的内容。与 Day 04 重启清空 Map 的结果比较。最后删除：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
curl -i -X DELETE "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"
curl -i "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"
curl -i -X DELETE "http://127.0.0.1:18088/api/documents/$DAY06_DOC_ID"
```

预期依次 204、404、404；数据库 SELECT 查不到该行。更换终端会丢失 DAY06_DOC_ID，先重新设置，不能拿空变量路径做验收。保留其他练习数据，不执行全表 DELETE。

### 5. 边界与失败

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
curl -i http://127.0.0.1:18088/api/documents/abc
curl -i --get http://127.0.0.1:18088/api/documents --data-urlencode 'keyword=NO_MATCH_DAY06'
curl -i -X POST http://127.0.0.1:18088/api/documents -H 'Content-Type: application/json' -d '{"title":" "}'
curl -i -X POST http://127.0.0.1:18088/api/documents -H 'Content-Type: application/json' -d '{"title":"No content"}'
curl -i --get http://127.0.0.1:18088/api/documents --data-urlencode "keyword=' OR 1=1 --"
```

依次预期：400（类型转换）、200/[]（确认此前没有该标题）、400（空标题）、201/content 空字符串、把输入当普通搜索值而非返回所有行。最后一条是参数绑定实验，不代表完成全套安全测试。另自行构造 101 个 ASCII 字符标题，预期 400；100 个允许。对不存在的 ID 执行 PUT/DELETE 预期 404；不要使用他人数据 ID。

数据库故障实验不要停止共享 MySQL。停止自己的应用后，用不存在的**本课配置库名**覆盖 JDBC URL，发起一次列表请求观察错误：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-06-mybatis-crud
java -jar target/day-06-mybatis-crud-0.0.1-SNAPSHOT.jar '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/kb_day06_missing_demo?useSSL=false&allowPublicKeyRetrieval=true&useAffectedRows=false' 
```

这个名字应先确认不是现有可用库。根据账号权限，可能是 Unknown database 或 Access denied；错误可能发生于启动或首次请求。记录实际异常链，不把 500/连接异常伪装成 404 或 []。移除覆盖参数，恢复正常 URL，重新启动并验证 200。只记录错误类型，不复制密码或整段连接凭据。

常见定位：缺 Mapper Bean → 扫描范围/@Mapper；Invalid bound statement → namespace/id/XML 路径；Table doesn't exist → 库与建表；Access denied → 用户/来源/权限；Communications link failure → 容器/端口；字符异常 → 连接与表字符集。按证据定位，不盲目重装中间件。

### 6. 验收记录与独立实现

| 检查项 | 通过证据 | 备课实际状态 |
|---|---|---|
| 环境 | 精确 MySQL 版本、端口、SQL 模式、专属库/授权 | 仅已确认停止的 mysql8 与 Compose 声明 |
| 构建/CRUD | 201/200/204/404 和同值更新结果 | 未运行 |
| 持久化 | HTTP 与数据库行对应；Java 重启后仍存在 | 未运行 |
| 参数/边界 | 空标题、过长、无匹配、类型错误与参数绑定 | 未运行 |
| 故障恢复 | 记录连接错误、恢复正确配置后 200 | 未运行 |
| 独立变化 | 正文包含筛选并与标题条件同时生效 | 待本人实现 |

独立变化：新增 contentKeyword，从 Controller 传到 Service/Mapper/XML，用参数绑定实现标题和正文同时满足；空字符串不缩小结果。不能先查全表再用 Java 过滤。验证默认参数、只按正文匹配、双条件无匹配三例，解释 XML 中两个参数如何对应 @Param。

完成后在当前终端执行 unset KB_DB_PASSWORD，保留未解决事项。仅通过本课不代表 Boot 阶段验收通过；Day 07 校验、Day 08 事务与 Day 10 综合练习仍需要完成。

## 面试追问

1. **Mapper 没有实现类为什么能注入？** Starter/扫描注册代理，方法对应 XML statement；用 DocumentMapper 与 namespace/id 展示证据。
2. **插入返回 1 就是 ID=1 吗？** 是行数；主键通过 generated keys 回填。断点分别看返回值和 entity.id。
3. **为什么不用 ${title}？** 它是 SQL 文本替换；值应使用参数绑定。动态表名等不能靠值占位，需要受控设计，本课不做。
4. **Entity 与 DTO 字段一样，为何分开？** 目的不同，输入不接受服务端 ID，输出不随数据库内部字段扩展；定位 prepare/toResponse。
5. **同值 UPDATE 为什么不应该返回不存在？** 匹配行与实际改变行不同；本课显式设置 useAffectedRows=false，并用连续相同 PUT 验证。
6. **应用重启后还能读取，证明了什么？** 数据不只在 Java 内存里；不等于已证明备份恢复或数据库故障容错。
7. **没有 @Transactional 是否代表不写入？** 本例 Spring 事务之外的单条 Mapper 写入会提交；多步操作仍不具备共同回滚保证，留到事务课实际验证。
8. **列表无结果与数据库宕机应相同吗？** 前者正常空数组，后者基础设施错误；不能吞异常返回空列表。
9. **今天能处理多少数据与并发？** 列表仅前 100 条、无分页/乐观锁、包含搜索可能扫描大量行；明确学习边界，不能声称生产实现已完成。
