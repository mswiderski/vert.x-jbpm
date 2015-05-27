package org.jbpm.vertx;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.platform.Container;

public class VerticleServiceProvider {
    
    private static VerticleServiceProvider INSTANCE;
    
    public static VerticleServiceProvider get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("VerticleServiceProvider not yet configured");
        }
        return INSTANCE;
    }
    
    protected static VerticleServiceProvider configure(Vertx vertx, Container container) {
        if (INSTANCE == null) {
            INSTANCE = new VerticleServiceProvider(vertx, container);
        }
        
        return INSTANCE;
    }

    private Vertx vertx;
    private Container container;
	private Map<String, Object> registry = new HashMap<String, Object>(); 
	
	private VerticleServiceProvider(Vertx vertx, Container container) {
	    this.vertx = vertx;
	    this.container = container;
	}
	
	public void addService(String type, Object service) {
		this.registry.put(type, service);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getService(String type) {
		if (!registry.containsKey(type)) {
			throw new IllegalArgumentException("No service " + type + " was found in registry");
		}
		return (T) registry.get(type);
		
	}

    public Vertx getVertx() {
        return vertx;
    }

    public Container getContainer() {
        return container;
    }

}
