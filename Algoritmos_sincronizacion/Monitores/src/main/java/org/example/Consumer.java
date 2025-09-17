package org.example;

public class Consumer extends Thread {
    private final Buffer buffer;

    public Consumer(Buffer buffer) {
        this.buffer = buffer;
    }

    public void run() {
        for (int i = 1; i <= 5; i++) {
            buffer.consume();
            try {
                sleep((int)(Math.random() * 1200));
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}