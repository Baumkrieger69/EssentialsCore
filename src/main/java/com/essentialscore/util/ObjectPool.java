package com.essentialscore.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ein generischer, thread-sicherer Objektpool zur Wiederverwendung von Objekten.
 * Reduziert GC-Overhead durch Objektwiederverwendung.
 *
 * @param <T> Der Typ der gepoolten Objekte
 */
public class ObjectPool<T> {
    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    private final int maxSize;
    private volatile int totalCreated = 0;

    /**
     * Erstellt einen neuen Objektpool.
     *
     * @param factory Die Factory-Methode zum Erstellen neuer Objekte
     * @param reset Die Methode zum Zurücksetzen der Objekte vor der Wiederverwendung
     * @param maxSize Die maximale Größe des Pools
     */
    public ObjectPool(Supplier<T> factory, Consumer<T> reset, int maxSize) {
        this.factory = factory;
        this.reset = reset;
        this.maxSize = maxSize;
    }

    /**
     * Holt ein Objekt aus dem Pool oder erstellt ein neues, wenn der Pool leer ist.
     *
     * @return Ein Objekt des geforderten Typs
     */
    public T borrow() {
        T obj = pool.poll();
        if (obj == null) {
            obj = factory.get();
            totalCreated++;
        }
        return obj;
    }

    /**
     * Gibt ein Objekt zurück in den Pool.
     *
     * @param obj Das zurückzugebende Objekt
     */
    public void release(T obj) {
        if (obj == null) return;
        
        // Objekt zurücksetzen
        reset.accept(obj);
        
        // Nur zurück in den Pool, wenn maxSize nicht überschritten
        if (pool.size() < maxSize) {
            pool.offer(obj);
        }
    }

    /**
     * Füllt den Pool vorab mit Objekten auf.
     *
     * @param count Anzahl der zu erstellenden Objekte
     */
    public void preload(int count) {
        for (int i = 0; i < count && totalCreated < maxSize; i++) {
            T obj = factory.get();
            pool.offer(obj);
            totalCreated++;
        }
    }

    /**
     * Liefert die aktuelle Anzahl der Objekte im Pool.
     *
     * @return Anzahl der Objekte im Pool
     */
    public int size() {
        return pool.size();
    }

    /**
     * Löscht alle Objekte im Pool.
     */
    public void clear() {
        pool.clear();
    }
} 
