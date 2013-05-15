/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PageRankBlocked;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sudharsan
 */
public class ReadPageRank {
  
    public static void main(String[] arg){
        FileInputStream fis = null;
        try {
            File fil = new File("/home/joe/output_gauss_siedel_6/part-r-00000");
            fis = new FileInputStream(fil);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = "";
            int count = 0;
            TreeMap<Integer, Double> pr = new TreeMap<Integer, Double>();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                line = line.replace("\t", ";");
                String[] tokens = line.split(";");
//                System.out.println(" tokens : " + Arrays.toString(tokens));
                pr.put(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1]));
            }
            TreeMap<Integer, Double> finalPR = new TreeMap<Integer, Double>();
//            System.out.println(" Map : " + pr);
            int[] nodesArray = getNodesArray();
            for( int i : nodesArray){
                --i;
                if(pr.containsKey(i)){
                    finalPR.put(i, pr.get(i));
                }else{
                    int n = i;
                    while(!pr.containsKey(n)){
                        --n;
                    }
                    finalPR.put(n, pr.get(n));
                }
            }
            System.out.println("Maps  : " + finalPR);
            for ( Entry entry : finalPR.entrySet()){
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
            System.out.println("Size : " + finalPR.size());
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static int[] getNodesArray(){
        FileInputStream fis = null;
        try {
            LinkedList<Integer> nodes = null;
            int[] nodesArray = null;
            File fil = new File("/home/joe/blocks.txt");
            fis = new FileInputStream(fil);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line = "";
            int count = 0;
            nodes = new LinkedList<Integer>();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                nodes.add(Integer.parseInt(line));
            }
            int i = 0;
            nodesArray = new int[nodes.size()];
            for (Integer n : nodes) {
                nodesArray[i++] = n;
            }
            return nodesArray;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(ReadPageRank.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
}
