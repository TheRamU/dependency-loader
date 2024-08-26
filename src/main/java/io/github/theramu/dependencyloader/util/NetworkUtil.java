package io.github.theramu.dependencyloader.util;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TheRamU
 * @since 2022/7/24 10:49
 */
public class NetworkUtil {

    public static int connectLatency(String urlStr) {
        try {
            InetAddress address = InetAddress.getByName(new URL(urlStr).getHost());
            long stamp = System.currentTimeMillis();
            if (address.isReachable(5000)) {
                return (int) (System.currentTimeMillis() - stamp);
            }
        } catch (Exception ignored) {
        }
        return Integer.MAX_VALUE;
    }

    public static List<String> sortUrlsByLatency(String[] urls) {
        List<String> urlList = new ArrayList<>();
        Collections.addAll(urlList, urls);

        Map<String, Integer> latencyMap = new HashMap<>();

        for (String url : urls) {
            latencyMap.put(url, connectLatency(url));
        }

        urlList.sort((url1, url2) -> {
            int latency1 = latencyMap.get(url1);
            int latency2 = latencyMap.get(url2);
            return Integer.compare(latency1, latency2);
        });

        return urlList;
    }
}