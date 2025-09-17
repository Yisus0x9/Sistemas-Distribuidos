package org.example;
import java.util.LinkedList;
import java.util.Queue;
/**
 * Todos los objetos en Java tienen un Monitor.
 *
 * ⚙️ Internals del monitor en Java:
 * --------------------------------
 * - Cada objeto tiene un monitor (candado) con:
 *   1. Entry Set: cola de hilos BLOQUEADOS que intentan entrar a un método synchronized
 *      pero el lock ya está ocupado.
 *   2. Wait Set: cola de hilos en ESPERA que estaban dentro del monitor, llamaron a wait()
 *      y liberaron el lock voluntariamente.
 *
 * Flujo general:
 *  - synchronized: el hilo intenta adquirir el lock.
 *      • Si el lock está libre → entra.
 *      • Si está ocupado → va al Entry Set (estado BLOCKED).
 *
 *  - wait():
 *      • Solo puede usarse dentro de synchronized.
 *      • Libera el lock y mueve el hilo al Wait Set (estado WAITING).
 *      • El hilo no avanza hasta que alguien lo despierte con notify() o notifyAll().
 *      • Cuando despierta → pasa al Entry Set y compite por el lock de nuevo.
 *
 *  - notify():
 *      • Despierta a UN hilo del Wait Set y lo pasa al Entry Set.
 *      • Ese hilo no corre inmediatamente: debe esperar a que se libere el lock.
 *
 *  - notifyAll():
 *      • Despierta a TODOS los hilos del Wait Set → pasan al Entry Set.
 *      • Todos competirán por el lock cuando se libere.
 */

public class Buffer extends Object {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity;



    public Buffer(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void produce(int value) {
        while (queue.size() == capacity) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        queue.add(value);
        System.out.println("Producer " + Thread.currentThread().getId() + " produces: " + value);
        notifyAll(); // notify consumers that they may consume
    }

    public synchronized int consume() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int value = queue.poll();
        System.out.println("Consumer " + Thread.currentThread().getId() + " consumes: " + value);
        notifyAll();
        return value;
    }

    public void increment() {
        synchronized(this) {
            // sección crítica
        }
    }

}
