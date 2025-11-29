# spring_records

spring records

## gradle

### dependencyManagement(依赖管理插件) vs implementation platform(平台依赖)

总结对比表格

| 特性   | dependencyManagement | implementation platform |
|:-----|:---------------------|:------------------------|
| 本质   | Gradle 插件            | Gradle 原生功能             |
| 作用域  | 项目级全局                | 配置级局部                   |
| 传递性  | 不传递                  | 可选传递 (api platform)     |
| 灵活性  | 高（支持细粒度覆盖）           | 中等（需要显式覆盖）              |
| 性能   | 较好                   | 更优（原生支持）                |
| 学习曲线 | 需要了解插件机制             | 纯 Gradle 知识             |
| 适用场景 | 复杂项目、Maven迁移         | 现代Gradle项目、库开发          |

推荐策略：新项目优先考虑 implementation platform + libs.versions.toml(版本目录)，现有项目根据实际情况选择最合适的方案。

#### dependencyManagement (依赖管理插件)

来源： `io.spring.dependency-management` 插件\
本质： 一个 Gradle 插件，模拟 Maven 的依赖管理机制\
主要目的： 在项目范围内提供统一的版本控制

dependencyManagement 的工作方式

```groovy
// 必须应用插件 io.spring.dependency-management
plugins {
    id 'java'
    id 'io.spring.dependency-management' version '1.1.0'
}

dependencyManagement {
    imports {
        // 导入 BOM
        mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.0'
    }

    // 也可以直接定义依赖约束
    dependencies {
        dependency 'com.google.guava:guava:32.0.0-jre'
        dependency 'org.apache.commons:commons-lang3:3.12.0'
    }
}

dependencies {
    // 声明依赖时不需要版本号
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.google.guava:guava'
}
```

工作机制：

- 插件会在依赖解析阶段介入
- 为所有配置（implementation、testImplementation 等）添加版本约束
- 作用范围是整个项目及其子项目

#### implementation platform (平台依赖)

来源： Gradle 原生功能（从 5.0 开始）\
本质： 依赖声明的一种特殊形式，基于 BOM 概念\
主要目的： 通过平台约束来管理依赖版本

implementation platform 的工作方式

```groovy
plugins {
    id 'java'
}

dependencies {
    // 声明平台依赖
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.1.0')
    // 或者使用 enforcedPlatform 强制版本
    // implementation enforcedPlatform('org.springframework.boot:spring-boot-dependencies:3.1.0')

    // 声明具体依赖（版本由平台提供）
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // 也可以为测试依赖使用平台
    testImplementation platform('org.springframework.boot:spring-boot-dependencies:3.1.0')
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

工作机制：

- 平台依赖本身是一个特殊的依赖项
- 它只向特定的依赖配置提供版本约束
- 约束作用范围仅限于声明了该平台的那个配置

#### 关键区别详解

##### 作用范围 (Scope)

dependencyManagement:

- 项目级作用域：一旦导入 BOM，所有依赖配置都会受到影响
- 影响：implementation、api、testImplementation、runtimeOnly 等所有配置

implementation platform:

- 配置级作用域：只影响声明了该平台的特定配置

- 示例：

```groovy
dependencies {
    implementation platform('com.example:platform:1.0.0') // 只影响 implementation
    testImplementation platform('com.example:test-platform:1.0.0') // 只影响 testImplementation

    implementation 'com.example:library' // 版本来自 platform
    testImplementation 'com.example:test-library' // 版本来自 test-platform
}
```

##### 传递性行为

dependencyManagement:

- 版本约束不会传递给消费者
- 如果你的项目被其他项目依赖，BOM 约束不会影响消费者

implementation platform:

- 平台依赖本身默认不传递
- 但可以使用 api platform() 让平台约束传递给消费者
- enforcedPlatform() 会强制使用指定版本，覆盖其他约束

##### 灵活性对比

dependencyManagement 更灵活：

```groovy
dependencyManagement {
    imports {
        mavenBom 'org.springframework.cloud:spring-cloud-dependencies:2022.0.0'
    }
    dependencies {
        // 可以覆盖 BOM 中的版本
        dependency 'org.springframework.boot:spring-boot-starter-web:3.1.0'
        // 添加 BOM 中没有的依赖约束
        dependency 'com.example:custom-dependency:1.0.0'
    }
}
```

implementation platform 更明确：

```groovy
dependencies {
    implementation platform('org.springframework.cloud:spring-cloud-dependencies:2022.0.0')

    // 覆盖平台版本需要显式声明
    implementation 'org.springframework.boot:spring-boot-starter-web:3.1.0'

    // 非平台依赖需要自己管理版本
    implementation 'com.example:custom-dependency:1.0.0'
}
```

##### 多平台支持

dependencyManagement 限制：

- 多个 BOM 导入时，版本冲突解决比较复杂
- 后导入的 BOM 可能覆盖前面的约束

implementation platform 更清晰：

```groovy
dependencies {
    // 为不同领域的依赖使用不同平台
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.1.0')
    implementation platform('io.micrometer:micrometer-bom:1.11.0')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'io.micrometer:micrometer-core' // 版本来自 micrometer-bom
}
```

#### 实际项目中的选择建议

使用 dependencyManagement 的场景：

- 迁移 Maven 项目：保持与 Maven 相似的配置结构
- 复杂的企业级项目：需要细粒度的版本覆盖控制
- 多模块项目的统一管理：在根项目中配置，所有子模块继承

```groovy
// 根项目 build.gradle
plugins {
    id 'io.spring.dependency-management' version '1.1.0'
}

dependencyManagement {
    imports {
        mavenBom 'org.springframework.boot:spring-boot-dependencies:3.1.0'
    }
    dependencies {
        dependencySet(group: 'com.fasterxml.jackson', version: '2.15.0') {
            entry 'jackson-core'
            entry 'jackson-databind'
            entry 'jackson-annotations'
        }
    }
}

// 子模块不需要任何额外配置
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.fasterxml.jackson:jackson-databind'
}
```

使用 implementation platform 的场景：

- 现代 Gradle 项目：利用 Gradle 原生特性
- 明确的依赖隔离：不同配置使用不同平台
- 库开发项目：需要控制向消费者传递的约束

```groovy
dependencies {
    // 主代码使用 Spring Boot BOM
    implementation platform('org.springframework.boot:spring-boot-dependencies:3.1.0')

    // 测试代码可以使用专门的测试 BOM
    testImplementation platform('org.springframework.boot:spring-boot-dependencies:3.1.0')

    // 第三方库的 BOM
    implementation platform('com.amazonaws:aws-java-sdk-bom:1.12.500')

    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.amazonaws:aws-java-sdk-s3'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

#### 性能与生态系统考虑

性能：

- implementation platform 是 Gradle 原生支持，通常有更好的性能
- dependency-management 插件需要额外的解析步骤

生态系统：

- Spring 官方逐渐转向推荐 implementation platform
- 但 dependency-management 插件在现有 Spring 生态中仍然广泛使用

现代最佳实践：

```
// 推荐：使用 Gradle 版本目录 + 平台
plugins {
    id 'java'
}

// 在 libs.versions.toml 中定义版本
dependencies {
    implementation platform(libs.versions.spring.boot.get())
    implementation platform(libs.versions.micrometer.get())
    
    implementation libs.bundles.spring.boot.web
    implementation libs.micrometer.core
}

// libs.versions.toml
[versions]
spring-boot = "3.1.0"
micrometer = "1.11.0"

[libraries]
spring-boot-bom = { module = "org.springframework.boot:spring-boot-dependencies", version.ref = "spring-boot" }
micrometer-bom = { module = "io.micrometer:micrometer-bom", version.ref = "micrometer" }

[bundles]
spring-boot-web = ["org.springframework.boot:spring-boot-starter-web", "org.springframework.boot:spring-boot-starter-validation"]
```



