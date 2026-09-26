# Day 02：IoC、组件扫描与构造器依赖注入

**当天独立模块：`my-enterprise-knowledge-base/day-02-spring-di/`。所有业务文件只在这个模块内创建，不修改 Day 01，也不依赖它的 JAR。**

仓库根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`

当天模块根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di`

目标：亲手完成 Controller → Service 的调用链，解释对象是谁创建和注入的，并制造、定位、修复一次缺少 Bean 的启动错误。

前置：Day 01 的独立模块、main、Web Starter 与 HTTP 验收。环境继续用 JDK 17 / Boot 3.2.0 / IDEA 自带 Maven。本课按 4 小时安排，先讲解、再写代码、最后做失败实验。

源码基线：自己的本地提交 `3844a0e`。本课代码是在你的 Day 01 实际实现基础上设计的独立 Day 02 增量；不是修改旧模块。教案编写时 Day 02 尚无源码，以下内容待你创建；示例运行状态见验收节。

## 原理

### 1. 从你已经写出的接口出发

你在 Day 01 的 HelloController.hello() 中直接返回 `Hello, day-01-spring-boot-hello`。当问候逻辑变为“接收名字、去除空格、没有名字时使用 World”时，可以把这组规则集中放到 GreetingService，Controller 负责 HTTP 参数与返回。

分层的目的，是让职责清楚：业务方法不需要知道请求来自浏览器还是其他入口。今天只用一个具体 Service 类，不为简单练习机械增加接口、实现类和 Mapper。

### 2. IoC 与 DI

IoC（控制反转）：应用对象的创建和装配由容器管理，而不是每个使用者都自行寻找或创建依赖。DI（依赖注入）是实现这种协作的一种方式：对象通过构造器声明需要什么，由外部提供。本课具体是 Spring 创建 GreetingService，再将它交给 HelloController 的构造器。[Spring 官方依赖注入说明](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)

`new` 没有消失；对象总要实例化。变化是控制器不负责构造自己的业务依赖。你自己在普通 Java 测试中也可以调用构造器传入依赖，这同样体现依赖注入思想，但不等于使用了 Spring 容器。

### 3. Bean 如何被发现

本课启动类在 `com.thingfaat.knowledge`，HelloController 与 GreetingService 在其子包。`@RestController` 与 `@Service` 使这些类成为组件扫描的候选，扫描注册后由容器管理。只写一个没有注册途径的普通类，不会自动变成 Bean。[Spring 组件扫描](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)

本课没有设置延迟初始化：启动期间需要创建 Controller；如果必需的 Service 找不到，启动会失败。失败发生在对象装配时，不是必须等第一次 HTTP 请求才触发。

### 4. 为什么用构造器注入

构造器把必需依赖写进类的创建条件，配合 final 字段，调用者一眼就知道控制器需要什么。只有一个构造器时不必再写 @Autowired。这里恰好只有一个 GreetingService 候选，因此可以按类型注入；多个候选如何选择留到理解基本链路后讨论。[Spring 构造器自动注入规则](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)

参考项目部分代码使用字段上的 @Resource。今天用显式构造器，是为了看清装配过程，不代表参考写法根本不能工作。也不要简单地认为 @Resource 和 @Autowired 在所有解析规则上完全相同。

### 5. 补充纠正 Day 01 的一个概念

你的记录写到“可以在 POM 指定 Java 的版本”。更准确地说，本课 Boot parent 下的 java.version 配置编译目标；Maven 实际运行在哪个 JDK 上，由环境或 IDE 的 Maven JDK 配置决定，需查看 Maven 的版本输出。改 POM 不会自动切换或安装 JDK。

## 现有数据流

### 你实际提交的 Day 01

文件都在 `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-01-spring-boot-hello/`：

- pom.xml：Boot 3.2.0、Java 17、独立 artifactId、Web Starter。
- 启动类为 `src/main/java/com/thingfaat/knowledge/Day01SpringBootHelloApplication.java`，不是旧教案中的类名；你的命名有效。
- `src/main/java/com/thingfaat/knowledge/controller/HelloController.java`：GET /api/hello，直接返回自己的问候语。
- `src/main/resources/application.yml`：应用名 day-01-spring-boot-hello，端口 18082。

```text
GET :18082/api/hello → HelloController.hello() → 固定文本
```

Day 02 目录尚未创建，因此不存在 Day 02 的现有请求链。下文“改动”指从你已理解的固定响应模式演进为新模块中的分层响应。

### 参考代码中的相同思想

参考根：`/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend`。

- `kb-document/src/main/java/com/knowledge/base/document/DocumentApplication.java` 明确扫描 document/common 包。
- `kb-document/src/main/java/com/knowledge/base/document/controller/DocumentController.java` 使用 @Resource 注入 DocumentService，createDocument 委派给 documentService.createDocument。
- `kb-document/src/main/java/com/knowledge/base/document/service/impl/CategoryServiceImpl.java` 标记 @Service，注入 CategoryMapper，并在 createCategory 中调用 Mapper 查询/插入。

这是两处独立的源码观察，不把 DocumentController 和 CategoryServiceImpl 拼成同一条调用链。今天借鉴组件注册和依赖协作，不引入参考的数据库、缓存和事务。

## 本次需要改动的数据流

启动时：

```text
Day02SpringDiApplication → 默认扫描根包及子包
    → 注册 GreetingService 和 HelloController
    → 创建 GreetingService
    → 创建 HelloController 时将 Service 作为构造器参数传入
    → 应用启动，监听 18083
```

请求时：

```text
GET :18083/api/hello?name=Alice
    → HelloController.hello(name)
    → GreetingService.greet(name)
    → null/空白：World；其余：去掉首尾空白
    → 返回 Hello, Alice!
```

失败实验：移除 Service 类上的 @Service，同时不添加其他注册方式 → Java 仍可编译 → Spring 无法满足 Controller 的构造器依赖 → 启动失败。恢复 @Service 后重新构建并启动，验证恢复。不要用控制器里 new Service 来“绕过”失败。

## 文件位置（复用/新增/修改）

### 仓库整体规划

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── learning/                   # 教案、记录、验收报告
├── day-01-spring-boot-hello/    # 已实现，保持不变
├── day-02-spring-di/           # 本课新建独立模块
├── day-11-cloud-lab/           # 后续连续 Cloud 实验
├── knowledge-base-backend/    # 后续正式后端
└── knowledge-base-frontend/   # 后续正式前端
```

这张图包含未来规划，今天不提前创建后续目录。Day 02 的源码、配置、POM 与构建产物均属于 day-02-spring-di，不放在仓库根。

### 当天模块内部目标结构

```text
my-enterprise-knowledge-base/
└── day-02-spring-di/
    ├── pom.xml
    └── src/main/
        ├── java/com/thingfaat/knowledge/
        │   ├── Day02SpringDiApplication.java
        │   ├── controller/HelloController.java
        │   └── service/GreetingService.java
        └── resources/application.yml
```

### 文件位置表

| 操作 | 绝对路径 | 职责 |
|---|---|---|
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/pom.xml` | 独立构建、Java 17 和 Web 依赖 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/Day02SpringDiApplication.java` | Day 02 启动与扫描入口 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/service/GreetingService.java` | 业务规则与 Bean 注册 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/controller/HelloController.java` | 控制器与构造器注入 |
| 新增 | `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/resources/application.yml` | 当天应用名及端口 18083 |

Day 01 全部文件仅作阅读基线、不修改；自己的根 .gitignore 继续复用。练习记录位于 `learning/notes/day-02-学习记录.md`，教案位于 learning/lessons。由于是新建独立模块，下面提供完整五文件内容，而不是依赖 Day 01 已有类的片段。

## 基于现有代码的完整增量代码

先尝试回答：Controller 声明哪个构造器参数？Service 要怎样注册？确认思路后在当天模块亲手实现，再对照下面完整代码。

先创建目录：

```bash
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/controller
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/service
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/resources
```

### 1. 新增 day-02-spring-di/pom.xml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <!-- 复用你在 Day 01 验证过的版本组合，直接继承 Boot parent。 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    <groupId>com.thingfaat</groupId>
    <artifactId>day-02-spring-di</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>
    <properties>
        <!-- 指定编译目标；Maven 实际使用的 JDK 仍需用 mvn -version 检查。 -->
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <dependencies>
        <!-- 独立声明 Web 依赖，不依赖 Day 01 的应用 JAR。 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. 新增 day-02-spring-di/src/main/java/com/thingfaat/knowledge/Day02SpringDiApplication.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/Day02SpringDiApplication.java`

```java
package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Day 02 独立应用入口。
 * controller 与 service 都在此包下，使默认组件扫描覆盖两者。
 */
@SpringBootApplication
public class Day02SpringDiApplication {
    /** 启动本课的容器与 Web 服务，保留命令行参数传递。 */
    public static void main(String[] args) {
        SpringApplication.run(Day02SpringDiApplication.class, args);
    }
}
```

### 3. 新增 day-02-spring-di/src/main/java/com/thingfaat/knowledge/service/GreetingService.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/service/GreetingService.java`

```java
package com.thingfaat.knowledge.service;

import org.springframework.stereotype.Service;

/**
 * 集中处理问候语规则：空白使用 World，非空白去掉首尾空格。
 * @Service 让此类在组件扫描时注册为 Bean，供控制器注入。
 */
@Service
public class GreetingService {
    /**
     * 业务方法不依赖 HTTP 对象，可以被不同入口复用。
     *
     * @param name 用户名字，允许 null 或空白
     * @return 格式统一的问候语
     */
    public String greet(String name) {
        String displayName = (name == null || name.isBlank()) ? "World" : name.strip();
        return "Hello, " + displayName + "!";
    }
}
```

### 4. 新增 day-02-spring-di/src/main/java/com/thingfaat/knowledge/controller/HelloController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/java/com/thingfaat/knowledge/controller/HelloController.java`

```java
package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.service.GreetingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 接收 HTTP 参数并委派业务处理，不在控制器中自行创建 Service。 */
@RestController
@RequestMapping("/api")
public class HelloController {
    // 必需依赖由构造器提供，final 防止创建后被替换。
    private final GreetingService greetingService;

    /**
     * Spring 创建控制器时注入已注册的 GreetingService。
     * 本类只有一个构造器，无需额外标注 @Autowired。
     */
    public HelloController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * 接收可选 name；未提供时为 null，由 Service 决定默认问候对象。
     *
     * @param name 请求中的名字
     * @return Service 生成的纯文本问候语
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello(@RequestParam(name = "name", required = false) String name) {
        return greetingService.greet(name);
    }
}
```

### 5. 新增 day-02-spring-di/src/main/resources/application.yml

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di/src/main/resources/application.yml`

```yaml
spring:
  application:
    # 与当天模块对应，和 Day 01 独立运行。
    name: day-02-spring-di
server:
  # 与 Day 01 的 18082 区分，避免同时运行时冲突。
  port: 18083
```

Controller 与 Service 名字可以相同于其他独立练习中的类，只要它们分别构建；不要为复用旧类把两个独立应用的 JAR 互相依赖。

## 测试验收

### 1. 构建与启动

所有 Maven 与 Java 应用命令均从当天模块执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-02-spring-di
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
java -jar target/day-02-spring-di-0.0.1-SNAPSHOT.jar
```

构建成功后启动进程，观察 18083 端口。此处没有自动化测试类；构建通过不能代替下面的请求及故障实验。

### 2. 正常与边界请求

在另一个终端执行：

```bash
curl -i http://localhost:18083/api/hello
curl -i 'http://localhost:18083/api/hello?name=Alice'
curl -i 'http://localhost:18083/api/hello?name=%20Alice%20'
curl -i 'http://localhost:18083/api/hello?name=%20%20'
curl -i 'http://localhost:18083/api/hello?name='
```

| 输入 | 预期状态与正文 | 本次教案实际验证 |
|---|---|---|
| 无 name | 200，Hello, World! | Hello, world! |
| Alice | 200，Hello, Alice! | Hello, Alice! |
| 首尾有空格的 Alice | 200，Hello, Alice! | Hello, Alice! |
| 全为空格 | 200，Hello, World! | Hello, world! |
| 空字符串 | 200，Hello, World! | Hello, world! |

### 3. 必做：制造缺少 Bean 的启动失败

1. 用 Ctrl+C 停掉刚才自己启动的 Day 02 进程，防止旧进程返回正常响应造成误判。
2. 只在 Day 02 的 `service/GreetingService.java` 中移除类上的 @Service；其余代码保持不变。
3. 重新执行当天模块的 clean package，再 java -jar；构建预期成功，但启动预期失败，错误指出 HelloController 需要 GreetingService 而容器中没有对应 Bean。只保留 import 不会注册 Bean。
4. 从日志中抄出“哪个类需要依赖、缺少哪个类型”，解释为什么这不是 Java 编译错误。确认失败进程已退出，且没有另一个 Day 02 进程占用端口。
5. 恢复 @Service，重新构建和启动，再请求 Alice，确认恢复。不要留下故障代码。

本实验的关键是定位依赖装配失败，而不是死记某一行日志文本。以上失败与恢复尚未在用户模块运行，不提前填通过。

### 4. 观察构造器，独立完成一次变化

在 HelloController 构造器和 hello 方法分别打断点：观察控制器实例创建时注入 Service；普通重复请求并不是每次重新构造控制器。本课默认单例且没有改作用域，不将此行为泛化为所有 Bean。

独立任务：只在 Service 中增加一条自己选择的问候语规则，保持 Controller 不变，并用至少两条请求验证。这用于区分“照抄依赖注入代码”与“理解职责分工”。把规则、预期和实际结果写入学习记录。

验收完成要求：目录正确、五个文件独立可构建、正常/边界请求正确、缺 Bean 故障与恢复有证据、能解释构造器注入、独立规则变化可演示。当前教案仅完成源码对照与静态核查，Day 02 的编译/运行/调试均留待用户实现验收。

## 面试追问

1. **IoC 与 DI 的关系是什么？** 回答要点：容器掌握创建与装配流程，依赖注入让对象声明并接收依赖。证据：本课控制器构造器。误区：IoC 就是给所有类加注解。
2. **为什么 Controller 里没有 new，仍能调用 Service？** 回答要点：Service 被注册为 Bean，容器创建控制器时提供依赖。证据：@Service、包位置和构造器。误区：Spring 能注入任何没注册的类。
3. **为什么这里没写 @Autowired？** 回答要点：只有一个构造器，可用于依赖注入；多个候选构造器或多个类型候选需要进一步明确。证据：控制器只有一个构造器。误区：任意情况下都可以不写注入配置。
4. **移除 @Service 为什么还可编译却不能启动？** 回答要点：Java 类型仍存在，编译器不验证容器 Bean 集合；启动装配时找不到必需依赖。证据：本课失败实验。误区：编译成功等于容器一定能启动。
5. **为什么加了 @Service 仍可能找不到 Bean？** 回答要点：扫描范围、条件配置等可能阻止注册；本课先检查是否在根包下。证据：启动类与 Service 的包名。误区：注解是脱离扫描规则自动生效的。
6. **Controller 与 Service 谁处理空白名字？** 回答要点：Controller 接收参数，Service 执行业务规则，其他入口可复用；请求绑定与业务默认值区分。证据：hello 和 greet 方法。误区：所有判断都必须放控制器。
7. **POM 的 java.version 会切换 Maven 使用的 JDK 吗？** 回答要点：不会，查看 Maven 实际运行版本和环境配置；本课二者为 17。证据：POM 与 mvn -version。误区：设置目标版本就安装了对应 JDK。
