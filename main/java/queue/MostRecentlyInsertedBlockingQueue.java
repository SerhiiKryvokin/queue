package queue;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/*
Blocking queue based on array.
Insertion blocking operations are not supported because queue always accept new elements and evict the oldest ones.
Iteration mechanics copied from java.util.concurrent.ArrayBlockingQueue.
 */
public class MostRecentlyInsertedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

    private Object[] elements;

    private int head;

    private int tail;

    private ReentrantLock lock;

    private Condition notEmpty;

    transient Itrs itrs = null;

    public MostRecentlyInsertedBlockingQueue(int capacity) {
        this(capacity, false);
    }

    public MostRecentlyInsertedBlockingQueue(int capacity, boolean fair) {
        if (capacity <= 0)
            throw new IllegalArgumentException();
        elements = new Object[capacity];
        lock = new ReentrantLock(fair);
        notEmpty = lock.newCondition();
    }


    @Override
    public Object[] toArray() {
        Object[] a;
        lock.lock();
        try {
            int size = size();
            a = new Object[size];
            int fromHeadToBorder = elements.length - head;
            if (size <= fromHeadToBorder)
                System.arraycopy(elements, head, a, 0, size);
            else {
                System.arraycopy(elements, head, a, 0, fromHeadToBorder);
                System.arraycopy(elements, 0, a, fromHeadToBorder, size - fromHeadToBorder);
            }
        } finally {
            lock.unlock();
        }
        return a;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        lock.lock();
        try {
            final int size = size();
            final int len = a.length;
            if (len < size)
                a = (T[]) java.lang.reflect.Array.newInstance(
                        a.getClass().getComponentType(), size);
            int fromHeadToBorder = elements.length - tail;
            if (size <= fromHeadToBorder)
                System.arraycopy(elements, head, a, 0, size);
            else {
                System.arraycopy(elements, head, a, 0, fromHeadToBorder);
                System.arraycopy(elements, 0, a, fromHeadToBorder, size - fromHeadToBorder);
            }
            if (len > size)
                a[size] = null;
        } finally {
            lock.unlock();
        }
        return a;
    }

    @Override
    public boolean remove(Object o) {
        if (o == null)
            return false;
        lock.lock();
        try {
            if (elements[head] == null) {
                return false;
            }
            int i = head;
            do {
                if (o.equals(elements[i])) {
                    removeAt(i);
                    return true;
                }
                i = cyclicIncrement(i);
            } while (i != tail);
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        offer(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (elements[head] == null)
                notEmpty.await();
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanosTimeout = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (elements[head] == null) {
                if (nanosTimeout <= 0)
                    return null;
                nanosTimeout = notEmpty.awaitNanos(nanosTimeout);
            }
            return dequeue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            throw new IllegalArgumentException();
        }
        lock.lock();
        try {
            int elementsToTransferNumber = Math.min(maxElements, size());
            int i = 0;
            try {
                while (i < elementsToTransferNumber) {
                    c.add(dequeue());
                    i++;
                }
                return elementsToTransferNumber;
            } finally {
                if (i > 0) {
                    if (itrs != null) {
                        if (elements[head] == null)
                            itrs.queueIsEmpty();
                        else if (i > tail)
                            itrs.takeIndexWrapped();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("Offered object should not be null");
        }
        lock.lock();
        try {
            if (head == tail && elements[head] != null) {
                head = cyclicIncrement(head);
            }
            elements[tail] = e;
            tail = cyclicIncrement(tail);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E poll() {
        lock.lock();
        try {
            return elements[head] == null ? null : dequeue();
        } finally {
            lock.unlock();
        }

    }

    @Override
    public E peek() {
        lock.lock();
        try {
            if (elements[head] == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            E headElement = (E) elements[head];
            return headElement;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public int size() {
        lock.lock();
        try {
            int size;

            if (tail > head) {
                size = tail - head;
            } else if (tail == head) {
                size = elements[head] == null ? 0 : elements.length;
            } else {
                size = elements.length - head + tail;
            }

            return size;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return elements[head] == null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            head = 0;
            tail = 0;
            Arrays.fill(elements, null);
            if (itrs != null)
                itrs.queueIsEmpty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        if (o == null)
            return false;
        lock.lock();
        try {
            if (elements[head] == null)
                return false;
            int i = head;
            do {
                if (o.equals(elements[i]))
                    return true;
                i = cyclicIncrement(i);
            } while (i != tail);
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            if (elements[head] == null) {
                return "[]";
            }
            int size = size();
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = head; ; i = cyclicIncrement(i)) {
                sb.append(elements[i]);
                if (--size == 0) {
                    return sb.append("]").toString();
                }
                sb.append(",").append(' ');
            }
        } finally {
            lock.unlock();
        }
    }

    private E dequeue() {
        @SuppressWarnings("unchecked")
        E headElement = (E) elements[head];
        elements[head] = null;
        head = cyclicIncrement(head);
        if (itrs != null)
            itrs.elementDequeued();
        return headElement;
    }

    private int cyclicIncrement(int index) {
        return ++index == elements.length ? 0 : index;
    }


    class Itrs {

        /**
         * Node in a linked list of weak iterator references.
         */
        private class Node extends WeakReference<Itr> {
            Node next;

            Node(Itr iterator, Node next) {
                super(iterator);
                this.next = next;
            }
        }

        /**
         * Incremented whenever takeIndex wraps around to 0
         */
        int cycles = 0;

        /**
         * Linked list of weak iterator references
         */
        private Node itrsHead;

        /**
         * Used to expunge stale iterators
         */
        private Node sweeper = null;

        private static final int SHORT_SWEEP_PROBES = 4;
        private static final int LONG_SWEEP_PROBES = 16;

        Itrs(Itr initial) {
            register(initial);
        }

        /**
         * Sweeps itrs, looking for and expunging stale iterators.
         * If at least one was found, tries harder to find more.
         * Called only from iterating thread.
         *
         * @param tryHarder whether to start in try-harder mode, because
         *                  there is known to be at least one iterator to collect
         */
        void doSomeSweeping(boolean tryHarder) {
            // assert lock.getHoldCount() == 1;
            // assert itrsHead != null;
            int probes = tryHarder ? LONG_SWEEP_PROBES : SHORT_SWEEP_PROBES;
            Node o, p;
            final Node sweeper = this.sweeper;
            boolean passedGo;   // to limit search to one full sweep

            if (sweeper == null) {
                o = null;
                p = itrsHead;
                passedGo = true;
            } else {
                o = sweeper;
                p = o.next;
                passedGo = false;
            }

            for (; probes > 0; probes--) {
                if (p == null) {
                    if (passedGo)
                        break;
                    o = null;
                    p = itrsHead;
                    passedGo = true;
                }
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.isDetached()) {
                    // found a discarded/exhausted iterator
                    probes = LONG_SWEEP_PROBES; // "try harder"
                    // unlink p
                    p.clear();
                    p.next = null;
                    if (o == null) {
                        itrsHead = next;
                        if (next == null) {
                            // We've run out of iterators to track; retire
                            itrs = null;
                            return;
                        }
                    } else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }

            this.sweeper = (p == null) ? null : o;
        }

        /**
         * Adds a new iterator to the linked list of tracked iterators.
         */
        void register(Itr itr) {
            // assert lock.getHoldCount() == 1;
            itrsHead = new Node(itr, itrsHead);
        }

        /**
         * Called whenever takeIndex wraps around to 0.
         * <p>
         * Notifies all iterators, and expunges any that are now stale.
         */
        void takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            cycles++;
            for (Node o = null, p = itrsHead; p != null; ) {
                final Itr it = p.get();
                final Node next = p.next;
                if (it == null || it.takeIndexWrapped()) {
                    // unlink p
                    // assert it == null || it.isDetached();
                    p.clear();
                    p.next = null;
                    if (o == null)
                        itrsHead = next;
                    else
                        o.next = next;
                } else {
                    o = p;
                }
                p = next;
            }
            if (itrsHead == null)   // no more iterators to track
                itrs = null;
        }

        /**
         * Called whenever the queue becomes empty.
         * <p>
         * Notifies all active iterators that the queue is empty,
         * clears all weak refs, and unlinks the itrs datastructure.
         */
        void queueIsEmpty() {
            // assert lock.getHoldCount() == 1;
            for (Node p = itrsHead; p != null; p = p.next) {
                Itr it = p.get();
                if (it != null) {
                    p.clear();
                    it.shutdown();
                }
            }
            itrsHead = null;
            itrs = null;
        }

        /**
         * Called whenever an element has been dequeued (at takeIndex).
         */
        void elementDequeued() {
            // assert lock.getHoldCount() == 1;
            if (elements[head] == null)
                queueIsEmpty();
            else if (head == 0)
                takeIndexWrapped();
        }
    }

    private class Itr implements Iterator<E> {
        /**
         * Index to look for new nextItem; NONE at end
         */
        private int cursor;

        /**
         * Element to be returned by next call to next(); null if none
         */
        private E nextItem;

        /**
         * Index of nextItem; NONE if none, REMOVED if removed elsewhere
         */
        private int nextIndex;

        /**
         * Last element returned; null if none or not detached.
         */
        private E lastItem;

        /**
         * Index of lastItem, NONE if none, REMOVED if removed elsewhere
         */
        private int lastRet;

        /**
         * Previous value of takeIndex, or DETACHED when detached
         */
        private int prevTakeIndex;

        /**
         * Previous value of iters.cycles
         */
        private int prevCycles;

        /**
         * Special index value indicating "not available" or "undefined"
         */
        private static final int NONE = -1;

        /**
         * Special index value indicating "removed elsewhere", that is,
         * removed by some operation other than a call to this.remove().
         */
        private static final int REMOVED = -2;

        /**
         * Special value for prevTakeIndex indicating "detached mode"
         */
        private static final int DETACHED = -3;

        Itr() {
            // assert lock.getHoldCount() == 0;
            lastRet = NONE;
            final ReentrantLock lock = MostRecentlyInsertedBlockingQueue.this.lock;
            lock.lock();
            try {
                if (elements[head] == null) {
                    // assert itrs == null;
                    cursor = NONE;
                    nextIndex = NONE;
                    prevTakeIndex = DETACHED;
                } else {
                    final int takeIndex = MostRecentlyInsertedBlockingQueue.this.head;
                    prevTakeIndex = takeIndex;
                    nextItem = itemAt(nextIndex = takeIndex);
                    cursor = incCursor(takeIndex);
                    if (itrs == null) {
                        itrs = new Itrs(this);
                    } else {
                        itrs.register(this); // in this order
                        itrs.doSomeSweeping(false);
                    }
                    prevCycles = itrs.cycles;
                    // assert takeIndex >= 0;
                    // assert prevTakeIndex == takeIndex;
                    // assert nextIndex >= 0;
                    // assert nextItem != null;
                }
            } finally {
                lock.unlock();
            }
        }

        boolean isDetached() {
            // assert lock.getHoldCount() == 1;
            return prevTakeIndex < 0;
        }

        private int incCursor(int index) {
            // assert lock.getHoldCount() == 1;
            if (++index == elements.length)
                index = 0;
            if (index == tail)
                index = NONE;
            return index;
        }

        /**
         * Returns true if index is invalidated by the given number of
         * dequeues, starting from prevTakeIndex.
         */
        private boolean invalidated(int index, int prevTakeIndex,
                                    long dequeues, int length) {
            if (index < 0)
                return false;
            int distance = index - prevTakeIndex;
            if (distance < 0)
                distance += length;
            return dequeues > distance;
        }

        /**
         * Adjusts indices to incorporate all dequeues since the last
         * operation on this iterator.  Call only from iterating thread.
         */
        private void incorporateDequeues() {
            // assert lock.getHoldCount() == 1;
            // assert itrs != null;
            // assert !isDetached();
            // assert count > 0;

            final int cycles = itrs.cycles;
            final int takeIndex = MostRecentlyInsertedBlockingQueue.this.head;
            final int prevCycles = this.prevCycles;
            final int prevTakeIndex = this.prevTakeIndex;

            if (cycles != prevCycles || takeIndex != prevTakeIndex) {
                final int len = elements.length;
                // how far takeIndex has advanced since the previous
                // operation of this iterator
                long dequeues = (cycles - prevCycles) * len
                        + (takeIndex - prevTakeIndex);

                // Check indices for invalidation
                if (invalidated(lastRet, prevTakeIndex, dequeues, len))
                    lastRet = REMOVED;
                if (invalidated(nextIndex, prevTakeIndex, dequeues, len))
                    nextIndex = REMOVED;
                if (invalidated(cursor, prevTakeIndex, dequeues, len))
                    cursor = takeIndex;

                if (cursor < 0 && nextIndex < 0 && lastRet < 0)
                    detach();
                else {
                    this.prevCycles = cycles;
                    this.prevTakeIndex = takeIndex;
                }
            }
        }

        /**
         * Called when itrs should stop tracking this iterator, either
         * because there are no more indices to update (cursor < 0 &&
         * nextIndex < 0 && lastRet < 0) or as a special exception, when
         * lastRet >= 0, because hasNext() is about to return false for the
         * first time.  Call only from iterating thread.
         */
        private void detach() {
            // Switch to detached mode
            // assert lock.getHoldCount() == 1;
            // assert cursor == NONE;
            // assert nextIndex < 0;
            // assert lastRet < 0 || nextItem == null;
            // assert lastRet < 0 ^ lastItem != null;
            if (prevTakeIndex >= 0) {
                // assert itrs != null;
                prevTakeIndex = DETACHED;
                // try to unlink from itrs (but not too hard)
                itrs.doSomeSweeping(true);
            }
        }

        /**
         * For performance reasons, we would like not to acquire a lock in
         * hasNext in the common case.  To allow for this, we only access
         * fields (i.e. nextItem) that are not modified by update operations
         * triggered by queue modifications.
         */
        public boolean hasNext() {
            // assert lock.getHoldCount() == 0;
            if (nextItem != null)
                return true;
            noNext();
            return false;
        }

        private void noNext() {
            final ReentrantLock lock = MostRecentlyInsertedBlockingQueue.this.lock;
            lock.lock();
            try {
                // assert cursor == NONE;
                // assert nextIndex == NONE;
                if (!isDetached()) {
                    // assert lastRet >= 0;
                    incorporateDequeues(); // might update lastRet
                    if (lastRet >= 0) {
                        lastItem = itemAt(lastRet);
                        // assert lastItem != null;
                        detach();
                    }
                }
                // assert isDetached();
                // assert lastRet < 0 ^ lastItem != null;
            } finally {
                lock.unlock();
            }
        }

        public E next() {
            // assert lock.getHoldCount() == 0;
            final E x = nextItem;
            if (x == null)
                throw new NoSuchElementException();
            final ReentrantLock lock = MostRecentlyInsertedBlockingQueue.this.lock;
            lock.lock();
            try {
                if (!isDetached())
                    incorporateDequeues();
                // assert nextIndex != NONE;
                // assert lastItem == null;
                lastRet = nextIndex;
                final int cursor = this.cursor;
                if (cursor >= 0) {
                    nextItem = itemAt(nextIndex = cursor);
                    // assert nextItem != null;
                    this.cursor = incCursor(cursor);
                } else {
                    nextIndex = NONE;
                    nextItem = null;
                }
            } finally {
                lock.unlock();
            }
            return x;
        }

        /**
         * Called to notify the iterator that the queue is empty, or that it
         * has fallen hopelessly behind, so that it should abandon any
         * further iteration, except possibly to return one more element
         * from next(), as promised by returning true from hasNext().
         */
        void shutdown() {
            // assert lock.getHoldCount() == 1;
            cursor = NONE;
            if (nextIndex >= 0)
                nextIndex = REMOVED;
            if (lastRet >= 0) {
                lastRet = REMOVED;
                lastItem = null;
            }
            prevTakeIndex = DETACHED;
            // Don't set nextItem to null because we must continue to be
            // able to return it on next().
            //
            // Caller will unlink from itrs when convenient.
        }

        /**
         * Called whenever takeIndex wraps around to zero.
         *
         * @return true if this iterator should be unlinked from itrs
         */
        boolean takeIndexWrapped() {
            // assert lock.getHoldCount() == 1;
            if (isDetached())
                return true;
            if (itrs.cycles - prevCycles > 1) {
                // All the elements that existed at the time of the last
                // operation are gone, so abandon further iteration.
                shutdown();
                return true;
            }
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    final E itemAt(int i) {
        return (E) elements[i];
    }

    void removeAt(final int removeIndex) {
        if (removeIndex == head) {
            elements[head] = null;
            head = cyclicIncrement(head);
            if (itrs != null)
                itrs.elementDequeued();
        } else {
            for (int i = removeIndex; ; ) {
                int next = cyclicIncrement(i);
                if (next != tail) {
                    elements[i] = elements[next];
                    i = next;
                } else {
                    elements[i] = null;
                    tail = i;
                    break;
                }
            }
        }
    }
}
