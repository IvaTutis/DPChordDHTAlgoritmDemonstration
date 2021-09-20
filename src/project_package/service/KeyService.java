package project_package.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Klasa koja ključ i omogućava usporedbu ključeva i veze njih i indeksa čvorova
 * @author Iva Tutiš
 *
 */
public class KeyService {

    /**
     * Generiraj indeks čvora ovisno o imenu ključa i # mogućih ključeva.
     * Ovisno o prostoru ključa, ključ može/ ne mora biti unikatan.
     *
     * @param keyName
     * @param maxNumberOfNodesInNetwork
     * @return
     */
    public static String generateNodeIndex(String keyName, int maxNumberOfNodesInNetwork) {

        String hashedKeyName = hashString(keyName);


        int characters = (int) (Math.log(maxNumberOfNodesInNetwork)/Math.log(2));
        characters = Math.min(characters, hashedKeyName.length());


        return hashedKeyName.substring(hashedKeyName.length()-characters-1);

    }

    /**
     * Odluči je li ključ keyString između IndexFrom i IndexTo u prstenu
     * @param keyString -> KEY
     * @param IndexFrom -> FROM
     * @param IndexTo -> TO
     * @return true ako keyString element u intervalu (IndexFrom, IndexTo]
     */
    public static boolean isKeyInbetween(String keyString, String IndexFrom, String IndexTo) {
        //iz Stringa u Float u float
        float key = Float.parseFloat(keyString);
        float from = Float.parseFloat(IndexFrom);
        float to = Float.parseFloat(IndexTo);
        //provjera je li u intervalu
        if(from > to) {
            return key > from || key <= to;
        }else if(from < to)
            return key > from && key <= to;
        else
            return true;
    }

    /**
     * Generira SHA1-hash ulaznog stringa s
     * @param s
     * @return String koji predstavlja SHA1-hash u bazi 2.
     */
    private static String hashString(String s) {
        String resultString = null;
        //hashiraj string algoritmom SHA1
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(s.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                sb.append(Integer.toString(result[i], 2).substring(1));
            }
            resultString = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        //vrati vrijednost
        return resultString;
    }
}
