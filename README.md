# 📦DependencyLoader

English | [简体中文](./README.zh-CN.md)

DependencyLoader is a lightweight dependency loader that can load dependencies from a Maven repository into the runtime environment during program execution. It is suitable for scenarios where dependencies need to be loaded dynamically.
## Usage

### Step 1: Add to Your Project

#### Maven
To include DependencyLoader in your Maven project, add the following dependency to your `pom.xml` file:
```xml
<dependencies>
    <dependency>
        <groupId>io.github.theramu</groupId>
        <artifactId>dependency-loader</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

#### Gradle
For Gradle projects, add the following content to your `build.gradle` file:
```groovy
dependencies {
    implementation 'io.github.theramu:dependency-loader:1.0.2'
}
```

#### Gradle Kotlin DSL
For projects using Gradle Kotlin DSL, add the following content to your `build.gradle.kts` file:
```kotlin
dependencies {
    implementation("io.github.theramu:dependency-loader:1.0.2")
}
```

### Step 2: Load Dependencies

#### Load a Single Dependency
The following example demonstrates how to load a single dependency at runtime:
```java
import io.github.theramu.dependencyloader.DependencyLoader;

public class Example {
    public static void main(String[] args) throws ClassNotFoundException {
        new DependencyLoader().loadDependency("com.mysql:mysql-connector-j:9.0.0");
        System.out.println(Class.forName("com.mysql.cj.jdbc.Driver"));
    }
}
```

#### Load Multiple Dependencies
To load multiple dependencies, use the following pattern:
```java
String[] dependencies = {
    "com.mysql:mysql-connector-j:9.0.0",
    "com.zaxxer:HikariCP:5.1.0"
};
new DependencyLoader().loadDependencies(dependencies);
```

#### Use a Custom Repository
If you need to load dependencies from a custom repository, specify the repository URL:
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
