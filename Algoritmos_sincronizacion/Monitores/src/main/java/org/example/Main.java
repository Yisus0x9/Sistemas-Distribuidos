package org.example;

public class Main {
    public static void main(String[] args) {
        Buffer buffer = new Buffer(3); // buffer capacity = 3

        for (int i = 0; i < 2; i++) { // 2 producers
            new Producer(buffer).start();
        }
        for (int i = 0; i < 3; i++) { // 3 consumers
            new Consumer(buffer).start();
        }
    }
}