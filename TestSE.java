/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;
import messif.objects.impl.ObjectSignatureSQFD;

/**
 *
 * @author TOm
 */
public class TestSE {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        long startTime = System.nanoTime();
        String path = "c:/Users/TOm/Pictures/testData";
        SignatureExtractor ext = new SignatureExtractor();
        ExtractorParameters param = new ExtractorParameters();
        ArrayList<ObjectSignatureSQFD> sqfd = new ArrayList<>();
        sqfd = ext.extractSignatures(path, param);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime);
        System.out.println(duration/1000000000 + "seconds" );
        float[] d = new float[sqfd.size()];
        ArrayList<Integer> show = new ArrayList<>();
        for (int i = 0; i < sqfd.size(); i++) {
            d[i] = sqfd.get(3).getDistance(sqfd.get(i));
            if(d[i]<0.6){
                show.add(i);
            }
            System.out.println("d1: " + d[i]);
        }
        
        ext.showResult(show, path);

    }

}
