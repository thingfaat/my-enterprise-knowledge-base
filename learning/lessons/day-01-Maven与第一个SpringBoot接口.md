# Day 01：在当天独立模块中创建第一个 Spring Boot 接口

**本课唯一业务模块：`my-enterprise-knowledge-base/day-01-spring-boot-hello/`。四个业务与配置文件全部在这个模块内部创建。**

仓库目录：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`

当天模块目录：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello`

环境：按你的要求使用 JDK 17；Spring Boot 3.2.0；复用 IDEA 自带 Maven。模块依赖：没有其他每日练习模块依赖。

目标：亲手建立当天模块，构建并运行 HTTP 接口，解释 Maven、启动类和 Controller 的作用。

时间安排：回顾 15 分钟、原理 45 分钟、源码阅读 60 分钟、独立实现 90 分钟、验收与记录 30 分钟。环境或理解卡住时允许延长。

本版重新编写于 2026-09-26。已重新读取计划、AGENTS.md、目录约定与参考源码。当前未创建当天业务模块；教案代码为你待实现的完整内容，尚未编译或运行。

## 原理

### 1. 仓库、当天模块、Java 包是三个层次

`my-enterprise-knowledge-base` 是保存整个学习过程的 Git 仓库。`day-01-spring-boot-hello` 是今天独立构建和运行的 Maven 应用。`com.thingfaat.knowledge` 是这个应用内部组织 Java 类的包。

因此，今天是先在仓库下创建 `day-01-spring-boot-hello`，再在它里面建立 Maven 标准目录。**仓库根目录不会因为打开在 IDEA 中，就自动成为每一节课的应用目录。**

这里说“每日模块”指一个独立 Maven 工程，不要求仓库根有父 POM，也不要求它依赖其他每日模块。明天的独立练习放入 `day-02-spring-di`，今天的应用保留用于复习。

### 2. Java、Maven、Spring Boot 分别负责什么

| 工具或框架 | 本课职责 | 可观察的结果 |
|---|---|---|
| JDK | 编译与运行 Java 程序 | `.java` 编译成字节码，JVM 执行启动入口 |
| Maven | 读取项目描述，解析依赖，组织编译和打包 | 当天模块的 `target/` 中生成构建产物 |
| Spring Boot | 帮助初始化 Spring 应用并装配运行所需的能力 | 本课 Web 应用启动服务器并监听端口 |
| Spring MVC | 将 HTTP 请求映射到处理方法并写出响应 | GET 请求进入 `HelloController.hello()` |

构建和运行是不同的动作：构建产生可运行的 JAR；启动这个 JAR 后，服务器才开始处理请求。`package` 属于 Maven 生命周期阶段，`spring-boot:run` 是插件目标。执行 `package` 不代表应用已启动，也不代表所有业务行为经过验证。参考：[Maven 生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)。

### 3. 如何理解 POM

- `groupId`、`artifactId`、`version`：标识工程；今天的 artifactId 是 `day-01-spring-boot-hello`。
- `parent`：继承父 POM 的配置。本课直接继承 Spring Boot parent，不继承学习仓库。
- `dependencies`：声明应用需要的依赖；今天只引入 Web Starter。
- `dependencyManagement`：管理依赖版本等信息，本身不会将所管理的依赖加入应用。参考工程在聚合父 POM 中使用它。
- `modules`：聚合构建多个子模块，与“Java 代码依赖另一个模块”不是一回事。今天不用它。
- `packaging`：今天是 `jar`；参考后端的聚合根使用 `pom`。
- `build/plugins`：配置构建插件；本课使用 Boot 插件运行开发应用和生成可执行 JAR。

### 4. 为什么 main 方法能启动一个接口服务

`SpringApplication.run` 初始化 Spring 应用。在本课的 Web Starter 和配置下，应用会启动内嵌 Web 服务器。服务器接到请求后，由 Spring MVC 根据映射找到 Controller 方法。

`@SpringBootApplication` 包含自动配置与组件扫描等能力。本课把启动类放在 `com.thingfaat.knowledge`，Controller 放在其子包，默认扫描能够覆盖它。`@RestController` 让方法返回值进入 HTTP 响应体，`@GetMapping` 声明 GET 请求映射。注入与自动配置分别在 Day 02、Day 03 深入。参考：[Spring Boot 3.2.0 入门](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/getting-started.html)。

### 5. 本课与参考项目的版本差异

参考父 POM 声明 Spring Boot 3.2.0 和 Java 21。用户选择本机 JDK 17，因此本课独立模块设置 Java 17。Boot 3.2.0 官方支持 Java 17；本课代码也不使用 Java 21 特性。此结论不等于整个参考项目都能直接在 Java 17 下编译，后续逐项核查差异。

本会话先前环境探测结果：终端 Java 17.0.17；直接执行 `mvn` 找不到命令；IDEA 自带 Maven 3.9.11 可执行且使用 Java 17。本课使用它的完整路径，不需要修改全局 PATH。

## 现有数据流

### 自己的学习仓库：尚无业务请求链

重新检查时，仓库已有规则、计划、模板和学习记录，没有当天模块、应用 POM 或 Java 源码。因此，当前没有属于本课的监听端口、Controller 或数据库操作，不能描述成“已有接口等待改造”。

### 参考项目：只读以下真实文件

以下文件均位于参考后端：`/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend`。

| 参考文件 | 本课已确认的内容 |
|---|---|
| `pom.xml` | Boot parent 3.2.0、Java 21、packaging=pom、聚合 10 个模块 |
| `kb-document/pom.xml` | 继承参考后端父 POM，packaging=jar，实际声明 Web Starter |
| `kb-document/src/main/java/com/knowledge/base/document/DocumentApplication.java` | main 调用 SpringApplication.run；还声明 Mapper、Feign、定时任务等配置 |
| `kb-document/src/main/java/com/knowledge/base/document/controller/DocumentController.java` | 控制器映射 `/documents`；createDocument 调用 documentService.createDocument 后返回 Result |

简化后的已读代码关系：

```text
参考父 POM → 文档模块 POM → DocumentApplication.main → SpringApplication.run
参考 DocumentController.createDocument → documentService.createDocument → Result.success
```

控制器还声明了参数校验及权限注解，但本课没有运行验证其安全配置是否生效，也没有核验创建文档方法内部的完整持久化流程。`/documents` 是控制器局部映射，外部完整 URL 还需结合网关与上下文配置确定。

源码阅读任务：在四个文件中找出 parent、Java 目标、Web Starter、main 和控制器方法；各用一句话解释作用。参考源码只读，本课不修改它。

## 本次需要改动的数据流

在仓库下新建独立应用模块 `day-01-spring-boot-hello`，从没有应用变成一个可访问的 HTTP 接口。

```text
读取 day-01-spring-boot-hello/pom.xml 并构建
    → 编译当天模块中的两个 Java 类
    → 将当天模块的 application.yml 放入应用资源
    → 生成当天模块 target/ 中的可执行 JAR
    → 运行 KnowledgeBaseApplication.main
    → 初始化 Spring 应用与 Web 服务器，监听 18082
```

```text
GET http://localhost:18082/api/hello
    → Web 服务器 → Spring MVC 请求分发
    → HelloController.hello()
    → HTTP 200，纯文本 Hello, Knowledge Base!
```

这是最小应用：无业务规则和数据读写，今天不引入 Service、Mapper、数据库、网关或注册中心。下一课在自己的 Day 02 模块学习 Service 和依赖注入。

验收失败分支：未知 URL 返回 404；对本课仅支持 GET 的接口发送 POST 返回 405；应用未启动时连接失败。这三者含义不同。

## 文件位置（复用/新增/修改）

### 1. 整个学习仓库的规划结构

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── .gitignore
├── learning/                   # 已有：教案、计划、笔记
├── day-01-spring-boot-hello/    # 今天创建的独立补强模块
├── day-02-spring-di/           # 后续独立补强模块示例
├── day-11-cloud-lab/           # 后续有真实服务依赖的连续实验
├── knowledge-base-backend/    # 正式项目阶段的后端
└── knowledge-base-frontend/   # 正式项目阶段的前端
```

这张图展示目录分工，未列出全部后续每日模块。今天只需新建 `day-01-spring-boot-hello`，后续模块和正式前后端按对应课程创建。正式后端将按参考项目职责划分 kb-common、kb-gateway 等子模块；正式前后端不会依赖每日练习模块。

### 2. 今天需要创建的模块内部结构

```text
my-enterprise-knowledge-base/
└── day-01-spring-boot-hello/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── com/
            │       └── thingfaat/
            │           └── knowledge/
            │               ├── KnowledgeBaseApplication.java
            │               └── controller/
            │                   └── HelloController.java
            └── resources/
                └── application.yml
```

注意缩进：`pom.xml` 和 `src` 的父目录都是 **day-01-spring-boot-hello**；`application.yml` 在这个模块的 `src/main/resources` 中。构建生成的 `target` 也在当天模块内。

### 3. 文件清单：全部使用绝对路径

| 操作 | 位置 | 用途 |
|---|---|---|
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/pom.xml` | 当天应用的依赖和构建配置 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/KnowledgeBaseApplication.java` | 启动入口 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/controller/HelloController.java` | HTTP 请求处理 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/resources/application.yml` | 应用名和监听端口 |
| 复用 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/.gitignore` | 现有规则已忽略 target 等文件，不需要新建根 POM |
| 修改 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/learning/notes/day-01-学习记录.md` | 本人填写实现过程与实际验收 |

所有“新增”都是本课你要亲手完成的动作，不代表助手已经生成业务文件。

## 基于现有代码的完整增量代码

### 0. 先创建当天模块，再写四个文件

亲手在终端执行下面的目录创建命令。命令使用绝对路径，无论终端当前在哪，都只会在当天模块内创建源码目录：

```bash
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/controller
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/resources
```

下面按每个代码块上方的“保存位置”创建文件。先自己尝试，再对照代码。IDEA 中应导入当天模块的 POM；仓库根继续保存学习资料。

### 1. 新增 day-01-spring-boot-hello/pom.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 使用参考工程的 Boot 版本；父 POM 提供依赖版本与构建默认配置。 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <!-- 不从学习仓库目录寻找父 POM；当天模块可以独立构建。 -->
        <relativePath/>
    </parent>

    <groupId>com.thingfaat</groupId>
    <artifactId>day-01-spring-boot-hello</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>day-01-spring-boot-hello</name>

    <properties>
        <!-- 按用户选择使用本机 JDK 17；参考项目目标为 21，不直接照抄。 -->
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 真正引入本课所需的 Web 能力，具体版本由 Boot parent 管理。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 配合 Boot parent 生成可执行 JAR，并提供开发运行目标。 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

这里没有 modules，也没有其他 day 模块的 dependencies。`java.version` 不会替你下载或切换 JDK；构建环境仍应使用已安装的 JDK 17。

### 2. 新增 day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/KnowledgeBaseApplication.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/KnowledgeBaseApplication.java`

```java
package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Day 01 独立应用的启动入口。
 * 放在根包，默认组件扫描可以发现 controller 子包中的控制器。
 */
@SpringBootApplication
public class KnowledgeBaseApplication {

    /**
     * 由 JVM 进入，再交给 Spring Boot 初始化上下文和 Web 服务器。
     * args 用于将命令行参数传给应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
}
```

对照参考 DocumentApplication：保留 Boot 启动入口即可。今天不引入 Mapper、Feign 或定时任务配置。

### 3. 新增 day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/controller/HelloController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/java/com/thingfaat/knowledge/controller/HelloController.java`

```java
package com.thingfaat.knowledge.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证 HTTP 请求能进入当天模块的应用。
 * 没有数据库操作，先用固定响应观察完整的请求与响应过程。
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    /**
     * 类路径 /api 与方法路径 /hello 组合为 GET /api/hello。
     * 返回字符串直接写入响应体，并明确使用纯文本响应类型。
     *
     * @return 本课用于验收的问候语
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello() {
        return "Hello, Knowledge Base!";
    }
}
```

直接在 Java 中调用 `hello()` 只能验证方法返回值，不能代替 HTTP 验收，因为它没有经过网络请求和路由处理。

### 4. 新增 day-01-spring-boot-hello/src/main/resources/application.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/src/main/resources/application.yml`

```yaml
spring:
  application:
    # 名称对应当天独立模块，不是整个学习仓库的应用名。
    name: day-01-spring-boot-hello

server:
  # 使用独立学习端口；如被占用，调整此处并同步验收请求地址。
  port: 18082
```

YAML 使用空格缩进。这个配置文件属于 Day 01 应用，不是放在仓库根的全局配置，也不会自动作用于明天的模块。

## 测试验收

### 1. 环境确认

以下为只读版本检查，可在任意目录运行：

```bash
java -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" -version
```

本会话先前已经探测到 JDK 17.0.17 与 Maven 3.9.11。若在 IDEA 中执行构建或运行，也要核实所用 SDK/JDK 为 17。环境探测通过不代表下面的应用构建通过。

### 2. 先检查文件位置，再构建

写完四个文件后，在当天模块目录执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello
pwd
find . -type f -not -path './target/*' -not -path './.idea/*'
```

`pwd` 的结尾必须是 `day-01-spring-boot-hello`；文件列表应包含本课四个文件。若没有模块目录或 POM，先按上一节创建，不要改为在仓库根写文件。

在当天模块进行构建：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
```

预期：BUILD SUCCESS，并生成 `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/target/day-01-spring-boot-hello-0.0.1-SNAPSHOT.jar`。

首次可能下载依赖；网络失败与编译失败分别记录。本课没有自动化测试类，因此即使 package 成功，也只能记录构建成功，不能写“单元测试全部通过”。

### 3. 运行当天模块的 JAR

终端 A：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello
java -jar target/day-01-spring-boot-hello-0.0.1-SNAPSHOT.jar
```

终端持续运行是正常情况，先检查启动日志是否显示监听 18082。终端 B 执行：

```bash
curl -i http://localhost:18082/api/hello
curl -i http://localhost:18082/api/not-exist
curl -i -X POST http://localhost:18082/api/hello
```

检查完后在终端 A 按 Ctrl+C 停止应用。另一个可选开发启动方法如下，不与前一个进程同时占用端口：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" spring-boot:run
```

### 4. 验收标准与当前实际状态

| 验收项 | 预期 | 当前实际结果 |
|---|---|---|
| 模块边界 | 四个业务文件全部在 day-01-spring-boot-hello 内，独立 POM 无跨日依赖 | 待用户创建和检查 |
| Java / Maven | Java 17，Maven 可用且使用 Java 17 | 本会话先前已执行版本检查；IDE 内设置待确认 |
| Maven 构建 | BUILD SUCCESS，JAR 在当天模块 target 下 | 未运行 |
| GET /api/hello | HTTP 200，正文 Hello, Knowledge Base! | 未运行 |
| 未知路径 | HTTP 404 | 未运行 |
| POST /api/hello | HTTP 405 | 未运行 |
| 停止应用 | 若没有其他进程接管端口，再请求将连接失败 | 未运行 |
| 独立变更 | 自己修改问候语，重建并重启后响应改变 | 待本人完成 |
| 理解检验 | 能解释文件职责、构建与启动区别及 HTTP 请求路径 | 待本人回答 |

遇到错误时按顺序查：目录及 POM 是否正确 → Maven 实际 JDK → 构建第一个错误 → 启动异常与端口 → 请求方法和路径。端口冲突可用 `lsof -nP -iTCP:18082 -sTCP:LISTEN` 观察，不随意结束未知进程。

将结果填入 `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/learning/notes/day-01-学习记录.md`。先记录实际输出，再判断通过与否；教案完成不算用户实现完成。

## 面试追问

1. **仓库和 Maven 模块有什么区别？**
   - 要点：仓库存放全部学习资料；当天模块具有独立 POM、源码和构建产物。不能因为 IDEA 打开仓库就把源码直接放在仓库根。
   - 证据：本课整体目录树、当天模块目录树和四个保存位置。
   - 误区：每个 Git 仓库都必须有一个根 POM。
2. **dependencies 与 dependencyManagement 有什么不同？**
   - 要点：前者声明使用依赖，后者管理默认版本等信息；管理本身不引入依赖。实际依赖还可能来自传递依赖。
   - 证据：参考父 POM 与文档模块 POM；本课模块直接声明 Web Starter。
   - 误区：只写版本管理就已经拥有依赖。
3. **参考根 POM 用 pom，本课为什么用 jar？**
   - 要点：参考根负责聚合和公共管理，本课是独立应用；不需要聚合其他日的模块。
   - 证据：参考父 POM 的 modules，以及当天 POM 的 packaging。
   - 误区：聚合关系自动等于模块之间的 Java 依赖。
4. **main 方法为什么能够启动 Web 服务？**
   - 要点：调用 SpringApplication.run，结合本课 Web 依赖和配置初始化应用与服务器；启动后由服务器处理请求。
   - 证据：当天启动类、Web Starter 和实际启动日志（待验收）。
   - 误区：只写一个 main 就天然具备 HTTP 能力。
5. **为什么 Controller 放在启动类子包？**
   - 要点：使用默认组件扫描范围；参考项目还显式配置了扫描包。
   - 证据：本课两个 Java 文件的 package 与参考 DocumentApplication。
   - 误区：任意包的类都一定会被自动注册。
6. **构建成功和接口验收通过为什么不同？**
   - 要点：构建验证产物生成；HTTP 验收还覆盖进程运行、端口、路由、方法和响应。本课没有测试类，不能声称构建时执行了业务测试。
   - 证据：模块 target 中的 JAR、运行日志、curl 实际输出。
   - 误区：BUILD SUCCESS 意味着服务已经运行。
7. **为什么参考 Java 21、本课 Java 17？**
   - 要点：这是用户明确选择的环境差异；Boot 3.2.0 支持 17，本课代码适配 17。参考的其他模块仍需逐项核对。
   - 证据：两个 POM 的版本目标与 Maven 实际 Java 版本。
   - 误区：更改 java.version 会自动安装 JDK，或证明所有参考源码都兼容。
