package org.example;

public class Producer extends Thread {
    private final Buffer buffer;

    public Producer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        for (int i = 1; i <= 5; i++) {
            buffer.produce(i);
            try {
                sleep((int)(Math.random() * 1000));
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }


}
