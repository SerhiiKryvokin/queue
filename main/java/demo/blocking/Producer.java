package demo.blocking;

import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable {

    private BlockingQueue blockingQueue;

    public Producer(BlockingQueue queue) {
        this.blockingQueue = queue;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                blockingQueue.offer(i);
                System.out.println(i + "is offered");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
