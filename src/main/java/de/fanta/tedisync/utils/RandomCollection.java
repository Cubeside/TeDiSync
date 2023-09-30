package de.fanta.tedisync.utils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.Collector;

public class RandomCollection<E> extends TreeMap<Double, E> {

    private double total = 0;
    private final Random random;

    public RandomCollection() {
        this.random = null;
    }

    /**
     * When using this constructor the specified Random will be used whenever an item is selected.
     *
     * @param random The random to use when selecting an item.
     */
    public RandomCollection(Random random) {
        this.random = random;
    }

    public static <T> Collector<T, RandomCollection<T>, RandomCollection<T>> getCollector(BiConsumer<RandomCollection<T>, T> accumulator) {
        return Collector.of(RandomCollection::new, accumulator, RandomCollection::addAll);
    }

    public RandomCollection<E> add(double weight, E result) {
        if (weight <= 0) return this;
        total += weight;
        put(total, result);
        return this;
    }

    /**
     * Gets the next value in the map depending on the weight.
     *
     * @return The next random value or null if none can be found.
     */
    @Nullable
    public E next() {
        return next(random != null ? random : ThreadLocalRandom.current());
    }

    /**
     * Gets the next value in the map depending on the weight.
     *
     * @return The next random value or null if none can be found.
     */
    @Nullable
    public E next(Random random) {
        double value = random.nextDouble() * total;
        Map.Entry<Double, E> entry = ceilingEntry(value);
        return entry != null ? entry.getValue() : null;
    }

    public RandomCollection<E> addAll(RandomCollection<E> randomCollection) {
        putAll(randomCollection);
        return this;
    }
}
