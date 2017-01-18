package demo;

import demo.blocking.Consumer;
import demo.blocking.Producer;
import queue.MostRecentlyInsertedBlockingQueue;
import queue.MostRecentlyInsertedQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DemoRunner {
    public static void main(String[] args) throws InterruptedException {
//        new DemoRunner().blockingDemo();
        new DemoRunner().mostRecentlyInsertedQueueDemo();
    }

    void mostRecentlyInsertedQueueDemo() {
        MostRecentlyInsertedQueue<Integer> queue = new MostRecentlyInsertedQueue<>(3);
        queue.offer(1);
        System.out.println(queue);
        queue.offer(2);
        System.out.println(queue);
        queue.offer(3);
        System.out.println(queue);
        queue.offer(4);
        System.out.println(queue);
        queue.offer(5);
        System.out.println(queue);
        int poll1 = queue.poll();
        System.out.println("poll1: " + poll1);
        System.out.println(queue);
        int poll2 = queue.poll();
        System.out.println("poll2: " + poll2);
        System.out.println(queue);
        queue.offer(6);
        System.out.println(queue);
        queue.offer(7);
        System.out.println(queue);
        int poll3 = queue.poll();
        System.out.println("poll3: " + poll3);
        System.out.println(queue);
        queue.offer(8);
        System.out.println(queue);
        queue.clear();
        System.out.println(queue);
    }

    /*
    Consumer is blocked at the start, waiting for elements.
    When 0 element comes, consumer takes it.
    Then some elements are dropped because producer produces faster than consumer processes.
     */
    void blockingDemo() throws InterruptedException {
        MostRecentlyInsertedBlockingQueue blockingQueue = new MostRecentlyInsertedBlockingQueue(3);
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Producer(blockingQueue));
        exec.execute(new Consumer(blockingQueue));
        TimeUnit.SECONDS.sleep(15);
        exec.shutdownNow();
    }

}
