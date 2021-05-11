package com.vfd.server;


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

    private static Map<String, String> map = null; //new ConcurrentHashMap<>();

    private static void initFactory (String interfaceToImplement) throws Exception {
        try (InputStream in = ServicesFactory.class.getResourceAsStream(interfaceToImplement)) {
            Properties properties = new Properties();
            properties.load(in);
            Set<String> names = properties.stringPropertyNames();
            map = new ConcurrentHashMap<>();
            for (String name : names) {
                map.put(name, properties.getProperty(name));
            }
        } catch (NullPointerException e) {
            throw new NullPointerException(interfaceToImplement + " in server not found!");
        } catch (IOException e) {
            throw new IOException(interfaceToImplement + " in server loading failed!");
        }
    }

    @SuppressWarnings("all")
    public static <T> T getService(String interfaceName, String interfaceToImplement) throws Exception {
        if (map == null || map.size() == 0) {       // 如果为0也去尝试重新初始化一下
            initFactory(interfaceToImplement);
        }
        final String impl = map.getOrDefault(interfaceName, null);
        if (impl == null) {
            throw new Exception("can't find the implement class of: " + interfaceName);
        }
        try {
            return (T) Class.forName(impl).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            //e.printStackTrace();
            throw new Exception("Class Not Found: " + impl);
        }
    }
}
