package utd.aos.p1.utils;

import java.io.Serializable;

// models a pair of values.
public class Pair<K, V> implements Serializable {
    private K k;
    private V v;

    public Pair(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getKey() {
        return k;
    }

    public V getValue() {
        return v;
    }

    public String toString() {
        return "{\"key\": " + k.toString() + ", \"value\": " + v.toString() + "}";
    }
}
