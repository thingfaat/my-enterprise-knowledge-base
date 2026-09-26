# Day 03：Starter 与 Spring Boot 自动配置

目标：自己解释“为什么加一个依赖后就能启动 HTTP 服务”，并用依赖树、条件报告、容器和请求四类证据验证。

前置：Day 01 的 Maven/启动流程、Day 02 的组件扫描/构造器注入。继续使用 JDK 17、Spring Boot 3.2.0。当天独立模块尚未实现；本教案已核对现有源码与本地框架源码，以下实验结果均为预期，尚未运行。

4 小时安排：回顾 15 分钟，原理 45 分钟，源码与依赖追踪 60 分钟，三个阶段动手 90 分钟，验收记录 30 分钟。先尝试画出启动与请求两条链路，再查看完整代码。

## 原理

### 1. Starter 解决依赖选择，自动配置解决条件装配

你在 Day 02 写了 Controller 和 Service，却没有手写 Tomcat 的创建和 DispatcherServlet 的注册。这部分需要分开理解：Maven 先根据 Starter 下载相关库；程序启动时，Spring Boot 再检查类路径、应用类型和已有 Bean 等条件，选择合适的配置。

Starter 是方便使用的依赖集合，不是一个“启动全部功能”的开关。基础 `spring-boot-starter` 提供核心启动能力；`spring-boot-starter-web` 进一步引入 Spring MVC 和默认嵌入式 Tomcat 等依赖。具体传递依赖请看当天的 dependency:tree。

自动配置也不等于无条件创建 Bean。例如已经提供某种基础设施 Bean 时，一些默认配置会退让；但并不是所有自动配置都使用同一个退让条件。[Spring Boot 3.2.0 官方说明](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/using.html#using.auto-configuration)介绍了这种条件配置以及 `--debug` 条件报告。

### 2. 组件扫描与自动配置

`@SpringBootApplication` 组合了配置类、组件扫描和启用自动配置的能力。组件扫描默认从启动类所在包向下寻找业务组件；自动配置从框架约定的候选配置中按条件装配基础设施。因此 Controller 在扫描范围内，不代表 HTTP 服务一定存在。

本课新增 `@Bean`：标注在配置类的方法上，返回值成为受管理对象；它与 `@Service` 标在类上的注册方式不同。方法参数同样可以由容器注入。`ApplicationRunner` 是 Boot 在启动过程中执行的回调，本课只用它观察容器。

### 3. 今天追踪哪些真实源码

在 IDEA External Libraries 打开本地 Boot **3.2.0** 的以下类（没有源码时先下载对应 sources）：

| 类 | 本课关注的实际条件/作用 |
|---|---|
| `ServletWebServerFactoryAutoConfiguration` | `@ConditionalOnClass(ServletRequest.class)`、Servlet Web 应用条件；导入嵌入式服务器相关配置 |
| `DispatcherServletAutoConfiguration` | 要求 Servlet Web 应用、存在 DispatcherServlet 类；其内部配置再决定默认 dispatcherServlet Bean |
| `WebMvcAutoConfiguration` | 检查 Servlet/MVC 类、Servlet Web 应用，且缺少 WebMvcConfigurationSupport Bean 才应用默认配置 |

本次备课已读取本机 `spring-boot-autoconfigure-3.2.0-sources.jar` 中上述类，并核对二进制 JAR 内 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 的候选清单。候选清单不是生效清单，最终要结合条件报告。不要套用只查旧版 spring.factories 的教程。

## 现有数据流

### 自己的当前实现

Day 02 已有独立模块 `day-02-spring-di`。本地代码链路为：启动类扫描 `com.thingfaat.knowledge` → 容器创建带 `@Service` 的 GreetingService → 构造器注入 HelloController。

请求链路：`GET /api/hello?name=Java` → `HelloController.hello(name)` → `GreetingService.greet(name)` → 文本响应。空值或全空白使用小写 `world`，其他输入用 `strip()` 去除首尾空白，返回 `Hello, Java!` 或 `Hello, world!`。没有数据库或远程调用。

用户已告知完成 Day 02；以上为本地静态核对。学习记录未填写，本次没有重跑 HTTP 和删除 @Service 的故障实验，不把它们标成已验收。

Day 03 当前尚无实现，本课不修改或依赖 Day 02 应用 JAR。

### 参考项目的对应入口

参考文件（只阅读）：

- `/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend/kb-document/pom.xml`：声明 Web Starter 等依赖。
- `/Users/hingfaattam/projects/learn_projects/enterprise-knowledge-base/knowledge-base-backend/kb-document/src/main/java/com/knowledge/base/document/DocumentApplication.java`：具有 `@SpringBootApplication`，并通过 `@ComponentScan` 扩展到 document/common 包，也声明 Mapper 和 Feign 扫描等。

本课只对照依赖与扫描入口，不启动整个参考服务，不把其数据库、Feign 或业务行为算作已验证。

## 本次需要改动的数据流

按顺序完成 A，再修改为 B，最后用同一个 B 产物运行 C。它们都属于当天一个模块。

| 阶段 | 类路径/源码变化 | 启动与业务链路（预期） |
|---|---|---|
| A 基础启动 | 基础 Starter；只有启动类和配置，无 Controller | 创建非 Web 容器 → 执行 Runner → 输出观察结果 → 正常退出 |
| B 加入 Web | POM 改为 Web Starter，新增 Controller | Servlet Web 容器和服务器启动 → 业务组件扫描和基础设施装配 → Runner 输出 → 持续等待 HTTP 请求 |
| C 关闭 Web 模式 | B 的源码与依赖不变，只加启动参数 | 非 Web 容器仍扫描 Controller → 无 DispatcherServlet/服务器 → Runner 输出 → 正常退出 |

B 的请求路径：客户端 → 本机 18084 端口的 Tomcat → DispatcherServlet → 路由匹配 → AutoConfigController.inspect → 文本响应。启动装配链和请求链发生在不同时间，不要把依赖下载画成每次请求都会执行的步骤。

本课不涉及数据库、身份认证、权限模型或中间件。失败场景包括错误路径 404、端口占用，以及误把“非 Web 正常退出”当成启动故障。C 用于证明业务 Bean 存在和 HTTP 服务存在是两件事。

## 文件位置（复用/新增/修改）

仓库根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base`。

当天模块根：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config`。独立 Maven 应用，只依赖第三方库；与 Day 01/02 没有编译或运行依赖。复用前两课已理解的启动思路，今天的必需文件全部在下方给出。

整体目录规划（仅 Day 01/02 已存在；后续目录按课创建）：

```text
my-enterprise-knowledge-base/
├── AGENTS.md
├── README.md
├── learning/                         # 教案、计划、笔记与验收
├── day-01-spring-boot-hello/          # 已实现
├── day-02-spring-di/                  # 已实现
├── day-03-boot-auto-config/           # 今天由你新建
├── day-11-cloud-lab/                 # 后续有真实服务互调的连续实验
│   ├── user-service/
│   ├── document-service/
│   └── gateway-service/
├── knowledge-base-backend/           # 后续正式后端，内部按职责分模块
└── knowledge-base-frontend/          # 后续正式前端
```

今天模块的最终结构（A 先建三个文件，B 再加 Controller）：

```text
my-enterprise-knowledge-base/
└── day-03-boot-auto-config/
    ├── pom.xml
    └── src/main/
        ├── java/com/thingfaat/knowledge/
        │   ├── Day03BootAutoConfigApplication.java
        │   └── controller/AutoConfigController.java  # B 才新增
        └── resources/application.yml
```

| 绝对文件位置 | 类型 | 所属/职责 |
|---|---|---|
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/pom.xml` | A 新增；B 修改 | Day 03：构建与依赖 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/java/com/thingfaat/knowledge/Day03BootAutoConfigApplication.java` | A 新增；B/C 复用 | Day 03：启动和容器观察 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/resources/application.yml` | A 新增；B/C 复用 | Day 03：应用名、本机端口 |
| `/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/java/com/thingfaat/knowledge/controller/AutoConfigController.java` | B 新增；C 复用 | Day 03：HTTP 入口 |

仓库根不创建 pom.xml、src 或 application.yml。今天不删除任何旧文件，也不提前创建正式前后端工程。

## 基于现有代码的完整增量代码

### A：先实现没有 Web 依赖的基础应用

以下命令只创建当天目录；在 IDEA 中单独导入当天的 pom.xml。

```bash
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/java/com/thingfaat/knowledge/controller
mkdir -p /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/resources
```

#### day-03-boot-auto-config/pom.xml：A 阶段完整内容

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/pom.xml`

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
    <artifactId>day-03-boot-auto-config</artifactId>
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
            <artifactId>spring-boot-starter</artifactId>
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

#### day-03-boot-auto-config 启动类：A/B/C 共用

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/java/com/thingfaat/knowledge/Day03BootAutoConfigApplication.java`

```java
package com.thingfaat.knowledge;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/** 启动当天独立实验，并输出容器证据；不依赖 Servlet 或 MVC 的类型。 */
@SpringBootApplication
public class Day03BootAutoConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day03BootAutoConfigApplication.class, args);
    }

    /**
     * @Bean 将返回对象交给容器管理，context 参数由容器注入。
     * Boot 在容器刷新后调用 ApplicationRunner，适合观察启动结果。
     * containsBean 只检查名称是否存在，不等于验证 HTTP 请求已经成功。
     */
    @Bean
    public ApplicationRunner inspectContext(ApplicationContext context) {
        return args -> {
            System.out.println("context=" + context.getClass().getSimpleName());
            System.out.println("autoConfigController=" + context.containsBean("autoConfigController"));
            System.out.println("dispatcherServlet=" + context.containsBean("dispatcherServlet"));
        };
    }
}
```

#### day-03-boot-auto-config/application.yml：实际保存在 resources 内

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: day-03-boot-auto-config
server:
  # 只对实际启动的 Web 服务生效；配置端口本身不会创建服务器。
  port: 18084
  # 本课只在本机访问，避免将练习接口暴露给局域网。
  address: 127.0.0.1
```

**现在先做第六节的 A 验收。暂时不要创建 Controller**，否则缺少 MVC 依赖会导致编译失败，无法观察纯基础 Starter 的启动行为。

### B：更换 Starter 并新增 HTTP 入口

先预测：依赖树、容器类型、两个 Bean 检查和进程是否退出，会怎样变化？完成 A 的记录后，再执行以下修改。

#### day-03-boot-auto-config/pom.xml：B 阶段完整替换内容

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/pom.xml`

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
    <artifactId>day-03-boot-auto-config</artifactId>
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

Web Starter 已传递引入基础 Starter，无需重复声明。启动类和 YAML 保持 A 的内容。

#### day-03-boot-auto-config/controller/AutoConfigController.java

保存位置：`/Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config/src/main/java/com/thingfaat/knowledge/controller/AutoConfigController.java`

```java
package com.thingfaat.knowledge.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 由组件扫描发现的业务 Bean；HTTP 映射还需要 MVC 基础设施。 */
@RestController
public class AutoConfigController {
    /** 无请求参数和存储操作，仅用固定文本验证 Web 请求链路。 */
    @GetMapping(value = "/api/auto-config", produces = MediaType.TEXT_PLAIN_VALUE)
    public String inspect() {
        return "Web auto-configuration is active!";
    }
}
```

### C：保留 B 的全部文件，仅修改启动方式

不把 POM 改回 A。Controller 使用 MVC 的注解，单独移除 Web 依赖但保留 Controller 会产生编译错误；这不是自动配置条件实验。C 用启动参数切换应用类型，依赖与源码不变，具体命令见下面。

## 测试验收

### 环境与记录规则

以下结果均为待你验证的预期，不是已执行日志。将实际输出记录在当日学习记录；每阶段先预测再运行。使用 Java 17；POM 的 java.version 不能替代实际 JDK 检查。

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config
java -version
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" -version
```

### A：构建、依赖树与非 Web 启动

只有 A 的三个文件时执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" dependency:tree
java -jar target/day-03-boot-auto-config-0.0.1-SNAPSHOT.jar --debug
```

预期构建成功；依赖树中无 spring-webmvc 和 tomcat-embed-core；Runner 输出 context=AnnotationConfigApplicationContext，两个 Bean 检查都是 false；进程正常结束。即使配置了 server.port，也没有服务器监听。

在 CONDITIONS EVALUATION REPORT 中找 ServletWebServerFactoryAutoConfiguration、DispatcherServletAutoConfiguration 和 WebMvcAutoConfiguration 的负向条件。记录真实的“缺少哪个类”或“不是 Servlet Web 应用”等原因，不要求日志逐字相同，也不要凭未出现某一行就下结论。

### B：Web 启动与请求验证

换好 POM、加好 Controller 后执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" clean package
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn" dependency:tree
java -jar target/day-03-boot-auto-config-0.0.1-SNAPSHOT.jar --debug
```

预期依赖树新增 spring-webmvc、tomcat-embed-core；context=AnnotationConfigServletWebServerApplicationContext，两个 Bean 检查均为 true。进程持续运行，日志显示 Tomcat 在 18084 启动。条件报告中检查上述自动配置的匹配原因，同时注意其内部配置有独立条件，不能把整个报告都当作 positive。

保持启动终端运行，在另一个终端验证：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config
curl -i http://127.0.0.1:18084/api/auto-config
curl -i http://127.0.0.1:18084/api/not-found
curl -i -X POST http://127.0.0.1:18084/api/auto-config
```

预期依次是 HTTP 200 和正文 `Web auto-configuration is active!`、HTTP 404、HTTP 405。错误响应格式不是本课验收重点，状态码和路由原因才是。默认 GET 映射也涉及 HEAD 的框架处理，因此不要用 HEAD 代替这里的 POST 故障用例。

在启动终端按 Ctrl+C 停止自己的进程，再请求一次，预期连接失败。如果仍然成功，先查端口上的其他进程，不把其响应记为本次应用结果。端口占用时用 `lsof -nP -iTCP:18084 -sTCP:LISTEN` 检查，不随意终止未知进程。

### C：有 Web 依赖，但指定非 Web 应用

确认 B 已停止，再用 B 的同一个 JAR 执行：

```bash
cd /Users/hingfaattam/projects/idea_projects/my-enterprise-knowledge-base/day-03-boot-auto-config
java -jar target/day-03-boot-auto-config-0.0.1-SNAPSHOT.jar --spring.main.web-application-type=none --debug
```

预期 context=AnnotationConfigApplicationContext，autoConfigController=true，dispatcherServlet=false；进程正常退出。此时 Controller 仍由组件扫描注册，但 Servlet Web 应用条件不成立，没有 HTTP 接收链路。条件报告应体现应用类型不匹配，与 A 的缺类证据比较。

再次 curl 18084 应连接失败。恢复时移除 `--spring.main.web-application-type=none` 参数即可按 B 启动，不需要修改源码或重新打包。

### 验收清单与独立变化

| 检查项 | 通过标准 | 本次备课实际状态 |
|---|---|---|
| A/B 编译 | 两阶段分别 BUILD SUCCESS，实际 JDK 为 17 | 未运行 |
| A/B 依赖对照 | 指出 MVC/Tomcat 的引入链 | 未运行 |
| A/B/C 容器证据 | 记录实际容器类型及两个 Bean 是否存在 | 未运行 |
| 条件报告 | 每阶段至少解释一条具体条件及其结果 | 未运行 |
| B HTTP | 200/404/405，以及停机后连接失败 | 未运行 |
| C 对照与恢复 | Controller 存在但无 HTTP，移除参数恢复 | 未运行 |
| 独立变化 | 不改 YAML，用启动参数把端口改成 18085，并验证旧端口/新端口 | 待本人完成 |

独立变化先自己完成，不复制答案；记录你使用的参数和请求结果。最后画出两张图：“Maven 依赖 → Boot 启动条件 → Bean/服务器”和“HTTP → DispatcherServlet → Controller → 响应”。能用自己的图解释 C，才算理解本课。

## 面试追问

1. **Starter 和自动配置各做什么？** 要点：依赖集合与运行时条件装配；证据是 A/B POM、dependency:tree 和条件报告。误区：下载到依赖就表示该功能必定生效。
2. **为什么 A 写了 server.port 仍不启动服务器？** 要点：配置属性不是服务器工厂，缺少对应依赖和 Web 应用条件；定位 YAML 和服务器自动配置条件。
3. **C 的 Controller 是 Bean，为什么不能访问？** 要点：组件扫描仍发生，Web 基础设施未装配；定位 Runner 的两个输出和应用类型参数。误区：@RestController 自己监听端口。
4. **不写 @Autowired，inspectContext 的参数从哪里来？** 要点：这是配置类的 @Bean 工厂方法，参数由容器解析；和普通 Java 手动方法调用区分。对照 Day 02 单构造器注入。
5. **为什么移除 Web Starter 后可能先出现编译错误？** 要点：如果留下 Controller 的 MVC import，编译就缺类；A 必须先没有 Controller。区分编译失败和启动时条件不匹配。
6. **用户自定义 Bean 能覆盖所有默认配置吗？** 要点：取决于每条配置的条件；阅读 WebMvcAutoConfiguration 的 @ConditionalOnMissingBean(WebMvcConfigurationSupport.class)，不能泛化为任何同名 Bean 都安全替代默认能力。
7. **排查接口没启动，你如何建立证据链？** 要点：确认实际构建产物与 JDK、依赖树、启动类型/参数、条件报告、服务器日志、Bean 与端口，最后请求验证；不能只凭源码里有注解断言运行正确。
