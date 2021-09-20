package project_package.network;

import project_package.service.KeyService;
import project_package.storage.Storage;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacija čvora u project_package.network.DHT-u.
 * Sučelja dht.Node i project_package.network.DHT su implementirana.
 *
 * @author Iva Tutiš
 * @param <T> tip vrijednosti koja se posprema u DHT
 *
 */
public class Node<T> extends UnicastRemoteObject implements Network<T> {

    private static final long serialVersionUID = 7837010474371220959L;

    /**
     * Maksimalan broj čvorova u mreži
     */
    private static final int N = 1048576;

    /**
     * Defaultni port za RMI-connection.
     */
    public static final int DEFAULT_PORT = 1099;
    /**
     * Ime čvora kao string
     */
    private String name;

    /**
     * Indeks cvora, ujedno minimalna vrijednost kljuca da bi se ovaj nasao u njemu
     */
    private String index;

    /**
     * Nasljednik u prstenu (mreži)
     */
    private Node<T> successor;

    /**
     * Prethodnik u prstenu (mreži)
     */
    private Node<T> predecessor;

    /**
     * Spremište za koje je ovaj čvor odgovoran
     */
    private Storage<T> storage = new Storage();

    /**
     * Routing tablica
     */
    private Map<String, Node<T>> fingers = new LinkedHashMap<>();

    //-----------------------------------------------------KONSTRUKTORI-----------------------------------------
    
    // Konstruktor koji inicijalizira jedan node
    // Ovaj konstruktor se koristi za inicijalizaciju cvora koji je sam u mreži
    public Node(String name) throws RemoteException {
        //settam ime i iz njega generiram indeks cvora
        this.setName(name);
        this.setIndex(KeyService.generateNodeIndex(name, N));

        //buduci nemam s cim spojiti, on je prethodnik i sljedbenik samome sebi
        this.setSuccessor(this);
        this.setPredecessor(this);

        //spajam ga kao klin
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(DEFAULT_PORT);
        } catch (Exception e) {
            registry = LocateRegistry.getRegistry(DEFAULT_PORT);
        }
        registry.rebind(name, this);

    }

    //Konstruktor koji inicijalizira jedan node i spaja ga na mrežu koja sadži Node other
    public Node(String name, Node<T> other) throws RemoteException {
        this(name);
        joinNetworkWithNode(other);
    }

    //Konstruktor koji spaja dht.Node na udaljenu (internet/remote) mrežu
    @SuppressWarnings("unchecked")
    public Node(String name, String host, int port, String otherName) throws RemoteException, NotBoundException {
        //konstruiraj nod kao da je zaseban u mreži
        this(name);

        //registry stuffre
        Registry registry = LocateRegistry.getRegistry(host, port);
        Node<T> other = (Node<T>) registry.lookup(otherName);

        //spoji na ostatak mreže
        joinNetworkWithNode(other);
    }

    //------------------------------------------------GETTERS AND SETTERS------------------------------------

    public Node<T> getSuccessor() throws RemoteException {return successor;}

    public Node<T> getPredecessor() throws RemoteException {return predecessor;}

    public String getIndex() throws RemoteException {return index;}

    public String getName() {
        return name;
    }

    public HashMap<String, T> getStorage() {
        return storage.getStorage();
    }

    public Map<String, Node<T>> getFingers() {
        return fingers;
    }

    public void setFingers(Map<String, Node<T>> fingers) {
        this.fingers = fingers;
    }

    public void setStorage(HashMap<String, T> storage) {
        this.storage.setStorage(storage);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIndex(String index) {this.index = index;}

    public void setSuccessor(Node<T> newSuccessor) throws RemoteException {successor = newSuccessor;}

    public void setPredecessor(Node<T> newPredcessor) throws RemoteException {predecessor = newPredcessor;}


    //--------------------------------------------------JOIN & LEAVE dht NETWORK------------------------------
    // Napusti trenutnu mrežu i pridruži se drugoj koja sadrži node other
    public void joinNetworkWithNode(Node<T> other) {
        try {
            boolean joined = false;
            while(!joined) {
                Node<T> pred = other.getPredecessor();
                String otherKey = other.getIndex();
                String predKey = pred.getIndex();

                if(KeyService.isKeyInbetween(this.getIndex(), predKey, otherKey)) {
                    pred.setSuccessor(this);
                    other.setPredecessor(this);
                    setSuccessor(other);
                    setPredecessor(pred);

                    /*Get a share of the project_package.storage from our new successor*/
                    Map<String, T> handover = this.getSuccessor().handoverStorageDueToNewPredcessor(predKey, index);
                    for(String k : handover.keySet())
                        storage.addStored(k, handover.get(k));

                    joined = true;
                } else
                    other = other.getSuccessor();
            }
            updateRoutingTable();

        } catch(RemoteException e) {
            System.err.println("Error joining " + other);
            e.printStackTrace();
        }
    }

    // Napusti trenutnu mrežu
    public void leaveCurrentNetwork() {
        try {
            /*Hand over items to successor.*/
//			System.out.println(name + ": I'm leaving. " + successor + " will handle my project_package.storage. (" + project_package.storage.size() + ") items.");
            for(String k : storage.keySet())
                this.getSuccessor().addStored(k, storage.getStored(k));
            System.out.println(name + ": Done.");


            //join empty hands inside the ring
            this.getSuccessor().setPredecessor(this.getPredecessor());
            this.getPredecessor().setSuccessor(this.getSuccessor());

            //isolate this node in his own project_package.network
            this.setSuccessor(this);
            this.setPredecessor(this);

            //aaand update the routing table
            updateRoutingTable();

        } catch(RemoteException e) { //if we've encountered a problem while leaving the project_package.network
            System.err.println("Error leaving.");
            e.printStackTrace();
        }
    }

    //čvor predaje dio svojeg project_package.storage-a novom prethodniku.
    public Map<String, T> handoverStorageDueToNewPredcessor(String oldPredIndex, String newPredIndex) throws RemoteException {
        Map<String, T> handover = new LinkedHashMap<>();
        List<String> keys = new ArrayList<String>(storage.keySet());
        for(String k : keys)
            if(KeyService.isKeyInbetween(k, oldPredIndex, newPredIndex))
                handover.put(k, storage.remove(k));
        return handover;
    }


    /**
     * Salji "probe" kroz mrežu.
     * Čvor kada se pozove ova funkcija
     * će samo isprintati svoje ime na standardni output (vjv konzola)
     * i proslijediti svom nasljedniku f-ju (tj pozvati je nad njime).
     *
     * @param nodeIndex - Index čvora koji je originalno slao probe
     * @param count - informacija o broju čvorova (inkrementira se svakim prolazom kroz novi čvor)
     * @throws RemoteException
     */
    public void probe(String nodeIndex, int count) throws RemoteException {
        if(this.getIndex().equals(nodeIndex) && count > 0) {
            System.out.println("Probe returned after " + count + " hops.");
        } else {
            System.out.println(this.getName() + ": Forwarding probe to " + this.getSuccessor());
            this.getSuccessor().probe(nodeIndex, count+1);
        }
    }

    /**
     * Nađi u prstenu/mreži čvor koji je odgovoran za project_package.storage ovog ključa key.
     * Koristi se pri CRUD operacijama.
     *
     * @param key
     * @return a reference to the (remote) dht.Node.
     * @throws RemoteException
     */
    public Node<T> lookupNodeResponsibleFor(String key) throws RemoteException {
        //ako je key u prostoru ključa (razlici indeksa) između prethodnika i ovog elementa
        //onda je za key odgovoran ovaj čvor
        //pa ga vrati
        if(KeyService.isKeyInbetween(key, this.getPredecessor().getIndex(), getIndex()))
            return this;
        //ako routing tablica ima manje od 3 člana, pitaj nasljednika isto pitanje
        else if(this.getFingers().keySet().size() < 3) {
            return this.getSuccessor().lookupNodeResponsibleFor(key);
        }
        //ako routing tablica ima jednako ili više od tri člana, po njenim ključevima
        //tj indexima čvorova pretraži
        else {
            String[] routingTableIndexList = {};
            routingTableIndexList = this.getFingers().keySet().toArray(routingTableIndexList);
            for(int i=0; i<(routingTableIndexList.length-1); i++) {
                String currentNodeIndex = routingTableIndexList[i];
                String nextNodeIndex = routingTableIndexList[i+1];
                if(KeyService.isKeyInbetween(key, currentNodeIndex, nextNodeIndex)) {
                    Node<T> currentNode = this.getFingers().get(currentNodeIndex);
                    Node<T> node = currentNode.getSuccessor();
                    return node.lookupNodeResponsibleFor(key);
                }
            }
            return this.getFingers().get(routingTableIndexList[routingTableIndexList.length-1]).getSuccessor().lookupNodeResponsibleFor(key);
        }

    }

    public void updateFingers(List<Node<T>> nodes) throws RemoteException {
        Map<String, Node<T>> newFingers = new LinkedHashMap<>();
        newFingers.put(this.getIndex(), this);
        try {
            int myIndex = nodes.indexOf(this);

            for(int i=1; i<nodes.size(); i = i*2) {
                int nodeIndex = (myIndex + i) % nodes.size();
                Node<T> n = nodes.get(nodeIndex);
                newFingers.put(n.getIndex(), n);
            }
        } catch(RemoteException e) {
            e.printStackTrace();
        }
        this.setFingers(newFingers);
    }

    //----------------------------------------------STORAGE----------------------------------------------

    //Vrati vrijednost mapiranu na dani ključ
    public T getStored(String key) throws RemoteException {
        return storage.getStored(key);
    }

    //GetAll vrijednosti u skladištu ovog čvora
    public List<T> getAllStored() throws RemoteException {
        ArrayList<T> values = new ArrayList<>(storage.getAllStored());
        return values;
    }

    //Add/update vrijednosti na ključ
    public void addStored(String key, T value) throws RemoteException {
        storage.addStored(key, value);
    }

    //Delete vrijednosti na ključ
    public void removeStored(String key) throws RemoteException {
        storage.remove(key);
    }

    //-----------------------------------------------NETWORK QUERY IMPLEMENTATIONS----------------------------------------

    @Override
    public T getValue(String key) {
        try {
            String nodeIndex = KeyService.generateNodeIndex(key, N);
            Node<T> node = lookupNodeResponsibleFor(nodeIndex);
            return node.getStored(nodeIndex);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void addKeyValuePair(String key, T object) {
        try {
            String k = KeyService.generateNodeIndex(key, N);
            Node<T> node = lookupNodeResponsibleFor(k);
            node.addStored(k, object);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteValue(String key) {
        try {
            String k = KeyService.generateNodeIndex(key, N);
            Node<T> node = lookupNodeResponsibleFor(k);
            node.removeStored(k);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> listAllValues() {
        Node<T> currentNode = this;
        ArrayList<String> all = new ArrayList<>();
        try {
            do {
                for(T element : currentNode.getAllStored())
                    all.add(element.toString());
                currentNode = currentNode.getSuccessor();
            } while(!this.equals(currentNode));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return all;
    }

    @Override
    public void updateRoutingTable() throws RemoteException {
        List<Node<T>> nodes = getAllNodes();
        for(Node<T> n : nodes)
            n.updateFingers(nodes);
    }

    @Override
    public List<Node<T>> getAllNodes() {
        ArrayList<Node<T>> nodes = new ArrayList<>();
        try {
            Node<T> current = this;
            do {
                nodes.add(current);
//				System.out.println(name + ": node=" + current.toString());
                current = current.getSuccessor();
            } while(!this.equals(current));
        } catch(RemoteException e){
            System.err.println("Error finding all nodes!");
        }
        Collections.sort(nodes, new Comparator<Node<T>>() {
            @Override
            public int compare(Node<T> n1, Node<T> n2) {
                try {
                    String key1 = n1.getIndex();
                    String key2 = n2.getIndex();
                    int i1 = Integer.parseInt(key1, 2);
                    int i2 = Integer.parseInt(key2, 2);

                    if(i1 > i2)
                        return 1;
                    if(i1 < i2)
                        return -1;
                    else
                        return 0;
                }catch(RemoteException e) {
                    return 0;
                }
            }
        });
        return nodes;
    }


    //----------------------------------------------OVERRIDING OBJECT CLASS METHODS-------------------------------

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof Node<?>)
            try {
                return this.getIndex().equals(((Node<?>) other).getIndex());
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
        else
            return false;
    }

}