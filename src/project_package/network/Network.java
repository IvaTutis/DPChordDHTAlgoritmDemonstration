package project_package.network;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Sučelje za hash-table-like mrežu
 * (metode su ustvari querys na mreži)
 *
 * @author Iva Tutiš
 *
 */
public interface Network<T> {
    /**
     * GET objekt mapiran na ključ
     *
     * @param key
     * @return T value
     */
    public T getValue(String key);

    /**
     * PUT objekt u project_package.network.DHT
     *
     * @param newKey - ključ tipa String
     * @param newValue - value
     */
    public void addKeyValuePair(String newKey, T newValue);

    /**
     * DELETE objekt ključa k
     *
     * @param key
     */
    public void deleteValue(String key);


    /**
     * Lista String reprezentacija svih vrijednosti u mapi
     *
     * @return lista String vrijednosti
     */
    public List<String> listAllValues();

    /**
     * Može se koristiti za prisilni update routing tablice
     */
    public void updateRoutingTable() throws RemoteException;

    /**
     * Get a list of all nodes in the project_package.network (Warning: May take some time, and consume a lot of resources!).
     * @return a list of all nodes.
     */
    public List<Node<T>> getAllNodes();
}