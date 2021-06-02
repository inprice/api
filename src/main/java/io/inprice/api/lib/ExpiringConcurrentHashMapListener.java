package io.inprice.api.lib;

/*
 * https://github.com/vivekjustthink/WeakConcurrentHashMap/blob/master/WeakConcurrentHashMapListener.java
 */
public interface ExpiringConcurrentHashMapListener<K, V> {

	public void notifyOnAdd(K key, V value);
	public void notifyOnRemoval(K key, V value);

}