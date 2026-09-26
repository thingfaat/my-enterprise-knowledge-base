# Day 05：配置、Profile 与日志

目标：同一个 JAR 使用不同配置运行，能拿出实际生效值和请求日志解释行为，定位一次配置错误。每天 4 小时：回顾 15 分钟、原理 45 分钟、源码追踪 60 分钟、动手 90 分钟、验收记录 30 分钟。

前置：Day 02 构造器注入、Day 03 自动配置、Day 04 请求绑定。JDK 17、Spring Boot 3.2.0。当天模块尚未创建；以下示例和实验尚未运行，表中是预期，不是验收结果。

## 原理

### 同一份程序，配置决定不同的行为

把业务限制写成 Java 常量，每次调整都要改代码。今天把“预览最大长度”移到配置，再注入 Service。先预测：同一请求，在最大长度为 8 和 4 时分别得到什么？代码无需改变，启动时提供的配置决定结果。

本课只比较几种常用来源：命令行参数优先于环境变量，环境变量优先于打包的 YAML；同一位置的 Profile 文件覆盖基础文件的同名键。其他来源及外置配置文件还会影响结果，不能把本课顺序当作完整规则。配置绑定和 Profile 的详细依据见 [Boot 3.2.0 官方配置文档](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/features.html#features.external-config)。

`application.yml` 是公共配置，`application-local.yml` 和 `application-test.yml` 提供差异。通过 `--spring.profiles.active=local` 激活；仅存在文件不等于已经激活。这里 local/test 是自己起的名字，和 Maven Profile 不是一回事。未指定时使用默认 Profile；本课没有 application-default.yml，因此使用基础配置。今天只激活一个 Profile。

### 配置如何进入 Java

`@ConfigurationProperties(prefix="app.preview")` 把一组键绑定为对象，启动类的 `@EnableConfigurationProperties` 注册它，Service 用构造器获取。`max-length` 对应 Java 的 maxLength；本课用 record 的单构造器绑定，不需要 setter。单个值也可用 @Value，但相关配置集中管理更便于检查。

`APP_PREVIEW_MAXLENGTH` 是 app.preview.max-length 对应的环境变量名：点换下划线、连字符去掉、转大写。`KB_LABEL` 则是我们通过 `${KB_LABEL:local}` 显式引用的自定义变量，两种机制不能混淆。所有演示值都不是凭据。

改源码目录的 YAML 后，使用旧 JAR 不会自动得到新值，需重新打包；改启动参数或该进程的环境变量后重新启动即可。本课没有热更新、Nacos 或配置刷新机制。

### 日志提供什么证据

使用 SLF4J 的 Logger/LoggerFactory，默认 Starter 带有日志实现；无需另加 Logback 版本。业务类的 Logger 名是类全名，可用包名设置级别。本课 local 设 DEBUG，test 设 INFO；INFO 下不会输出 DEBUG，WARN/ERROR 仍可输出。`--debug` 的 Boot 调试报告不是“所有业务 Logger 都改为 DEBUG”。

用 `{}` 参数占位记录字段，成功链路用 INFO，详细分支用 DEBUG，可预期的空输入拒绝用 WARN。本例无需凭空制造 ERROR；真正异常诊断需要保留异常堆栈，同时避免同一错误重复记录。日志只记录请求 ID、长度和分支，不输出正文、完整请求或所有环境变量。占位符并不自动脱敏。

## 现有数据流

### 自己的 Day 04：以当前代码为准

POST /api/documents → CreateDocumentRequest → Controller 检查 title → Service 生成 ID → ConcurrentHashMap 保存 → 201/响应 DTO；GET 详情按 ID 查询，列表按标题筛选。application.yml 固定 18086，当前没有自定义配置 Bean 和业务日志。

此次发现并记录以下事实，不自动修改你的学习代码：

1. DocumentController.create 的 Location 拼接为 `/api/documents` + id，少了斜杠，产生 /api/documents1。应改为 `/api/documents/` + id；修正后用响应 Location 请求详情，确认 200。
2. DocumentService.create 对正文也调用 strip()，会去掉首尾空白；Day 04 教案原设计保留正文排版。请明确是否有意如此；若保持原要求，应保存原 content。用含前后空白的正文验证。
3. 独立 contentKeyword 练习未在本地源码中出现；学习记录仍为空。实现主体已存在，不能因此标记全部验收通过。

这些待办不阻止学习独立 Day 05，但要在阶段验收前补齐。Day 05 当前尚无实现，也不复用 Day 04 的内存状态。

### 参考项目：已核对的配置入口

参考文件 `/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend/kb-document/src/main/resources/application.yml` 声明 root=INFO、com.knowledge.base=DEBUG、com.baomidou.mybatisplus=DEBUG，并配置控制台日志格式。仅核对这些声明，不推断部署时最终配置；运行参数等可能覆盖它们。

参考 CategoryController 使用日志记录请求字段、委托 Service（上一课已读）；今天借鉴日志位置，用手写 Logger 明确来源，不引入 Lombok。这里只讨论本课需要的配置/日志，不复用参考数据库连接或凭据，不声称参考项目已经采用我们今天的 local/test 文件。

## 本次需要改动的数据流

启动链：基础 YAML + 激活的 Profile + 环境变量 + 命令行 → Environment → PreviewProperties 绑定/检查 → 构造器注入 PreviewService → 启动日志展示允许公开的生效值。

请求链：GET /api/previews?text=abcdefghij → Controller 生成 requestId/记录收到请求 → Service 检查空白并按配置截断 → DEBUG 分支日志/INFO 完成日志 → DTO 返回 label、maxLength、preview 和 requestId。无存储、SQL、中间件或权限模块；只在本机运行。

| 场景 | 行为 |
|---|---|
| 文本短于或等于上限 | 原样返回，不 strip |
| 文本超过上限 | 按 Unicode 码点截断 |
| 全空白 | WARN，HTTP 400 |
| 缺少 text | MVC 在进入 Controller 前返回 400，不会生成本例 requestId |
| 非数字或超范围的 max-length | 启动配置绑定失败，不等到收到请求才处理 |

本例返回的配置仅含教学白名单字段；不是通用配置查询接口。请求 ID 只用于当前进程内两层日志关联，完整分布式追踪后续再学。

## 文件位置（复用/新增/修改）

仓库根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`。当天模块根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging`。

整体规划（Day 01–04 已存在，今天及以后按课创建）：

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── learning/
├── day-01-spring-boot-hello/
├── day-02-spring-di/
├── day-03-boot-auto-config/
├── day-04-spring-mvc/
├── day-05-config-logging/       # 今天创建
├── day-11-cloud-lab/           # 后续连续服务实验
│   ├── user-service/
│   ├── document-service/
│   └── gateway-service/
├── knowledge-base-backend/     # 后续正式后端
└── knowledge-base-frontend/    # 后续正式前端
```

当天模块目标结构：

```text
my-enterprise-knowledge-base/
└── day-05-config-logging/
    ├── pom.xml
    └── src/main/
        ├── resources/
        │   ├── application.yml
        │   ├── application-local.yml
        │   └── application-test.yml
        └── java/com/thingfaat/knowledge/
            ├── Day05ConfigLoggingApplication.java
            ├── config/PreviewProperties.java
            ├── controller/PreviewController.java
            ├── service/PreviewService.java
            └── dto/PreviewResponse.java
```

Day 05 独立构建，无跨日编译/运行依赖。复用已掌握的 Web POM 和构造器注入思路，文件全部新增；本课不改 Day 04 源码，不在仓库根创建业务 POM/src，不提前创建正式工程。

| 绝对位置 | 操作/所属工程 | 职责 |
|---|---|---|
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/pom.xml` | 新增 / Day 05 | 独立 Web 应用构建 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application.yml` | 新增 / Day 05 | 公共配置 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application-local.yml` | 新增 / Day 05 | local 覆盖与 DEBUG |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application-test.yml` | 新增 / Day 05 | test 覆盖与 INFO |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/Day05ConfigLoggingApplication.java` | 新增 / Day 05 | 启动、注册配置 Bean、打印白名单 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/config/PreviewProperties.java` | 新增 / Day 05 | 强类型绑定/启动检查 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/dto/PreviewResponse.java` | 新增 / Day 05 | HTTP 响应 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/service/PreviewService.java` | 新增 / Day 05 | 按配置生成预览与分支日志 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/controller/PreviewController.java` | 新增 / Day 05 | 参数绑定、生成请求 ID |

## 基于现有代码的完整增量代码

先预测 local/test 下的响应和日志，再自己实现；下面提供当天全部九个文件。POM 根据已读取的 Day 04 Web POM 改 artifactId，不复制旧 Controller/Service。

```bash
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/{config,controller,service,dto}
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources
```

### day-05-config-logging/pom.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/pom.xml`

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
    <artifactId>day-05-config-logging</artifactId>
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

### day-05-config-logging/src/main/resources/application.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: day-05-config-logging
server:
  address: 127.0.0.1
  port: 18087
app:
  preview:
    # KB_LABEL 是本课自定义环境变量；冒号后是未设置时的默认值。
    label: "${KB_LABEL:base}"
    max-length: 20
logging:
  level:
    root: INFO
    com.thingfaat.knowledge: INFO
```

### day-05-config-logging/src/main/resources/application-local.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application-local.yml`

```yaml
# 只有激活 local 才加载；没有覆盖的键继续使用基础文件值。
app:
  preview:
    label: "${KB_LABEL:local}"
    max-length: 8
logging:
  level:
    com.thingfaat.knowledge: DEBUG
```

### day-05-config-logging/src/main/resources/application-test.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/resources/application-test.yml`

```yaml
# test 是本课的命名 Profile，不代表自动执行测试或真实测试环境。
app:
  preview:
    label: "${KB_LABEL:test}"
    max-length: 4
logging:
  level:
    com.thingfaat.knowledge: INFO
```

### day-05-config-logging/src/main/java/com/thingfaat/knowledge/Day05ConfigLoggingApplication.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/Day05ConfigLoggingApplication.java`

```java
package com.thingfaat.knowledge;

import com.thingfaat.knowledge.config.PreviewProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/** 启动独立实验，并显式注册强类型配置 Bean。 */
@SpringBootApplication
@EnableConfigurationProperties(PreviewProperties.class)
public class Day05ConfigLoggingApplication {
    private static final Logger log = LoggerFactory.getLogger(Day05ConfigLoggingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(Day05ConfigLoggingApplication.class, args);
    }

    /** 只记录明确允许展示的教学配置，不遍历或打印整个 Environment。 */
    @Bean
    public ApplicationRunner reportConfiguration(Environment environment, PreviewProperties properties) {
        return args -> log.info("config activeProfiles={} label={} maxLength={}",
                Arrays.toString(environment.getActiveProfiles()), properties.label(), properties.maxLength());
    }
}
```

### day-05-config-logging/src/main/java/com/thingfaat/knowledge/config/PreviewProperties.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/config/PreviewProperties.java`

```java
package com.thingfaat.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.preview.max-length 绑定到 maxLength；单构造器 record 使用构造器绑定。
 * 注册由启动类 @EnableConfigurationProperties 完成，不再给本类加 @Component。
 */
@ConfigurationProperties(prefix = "app.preview")
public record PreviewProperties(String label, int maxLength) {
    public PreviewProperties {
        // 提前拒绝不可用配置：比收到请求后才报错更容易定位。
        // 完整 Bean Validation 留到 Day 07，这里使用 Java 构造器检查。
        if (label == null || !label.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("app.preview.label must be a short alphanumeric label");
        }
        if (maxLength < 1 || maxLength > 100) {
            throw new IllegalArgumentException("app.preview.max-length must be between 1 and 100");
        }
    }
}
```

### day-05-config-logging/src/main/java/com/thingfaat/knowledge/dto/PreviewResponse.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/dto/PreviewResponse.java`

```java
package com.thingfaat.knowledge.dto;

/** 本机实验的响应；requestId 用于关联日志，label/maxLength 仅是无敏感信息的教学配置。 */
public record PreviewResponse(String requestId, String label, int maxLength, String preview) {
}
```

### day-05-config-logging/src/main/java/com/thingfaat/knowledge/service/PreviewService.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/service/PreviewService.java`

```java
package com.thingfaat.knowledge.service;

import com.thingfaat.knowledge.config.PreviewProperties;
import com.thingfaat.knowledge.dto.PreviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 根据启动时绑定的长度上限生成预览；不保存正文、不访问数据库。 */
@Service
public class PreviewService {
    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private final PreviewProperties properties;

    public PreviewService(PreviewProperties properties) {
        this.properties = properties;
    }

    /** requestId 由 Controller 生成；日志只记录 ID、长度和分支，不记录原文。 */
    public PreviewResponse preview(String text, String requestId) {
        if (text.isBlank()) {
            log.warn("preview rejected requestId={} reason=blank_text", requestId);
            // 本课暂在 Service 使用 HTTP 异常；Day 07 再拆分业务异常与 HTTP 映射。
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
        }
        // 按 Unicode 码点截断，避免把 emoji 的 UTF-16 代理对切断。
        // 码点仍不等于用户感知字符，例如组合字符，今天不实现完整字素分割。
        int inputLength = text.codePointCount(0, text.length());
        int outputLength = Math.min(inputLength, properties.maxLength());
        int end = text.offsetByCodePoints(0, outputLength);
        String result = text.substring(0, end);
        log.debug("preview decision requestId={} inputLength={} limit={} truncated={}",
                requestId, inputLength, properties.maxLength(), inputLength > outputLength);
        log.info("preview completed requestId={} outputLength={}", requestId, outputLength);
        return new PreviewResponse(requestId, properties.label(), properties.maxLength(), result);
    }
}
```

### day-05-config-logging/src/main/java/com/thingfaat/knowledge/controller/PreviewController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging/src/main/java/com/thingfaat/knowledge/controller/PreviewController.java`

```java
package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.PreviewResponse;
import com.thingfaat.knowledge.service.PreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 复习查询参数绑定与构造器注入；请求日志与业务日志共享同一个 ID。 */
@RestController
@RequestMapping("/api/previews")
public class PreviewController {
    private static final Logger log = LoggerFactory.getLogger(PreviewController.class);
    private final PreviewService previewService;

    public PreviewController(PreviewService previewService) {
        this.previewService = previewService;
    }

    @GetMapping
    public PreviewResponse preview(@RequestParam("text") String text) {
        // 本地请求关联示例，不是分布式追踪系统，也不信任客户端提供的日志 ID。
        String requestId = UUID.randomUUID().toString();
        log.info("preview received requestId={}", requestId);
        return previewService.preview(text, requestId);
    }
}
```

## 测试验收

### 构建与实验前提

以下命令在当天模块执行。每一轮先停止上一轮自己的进程，确认 18087 端口释放；保持同一 JAR，改变启动配置。不要同时运行多个版本后混淆响应。确认 IDEA 或终端没有遗留 SPRING_PROFILES_ACTIVE、APP_PREVIEW_MAXLENGTH、KB_LABEL、SPRING_APPLICATION_JSON 或额外配置位置设置；不要打印完整环境变量列表。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
```

预期 Java 17、BUILD SUCCESS。构建通过不等于运行验收通过。启动终端保持运行，在另一终端请求：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=abcdefghij'
```

### 四轮配置对照

第 1 轮：不指定 Profile。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar
```

预期 activeProfiles=[]（并不表示没有默认 Profile）、label=base、maxLength=20，preview=abcdefghij；业务 INFO 有，DEBUG 无。

第 2 轮：停止上轮，激活 local。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

预期 activeProfiles=[local]、label=local、maxLength=8，preview=abcdefgh；同一 requestId 出现在 received、decision、completed 日志中，DEBUG 可见。

第 3 轮：停止上轮，激活 test。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

预期 label=test、maxLength=4，preview=abcd；DEBUG decision 不可见，INFO 仍有。这不表示 Service 没执行。

第 4 轮：环境变量与命令行覆盖。先执行 A，停止后再执行 B。下面的赋值只作用于紧随的 Java 进程，不使用 export 污染后续实验。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
APP_PREVIEW_MAXLENGTH=6 KB_LABEL=from-env java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

A 预期 label=from-env、maxLength=6、preview=abcdef。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
APP_PREVIEW_MAXLENGTH=6 KB_LABEL=from-env java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --app.preview.max-length=3
```

B 预期 label=from-env、maxLength=3、preview=abc，证明命令行覆盖环境变量。每轮都保存启动日志与 HTTP 结果，不仅查看磁盘上的 YAML。

### 请求边界与日志定位

停止覆盖实验，重新按 local 启动（上限 8）：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

另一终端依次请求：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=abc'
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=abcdefgh'
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=abcdefghij'
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=   '
curl -i http://127.0.0.1:18087/api/previews
curl -i --get http://127.0.0.1:18087/api/previews --data-urlencode 'text=abcdefg😀Z'
```

预期前三个分别返回 abc、abcdefgh、abcdefgh；DEBUG 的 truncated 分别为 false、false、true。全空白返回 400，received 和 rejected 有相同 ID，无 completed；缺参数返回 400，但没有进入本课方法，没有这组业务日志。最后返回 abcdefg😀，不能切出半个代理对。默认错误响应未必展示 reason，不把错误正文固定结构作为本课目标。

检查日志中没有请求原文；客户端响应含 preview 是接口功能，不等于服务器日志也应记录正文。为了后续复查，可按 IDE 控制台的 requestId 搜索日志，无需今天接入日志平台。

### 故障：配置类型/范围错误并恢复

先停止正在运行的应用；分别启动下面两条，每条失败退出后再运行下一条：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-05-config-logging
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --app.preview.max-length=oops
java -jar target/day-05-config-logging-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --app.preview.max-length=0
```

第一条预期无法转成 int；第二条能转成 int，但构造器范围检查失败。记录异常链中 app.preview、max-length、转换错误或范围错误所在位置。Spring 包装异常可能有多层，不要求逐字匹配。两者都不能只因为终端打印过 Tomcat 初始化就判定启动成功，应检查失败退出和请求不可用。

移除错误参数，重新按 local 启动并请求，确认恢复。修改 Profile 文件的 max-length 后，如果跑的是打包 JAR，应重新 package；本实验环境变量和启动参数变化不需要重新打包。

### 验收台账与独立变化

| 项目 | 通过标准 | 本次备课实际状态 |
|---|---|---|
| 基础/local/test | 启动配置、响应与日志级别符合三轮预期 | 未运行 |
| 环境变量/命令行 | 6 → 3，label 使用 from-env | 未运行 |
| 请求边界 | 短/等长/超长/空白/缺参数/emoji 有实际结果 | 未运行 |
| 配置故障与恢复 | 解释类型失败和范围失败，恢复 200 | 未运行 |
| 日志关联 | 一次成功与一次拒绝能用 ID 串起日志且无原文 | 未运行 |
| 独立变化 | 增加配置 app.preview.uppercase，默认 false；test 为 true | 待本人实现 |

独立变化先写自己的数据流：配置字段 → 绑定 → Service → 返回。实现后用 ASCII 输入 abcdef 验证默认/启用区别；明确先截断再变大写还是相反，并解释次序。无需提交标准答案式截图，保留代码和实际结果即可。

下次进入 Day 06 前，先核对现有 middleware 的 MySQL 配置和可用学习库，不直接沿用参考项目连接，也不在今天提前创建数据库。

## 面试追问

1. **为什么 local 文件存在却没生效？** 先确认激活参数和启动日志，再核对打包产物/外部覆盖；文件存在不是生效证据。
2. **为何环境变量叫 APP_PREVIEW_MAXLENGTH？与 KB_LABEL 有什么不同？** 前者是属性名的环境变量映射，后者来自 YAML 显式占位符；定位三份配置和 PreviewProperties。
3. **@ConfigurationProperties 为什么不只加一个注解就结束？** 还要注册 Bean；今天启动类显式 EnableConfigurationProperties，并构造器注入。无注册时不是可注入对象。
4. **改 YAML 为什么没改变返回？** 可能是旧 JAR、未激活 Profile、被更高优先级覆盖，或运行了另一个进程；按证据排查，不靠重复重启猜测。
5. **INFO 模式没有 decision 日志，是否没执行截断？** Logger 级别控制可见性，逻辑照常执行；用实际 preview 交叉验证。
6. **缺 text 与空 text 的日志为什么不同？** 前者在 MVC 参数绑定阶段拒绝，后者进入业务链；本例关联 ID 不覆盖所有框架错误，完整入口追踪需要后续过滤器/拦截器。
7. **为什么不打印全部配置排障？** 配置可能含凭据；今天仅记录白名单字段。占位符日志不是自动脱敏工具。
8. **配置对象会自动刷新吗？** 本例只在启动绑定，改变环境或 YAML 不会自动刷新已注入对象；本地 Profile 与未来 Nacos 动态配置要分开理解。
