package io.github.theramu.dependencyloader;

import io.github.theramu.dependencyloader.util.ExceptionUtil;
import io.github.theramu.dependencyloader.util.NetworkUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author TheRamU
 * @since 2022/7/24 10:49
 */
public class DependencyDownloader {
    private static final String[] CENTRAL_REPOSITORIES = new String[]{
            "https://repo.maven.apache.org/maven2/",
//            "https://repo.huaweicloud.com/repository/maven/",
//            "https://maven.aliyun.com/nexus/content/groups/public/"
    };
    private final Logger logger;
    private final File librariesFolder;

    protected DependencyDownloader(DependencyLoader dependencyLoader) {
        this.logger = dependencyLoader.logger;
        this.librariesFolder = dependencyLoader.librariesFolder;
    }

    protected void downloadDependencies(@NotNull List<Dependency> dependencies, String[] repositories) {
        if (repositories == null || repositories.length == 0) {
            repositories = CENTRAL_REPOSITORIES;
        }
        List<String> repositoryList = NetworkUtil.sortUrlsByLatency(repositories);
        for (Dependency dependency : dependencies) {
            downloadDependency(repositoryList, dependency);
        }
    }

    private void downloadDependency(List<String> repositoryList, Dependency dependency) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String version = dependency.getVersion();
        File file = null;

        for (String repository : repositoryList) {
            repository = repository.endsWith("/") ? repository : repository + "/";

            assert version != null;
            if (version.equals("latest") || version.equals("release")) {
                version = getVersioning(repository, dependency);
                if (version == null) {
                    logger.severe(String.format("Failed to get the latest version number of the %s !", dependency));
                    continue;
                }
            }

            String fileName = String.format("%s-%s.jar", artifactId, version);
            file = new File(librariesFolder, String.join(File.separator, groupId.replace(".", File.separator), artifactId, version, fileName));
            String urlStr = String.format("%s%s/%s/%s/%s", repository, groupId.replace(".", "/"), artifactId, version, fileName);
            int statusCode = downloadFile(urlStr, file.getPath());
            if (statusCode == 1) {
                break;
            }
            file = null;

            if (statusCode == 2) {
                logger.warning(String.format("Cannot find dependency %s in repository %s", dependency, repository));
            } else {
                logger.warning(String.format("Failed to download %s from %s", fileName, repository));
            }
        }
        dependency.setFile(file);
    }

    private int downloadFile(String urlStr, String filePath) {
        try (InputStream input = new URL(urlStr).openStream()) {
            logger.info(String.format("Downloading %s ...", new File(filePath).getName()));
            URLConnection connection = new URL(urlStr).openConnection();
            connection.setConnectTimeout(100000);
            connection.setReadTimeout(100000);
            int length = connection.getContentLength();
            return writeToLocal(input, length, filePath);
        } catch (FileNotFoundException e) {
            return 2;
        } catch (UnknownHostException e) {
            logger.severe(String.format("Failed to connect to the server!\n%s", ExceptionUtil.stackTraceToString(e)));
            return 3;
        } catch (IOException e) {
            logger.severe(String.format("Failed to download %s!\n%s", urlStr, ExceptionUtil.stackTraceToString(e)));
            return 5;
        }
    }

    private int writeToLocal(InputStream input, int length, String filePath) {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        try (FileOutputStream output = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead, totalBytesRead = 0;
            int tempBytesRead = 0;
            long tempTime = System.currentTimeMillis();
            while ((bytesRead = input.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                tempBytesRead += bytesRead;
                output.write(buffer, 0, bytesRead);
                long currentTime = System.currentTimeMillis();
                if (currentTime - tempTime < 500 && totalBytesRead < length) {
                    continue;
                }
                int rate = (int) (tempBytesRead / ((currentTime - tempTime) / 1000.0));
                printProgressBar(file.getName(), length, totalBytesRead, rate);
                tempTime = currentTime;
                tempBytesRead = 0;
            }
            return 1;
        } catch (IOException e) {
            logger.severe(String.format("Failed to write to file %s\n%s", filePath, ExceptionUtil.stackTraceToString(e)));
            // 删除损坏文件
            file.delete();
            return 4;
        }
    }

    // 获取最新版本号
    private String getVersioning(String repository, Dependency dependency) {
        String url = String.format(
                "%s%s/%s/maven-metadata.xml",
                repository,
                dependency.getGroupId().replace(".", "/"),
                dependency.getArtifactId()
        );
        try (InputStream input = new URL(url).openStream()) {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            return getXmlNode(document, "metadata.versioning." + dependency.getVersion()).getFirstChild().getNodeValue();
        } catch (Exception e) {
            logger.severe(String.format("Failed to get the latest version number of the %s !\n%s", dependency, ExceptionUtil.stackTraceToString(e)));
            return null;
        }
    }

    private Node getXmlNode(Document document, String nodePath) {
        String[] pathArray = nodePath.split("\\.");
        NodeList nodeList = document.getElementsByTagName(pathArray[0]).item(0).getChildNodes();
        List<String> nodePathList = new ArrayList<>(Arrays.asList(pathArray));
        nodePathList.remove(0);
        return getNode(nodeList, nodePathList);
    }

    private Node getNode(NodeList nodeList, List<String> nodePath) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeName().equals(nodePath.get(0))) {
                if (nodePath.size() == 1) {
                    return node;
                } else {
                    nodePath.remove(0);
                    return getNode(node.getChildNodes(), nodePath);
                }
            }
        }
        return null;
    }

    private void printProgressBar(String fileName, int length, int byteSum, int rate) {
        DecimalFormat format = new DecimalFormat("#0.0");
        String count = getByteSize(byteSum) + "/" + getByteSize(length);
        String progress = format.format((byteSum / (double) length) * 100) + "%";
        String progressBar = toProgressBar(byteSum / (double) length);
        String message;
        if (byteSum >= length) {
            message = String.format("Downloaded %s %s [%s] %s", fileName, progress, progressBar, count);
        } else {
            String rateStr = getByteSize(rate) + "/s";
            String formattedTime = formatTime((int) Math.ceil((length - byteSum) / (double) rate));
            message = String.format("Downloading %s %s [%s] %s  %s\t eta %s", fileName, progress, progressBar, count, rateStr, formattedTime);
        }
        logger.info(message);
    }

    private String toProgressBar(double progress) {
        int max = 15;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < max; i++) {
            bar.append(i < progress * max ? "=" : " ");
        }
        return bar.toString();
    }

    private String formatTime(int time) {
        StringBuilder formattedTime = new StringBuilder();
        int hour = time / 3600;
        int min = (time % 3600) / 60;
        int sec = time % 60;
        if (hour > 0) formattedTime.append(hour).append("h ");
        if (min > 0) formattedTime.append(min).append("m ");
        formattedTime.append(sec).append("s");
        return formattedTime.toString().trim();
    }

    private String getByteSize(int length) {
        DecimalFormat format = new DecimalFormat("#0.0");
        if (length >= 1024 * 1024 * 1024) {
            return format.format((double) length / (1024 * 1024 * 1024)) + " GB";
        }
        if (length >= 1024 * 1024) {
            return format.format((double) length / (1024 * 1024)) + " MB";
        }
        if (length >= 1024) {
            return format.format((double) length / 1024) + " kB";
        }
        return length + " Byte";
    }
}
