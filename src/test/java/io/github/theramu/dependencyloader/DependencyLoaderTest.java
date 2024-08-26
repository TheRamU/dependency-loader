package io.github.theramu.dependencyloader;

public class DependencyLoaderTest {
    public static void main(String[] args) throws ClassNotFoundException {
        new DependencyLoader().loadDependency("com.mysql:mysql-connector-j:9.0.0");
        System.out.println(Class.forName("com.mysql.cj.jdbc.Driver"));
    }
}