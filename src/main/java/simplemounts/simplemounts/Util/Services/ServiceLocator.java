package simplemounts.simplemounts.Util.Services;

import simplemounts.simplemounts.Util.Managers.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps a reference to all plugin services and dispatches them as needed
 */
public class ServiceLocator{

    private static ServiceLocator locator;

    private Map<Class,Object> registry;

    private ServiceLocator() {
        registry = new HashMap<>();
    }

    public static ServiceLocator getLocator() {
        if(locator != null) return locator;
        locator = new ServiceLocator();
        return locator;
    }

    public <T> T getService(Class c) {
        Object o = registry.get(c);

        return (T)registry.get(c);
    }

    public <T> void registerService(Class c, T t) {
        registry.put(c,t);
    }

}
