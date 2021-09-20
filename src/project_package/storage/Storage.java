package project_package.storage;

import java.rmi.RemoteException;
import java.util.*;

public class Storage<T> {

    private HashMap<String, T> storage;

    public Storage(){
        storage = new HashMap<>();
    }

    public HashMap<String, T> getStorage() {
        return storage;
    }

    public void setStorage(HashMap<String, T> storage) {
        this.storage = storage;
    }

    //Vrati vrijednost mapiranu na dani ključ
    public T getStored(String key) throws RemoteException {
        return storage.get(key);
    }

    //GetAll vrijednosti u skladištu ovog čvora
    public List<T> getAllStored() throws RemoteException {
        ArrayList<T> values = new ArrayList<>(storage.values());
        return values;
    }

    //Add/update vrijednosti na ključ
    public void addStored(String key, T value) throws RemoteException {
        storage.put(key, value);
    }

    //Delete vrijednosti na ključ
    public void removeStored(String key) throws RemoteException {
        storage.remove(key);
    }

    //varca set kljuceva
    public Set<String> keySet(){
        return storage.keySet();
    }

    public T remove(String key){
        return storage.remove(key);
    }
}
