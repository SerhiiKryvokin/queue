package queue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;

/*
Synchronized wrapper of MostRecentlyInsertedQueue
 */
public class ConcurrentMostRecentlyInsertedQueue<E> extends AbstractQueue<E> {

    private MostRecentlyInsertedQueue<E> delegateQueue;

    public ConcurrentMostRecentlyInsertedQueue(int capacity) {
        delegateQueue = new MostRecentlyInsertedQueue<>(capacity);
    }

    @Override
    public synchronized boolean offer(E e) {
        return delegateQueue.offer(e);
    }

    @Override
    public synchronized E peek() {
        return delegateQueue.peek();
    }

    @Override
    public synchronized E poll() {
        return delegateQueue.poll();
    }

    @Override
    public synchronized void clear() {
        delegateQueue.clear();
    }

    @Override
    public synchronized boolean contains(Object o) {
        return delegateQueue.contains(o);
    }

    @Override
    public synchronized boolean containsAll(Collection<?> c) {
        return delegateQueue.containsAll(c);
    }

    @Override
    public synchronized boolean isEmpty() {
        return delegateQueue.isEmpty();
    }

    /*
    Should be manually synchronized on the queue object
     */
    @Override
    public Iterator<E> iterator() {
        return delegateQueue.iterator();
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        return delegateQueue.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection<?> c) {
        return delegateQueue.retainAll(c);
    }

    @Override
    public synchronized int size() {
        return delegateQueue.size();
    }

    @Override
    public synchronized Object[] toArray() {
        return delegateQueue.toArray();
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        return delegateQueue.toArray(a);
    }

    @Override
    public synchronized boolean remove(Object o) {
        return delegateQueue.remove(o);
    }

    @Override
    public synchronized String toString() {
        return delegateQueue.toString();
    }
}