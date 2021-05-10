package com.vfd.server;

import com.vfd.test.BookServeiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @PackageName: com.vfd.server
 * @ClassName: ServicesFactory
 * @Description:
 * @author: vfdxvffd
 * @date: 2021/5/10 上午11:33
 */
public class ServicesFactory {

    static Properties properties;
    static Map<String, String> map = new ConcurrentHashMap<>();

    static {
        try (InputStream in = ServicesFactory.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
            Set<String> names = properties.stringPropertyNames();
            for (String name : names) {
                map.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("all")
    public static <T> T getService(String interfaceName) throws Exception {
        final String impl = map.getOrDefault(interfaceName, null);
        if (impl == null) {
            throw new Exception("can't find the implement class of: " + interfaceName);
        }
        try {
            return (T) Class.forName(impl).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new Exception("Class Not Found: " + impl);
        }
    }
}
