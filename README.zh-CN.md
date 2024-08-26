# 📦DependencyLoader

[English](./README.md) | 简体中文

DependencyLoader 是一个轻量的依赖加载器，可以在程序执行期间从 Maven 仓库加载依赖到运行环境中，适用于那些需要动态加载依赖的场景。

##  使用方法

### 步骤 1：添加到项目中

#### Maven
要在 Maven 项目中使用依赖加载器，请在 `pom.xml` 文件中添加以下依赖：
```xml
<dependencies>
    <dependency>
        <groupId>io.github.theramu</groupId>
        <artifactId>dependency-loader</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### Gradle
对于 Gradle 项目，请在 `build.gradle` 文件中添加以下内容：
```groovy
dependencies {
    implementation 'io.github.theramu:dependency-loader:1.0.0'
}
```

#### Gradle Kotlin DSL
对于 Gradle Kotlin DSL 项目，请在 `build.gradle.kts` 文件中添加以下内容：
```kotlin
dependencies {
    implementation("io.github.theramu:dependency-loader:1.0.0")
}
```

### 步骤 2：加载依赖

#### 加载单个依赖
以下示例展示了如何在运行时加载单个依赖：
```java
import io.github.theramu.dependency.loader.DependencyLoader;

public class Example {
    public static void main(String[] args) {
        new DependencyLoader().loadDependency("com.mysql:mysql-connector-j:9.0.0");
        System.out.println(Class.forName("com.mysql.cj.jdbc.Driver"));
    }
}
```

#### 加载多个依赖
要加载多个依赖，请使用以下方法：
```java
String[] dependencies = {
    "com.mysql:mysql-connector-j:9.0.0",
    "com.zaxxer:HikariCP:5.1.0"
};
new DependencyLoader().loadDependencies(dependencies);
```

#### 使用自定义仓库
如果需要从自定义仓库加载依赖，请指定仓库 URL：
```java
String[] dependencies = {
    "com.mysql:mysql-connector-j:9.0.0",
    "com.zaxxer:HikariCP:5.1.0"
};
String[] repositories = {
    "https://maven.aliyun.com/repository/public/"
};
new DependencyLoader().loadDependencies(dependencies, repositories);
```
