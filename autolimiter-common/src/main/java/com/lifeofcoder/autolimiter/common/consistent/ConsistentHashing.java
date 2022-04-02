package com.lifeofcoder.autolimiter.common.consistent;

import com.lifeofcoder.autolimiter.common.config.HashNode;

import java.util.*;

/**
 * 一致性Hash
 *
 * @author xbc
 * @date 2022/4/2
 */
public class ConsistentHashing<T extends HashNode> {
    private int virtualNodes = 1024;
    private TreeMap<Integer, T> hashCircle = new TreeMap();
    private Set<T> realNodeSet = new HashSet();

    public ConsistentHashing(int virtualNodes, List<T> realNodeList) {
        this.virtualNodes = virtualNodes;
        this.realNodeSet.addAll(realNodeList);
        this.buildHashCircle(realNodeList);
    }

    private void buildHashCircle(List<T> realNodeList) {
        Iterator<T> var2 = realNodeList.iterator();

        while (var2.hasNext()) {
            T hashNode = var2.next();
            this.addHashNode(hashNode);
        }

    }

    public void addHashNode(T hashNode) {
        for (int idx = 0; idx < this.virtualNodes; ++idx) {
            int hash = this.hash(hashNode, idx);
            this.hashCircle.put(hash, hashNode);
        }

    }

    public boolean removeHashNode(T hashNode) {
        if (!this.realNodeSet.contains(hashNode)) {
            return false;
        }
        else {
            for (int idx = 0; idx < this.virtualNodes; ++idx) {
                int hash = this.hash(hashNode, idx);
                this.hashCircle.remove(hash);
            }

            return true;
        }
    }

    private int hash(T hashNode, int idx) {
        String tmpKey = hashNode.key();
        return hash(tmpKey + "." + idx);
    }

    public T getNode(Object data) {
        int hash = hash(data.toString());
        SortedMap<Integer, T> tailMap = this.hashCircle.tailMap(hash);
        int key = tailMap.isEmpty() ? (Integer) this.hashCircle.firstKey() : (Integer) tailMap.firstKey();
        return (T) this.hashCircle.get(key);
    }

    private static int hash(String key) {
        return hash4Fnv1(key);
    }

    private static int hash18(String str) {
        int h = str == null ? 0 : (h = str.hashCode()) ^ h >>> 16;
        return h < 0 ? -h : h;
    }

    private static int hash4Fnv1(String str) {
        int p = 16777619;
        int hash = -2128831035;

        for (int i = 0; i < str.length(); ++i) {
            hash = (hash ^ str.charAt(i)) * 16777619;
        }

        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return hash;
    }
}
