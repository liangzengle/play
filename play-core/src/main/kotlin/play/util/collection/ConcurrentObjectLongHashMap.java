package play.util.collection;

import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.jetbrains.annotations.NotNull;
import play.util.function.ObjLongToObjFunction;
import play.util.function.ObjToLongFunction;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.*;

public class ConcurrentObjectLongHashMap<K> implements ConcurrentObjectLongMap<K> {

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    private static final int DEFAULT_CAPACITY = 16;

    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    private static final float LOAD_FACTOR = 0.75f;

    static final int TREEIFY_THRESHOLD = 8;

    static final int UNTREEIFY_THRESHOLD = 6;

    static final int MIN_TREEIFY_CAPACITY = 64;

    private static final int MIN_TRANSFER_STRIDE = 16;

    private static final int RESIZE_STAMP_BITS = 16;

    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /*
     * Encodings for Node hash fields. See above for explanation.
     */
    static final int MOVED = -1; // hash for forwarding nodes
    static final int TREEBIN = -2; // hash for roots of trees
    static final int RESERVED = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /**
     * Number of CPUS, to place bounds on some sizings
     */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /* ---------------- Nodes -------------- */
    static class Node<K> implements Entry<K> {
        final int hash;
        final K key;
        volatile long val;
        volatile Node<K> next;

        Node(int hash, K key, long val) {
            this.hash = hash;
            this.key = key;
            this.val = val;
        }

        Node(int hash, K key, long val, Node<K> next) {
            this(hash, key, val);
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final long getValue() {
            return val;
        }

        public final int hashCode() {
            return key.hashCode() ^ Long.hashCode(val);
        }

        public final String toString() {
            return key + "=" + val;
        }

        public final boolean equals(Object o) {
            if (!(o instanceof ConcurrentObjectLongMap.Entry)) {
                return false;
            }
            Entry<?> that = (Entry<?>) o;
            Object k = that.getKey();
            return k != null && (key == k || k.equals(key)) && that.getValue() == val;
        }

        /**
         * Virtualized support for map.get(); overridden in subclasses.
         */
        Node<K> find(int h, Object k) {
            Node<K> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- Static utilities -------------- */
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * Returns a power of two table size for the given desired capacity.
     * See Hackers Delight, sec 3.2
     */
    private static final int tableSizeFor(int c) {
        int n = -1 >>> Integer.numberOfLeadingZeros(c - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
                for (Type t : ts) {
                    if ((t instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    @SuppressWarnings({"rawtypes", "unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable) k).compareTo(x));
    }

    /* ---------------- Table element access -------------- */
    @SuppressWarnings("unchecked")
    static final <K> Node<K> tabAt(Node<K>[] tab, int i) {
        return (Node<K>) U.getReferenceAcquire(tab, ((long) i << ASHIFT) + ABASE);
    }

    static final <K> boolean casTabAt(Node<K>[] tab, int i,
                                      Node<K> c, Node<K> v) {
        return U.compareAndSetReference(tab, ((long) i << ASHIFT) + ABASE, c, v);
    }

    static final <K> void setTabAt(Node<K>[] tab, int i, Node<K> v) {
        U.putReferenceRelease(tab, ((long) i << ASHIFT) + ABASE, v);
    }

    /* ---------------- Fields -------------- */

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    transient volatile Node<K>[] table;

    /**
     * The next table to use; non-null only while resizing.
     */
    private transient volatile Node<K>[] nextTable;

    /**
     * Base counter value, used mainly when there is no contention,
     * but also as a fallback during table initialization
     * races. Updated via CAS.
     */
    private transient volatile long baseCount;

    /**
     * Table initialization and resizing control.  When negative, the
     * table is being initialized or resized: -1 for initialization,
     * else -(1 + the number of active resizing threads).  Otherwise,
     * when table is null, holds the initial table size to use upon
     * creation, or 0 for default. After initialization, holds the
     * next element count value upon which to resize the table.
     */
    private transient volatile int sizeCtl;

    /**
     * The next table index (plus one) to split while resizing.
     */
    private transient volatile int transferIndex;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
     */
    private transient volatile int cellsBusy;

    /**
     * Table of counter cells. When non-null, size is a power of 2.
     */
    private transient volatile CounterCell[] counterCells;

    // views
    private transient KeySetView<K> keySet;
    private transient ValuesView values;
    private transient EntrySetView<K> entrySet;


    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    public ConcurrentObjectLongHashMap() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative
     */
    public ConcurrentObjectLongHashMap(int initialCapacity) {
        this(initialCapacity, LOAD_FACTOR, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param initialCapacity the initial capacity. The implementation
     *                        performs internal sizing to accommodate this many elements,
     *                        given the specified load factor.
     * @param loadFactor      the load factor (table density) for
     *                        establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative or the load factor is nonpositive
     * @since 1.6
     */
    public ConcurrentObjectLongHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), initial
     * table density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * @param initialCapacity  the initial capacity. The implementation
     *                         performs internal sizing to accommodate this many elements,
     *                         given the specified load factor.
     * @param loadFactor       the load factor (table density) for
     *                         establishing the initial table size
     * @param concurrencyLevel the estimated number of concurrently
     *                         updating threads. The implementation may use this value as
     *                         a sizing hint.
     * @throws IllegalArgumentException if the initial capacity is
     *                                  negative or the load factor or concurrencyLevel are
     *                                  nonpositive
     */
    public ConcurrentObjectLongHashMap(int initialCapacity,
                                       float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        int cap = (size >= (long) MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }

    // Original (since JDK1.2) Map methods

    /**
     * {@inheritDoc}
     */
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long) Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int) n);
    }

    @Override
    public int getSize() {
        return size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }

    @Override
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Entry<K>> iterator() {
        return entrySet().iterator();
    }

    @Nonnull
    public Iterable<K> keys() {
        return keySet();
    }

    @NotNull
    @Override
    public Iterable<K> getKeys() {
        return keys();
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    public Long get(Object key) {
        Node<K>[] tab;
        Node<K> e, p;
        int n, eh;
        K ek;
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            } else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }

    /**
     * Tests if the specified object is a key in this table.
     *
     * @param key possible key
     * @return {@code true} if and only if the specified object
     * is a key in this table, as determined by the
     * {@code equals} method; {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public Long put(K key, long value) {
        return putVal(key, value, false);
    }

    /**
     * Implementation for put and putIfAbsent
     */
    final Long putVal(K key, long value, boolean onlyIfAbsent) {
        if (key == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            K fk;
            long fv;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<K>(hash, key, value)))
                    break;                   // no lock when adding to empty bin
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else if (onlyIfAbsent // check first node without acquiring lock
                    && fh == hash
                    && ((fk = f.key) == key || (fk != null && key.equals(fk)))) {
                fv = f.val;
                return fv;
            } else {
                Long oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K> e = f; ; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K>(hash, key, value);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            Node<K> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K>) f).putTreeVal(hash, key,
                                    value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }

    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     * {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    public Long remove(Object key) {
        return replaceNode(key, null, null);
    }

    /**
     * Implementation for the four public remove/replace methods:
     * Replaces node value with v, conditional upon match of cv if
     * non-null.  If resulting value is null, delete.
     */
    final Long replaceNode(Object key, Long value, Long cv) {
        int hash = spread(key.hashCode());
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0 ||
                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                Long oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (Node<K> e = f, pred = null; ; ) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    long ev = e.val;
                                    if (cv == null || cv == ev) {
                                        oldVal = ev;
                                        if (value != null)
                                            e.val = value;
                                        else if (pred != null)
                                            pred.next = e.next;
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        } else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(hash, key, null)) != null) {
                                long pv = p.val;
                                if (cv == null || cv == pv) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            Node<K> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K> p = (fh >= 0 ? f :
                                (f instanceof TreeBin) ?
                                        ((TreeBin<K>) f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa. The set supports element
     * removal, which removes the corresponding mapping from this map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or
     * {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public KeySetView<K> keySet() {
        KeySetView<K> ks;
        if ((ks = keySet) != null) return ks;
        return keySet = new KeySetView<K>(this);
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll}, and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT}
     * and {@link Spliterator#NONNULL}.
     *
     * @return the collection view
     */
    @Nonnull
    public LongIterable values() {
        ValuesView vs;
        if ((vs = values) != null) return vs;
        return values = new ValuesView(this);
    }

    @NotNull
    @Override
    public LongIterable getValues() {
        return values();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public Iterable<Entry<K>> entrySet() {
        EntrySetView<K> es;
        if ((es = entrySet) != null) return es;
        return entrySet = new EntrySetView<K>(this);
    }

    /**
     * Returns the hash code value for this {@link Map}, i.e.,
     * the sum of, for each key-value pair in the map,
     * {@code key.hashCode() ^ value.hashCode()}.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        int h = 0;
        Node<K>[] t;
        if ((t = table) != null) {
            Traverser<K> it = new Traverser<K>(t, t.length, 0, t.length);
            for (Node<K> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ Long.hashCode(p.val);
        }
        return h;
    }

    /**
     * Returns a string representation of this map.  The string
     * representation consists of a list of key-value mappings (in no
     * particular order) enclosed in braces ("{@code {}}").  Adjacent
     * mappings are separated by the characters {@code ", "} (comma
     * and space).  Each key-value mapping is rendered as the key
     * followed by an equals sign ("{@code =}") followed by the
     * associated value.
     *
     * @return a string representation of this map
     */
    public String toString() {
        Node<K>[] t;
        int f = (t = table) == null ? 0 : t.length;
        Traverser<K> it = new Traverser<K>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K> p;
        if ((p = it.advance()) != null) {
            for (; ; ) {
                K k = p.key;
                long v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is a map with the same
     * mappings as this map.  This operation may return misleading
     * results if either map is concurrently modified during execution
     * of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?, ?> m = (Map<?, ?>) o;
            Node<K>[] t;
            int f = (t = table) == null ? 0 : t.length;
            Traverser<K> it = new Traverser<K>(t, f, 0, f);
            for (Node<K> p; (p = it.advance()) != null; ) {
                long val = p.val;
                Object v = m.get(p.key);
                if (v == null || !(v instanceof Long) || ((Long) v) != val)
                    return false;
            }
            for (Map.Entry<?, ?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public Long putIfAbsent(K key, long value) {
        return putVal(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(K key, long value) {
        if (key == null)
            throw new NullPointerException();
        return replaceNode(key, null, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, long oldValue, long newValue) {
        if (key == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public Long replace(K key, long value) {
        if (key == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // Overrides of JDK8+ Map extension method defaults

    /**
     * Returns the value to which the specified key is mapped, or the
     * given default value if this map contains no mapping for the
     * key.
     *
     * @param key          the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     *                     no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     * @throws NullPointerException if the specified key is null
     */
    public long getOrDefault(K key, long defaultValue) {
        Long v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(ObjLongConsumer<? super K> action) {
        if (action == null) throw new NullPointerException();
        Node<K>[] t;
        if ((t = table) != null) {
            Traverser<K> it = new Traverser<K>(t, t.length, 0, t.length);
            for (Node<K> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping function
     * and enters it into this map unless {@code null}.  The entire
     * method invocation is performed atomically.  The supplied
     * function is invoked exactly once per invocation of this method
     * if the key is absent, else not at all.  Some attempted update
     * operations on this map by other threads may be blocked while
     * computation is in progress, so the computation should be short
     * and simple.
     *
     * <p>The mapping function must not modify this map during computation.
     *
     * @param key             key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     * the specified key, or null if the computed value is null
     * @throws NullPointerException  if the specified key or mappingFunction
     *                               is null
     * @throws IllegalStateException if the computation detectably
     *                               attempts a recursive update to this map that would
     *                               otherwise never complete
     * @throws RuntimeException      or Error if the mappingFunction does so,
     *                               in which case the mapping is left unestablished
     */
    public Long computeIfAbsent(K key, Function<K, Long> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        Long val = null;
        int binCount = 0;
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            K fk;
            long fv;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K> r = new ReservationNode<K>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new Node<K>(h, key, val);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else if (fh == h    // check first node without acquiring lock
                    && ((fk = f.key) == key || (fk != null && key.equals(fk)))) {
                fv = f.val;
                return fv;
            } else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K> e = f; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        if (pred.next != null)
                                            throw new IllegalStateException("Recursive update");
                                        added = true;
                                        pred.next = new Node<>(h, key, val);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }

    @Override
    public long computeIfAbsent(K key, ObjToLongFunction<K> remappingFunction) {
        Long v = computeIfAbsent(key, (Function<K, Long>) remappingFunction::apply);
        assert v != null;
        return v;
    }

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped
     * value.  The entire method invocation is performed atomically.
     * The supplied function is invoked exactly once per invocation of
     * this method if the key is present, else not at all.  Some
     * attempted update operations on this map by other threads may be
     * blocked while computation is in progress, so the computation
     * should be short and simple.
     *
     * <p>The remapping function must not modify this map during computation.
     *
     * @param key               key with which a value may be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException  if the specified key or remappingFunction
     *                               is null
     * @throws IllegalStateException if the computation detectably
     *                               attempts a recursive update to this map that would
     *                               otherwise never complete
     * @throws RuntimeException      or Error if the remappingFunction does so,
     *                               in which case the mapping is unchanged
     */
    public Long computeIfPresent(K key, ObjLongToObjFunction<K, Long> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        Long val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }

    /**
     * Attempts to compute a mapping for the specified key and its
     * current mapped value (or {@code null} if there is no current
     * mapping). The entire method invocation is performed atomically.
     * The supplied function is invoked exactly once per invocation of
     * this method.  Some attempted update operations on this map by
     * other threads may be blocked while computation is in progress,
     * so the computation should be short and simple.
     *
     * <p>The remapping function must not modify this map during computation.
     *
     * @param key               key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException  if the specified key or remappingFunction
     *                               is null
     * @throws IllegalStateException if the computation detectably
     *                               attempts a recursive update to this map that would
     *                               otherwise never complete
     * @throws RuntimeException      or Error if the remappingFunction does so,
     *                               in which case the mapping is unchanged
     */
    public Long compute(K key,
                        BiFunction<? super K, Long, Long> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        Long val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K> r = new ReservationNode<K>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K>(h, key, val);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        if (pred.next != null)
                                            throw new IllegalStateException("Recursive update");
                                        delta = 1;
                                        pred.next = new Node<K>(h, key, val);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> r, p;
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            Long pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }

    /**
     * If the specified key is not already associated with a
     * (non-null) value, associates it with the given value.
     * Otherwise, replaces the value with the results of the given
     * remapping function, or removes if {@code null}. The entire
     * method invocation is performed atomically.  Some attempted
     * update operations on this map by other threads may be blocked
     * while computation is in progress, so the computation should be
     * short and simple, and must not attempt to update any other
     * mappings of this Map.
     *
     * @param key               key with which the specified value is to be associated
     * @param value             the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or the
     *                              remappingFunction is null
     * @throws RuntimeException     or Error if the remappingFunction does so,
     *                              in which case the mapping is unchanged
     */
    public long merge(K key, long value, LongBinaryOperator remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        Long val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K>[] tab = table; ; ) {
            Node<K> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K>(h, key, value))) {
                    delta = 1;
                    val = value;
                    break;
                }
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.applyAsLong(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next = new Node<K>(h, key, val);
                                    break;
                                }
                            }
                        } else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> r = t.root;
                            TreeNode<K> p = (r == null) ? null :
                                    r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                    remappingFunction.applyAsLong(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }

    // ConcurrentHashMap-only methods

    /**
     * Returns the number of mappings. This method should be used
     * instead of {@link #size} because a ConcurrentHashMap may
     * contain more mappings than can be represented as an int. The
     * value returned is an estimate; the actual count may differ if
     * there are concurrent insertions or removals.
     *
     * @return the number of mappings
     * @since 1.8
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param <K> the element type of the returned set
     * @return the new set
     * @since 1.8
     */
    public static <K> KeySetView<K> newKeySet() {
        return new KeySetView<>(new ConcurrentObjectLongHashMap<>());
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param initialCapacity The implementation performs internal
     *                        sizing to accommodate this many elements.
     * @param <K>             the element type of the returned set
     * @return the new set
     * @throws IllegalArgumentException if the initial capacity of
     *                                  elements is negative
     * @since 1.8
     */
    public static <K> KeySetView<K> newKeySet(int initialCapacity) {
        return new KeySetView<K>(new ConcurrentObjectLongHashMap<>(initialCapacity));
    }

    /* ---------------- Special Nodes -------------- */

    /**
     * A node inserted at head of bins during transfer operations.
     */
    static final class ForwardingNode<K> extends Node<K> {
        final Node<K>[] nextTable;

        ForwardingNode(Node<K>[] tab) {
            super(MOVED, null, 0);
            this.nextTable = tab;
        }

        Node<K> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer:
            for (Node<K>[] tab = nextTable; ; ) {
                Node<K> e;
                int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (; ; ) {
                    int eh;
                    K ek;
                    if ((eh = e.hash) == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K>) e).nextTable;
                            continue outer;
                        } else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute.
     */
    static final class ReservationNode<K> extends Node<K> {
        ReservationNode() {
            super(RESERVED, null, 0);
        }

        Node<K> find(int h, Object k) {
            return null;
        }
    }

    /* ---------------- Table Initialization and Resizing -------------- */

    /**
     * Returns the stamp bits for resizing a table of size n.
     * Must be negative when shifted left by RESIZE_STAMP_SHIFT.
     */
    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    private final Node<K>[] initTable() {
        Node<K>[] tab;
        int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin
            else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K>[] nt = (Node<K>[]) new Node<?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }

    /**
     * Adds to count, and if table is too small and not already
     * resizing, initiates transfer. If already resizing, helps
     * perform transfer if work is available.  Rechecks occupancy
     * after a transfer to see if another resize is already needed
     * because resizings are lagging additions.
     *
     * @param x     the count to add
     * @param check if <0, don't check resize, if <= 1 only check if uncontended
     */
    private final void addCount(long x, int check) {
        CounterCell[] cs;
        long b, s;
        if ((cs = counterCells) != null ||
                !U.compareAndSetLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell c;
            long v;
            int m;
            boolean uncontended = true;
            if (cs == null || (m = cs.length - 1) < 0 ||
                    (c = cs[ThreadLocalRandom.getProbe() & m]) == null ||
                    !(uncontended =
                            U.compareAndSetLong(c, CELLVALUE, v = c.value, v + x))) {
                fullAddCount(x, uncontended);
                return;
            }
            if (check <= 1)
                return;
            s = sumCount();
        }
        if (check >= 0) {
            Node<K>[] tab, nt;
            int n, sc;
            while (s >= (long) (sc = sizeCtl) && (tab = table) != null &&
                    (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n) << RESIZE_STAMP_SHIFT;
                if (sc < 0) {
                    if (sc == rs + MAX_RESIZERS || sc == rs + 1 ||
                            (nt = nextTable) == null || transferIndex <= 0)
                        break;
                    if (U.compareAndSetInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                } else if (U.compareAndSetInt(this, SIZECTL, sc, rs + 2))
                    transfer(tab, null);
                s = sumCount();
            }
        }
    }

    /**
     * Helps transfer if a resize is in progress.
     */
    final Node<K>[] helpTransfer(Node<K>[] tab, Node<K> f) {
        Node<K>[] nextTab;
        int sc;
        if (tab != null && (f instanceof ForwardingNode) &&
                (nextTab = ((ForwardingNode<K>) f).nextTable) != null) {
            int rs = resizeStamp(tab.length) << RESIZE_STAMP_SHIFT;
            while (nextTab == nextTable && table == tab &&
                    (sc = sizeCtl) < 0) {
                if (sc == rs + MAX_RESIZERS || sc == rs + 1 ||
                        transferIndex <= 0)
                    break;
                if (U.compareAndSetInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }

    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K>[] nt = (Node<K>[]) new Node<?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                int rs = resizeStamp(n);
                if (U.compareAndSetInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }

    /**
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     */
    private final void transfer(Node<K>[] tab, Node<K>[] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range
        if (nextTab == null) {            // initiating
            try {
                @SuppressWarnings("unchecked")
                Node<K>[] nt = (Node<K>[]) new Node<?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        ForwardingNode<K> fwd = new ForwardingNode<K>(nextTab);
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab
        for (int i = 0, bound = 0; ; ) {
            Node<K> f;
            int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                } else if (U.compareAndSetInt
                        (this, TRANSFERINDEX, nextIndex,
                                nextBound = (nextIndex > stride ?
                                        nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSetInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;
                    finishing = advance = true;
                    i = n; // recheck before commit
                }
            } else if ((f = tabAt(tab, i)) == null)
                advance = casTabAt(tab, i, null, fwd);
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            Node<K> lastRun = f;
                            for (Node<K> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (Node<K> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash;
                                K pk = p.key;
                                long pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof TreeBin) {
                            TreeBin<K> t = (TreeBin<K>) f;
                            TreeNode<K> lo = null, loTail = null;
                            TreeNode<K> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (Node<K> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K> p = new TreeNode<K>
                                        (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new TreeBin<K>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new TreeBin<K>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof ReservationNode)
                            throw new IllegalStateException("Recursive update");
                    }
                }
            }
        }
    }

    /* ---------------- Counter support -------------- */

    /**
     * A padded cell for distributing counts.  Adapted from LongAdder
     * and Striped64.  See their internal docs for explanation.
     */
    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            value = x;
        }
    }

    final long sumCount() {
        CounterCell[] cs = counterCells;
        long sum = baseCount;
        if (cs != null) {
            for (CounterCell c : cs)
                if (c != null)
                    sum += c.value;
        }
        return sum;
    }

    // See LongAdder version for explanation
    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;                // True if last slot nonempty
        for (; ; ) {
            CounterCell[] cs;
            CounterCell c;
            int n;
            long v;
            if ((cs = counterCells) != null && (n = cs.length) > 0) {
                if ((c = cs[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {            // Try to attach new Cell
                        CounterCell r = new CounterCell(x); // Optimistic create
                        if (cellsBusy == 0 &&
                                U.compareAndSetInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {               // Recheck under lock
                                CounterCell[] rs;
                                int m, j;
                                if ((rs = counterCells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;           // Slot is now non-empty
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)       // CAS already known to fail
                    wasUncontended = true;      // Continue after rehash
                else if (U.compareAndSetLong(c, CELLVALUE, v = c.value, v + x))
                    break;
                else if (counterCells != cs || n >= NCPU)
                    collide = false;            // At max size or stale
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 &&
                        U.compareAndSetInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == cs) // Expand table unless stale
                            counterCells = Arrays.copyOf(cs, n << 1);
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;                   // Retry with expanded table
                }
                h = ThreadLocalRandom.advanceProbe(h);
            } else if (cellsBusy == 0 && counterCells == cs &&
                    U.compareAndSetInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {                           // Initialize table
                    if (counterCells == cs) {
                        CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (U.compareAndSetLong(this, BASECOUNT, v = baseCount, v + x))
                break;                          // Fall back on using base
        }
    }

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    private final void treeifyBin(Node<K>[] tab, int index) {
        Node<K> b;
        int n;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        TreeNode<K> hd = null, tl = null;
                        for (Node<K> e = b; e != null; e = e.next) {
                            TreeNode<K> p =
                                    new TreeNode<K>(e.hash, e.key, e.val,
                                            null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new TreeBin<K>(hd));
                    }
                }
            }
        }
    }

    /**
     * Returns a list of non-TreeNodes replacing those in given list.
     */
    static <K> Node<K> untreeify(Node<K> b) {
        Node<K> hd = null, tl = null;
        for (Node<K> q = b; q != null; q = q.next) {
            Node<K> p = new Node<K>(q.hash, q.key, q.val);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }
        return hd;
    }

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins.
     */
    static final class TreeNode<K> extends Node<K> {
        TreeNode<K> parent;  // red-black tree links
        TreeNode<K> left;
        TreeNode<K> right;
        TreeNode<K> prev;    // needed to unlink next upon deletion
        boolean red;

        TreeNode(int hash, K key, long val, Node<K> next,
                 TreeNode<K> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        Node<K> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        final TreeNode<K> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K> p = this;
                do {
                    int ph, dir;
                    K pk;
                    TreeNode<K> q;
                    TreeNode<K> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h)
                        p = pl;
                    else if (ph < h)
                        p = pr;
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;
                    else if (pl == null)
                        p = pr;
                    else if (pr == null)
                        p = pl;
                    else if ((kc != null ||
                            (kc = comparableClassFor(k)) != null) &&
                            (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;
                    else
                        p = pl;
                } while (p != null);
            }
            return null;
        }
    }

    /* ---------------- TreeBins -------------- */

    /**
     * TreeNodes used at the heads of bins. TreeBins do not hold user
     * keys or values, but instead point to list of TreeNodes and
     * their root. They also maintain a parasitic read-write lock
     * forcing writers (who hold bin lock) to wait for readers (who do
     * not) to complete before tree restructuring operations.
     */
    static final class TreeBin<K> extends Node<K> {
        TreeNode<K> root;
        volatile TreeNode<K> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock

        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }

        /**
         * Creates bin with initial set of nodes headed by b.
         */
        TreeBin(TreeNode<K> b) {
            super(TREEBIN, null, 0);
            this.first = b;
            TreeNode<K> r = null;
            for (TreeNode<K> x = b, next; x != null; x = next) {
                next = (TreeNode<K>) x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (TreeNode<K> p = r; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);
                        TreeNode<K> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }

        /**
         * Acquires write lock for tree restructuring.
         */
        private final void lockRoot() {
            if (!U.compareAndSetInt(this, LOCKSTATE, 0, WRITER))
                contendedLock(); // offload to separate method
        }

        /**
         * Releases write lock for tree restructuring.
         */
        private final void unlockRoot() {
            lockState = 0;
        }

        /**
         * Possibly blocks awaiting root lock.
         */
        private final void contendedLock() {
            boolean waiting = false;
            for (int s; ; ) {
                if (((s = lockState) & ~WAITER) == 0) {
                    if (U.compareAndSetInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    if (U.compareAndSetInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                } else if (waiting)
                    LockSupport.park(this);
            }
        }

        /**
         * Returns matching node or null if none. Tries to search
         * using tree comparisons from root, but continues linear
         * search when lock not available.
         */
        final Node<K> find(int h, Object k) {
            if (k != null) {
                for (Node<K> e = first; e != null; ) {
                    int s;
                    K ek;
                    if (((s = lockState) & (WAITER | WRITER)) != 0) {
                        if (e.hash == h &&
                                ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    } else if (U.compareAndSetInt(this, LOCKSTATE, s,
                            s + READER)) {
                        TreeNode<K> r, p;
                        try {
                            p = ((r = root) == null ? null :
                                    r.findTreeNode(h, k, null));
                        } finally {
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                                    (READER | WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }

        /**
         * Finds or adds a node.
         *
         * @return null if added
         */
        final TreeNode<K> putTreeVal(int h, K k, long v) {
            Class<?> kc = null;
            boolean searched = false;
            for (TreeNode<K> p = root; ; ) {
                int dir, ph;
                K pk;
                if (p == null) {
                    first = root = new TreeNode<K>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h)
                    dir = -1;
                else if (ph < h)
                    dir = 1;
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;
                else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        TreeNode<K> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.findTreeNode(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }
                    dir = tieBreakOrder(k, pk);
                }

                TreeNode<K> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<K> x, f = first;
                    first = x = new TreeNode<K>(h, k, v, f, xp);
                    if (f != null)
                        f.prev = x;
                    if (dir <= 0)
                        xp.left = x;
                    else
                        xp.right = x;
                    if (!xp.red)
                        x.red = true;
                    else {
                        lockRoot();
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }

        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         *
         * @return true if now too small, so should be untreeified
         */
        final boolean removeTreeNode(TreeNode<K> p) {
            TreeNode<K> next = (TreeNode<K>) p.next;
            TreeNode<K> pred = p.prev;  // unlink traversal pointers
            TreeNode<K> r, rl;
            if (pred == null)
                first = next;
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null || // too small
                    (rl = r.left) == null || rl.left == null)
                return true;
            lockRoot();
            try {
                TreeNode<K> replacement;
                TreeNode<K> pl = p.left;
                TreeNode<K> pr = p.right;
                if (pl != null && pr != null) {
                    TreeNode<K> s = pr, sl;
                    while ((sl = s.left) != null) // find successor
                        s = sl;
                    boolean c = s.red;
                    s.red = p.red;
                    p.red = c; // swap colors
                    TreeNode<K> sr = s.right;
                    TreeNode<K> pp = p.parent;
                    if (s == pr) { // p was s's direct parent
                        p.parent = s;
                        s.right = p;
                    } else {
                        TreeNode<K> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left)
                                sp.left = p;
                            else
                                sp.right = p;
                        }
                        if ((s.right = pr) != null)
                            pr.parent = s;
                    }
                    p.left = null;
                    if ((p.right = sr) != null)
                        sr.parent = p;
                    if ((s.left = pl) != null)
                        pl.parent = s;
                    if ((s.parent = pp) == null)
                        r = s;
                    else if (p == pp.left)
                        pp.left = s;
                    else
                        pp.right = s;
                    if (sr != null)
                        replacement = sr;
                    else
                        replacement = p;
                } else if (pl != null)
                    replacement = pl;
                else if (pr != null)
                    replacement = pr;
                else
                    replacement = p;
                if (replacement != p) {
                    TreeNode<K> pp = replacement.parent = p.parent;
                    if (pp == null)
                        r = replacement;
                    else if (p == pp.left)
                        pp.left = replacement;
                    else
                        pp.right = replacement;
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {  // detach pointers
                    TreeNode<K> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left)
                            pp.left = null;
                        else if (p == pp.right)
                            pp.right = null;
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot();
            }
            assert checkInvariants(root);
            return false;
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        static <K> TreeNode<K> rotateLeft(TreeNode<K> root,
                                          TreeNode<K> p) {
            TreeNode<K> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K> TreeNode<K> rotateRight(TreeNode<K> root,
                                           TreeNode<K> p) {
            TreeNode<K> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K> TreeNode<K> balanceInsertion(TreeNode<K> root,
                                                TreeNode<K> x) {
            x.red = true;
            for (TreeNode<K> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null)
                    return root;
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K> TreeNode<K> balanceDeletion(TreeNode<K> root,
                                               TreeNode<K> x) {
            for (TreeNode<K> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        TreeNode<K> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else { // symmetric
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        TreeNode<K> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Checks invariants recursively for the tree of Nodes rooted at t.
         */
        static <K> boolean checkInvariants(TreeNode<K> t) {
            TreeNode<K> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (TreeNode<K>) t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }

        private static final long LOCKSTATE
                = U.objectFieldOffset(TreeBin.class, "lockState");
    }

    /* ----------------Table Traversal -------------- */

    /**
     * Records the table, its length, and current traversal index for a
     * traverser that must process a region of a forwarded table before
     * proceeding with current table.
     */
    static final class TableStack<K> {
        int length;
        int index;
        Node<K>[] tab;
        TableStack<K> next;
    }

    /**
     * Encapsulates traversal for methods such as containsValue; also
     * serves as a base class for other iterators and spliterators.
     * <p>
     * Method advance visits once each still-valid node that was
     * reachable upon iterator construction. It might miss some that
     * were added to a bin after the bin was visited, which is OK wrt
     * consistency guarantees. Maintaining this property in the face
     * of possible ongoing resizes requires a fair amount of
     * bookkeeping state that is difficult to optimize away amidst
     * volatile accesses.  Even so, traversal maintains reasonable
     * throughput.
     * <p>
     * Normally, iteration proceeds bin-by-bin traversing lists.
     * However, if the table has been resized, then all future steps
     * must traverse both the bin at the current index as well as at
     * (index + baseSize); and so on for further resizings. To
     * paranoically cope with potential sharing by users of iterators
     * across threads, iteration terminates if a bounds checks fails
     * for a table read.
     */
    static class Traverser<K> {
        Node<K>[] tab;        // current table; updated if resized
        Node<K> next;         // the next entry to use
        TableStack<K> stack, spare; // to save/restore on ForwardingNodes
        int index;              // index of bin to use next
        int baseIndex;          // current index of initial table
        int baseLimit;          // index bound for initial table
        final int baseSize;     // initial table size

        Traverser(Node<K>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        /**
         * Advances if possible, returning next valid node, or null if none.
         */
        final Node<K> advance() {
            Node<K> e;
            if ((e = next) != null)
                e = e.next;
            for (; ; ) {
                Node<K>[] t;
                int i, n;  // must use locals in checks
                if (e != null)
                    return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0)
                    return next = null;
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K>) e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    } else if (e instanceof TreeBin)
                        e = ((TreeBin<K>) e).first;
                    else
                        e = null;
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex; // visit upper slots if present
            }
        }

        /**
         * Saves traversal state upon encountering a forwarding node.
         */
        private void pushState(Node<K>[] t, int i, int n) {
            TableStack<K> s = spare;  // reuse if possible
            if (s != null)
                spare = s.next;
            else
                s = new TableStack<K>();
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        /**
         * Possibly pops traversal state.
         *
         * @param n length of current table
         */
        private void recoverState(int n) {
            TableStack<K> s;
            int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K> next = s.next;
                s.next = spare; // save for reuse
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }

    /**
     * Base of key, value, and entry Iterators. Adds fields to
     * Traverser to support iterator.remove.
     */
    static class BaseIterator<K> extends Traverser<K> {
        final ConcurrentObjectLongHashMap<K> map;
        Node<K> lastReturned;

        BaseIterator(Node<K>[] tab, int size, int index, int limit,
                     ConcurrentObjectLongHashMap<K> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() {
            return next != null;
        }

        public final boolean hasMoreElements() {
            return next != null;
        }

        public final void remove() {
            Node<K> p;
            if ((p = lastReturned) == null)
                throw new IllegalStateException();
            lastReturned = null;
            map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K> extends BaseIterator<K>
            implements Iterator<K>, Enumeration<K> {
        KeyIterator(Node<K>[] tab, int size, int index, int limit,
                    ConcurrentObjectLongHashMap<K> map) {
            super(tab, size, index, limit, map);
        }

        public final K next() {
            Node<K> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            K k = p.key;
            lastReturned = p;
            advance();
            return k;
        }

        public final K nextElement() {
            return next();
        }
    }

    static final class ValueIterator<K> extends BaseIterator<K>
            implements LongIterator {
        ValueIterator(Node<K>[] tab, int size, int index, int limit,
                      ConcurrentObjectLongHashMap<K> map) {
            super(tab, size, index, limit, map);
        }

        public final long next() {
            Node<K> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            long v = p.val;
            lastReturned = p;
            advance();
            return v;
        }
    }

    static final class EntryIterator<K> extends BaseIterator<K>
            implements Iterator<Entry<K>> {
        EntryIterator(Node<K>[] tab, int size, int index, int limit,
                      ConcurrentObjectLongHashMap<K> map) {
            super(tab, size, index, limit, map);
        }

        public final Entry<K> next() {
            Node<K> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            K k = p.key;
            long v = p.val;
            lastReturned = p;
            advance();
            return new MapEntry<>(k, v, map);
        }
    }

    /**
     * Exported Entry for EntryIterator.
     */
    static final class MapEntry<K> implements Entry<K> {
        final K key; // non-null
        long val;       // non-null
        final ConcurrentObjectLongHashMap<K> map;

        MapEntry(K key, long val, ConcurrentObjectLongHashMap<K> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        public K getKey() {
            return key;
        }

        public long getValue() {
            return val;
        }

        public int hashCode() {
            return key.hashCode() ^ Long.hashCode(val);
        }

        public String toString() {
            return key + "=" + val;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ConcurrentObjectLongMap.Entry)) {
                return false;
            }
            Entry<?> that = (Entry<?>) o;
            Object k = that.getKey();
            long v = that.getValue();
            return k != null && k.equals(key) && v == val;
        }
    }

    static final class KeySpliterator<K> extends Traverser<K>
            implements Spliterator<K> {
        long est;               // size estimate

        KeySpliterator(Node<K>[] tab, int size, int index, int limit,
                       long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public KeySpliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new KeySpliterator<K>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K> p; (p = advance()) != null; )
                action.accept(p.key);
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.key);
            return true;
        }

        public long estimateSize() {
            return est;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }

    static final class ValueSpliterator<K> extends Traverser<K>
            implements Spliterator.OfLong {
        long est;               // size estimate

        ValueSpliterator(Node<K>[] tab, int size, int index, int limit,
                         long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public ValueSpliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new ValueSpliterator<K>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        public void forEachRemaining(LongConsumer action) {
            if (action == null) throw new NullPointerException();
            for (Node<K> p; (p = advance()) != null; )
                action.accept(p.val);
        }

        public boolean tryAdvance(LongConsumer action) {
            if (action == null) throw new NullPointerException();
            Node<K> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.val);
            return true;
        }

        public long estimateSize() {
            return est;
        }

        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL;
        }
    }

    static final class EntrySpliterator<K> extends Traverser<K>
            implements Spliterator<Entry<K>> {
        final ConcurrentObjectLongHashMap<K> map; // To export MapEntry
        long est;               // size estimate

        EntrySpliterator(Node<K>[] tab, int size, int index, int limit,
                         long est, ConcurrentObjectLongHashMap<K> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        public EntrySpliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new EntrySpliterator<K>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1, map);
        }

        public void forEachRemaining(Consumer<? super Entry<K>> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K> p; (p = advance()) != null; )
                action.accept(new MapEntry<K>(p.key, p.val, map));
        }

        public boolean tryAdvance(Consumer<? super Entry<K>> action) {
            if (action == null) throw new NullPointerException();
            Node<K> p;
            if ((p = advance()) == null)
                return false;
            action.accept(new MapEntry<K>(p.key, p.val, map));
            return true;
        }

        public long estimateSize() {
            return est;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }

    /* ----------------Views -------------- */

    /**
     * Base class for views.
     */
    abstract static class CollectionView<K, E>
            implements Collection<E>, Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentObjectLongHashMap<K> map;

        CollectionView(ConcurrentObjectLongHashMap<K> map) {
            this.map = map;
        }

        /**
         * Returns the map backing this view.
         *
         * @return the map backing this view
         */
        public ConcurrentObjectLongHashMap<K> getMap() {
            return map;
        }

        /**
         * Removes all of the elements from this view, by removing all
         * the mappings from the map backing this view.
         */
        public final void clear() {
            map.clear();
        }

        public final int size() {
            return map.size();
        }

        public final boolean isEmpty() {
            return map.isEmpty();
        }

        // implementations below rely on concrete classes supplying these
        // abstract methods

        /**
         * Returns an iterator over the elements in this collection.
         *
         * <p>The returned iterator is
         * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
         *
         * @return an iterator over the elements in this collection
         */
        public abstract Iterator<E> iterator();

        public abstract boolean contains(Object o);

        public abstract boolean remove(Object o);

        private static final String OOME_MSG = "Required array size too large";

        public final Object[] toArray() {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(OOME_MSG);
            int n = (int) sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(OOME_MSG);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(OOME_MSG);
            int m = (int) sz;
            T[] r = (a.length >= m) ? a :
                    (T[]) java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(OOME_MSG);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T) e;
            }
            if (a == r && i < n) {
                r[i] = null; // null-terminate
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        /**
         * Returns a string representation of this collection.
         * The string representation consists of the string representations
         * of the collection's elements in the order they are returned by
         * its iterator, enclosed in square brackets ({@code "[]"}).
         * Adjacent elements are separated by the characters {@code ", "}
         * (comma and space).  Elements are converted to strings as by
         * {@link String#valueOf(Object)}.
         *
         * @return a string representation of this collection
         */
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (; ; ) {
                    Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext())
                        break;
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e))
                        return false;
                }
            }
            return true;
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            // Use (c instanceof Set) as a hint that lookup in c is as
            // efficient as this view
            Node<K>[] t;
            if ((t = map.table) == null) {
                return false;
            } else if (c instanceof Set<?> && c.size() > t.length) {
                for (Iterator<?> it = iterator(); it.hasNext(); ) {
                    if (c.contains(it.next())) {
                        it.remove();
                        modified = true;
                    }
                }
            } else {
                for (Object e : c)
                    modified |= remove(e);
            }
            return modified;
        }

        public final boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext(); ) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of keys, in
     * which additions may optionally be enabled by mapping to a
     * common value.  This class cannot be directly instantiated.
     * See {@link #keySet() keySet()},
     * {@link #newKeySet() newKeySet()},
     * {@link #newKeySet(int) newKeySet(int)}.
     *
     * @since 1.8
     */
    public static class KeySetView<K> implements Iterable<K>, Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private ConcurrentObjectLongHashMap<K> map;

        KeySetView(ConcurrentObjectLongHashMap<K> map) {  // non-public
            this.map = map;
        }

        /**
         * {@inheritDoc}
         *
         * @throws NullPointerException if the specified key is null
         */
        public boolean contains(K o) {
            return map.containsKey(o);
        }

        /**
         * Removes the key from this map view, by removing the key (and its
         * corresponding value) from the backing map.  This method does
         * nothing if the key is not in the map.
         *
         * @param o the key to be removed from the backing map
         * @return {@code true} if the backing map contained the specified key
         * @throws NullPointerException if the specified key is null
         */
        public boolean remove(K o) {
            return map.remove(o) != null;
        }

        /**
         * @return an iterator over the keys of the backing map
         */
        public Iterator<K> iterator() {
            Node<K>[] t;
            ConcurrentObjectLongHashMap<K> m = map;
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeyIterator<K>(t, f, 0, f, m);
        }

        public Spliterator<K> spliterator() {
            Node<K>[] t;
            ConcurrentObjectLongHashMap<K> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeySpliterator<K>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K>[] t;
            if ((t = map.table) != null) {
                Traverser<K> it = new Traverser<K>(t, t.length, 0, t.length);
                for (Node<K> p; (p = it.advance()) != null; )
                    action.accept(p.key);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Collection} of
     * values, in which additions are disabled. This class cannot be
     * directly instantiated. See {@link #values()}.
     */
    static final class ValuesView
            implements LongIterable, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        private ConcurrentObjectLongHashMap<?> map;

        ValuesView(ConcurrentObjectLongHashMap<?> map) {
            this.map = map;
        }

        public final LongIterator iterator() {
            ConcurrentObjectLongHashMap<?> m = map;
            Node<?>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueIterator(t, f, 0, f, m);
        }

        public Spliterator.OfLong spliterator() {
            Node<?>[] t;
            ConcurrentObjectLongHashMap<?> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueSpliterator(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(LongConsumer action) {
            if (action == null) throw new NullPointerException();
            Node<?>[] t;
            if ((t = map.table) != null) {
                Traverser<?> it = new Traverser(t, t.length, 0, t.length);
                for (Node<?> p; (p = it.advance()) != null; )
                    action.accept(p.val);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of (key, value)
     * entries.  This class cannot be directly instantiated. See
     * {@link #entrySet()}.
     */
    static final class EntrySetView<K> implements Iterable<Entry<K>>, Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        private ConcurrentObjectLongHashMap<K> map;

        EntrySetView(ConcurrentObjectLongHashMap<K> map) {
            this.map = map;
        }

        /**
         * @return an iterator over the entries of the backing map
         */
        public Iterator<Entry<K>> iterator() {
            ConcurrentObjectLongHashMap<K> m = map;
            Node<K>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntryIterator<K>(t, f, 0, f, m);
        }

        public final int hashCode() {
            int h = 0;
            Node<K>[] t;
            if ((t = map.table) != null) {
                Traverser<K> it = new Traverser<K>(t, t.length, 0, t.length);
                for (Node<K> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        public Spliterator<Entry<K>> spliterator() {
            Node<K>[] t;
            ConcurrentObjectLongHashMap<K> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntrySpliterator<>(t, f, 0, f, n < 0L ? 0L : n, m);
        }

        public void forEach(Consumer<? super Entry<K>> action) {
            if (action == null) throw new NullPointerException();
            Node<K>[] t;
            if ((t = map.table) != null) {
                Traverser<K> it = new Traverser<K>(t, t.length, 0, t.length);
                for (Node<K> p; (p = it.advance()) != null; )
                    action.accept(new MapEntry<K>(p.key, p.val, map));
            }
        }

    }

    // Unsafe mechanics
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long SIZECTL
            = U.objectFieldOffset(ConcurrentObjectLongHashMap.class, "sizeCtl");
    private static final long TRANSFERINDEX
            = U.objectFieldOffset(ConcurrentObjectLongHashMap.class, "transferIndex");
    private static final long BASECOUNT
            = U.objectFieldOffset(ConcurrentObjectLongHashMap.class, "baseCount");
    private static final long CELLSBUSY
            = U.objectFieldOffset(ConcurrentObjectLongHashMap.class, "cellsBusy");
    private static final long CELLVALUE
            = U.objectFieldOffset(CounterCell.class, "value");
    private static final int ABASE = U.arrayBaseOffset(Node[].class);
    private static final int ASHIFT;

    static {
        int scale = U.arrayIndexScale(Node[].class);
        if ((scale & (scale - 1)) != 0)
            throw new ExceptionInInitializerError("array index scale not a power of two");
        ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);

        // Reduce the risk of rare disastrous classloading in first call to
        // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
        Class<?> ensureLoaded = LockSupport.class;

        // Eager class load observed to help JIT during startup
        ensureLoaded = ReservationNode.class;
    }
}
