# Day 04：HTTP 参数绑定与 Spring MVC

目标：亲手完成文档创建、按 ID 查询和标题搜索，解释请求如何变成 Java 参数、Java 对象如何变成 JSON。

前置：Day 02 的构造器注入、Day 03 的 Web 自动配置。使用 JDK 17、Boot 3.2.0。4 小时安排：回顾 15 分钟、原理 45 分钟、源码追踪 60 分钟、动手 90 分钟、验收记录 30 分钟。

证据边界：已核对 Day 03 最终源码与参考分类入口，本课新模块尚未由用户实现。下文运行结果是预期，未运行；文档完成不代表用户验收通过。

## 原理

### 从 HTTP 报文到方法调用

HTTP 请求由方法、路径/查询字符串、请求头和可选请求体组成。`GET /api/documents/1` 中的 1 位于路径；`GET /api/documents?keyword=Spring` 中的 keyword 位于查询字符串；创建文档的标题、正文则放在 POST 的 JSON 请求体。

| Java 注解/类型 | 数据来源或作用 | 今天的例子 |
|---|---|---|
| @PathVariable | 匹配路径模板中的变量 | /api/documents/{id} → Long id |
| @RequestParam | Servlet 请求参数；本课使用 URL 查询参数 | ?keyword=Spring → String keyword |
| @RequestBody | 用消息转换器读取请求体 | JSON → CreateDocumentRequest |
| @RestController | 控制器返回值按响应体处理 | DocumentResponse → JSON |
| ResponseEntity | 同时指定响应状态、头和正文 | 201、Location、DocumentResponse |

不要把 @RequestParam 理解成“读取任意 JSON 字段”：它也能处理表单参数，但本课 JSON 由 @RequestBody 处理。Content-Type 描述发送内容的格式，Accept 表达客户端希望收到的格式；它们不是同一个请求头。

请求进入 DispatcherServlet 后，HandlerMapping 找到处理方法，HandlerAdapter 配合参数解析器准备 Java 参数，再调用 Controller。JSON 请求体由消息转换器读取；返回对象也由消息转换器写成响应。路径字符串转 Long 属于参数类型转换，不是 JSON 反序列化。[Spring 官方消息转换说明](https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html)与 [RequestBody 说明](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html)可用于理解这两个过程；页面可能展示新版本，本课仍固定 Boot 3.2.0，不照搬新版配置 API。

### 对象和状态码

DTO 是用于边界传输的数据结构。今天请求对象没有 ID，响应对象有服务端生成的 ID；这样不会把客户端输入直接当作存储实体。使用 Java 17 的 record 简化只承载数据的类：构造器和访问器自动生成，例如 request.title()。record 不负责校验字段，JSON 能解析也不意味着标题非空。

GET 查询返回 200；POST 创建成功返回 201 和 Location；参数无法解析返回 400；ID 不存在返回 404；HTTP 方法不匹配返回 405；不支持请求体媒体类型返回 415。JSON 中某个叫 code 的字段和真实 HTTP 状态码不是同一概念。

## 现有数据流

### 自己的项目

Day 03 已存在 Web Starter、Day03BootAutoConfigApplication、ApplicationRunner 和 AutoConfigController：容器启动 → Runner 打印容器/Bean 信息；GET /api/auto-config → Controller.inspect → 固定文本。配置绑定 127.0.0.1:18084。源码静态检查未发现阻塞本课的问题。

用户报告 Day 03 已完成；本地学习记录仍为空，A/B/C 实验、条件报告和改端口的运行证据待补。本次未重跑这些实验，不将最终源码视为证明此前每个阶段都已执行。

当前没有 Day 04 的接口或数据存储。本课新建独立模块，不在 Day 03 中继续增加文件，不依赖 Day 03 的 JAR。

### 参考项目：只对照本课相关入口

以下文件位于参考后端 kb-document 的 `src/main/java/com/knowledge/base/document/` 下：

- controller/CategoryController.java：POST /categories 使用 @RequestBody CategoryDTO，调用 categoryService.createCategory；GET /categories/{categoryId} 使用路径变量；移动分类方法同时使用路径变量和 newParentId 查询参数。
- dto/CategoryDTO.java：包含 name、description、parentId 等字段，name 声明 @NotBlank；Controller 创建入口同时声明 @Valid。
- service/impl/CategoryServiceImpl.java：createCategory 检查重名/父分类，构造 Category 后调用 categoryMapper.insert；查询按 ID 读取并转换 VO。

已阅读以上源码。这里说明的是代码调用关系，没有验证参考服务的运行、权限或数据库结果。参考控制器包装 Result 并含权限注解；本课采用普通 DTO/ResponseEntity、内存存储和构造器注入，暂不复刻其完整分类业务。正式阶段还要落实数据库、权限与前端，不以本实验替代正式文档模块。

## 本次需要改动的数据流

| 请求 | 参数进入方式 | Controller → Service → 结果 |
|---|---|---|
| POST /api/documents | JSON 请求体 → CreateDocumentRequest | 检查标题 → 生成 ID、存入 Map → 201 + Location + JSON |
| GET /api/documents/{id} | 路径字符串 → Long | 查 Map → 存在则 200 JSON，否则 404 |
| GET /api/documents?keyword=Spring | 查询参数 → String | 标题包含筛选、按 ID 排序 → 200 JSON 数组 |

完整创建链：客户端 JSON → Tomcat → DispatcherServlet/方法定位 → 消息转换器 → 请求 DTO → Controller → Service → Map → 响应 DTO → 消息转换器 → JSON。去掉任何数据库箭头：本课确实没有 Mapper、SQL、中间件或远程服务。

空标题由本课手动返回 400，正文省略时保存空字符串；查询条件省略/空白时返回全部；无匹配返回 []。正文不会用 strip 改写，避免改变用户的文本排版。搜索区分大小写。内存状态只属于当前进程，重启后丢失。

参数绑定失败可能发生在 Controller 方法执行前。用断点区分“没有进入方法”和“进入后找不到数据”。本课无认证授权，权限设计不适用；仅用于本机补强实验。

## 文件位置（复用/新增/修改）

仓库根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`。当天模块根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc`。

整体规划：Day 01–03 已存在，Day 04 今天由你创建，后续按课创建。

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── learning/
├── day-01-spring-boot-hello/
├── day-02-spring-di/
├── day-03-boot-auto-config/
├── day-04-spring-mvc/             # 当天独立模块
├── day-11-cloud-lab/             # 后续连续服务实验
│   ├── user-service/
│   ├── document-service/
│   └── gateway-service/
├── knowledge-base-backend/       # 后续正式后端
└── knowledge-base-frontend/      # 后续正式前端
```

当天模块目标目录：

```text
my-enterprise-knowledge-base/
└── day-04-spring-mvc/
    ├── pom.xml
    └── src/main/
        ├── resources/application.yml
        └── java/com/thingfaat/knowledge/
            ├── Day04SpringMvcApplication.java
            ├── controller/DocumentController.java
            ├── service/DocumentService.java
            └── dto/
                ├── CreateDocumentRequest.java
                └── DocumentResponse.java
```

所有文件属于 Day 04。仅复用前几课的骨架思路，文件全部新建；没有修改或删除旧模块，没有日间 Maven 依赖。根目录不放业务 POM、src 或 application.yml。

| 绝对文件位置 | 操作 | 职责/依赖 |
|---|---|---|
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/pom.xml` | 新增 | Web Starter/构建，Boot 3.2.0、Java 17 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/resources/application.yml` | 新增 | 本机端口 18086 和应用名 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/Day04SpringMvcApplication.java` | 新增 | 启动/扫描 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/CreateDocumentRequest.java` | 新增 | JSON 输入 DTO |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java` | 新增 | JSON 输出 DTO |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/service/DocumentService.java` | 新增 | 内存保存与查询，依赖 DTO |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java` | 新增 | HTTP 绑定与状态，依赖 Service/DTO |

## 基于现有代码的完整增量代码

先自己设计三条请求的输入/输出，再对照以下七个完整文件。当天 POM 从 Day 03 已核对的 Web 版本改 artifactId；不复制 Runner 或旧 Controller。

```bash
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/{controller,service,dto}
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/resources
```

### day-04-spring-mvc/pom.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/pom.xml`

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
    <artifactId>day-04-spring-mvc</artifactId>
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

### day-04-spring-mvc/src/main/resources/application.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: day-04-spring-mvc
server:
  # 当天独立应用只监听本机，避免与前一天端口冲突。
  port: 18086
  address: 127.0.0.1
```

### day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/Day04SpringMvcApplication.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/Day04SpringMvcApplication.java`

```java
package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 启动当天独立应用，扫描本包及其子包中的 Controller 和 Service。 */
@SpringBootApplication
public class Day04SpringMvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day04SpringMvcApplication.class, args);
    }
}
```

### day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/CreateDocumentRequest.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/CreateDocumentRequest.java`

```java
package com.thingfaat.knowledge.dto;

/**
 * 接收 JSON 请求体，只允许调用方提供标题和正文，不让调用方决定服务端 ID。
 * Java 17 record 自动提供构造器和 title()/content() 访问器；这里不使用 Lombok。
 * JSON 解析成功不代表字段合法，标题的最小检查在 Controller 中完成。
 */
public record CreateDocumentRequest(String title, String content) {
}
```

### day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/dto/DocumentResponse.java`

```java
package com.thingfaat.knowledge.dto;

/** 返回给客户端的不可变数据载体，由 Jackson 序列化成 JSON 字段。 */
public record DocumentResponse(Long id, String title, String content) {
}
```

### day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/service/DocumentService.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/service/DocumentService.java`

```java
package com.thingfaat.knowledge.service;

import com.thingfaat.knowledge.dto.CreateDocumentRequest;
import com.thingfaat.knowledge.dto.DocumentResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 当天的内存存储：复习 Service 和构造器注入，不依赖数据库或前一天的应用。
 * 多请求共享这个单例；Map 和序号使用并发安全类型，但不提供数据库事务能力。
 * 重启丢失全部数据，不能直接作为正式项目的持久化设计。
 */
@Service
public class DocumentService {
    private final Map<Long, DocumentResponse> documents = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    /** 接收已经检查过标题的输入，生成服务端 ID，保存并返回文档。 */
    public DocumentResponse create(CreateDocumentRequest request) {
        long id = sequence.incrementAndGet();
        DocumentResponse document = new DocumentResponse(
                id, request.title().strip(), request.content() == null ? "" : request.content());
        documents.put(id, document);
        return document;
    }

    /** 不存在时返回空 Optional，让 HTTP 层决定对应状态码。 */
    public Optional<DocumentResponse> findById(Long id) {
        return Optional.ofNullable(documents.get(id));
    }

    /** 标题区分大小写的包含查询；空关键词返回全部，按 ID 排序方便验收。 */
    public List<DocumentResponse> search(String keyword) {
        String normalized = keyword.strip();
        return documents.values().stream()
                .filter(document -> document.title().contains(normalized))
                .sorted(Comparator.comparing(DocumentResponse::id))
                .toList();
    }
}
```

### day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc/src/main/java/com/thingfaat/knowledge/controller/DocumentController.java`

```java
package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.CreateDocumentRequest;
import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

/** HTTP 边界：绑定参数、做最小输入检查、委托 Service，并选择响应状态。 */
@RestController
@RequestMapping(value = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentController {
    private final DocumentService documentService;

    // 复习 Day 02：单构造器无需 @Autowired，由容器注入 Service。
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** 路径中的 id 由 MVC 从字符串转换为 Long；转换失败时不会进入方法体。 */
    @GetMapping("/{id}")
    public DocumentResponse detail(@PathVariable("id") Long id) {
        return documentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    /** 查询参数不写或为空时使用空字符串，从而查询全部文档。 */
    @GetMapping
    public List<DocumentResponse> search(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return documentService.search(keyword);
    }

    /**
     * @RequestBody 让消息转换器把 JSON 读为请求对象，不是读取 URL 查询参数。
     * consumes 限制请求媒体类型；ResponseEntity 控制状态码、Location 和正文。
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> create(@RequestBody CreateDocumentRequest request) {
        // 今天手动做最小校验，Day 07 再学习 @Valid 和统一异常处理。
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        DocumentResponse document = documentService.create(request);
        return ResponseEntity.created(URI.create("/api/documents/" + document.id())).body(document);
    }
}
```

这里的 Service 直接使用响应 DTO 作为不可变内存值，目的是控制今天的学习量；正式数据库课再区分 Entity、请求 DTO 和响应 VO。本课最小检查位于 HTTP 边界，未来若增加其他入口，也必须保证相同约束，不能假设 Service 永远收到合法数据。

## 测试验收

### 构建与启动

在 IDEA 导入当天 POM；实际 JDK 应为 17。不要在仓库根运行 Maven。以下是操作步骤和预期，备课时未执行运行验收。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc
java -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
java -jar target/day-04-spring-mvc-0.0.1-SNAPSHOT.jar
```

预期 BUILD SUCCESS、Tomcat 监听 18086。这里尚无自动化测试，构建成功不等于接口通过。若端口占用，先检查对应进程；不要随意终止其他服务。保持启动终端运行，另一终端按顺序执行下列请求。

### 正常链路：创建后按返回 ID 查询

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc
curl -i -X POST http://127.0.0.1:18086/api/documents   -H 'Content-Type: application/json'   -d '{"title":" Spring MVC ","content":"学习参数绑定"}'
curl -i http://127.0.0.1:18086/api/documents/1
curl -i --get http://127.0.0.1:18086/api/documents --data-urlencode 'keyword=Spring'
curl -i http://127.0.0.1:18086/api/documents
```

刚启动且没有其他创建请求时，预期 POST 返回 201、Location: /api/documents/1，JSON 等价于 {"id":1,"title":"Spring MVC","content":"学习参数绑定"}。字段顺序无需一致。若已创建过数据，用实际返回 ID 替换 1。详情返回该对象，搜索和列表返回包含该对象的数组，状态均为 200。

### 边界与错误：先预测，再执行

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-04-spring-mvc
# 无匹配、空关键词、无正文的合法创建
curl -i --get http://127.0.0.1:18086/api/documents --data-urlencode 'keyword=NoMatch'
curl -i --get http://127.0.0.1:18086/api/documents --data-urlencode 'keyword=   '
curl -i -X POST http://127.0.0.1:18086/api/documents -H 'Content-Type: application/json' -d '{"title":"Second"}'
# 不存在的 ID、不能转换为 Long 的路径值
curl -i http://127.0.0.1:18086/api/documents/999999
curl -i http://127.0.0.1:18086/api/documents/abc
# JSON 可解析但业务字段非法；JSON 本身损坏；请求体为空
curl -i -X POST http://127.0.0.1:18086/api/documents -H 'Content-Type: application/json' -d '{"title":" "}'
curl -i -X POST http://127.0.0.1:18086/api/documents -H 'Content-Type: application/json' -d '{"title":'
curl -i -X POST http://127.0.0.1:18086/api/documents -H 'Content-Type: application/json'
# 媒体类型和方法不匹配
curl -i -X POST http://127.0.0.1:18086/api/documents -H 'Content-Type: text/plain' -d 'hello'
curl -i -X PUT http://127.0.0.1:18086/api/documents/1
```

| 用例 | 预期 | 能否进入目标方法体 | 实际结果 |
|---|---|---|---|
| 搜索无匹配 | 200，[] | 是 | 待填 |
| 空白关键词 | 200，全部文档数组 | 是 | 待填 |
| 省略 content | 201，content 为 "" | 是 | 待填 |
| 不存在的数字 ID | 404 | 是，Service 返回空 | 待填 |
| id=abc | 400 | 否，类型转换失败 | 待填 |
| 空标题 | 400 | 是，手动检查拒绝 | 待填 |
| 损坏 JSON/空请求体 | 400 | 否，读取请求体失败 | 待填 |
| text/plain 创建 | 415 | 否，consumes 不匹配 | 待填 |
| PUT 详情路径 | 405 | 无匹配的 PUT 方法 | 待填 |

验收状态码与业务结果即可，不依赖默认错误 JSON 的 message 字段；默认配置未必公开异常原因。不要为使错误正文“好看”提前加整套异常框架，Day 07 会专门学习。

### 断点、重启和独立变化

1. 用 IDEA Debug 启动当天应用（先停止终端里的同一应用），在 detail/create 方法首行和 Service 中设断点。比较数字但不存在的 ID 与 abc，记录哪次进入方法；观察 request.title() 是否来自 JSON。
2. 停止应用，再请求确认连接失败；重新启动后查询列表应为 []，旧 ID 返回 404。ID 序号重新开始，这是内存实验的边界，不是持久化成功。
3. 脱离完整答案，新增可选查询参数 `contentKeyword`，默认空字符串；同时满足标题和正文的包含条件才返回。自行决定参数如何进入 Service，至少验证默认值不破坏旧行为、只按正文筛选、组合无匹配三例。保留改动用于下一次验收。
4. 在学习记录画出创建与详情请求的数据流，标明 JSON 转换、参数转换、业务分支和状态码发生的位置。

完成判定：当天模块能独立构建启动，正常/边界/失败用例有实际结果，能解释 400 的两种发生位置，独立变化有代码和请求证据。本教案代码尚未运行，不能直接把表格预期复制成实际结果。

## 面试追问

1. **三种参数注解能互换吗？** 回答要点：位置和解析机制不同；定位 detail/search/create。误区：@RequestParam 自动读取 JSON。
2. **id=abc 为什么没有进入方法？** 路径匹配后类型转换失败，HTTP 400；对比数字不存在时进入 Service 后的 404。
3. **对象如何变为 JSON？** @RestController 的响应体语义与消息转换器协作，Web Starter 提供相关依赖；不是调用 DTO.toString()。定位两个 record 和返回类型。
4. **@RequestBody 是否自动保证标题非空？** 它负责绑定，不代表业务校验已完成；本课显式检查，参考 CategoryDTO 的约束还需配合 @Valid 和验证能力。
5. **201 与 Location 有何作用？** 创建成功和新资源地址，证据在 ResponseEntity.created；不是在 JSON 里写 code=201 就改变 HTTP 状态。
6. **为什么不用可变 HashMap 做共享存储？** Spring Service 默认单例，多请求共享；这里用 ConcurrentHashMap/AtomicLong 和不可变值，但不保证多步骤事务或搜索快照，更不保证重启数据存在。
7. **正式项目如何衔接？** 保留 Controller/Service 边界，后续引入持久化、统一校验、异常、权限和前端；每日实验不成为正式工程运行依赖。能解释取舍比机械搬文件更重要。
