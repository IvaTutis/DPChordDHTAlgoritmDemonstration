package project_package.tests;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project_package.network.Network;
import project_package.network.Node;

/**
 * This class provides some static methods to perform performance project_package.tests.
 * @author Iva Tutiš
 *
 */
public class AutomatedTests {

    /**
     * Concurrency testovi
     *      Stvori n čvorova
     *      Stavi n vrijednosti u svaki čvor
     *      Get n vrijednosti iz svakog čvora
     * Isprintaj potrebno vrijeme u milisekundama
     *
     * @param n
     */
    @SuppressWarnings("unchecked")
    public static void testConcurrency(int n) {
        try {
            long start, end;
            Network<String> network = new Node<>("table");

            System.out.println("Creating " + n + " nodes.");
            start = System.currentTimeMillis();
            List<Network<String>> nodes = concurrentCreate(n, (Node<String>) network);
            end = System.currentTimeMillis();
            System.out.println("Time: " + (int)(end-start) + " miliseconds");

            System.out.println("Putting " + n + " values.");
            start = System.currentTimeMillis();
            List<String> keys = concurrentPut(nodes);
            end = System.currentTimeMillis();
            System.out.println("Time: " + (int)(end-start) + " miliseconds");

            System.out.println("Getting " + keys.size() + " random values from DHT");
            start = System.currentTimeMillis();
            concurrentGet(nodes, keys);
            end = System.currentTimeMillis();
            System.out.println("Time: " + (int)(end-start) + " miliseconds");

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stvori n čvorova koji se linkaju u mrežu
     *
     * @param n
     * @param dht
     * @return listu čvorova
     * @throws RemoteException
     */
    private static List<Network<String>> concurrentCreate(int n, final Node<String> dht) throws RemoteException {
        final List<Network<String>> nodes = new ArrayList<>();
        for(int i=0; i<n; i++) {
            nodes.add(new Node<>("node"+i, dht));
        }
        return nodes;
    }

    /**
     * Stavi istovremeno 1 vrijednost u svaki čvor
     * @param networks - lista čvorova u koje insertamo
     * @return lista ključeva vrijednosti
     * @throws InterruptedException
     */
    private static List<String> concurrentPut(List<Network<String>> networks) throws InterruptedException {
        Random rand = new Random();
        final List<String> keys = new ArrayList<>();
        final Lock write = new ReentrantLock();

        final AtomicInteger finnishedWork = new AtomicInteger();
        int workStarted = 0;

        for(final Network<String> network : networks) {
            final String key = "key" + rand.nextInt();
            new Thread(){
                public void run() {
                    network.addKeyValuePair(key, "v");
                    write.lock();
                    keys.add(key);
                    finnishedWork.incrementAndGet();
                    write.unlock();
                }
            }.start();
            workStarted++;
        }
        while(finnishedWork.get() != workStarted);
        return keys;
    }

    /**
     * Istovremeno GET 1 random vrijednost iz svakog čvora
     *
     * @param networks - lista čvorova iz koje uzimamo vrijednosti
     * @param keys - lista ključeva iz koje možemo randomly birati
     */
    private static void concurrentGet(List<Network<String>> networks, List<String> keys) {
        Random rand = new Random();

        final AtomicInteger finnishedWork = new AtomicInteger();
        int workStarted = 0;

        for(final Network<String> network : networks) {
            final String key = keys.get(rand.nextInt(keys.size()));
            new Thread() {
                public void run() {
                    network.getValue(key);
                    finnishedWork.incrementAndGet();
                }
            }.start();
            workStarted++;
        }

        while(finnishedWork.get() != workStarted);
    }

    /**
     * Jos neki standardni testovi preformansi, ali bez zahtjeva na istovremenost
     * Stvori n čvorova, insert n vrijednosti, get n vrijednosti
     *
     * @param nodes - broj čvorova
     * @param n - broj vrijednosti za insert/get
     */
    public static void testovi(int nodes, int n) {
        try {
            long start, end;
            String[] keys = new String[n];
            for(int i=0; i<n; i++)
                keys[i] = "key" + i;

            System.out.println("Starting test with " + nodes + " nodes and " + n + " operations.");

            start = System.currentTimeMillis();
            Network<String> network = new Node<>("node");
            for(int i=0; i<nodes; i++)
                new Node<>("node" +i, (Node<String>) network);
            end = System.currentTimeMillis();
            System.out.println("Create time: " + (int)(end-start) + " miliseconds");


            start = System.currentTimeMillis();
            for (String key : keys)
                network.addKeyValuePair(key, "value");
            end = System.currentTimeMillis();
            System.out.println("Put time: " + (int)(end-start) + " miliseconds");


            start = System.currentTimeMillis();
            for (String key : keys)
                network.getValue(key);
            end = System.currentTimeMillis();
            System.out.println("Get time: " + (int)(end-start) + " miliseconds");


        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}

