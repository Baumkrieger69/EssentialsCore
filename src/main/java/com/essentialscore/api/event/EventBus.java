package com.essentialscore.api.event;

import com.essentialscore.ConsoleFormatter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central event bus for registering and dispatching events.
 * Modules can register listeners with this bus and fire events through it.
 */
public class EventBus {
    private final Map<Class<? extends Event>, List<RegisteredListener>> handlerMap;
    private final Map<Object, List<RegisteredListener>> listenerMap;
    private final Logger logger;
    private final ConsoleFormatter console;
    
    /**
     * Creates a new event bus.
     *
     * @param logger The logger to use for error reporting
     */
    public EventBus(Logger logger) {
        this.handlerMap = new ConcurrentHashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
        this.logger = logger;
        
        // Create formatter for nice console output
        String rawPrefix = "&8[&5&lEventBus&8]";
        this.console = new ConsoleFormatter(
            logger,
            rawPrefix,
            true, false, true, "default"
        );
    }
    
    /**
     * Registers all event handlers in a listener object.
     * Methods annotated with @EventHandler will be registered.
     *
     * @param listener The listener object to register
     * @param plugin The plugin/module that owns this listener
     */
    public void registerEvents(Object listener, String plugin) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        List<RegisteredListener> registeredListeners = new ArrayList<>();
        
        // Find all methods annotated with @EventHandler
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) {
                continue;
            }
            
            // Check method signature
            if (method.getParameterCount() != 1) {
                logger.warning("Method " + method.getName() + " in " + listener.getClass().getName() + 
                              " has @EventHandler but has incorrect parameter count");
                continue;
            }
            
            Class<?> paramType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(paramType)) {
                logger.warning("Method " + method.getName() + " in " + listener.getClass().getName() + 
                              " has @EventHandler but parameter is not an Event type");
                continue;
            }
            
            // Make method accessible
            if (!Modifier.isPublic(method.getModifiers())) {
                method.setAccessible(true);
            }
            
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) paramType;
            
            // Create registered listener
            RegisteredListener registeredListener = new RegisteredListener(
                listener, method, annotation.priority(), plugin, annotation.ignoreCancelled());
                
            // Add to maps
            handlerMap.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                     .add(registeredListener);
            registeredListeners.add(registeredListener);
            
            // Sort the handlers by priority
            sortHandlers(eventClass);
        }
        
        // Store the registered listeners for this listener object
        listenerMap.put(listener, registeredListeners);
        
        console.info("Registered " + registeredListeners.size() + " event handlers for " + 
                    listener.getClass().getSimpleName() + " from " + plugin);
    }
    
    /**
     * Unregisters all event handlers for a listener object.
     *
     * @param listener The listener object to unregister
     */
    public void unregisterEvents(Object listener) {
        List<RegisteredListener> registeredListeners = listenerMap.remove(listener);
        if (registeredListeners == null) {
            return;
        }
        
        // Remove from handler map
        for (RegisteredListener registeredListener : registeredListeners) {
            Class<? extends Event> eventClass = registeredListener.getEventClass();
            List<RegisteredListener> handlers = handlerMap.get(eventClass);
            if (handlers != null) {
                handlers.remove(registeredListener);
                if (handlers.isEmpty()) {
                    handlerMap.remove(eventClass);
                }
            }
        }
        
        console.info("Unregistered " + registeredListeners.size() + " event handlers for " + 
                    listener.getClass().getSimpleName());
    }
    
    /**
     * Unregisters all event handlers for a plugin/module.
     *
     * @param plugin The plugin/module to unregister
     */
    public void unregisterPlugin(String plugin) {
        // Find all listeners for this plugin
        List<Object> listenersToRemove = new ArrayList<>();
        for (Map.Entry<Object, List<RegisteredListener>> entry : listenerMap.entrySet()) {
            for (RegisteredListener listener : entry.getValue()) {
                if (listener.getPlugin().equals(plugin)) {
                    listenersToRemove.add(entry.getKey());
                    break;
                }
            }
        }
        
        // Unregister each listener
        for (Object listener : listenersToRemove) {
            unregisterEvents(listener);
        }
        
        console.info("Unregistered all event handlers for plugin: " + plugin);
    }
    
    /**
     * Fires an event to all registered handlers.
     *
     * @param event The event to fire
     * @return The event that was fired (may have been modified by handlers)
     */
    public <T extends Event> T fireEvent(T event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        Class<? extends Event> eventClass = event.getClass();
        List<RegisteredListener> handlers = handlerMap.get(eventClass);
        
        if (handlers == null || handlers.isEmpty()) {
            return event; // No handlers registered
        }
        
        // Call all handlers
        for (RegisteredListener handler : handlers) {
            // Skip if event is cancelled and handler doesn't ignore cancelled
            if (event.isCancelled() && !handler.isIgnoreCancelled()) {
                continue;
            }
            
            try {
                handler.callEvent(event);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error dispatching event " + event.getEventName() + 
                          " to handler " + handler.getMethod().getName() + 
                          " in " + handler.getListener().getClass().getName(), e);
            }
            
            // If event is now cancelled and we don't need to call all handlers, stop
            if (event.isCancelled()) {
                break;
            }
        }
        
        return event;
    }
    
    /**
     * Sorts the handlers for an event class by priority.
     *
     * @param eventClass The event class
     */
    private void sortHandlers(Class<? extends Event> eventClass) {
        List<RegisteredListener> handlers = handlerMap.get(eventClass);
        if (handlers == null) {
            return;
        }
        
        // Sort by priority (highest first)
        handlers.sort((a, b) -> Integer.compare(b.getPriority().getValue(), a.getPriority().getValue()));
    }
    
    /**
     * Represents a registered event handler.
     */
    private static class RegisteredListener {
        private final Object listener;
        private final Method method;
        private final EventPriority priority;
        private final String plugin;
        private final boolean ignoreCancelled;
        
        public RegisteredListener(Object listener, Method method, EventPriority priority, 
                                String plugin, boolean ignoreCancelled) {
            this.listener = listener;
            this.method = method;
            this.priority = priority;
            this.plugin = plugin;
            this.ignoreCancelled = ignoreCancelled;
        }
        
        /**
         * Calls this event handler with the specified event.
         *
         * @param event The event to pass to the handler
         * @throws Exception if an error occurs
         */
        public void callEvent(Event event) throws Exception {
            method.invoke(listener, event);
        }
        
        /**
         * Gets the class of event this handler handles.
         *
         * @return The event class
         */
        @SuppressWarnings("unchecked")
        public Class<? extends Event> getEventClass() {
            return (Class<? extends Event>) method.getParameterTypes()[0];
        }
        
        public Object getListener() {
            return listener;
        }
        
        public Method getMethod() {
            return method;
        }
        
        public EventPriority getPriority() {
            return priority;
        }
        
        public String getPlugin() {
            return plugin;
        }
        
        public boolean isIgnoreCancelled() {
            return ignoreCancelled;
        }
    }
}
