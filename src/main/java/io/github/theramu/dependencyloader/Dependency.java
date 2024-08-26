package io.github.theramu.dependencyloader;

import lombok.Getter;
import lombok.Setter;

import java.io.File;

/**
 * @author TheRamU
 * @since 2022/7/24 10:49
 */
@Getter
public class Dependency {

    private final String groupId;
    private final String artifactId;
    private final String version;
    @Setter
    private File file;

    protected Dependency(String dependencyNotation) {
        String[] ary = dependencyNotation.split(":");
        if (ary.length != 3) {
            throw new IllegalArgumentException("Invalid dependency notation!");
        }
        groupId = ary[0];
        artifactId = ary[1];
        version = ary[2];
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
