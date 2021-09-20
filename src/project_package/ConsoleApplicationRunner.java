package project_package;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Scanner;

import project_package.network.Node;
import project_package.tests.AutomatedTests;

/**
 * Ova klasa posjeduje main() funkciju za pokretanje u konzoli/terminalu i UI
 *
 * @author Iva Tutiš
 *
 */
public class ConsoleApplicationRunner {

    //"naš" čvor iz kojeg promatramo mrežu
    private Node<String> myComputerNode;

    /**
     * Konstruktor koji stvara lokalnu mrežu od 5 čvorova
     *
     * @param name
     */
    public ConsoleApplicationRunner(String name) {
        try {
            myComputerNode = new Node<>(name);
            new Node<>("prvi", (Node<String>) myComputerNode);
            new Node<>("drugi", (Node<String>) myComputerNode);
            new Node<>("treci", (Node<String>) myComputerNode);
            new Node<>("cetvrti", (Node<String>) myComputerNode);
            userInterface();
        } catch (RemoteException e) {
            System.err.println("Can't create nodes. (RemoteException)");
			e.printStackTrace();
        }
    }

    /**
     * Konstruktor kojim se spajamo na neki port na postojeći DHT
     *
     * @param name - lokalno ime našeg čvora
     * @param rName - remote ime drugog čvora
     * @param host - remote host tablice
     * @param port - port za RMI konekciju
     */
    public ConsoleApplicationRunner(String name, String rName, String host, int port) {
        try {
            //create 1 node, and link it to the given ports
            myComputerNode = new Node<>(name, host, port, rName);
            userInterface();
        } catch (RemoteException e) {
            System.err.println("RemoteException");
			e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println("RMI Registry error.");
			e.printStackTrace();
        }
    }

    /**
     * Jednostavan User Interface kroz konzolu
     */
    private void userInterface() {
        System.out.println("You're inside the user interface. Press ANY_KEY+ENTER to continue.");
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        while(!input.equals("exit")) {
            try {
                String[] inputs = input.split(" ");
                switch(inputs[0]) {
                    case "put":
                        //read
                        String key = inputs[1];
                        String value = inputs[2];
                        //do
                        myComputerNode.addKeyValuePair(key, value);
                        //write
                        System.out.println("The pair has been stored");
                        break;
                    case "get":
                        //read
                        key = inputs[1];
                        //do
                        String newValue = myComputerNode.getValue(key);
                        //write
                        System.out.println("Got from dht: " + newValue);
                        break;
                    case "remove":
                        //read
                        key = inputs[1];
                        //do
                        myComputerNode.deleteValue(key);
                        //write
                        System.out.println("Removed " + key);
                        break;
                    case "test":
                        //read
                        int nodes1 = Integer.parseInt(inputs[1]);
                        int n1 = Integer.parseInt(inputs[2]);
                        //do
                        AutomatedTests.testovi(nodes1, n1);
                        break;
                    case "concurrency_test":
                        //read
                        int n2 = Integer.parseInt(inputs[1]);
                        //do
                        AutomatedTests.testConcurrency(n2);
                        break;
                    case "list":
                        //do
                        List<String> valueList = myComputerNode.listAllValues();
                        //write
                        System.out.println("Items:");
                        for(String s : valueList)
                            System.out.println(s);
                        break;
                    default:
                        System.out.println("You can use the following commands:");
                        System.out.println("put 'key' 'value'       -> saves value with key to project_package.storage");
                        System.out.println("get 'key'               -> gets the value mapped to the key");
                        System.out.println("remove 'key'            -> deletes the value mapped to the key");
                        System.out.println("test 'n' 'v'            -> benchmark operations on n nodes with v values");
                        System.out.println("concurrency_test 'n'    -> benchmark concurrent operations on n nodes");
                        System.out.println("list                    -> lists all values in project_package.storage");
                        System.out.println("exit                    -> exits the program");
                }
            } catch (Exception e) {
                System.out.println("Something went wrong.. a bad input?");
            }
            input = scan.nextLine();
        }
        myComputerNode.leaveCurrentNetwork();
        scan.close();
    }

    /**
     * Main koji stvara ili se spaja na mrežu ovisno o danim argumentima.
     *
     * pokretanje1: args.length = 0
     *              -> unutar npr IntelliJ-a, stvara se lokalna mreža čvorova
     *
     * pokretanje2: args.length = 1
     *              -> dan argument [name]
     *              -> stvara se lokalna mreža čvorova sa imenom čvora "u kojem smo" [name]
     *
     * pokretanje3: args.length = 4
     *              -> dani argumenti  [localname, remotename, remotehost, remoteport]
     *              -> stvara se 1 čvor koji se spaja na postojeći network uz pomoć RMI-ja
     *
     * Nakon toga se pokreće User Interface.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0){ //if I run it inside IntelliJ
            new ConsoleApplicationRunner("Mock Network");
        }
        else if(args.length == 1) { //if I want to run it with a specific name
            String name = args[0];
            new ConsoleApplicationRunner(name);
        }
        else if(args.length == 4) { //running it as a web app
            String name = args[0];
            String rName = args[1];
            String rHost = args[2];
            int port = Integer.parseInt(args[3]);
            new ConsoleApplicationRunner(name, rName, rHost, port);
        }
        else
            System.out.println("Expected arguments: [localname] or [localname, remotename, remotehost, remoteport]");
    }
}