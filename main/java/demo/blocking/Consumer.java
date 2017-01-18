package demo.blocking;

import java.util.concurrent.BlockingQueue;

public class Consumer implements Runnable {

    private BlockingQueue blockingQueue;

    public Consumer(BlockingQueue blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Object taken = blockingQueue.take();
                System.out.println("Taken: " + taken);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
