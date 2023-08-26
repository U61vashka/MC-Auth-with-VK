package com.bivashy.auth.api.bucket;

import java.util.function.Predicate;

public interface ModifiableBucket<T> extends Bucket<T> {

    boolean add(T element);

    default boolean addIfAbsent(T element) {
        if (getUnmodifiableRaw().contains(element))
            return false;
        return add(element);
    }

    boolean remove(T element);

    void removeIf(Predicate<T> predicate);

}
