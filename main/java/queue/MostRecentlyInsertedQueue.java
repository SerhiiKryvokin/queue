package queue;


import java.util.*;

public class MostRecentlyInsertedQueue<E> extends AbstractQueue<E> {

    private Object[] elements;

    private int head;

    private int tail;


    public MostRecentlyInsertedQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity should be a positive integer");
        }
        elements = new Object[capacity];
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException("Offered object should not be null");
        }
        if (isFull()) {
            head = cyclicIncrement(head);
        }
        elements[tail] = e;
        tail = cyclicIncrement(tail);
        return true;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E headElement = (E) elements[head];
        elements[head] = null;
        head = cyclicIncrement(head);
        return headElement;
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E headElement = (E) elements[head];
        return headElement;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int cursor = head;
            boolean fullQueueState = isFull();

            @Override
            public boolean hasNext() {
                return !isEmpty() && (fullQueueState || cursor != tail);
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                fullQueueState = false;
                int returnIndex = cursor;
                cursor = cyclicIncrement(cursor);
                return (E) elements[returnIndex];
            }
        };
    }

    @Override
    public int size() {
        int size = 0;

        if (tail > head) {
            size = tail - head;
        } else if (tail == head) {
            size = isEmpty() ? 0 : elements.length;
        } else {
            size = elements.length - head + tail;
        }

        return size;
    }

    @Override
    public boolean isEmpty() {
        return elements[head] == null;
    }

    @Override
    public void clear() {
        head = 0;
        tail = 0;
        Arrays.fill(elements, null);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
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
    }

    private boolean isFull() {
        return head == tail && !isEmpty();
    }

    private int cyclicIncrement(int index) {
        return ++index == elements.length ? 0 : index;
    }
}
