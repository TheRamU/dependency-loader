package io.github.theramu.dependencyloader;

import io.github.theramu.dependencyloader.util.ExceptionUtil;
import io.github.theramu.dependencyloader.util.ReflectUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author TheRamU
 * @since 2022/7/24 9:34
 */
public class DependencyLoader {

    protected final Logger logger;
    protected final File librariesFolder;
    private final DependencyDownloader downloader;

    public DependencyLoader() {
        this(Logger.getLogger(DependencyLoader.class.getName()));
    }

    public DependencyLoader(File librariesFolder) {
        this(Logger.getLogger(DependencyLoader.class.getName()), librariesFolder);
    }

    public DependencyLoader(Logger logger) {
        this(logger, new File("libraries"));
    }

    public DependencyLoader(Logger logger, File librariesFolder) {
        this.logger = logger;
        this.librariesFolder = librariesFolder;
        downloader = new DependencyDownloader(this);
    }

    /**
     * Loads a single dependency specified by its notation.
     *
     * @param dependency The dependency notation in the format "groupId:artifactId:version".
     * @return true if the dependency was successfully loaded, false otherwise.
     */
    public boolean loadDependency(@NotNull String dependency) {
        return loadDependency(dependency, null);
    }

    /**
     * Loads a single dependency from specified repositories using its notation.
     *
     * @param dependency   The dependency notation in the format "groupId:artifactId:version".
     * @param repositories An array of repository URLs to search for the dependency.
     * @return true if the dependency was successfully loaded, false otherwise.
     */
    public boolean loadDependency(@NotNull String dependency, String[] repositories) {
        return loadDependencies(new String[]{dependency}, repositories);
    }

    /**
     * Loads multiple dependencies specified by their notations.
     *
     * @param dependencies An array of dependency notations, each in the format "groupId:artifactId:version".
     * @return true if all dependencies were successfully loaded, false otherwise.
     */
    public boolean loadDependencies(@NotNull String[] dependencies) {
        return loadDependencies(dependencies, null);
    }

    /**
     * Loads multiple dependencies from specified repositories using their notations.
     *
     * @param dependencies An array of dependency notations, each in the format "groupId:artifactId:version".
     * @param repositories An array of repository URLs to search for the dependencies.
     * @return true if all dependencies were successfully loaded, false otherwise.
     */
    public boolean loadDependencies(@NotNull String[] dependencies, String[] repositories) {
        List<Dependency> dependencyList = Arrays.stream(dependencies).map(Dependency::new).collect(Collectors.toList());
        for (Dependency dependency : dependencyList) {
            findDependencyFile(dependency);
        }
        List<Dependency> notDownList = dependencyList.stream().filter(dependency -> dependency.getFile() == null).collect(Collectors.toList());
        if (!notDownList.isEmpty()) {
            downloader.downloadDependencies(notDownList, repositories);
        }
        for (Dependency dependency : dependencyList) {
            if (dependency.getFile() == null || !loadJarFile(dependency.getFile())) {
                logger.severe(String.format("Failed to load dependency %s", dependency));
                return false;
            }
        }
        return true;
    }

    private void findDependencyFile(Dependency dependency) {
        if (!librariesFolder.exists() || librariesFolder.isFile()) {
            return;
        }

        String groupIdPath = dependency.getGroupId().replace(".", File.separator);
        File dependencyFolder = new File(librariesFolder, groupIdPath + File.separator + dependency.getArtifactId());

        if (!dependencyFolder.exists() || !dependencyFolder.isDirectory()) {
            return;
        }

        File[] versionFolders = dependencyFolder.listFiles(File::isDirectory);
        if (versionFolders == null) {
            return;
        }

        // 翻转数组，以便从最新版本开始查找
        for (File versionFolder : reverseArray(versionFolders)) {
            if (!matchesVersion(dependency.getVersion(), versionFolder.getName())) {
                continue;
            }

            File jarFile = new File(versionFolder, dependency.getArtifactId() + "-" + versionFolder.getName() + ".jar");
            if (isValidJarFile(jarFile)) {
                dependency.setFile(jarFile);
            }
        }
    }

    private boolean matchesVersion(String version, String folderName) {
        if (version.equals("latest") || version.equals("release")) {
            version = ".*";
        }
        return Pattern.matches(version, folderName);
    }

    private File[] reverseArray(File[] array) {
        if (array == null) {
            return new File[0];
        }
        for (int i = 0; i < array.length / 2; i++) {
            File temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
        return array;
    }

    private boolean isValidJarFile(File file) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        try (JarInputStream jarInputStream = new JarInputStream(file.toURI().toURL().openStream())) {
            // 循环读取所有实体，以确保文件不损坏
            while (jarInputStream.getNextJarEntry() != null) ;
            return true;
        } catch (IOException e) {
            file.delete();
            return false;
        }
    }

    private boolean loadJarFile(File file) {
        try {
            ReflectUtil.loadJarFile(file);
            return true;
        } catch (Exception e) {
            logger.severe(String.format("Failed to load dependency %s\n%s", file.getName(), ExceptionUtil.stackTraceToString(e)));
        }
        return false;
    }
}
