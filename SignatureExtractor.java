/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.imageio.ImageIO;

import messif.objects.impl.ObjectSignatureSQFD;
import org.joda.time.DateTime;

/**
 *
 * @author TOm
 */
public class SignatureExtractor {

    public ArrayList<ObjectSignatureSQFD> extractSignatures(String path, ExtractorParameters p) throws Exception {
        if (p == null) {
            p = new ExtractorParameters();
        }

        Random r = new Random(137);

        if (p.xyPoints == null) {
            p.xyPoints = new ArrayList<PointF>();
            for (int i = 0; i < p.NumberOfSamplePoints; i++) {
                double x = Math.abs(r.nextDouble());
                double y = Math.abs(r.nextDouble());
                p.xyPoints.add(new PointF((float) x, (float) y));
            }
        }

        if (p.xySeedPoints == null) {
            p.xySeedPoints = new ArrayList<PointF>();
            for (int i = 0; i < p.NumberOfSeeds; i++) {
                double x = r.nextDouble();
                double y = r.nextDouble();
                p.xySeedPoints.add(new PointF((float) x, (float) y));
            }
        }

        File[] files = new File(path).listFiles();

        int count = Math.min(p.NumberOfObjects, files.length);
        ArrayList<ArrayList<Cluster>> signatures = new ArrayList<>();

        if (p.ParallelProcessing) {
            for (int i = 0; i < count; i++) {
                ImagePlus bmp = new ImagePlus(files[i].getAbsolutePath());
                signatures.add(extractSignature(bmp, p));
            }

        } else {
            for (int i = 0; i < count; i++) {
                ImagePlus bmp = new ImagePlus(files[i].getAbsolutePath());
                signatures.add(extractSignature(bmp, p));

            }
        }

        ArrayList<ObjectSignatureSQFD> objectsSQFD = new ArrayList<>();

        for (int i = 0; i < signatures.size(); i++) {
            int signaturesCount = signatures.get(i).size();
            int indexC = 0;
            int indexR = 0;
            int sizeOfClusters = signaturesCount * 7;
            int size = sizeOfClusters + signaturesCount;
            float[] clusters = new float[sizeOfClusters];
            float[] result = new float[size];

            for (int j = 0; j < signaturesCount; j++) {
                result[indexR] = (float) (signatures.get(i)).get(j).weight;
                indexR++;
                float[] data = (signatures.get(i)).get(j).Values;
                for (int k = 0; k < 7; k++) {
                    clusters[indexC] = data[k];
                    indexC++;
                }
            }
            for (int k = 0; k < size - indexR; k++) {
                result[indexR + k] = clusters[k];
            }
            objectsSQFD.add(new ObjectSignatureSQFD(signaturesCount, 7, result));
        }

        
        return objectsSQFD;
    }

    public ArrayList<Cluster> extractSignature(ImagePlus bmp, ExtractorParameters p) throws Exception {

        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int R, G, B;

        // maximal bitmap size = 256 * 256
        int[][][] grayscale = new int[width][height][4];

        // TODO adapt to bmp.PixelFormat
        /*if (bmp.getBitDepth() != 32) {
         throw new Exception("Only ARGB PixelFormat is supported.");
         }*/
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int[] rgb = new int[4];
                rgb = bmp.getPixel(i, j);
                grayscale[i][j][0] = (int) (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]);
                grayscale[i][j][0] = (int) (grayscale[i][j][0] / 16.0);
                grayscale[i][j][1] = (int) rgb[0];
                grayscale[i][j][2] = (int) rgb[1];
                grayscale[i][j][3] = (int) rgb[2];
            }
        }

        // prepare sampling points and seeds
        DateTime t = DateTime.now();
        ArrayList<Cluster> points = new ArrayList<>();
        for (PointF point : p.xyPoints) {
            points.add(new Cluster(bmp, grayscale, p, point));
        }

        ArrayList<Cluster> seedPoints = new ArrayList<>();
        for (PointF point : p.xySeedPoints) {
            seedPoints.add(new Cluster(bmp, grayscale, p, point));

        }

        // dynamic k-means clustering with grid index support
        for (int iteration = 0; iteration < p.IterationCount; iteration++) {
            for (Cluster seed : seedPoints) {
                seed.weight = 0;
            }
            // remove too close seeds
            for (int i = 0; i < seedPoints.size(); i++) {
                if (!seedPoints.get(i).remove) {
                    for (int j = i + 1; j < seedPoints.size(); j++) {
                        if (!seedPoints.get(j).remove) {
                            if (seedPoints.get(i).squareDistance(seedPoints.get(j)) < p.MinSquareDistance) {
                                seedPoints.get(j).remove = true;
                            }
                        }
                    }
                }
            }
            Iterator<Cluster> it = seedPoints.iterator();
            while (it.hasNext()) {
                Cluster sp = it.next();
                if (sp.remove) {
                    it.remove();
                }
            }
            // create grid index to optimize seed selection
            int gridSize = 6;
            ArrayList<FixedSizeList<Cluster>> index = new ArrayList<>();
            if (p.OptimizeKMeans) {
                for (int i = 0; i < gridSize; i++) {
                    index.add(i, new FixedSizeList(gridSize));
                }

                for (Cluster seed : seedPoints) {
                    int x = (int) Math.round(seed.Values[0] * gridSize);
                    int y = (int) Math.round(seed.Values[1] * gridSize);

                    if (x == 6) {
                        x = 5;
                    }
                    if (y == 6) {
                        y = 5;
                    }
                    index.get(x).set(y, seed);
                }
            }

            // assign objects to seeds - bottleneck
            ArrayList<Cluster> actualList = new ArrayList<>();
            for (Cluster sp : points) {
                Double minDist = 0.0;
                sp.minDist = Double.MAX_VALUE;

                if (p.OptimizeKMeans && iteration < p.IterationCount - 1) {
                    int x = (int) Math.round((sp.Values[0] * gridSize)), y = (int) Math.round((sp.Values[1] * gridSize));
                    int rx = Math.min(x + 1, gridSize - 1), ry = Math.min(y + 1, gridSize - 1);

                    for (int i = Math.max(0, x - 1); i <= rx; i++) {
                        for (int j = Math.max(0, y - 1); j <= ry; j++) {
                            Cluster sPoint = index.get(i).get(j);
                            //System.out.println(sPoint == null);
                            if (sPoint != null) {
                                actualList.add(sPoint);
                            }
                            for (int k = 0; k < actualList.size(); k++) {
                                minDist = sp.squareDistance(actualList.get(0));
                                if (minDist < sp.minDist) {
                                    sp.minDist = minDist;
                                    sp.seed = actualList.get(k);
                                }
                            }
                        }
                    }
                } else {
                    for (Cluster seed : seedPoints) {
                        minDist = sp.squareDistance(seed);
                        if (minDist < sp.minDist) {
                            sp.minDist = minDist;
                            sp.seed = seed;
                        }
                    }
                }
                if (sp.seed != null) {
                    sp.seed.weight += 1;
                }
            }

            Iterator<Cluster> iter = seedPoints.iterator();
            // remove too small seeds
            if (iteration < p.IterationCount - 1) {
                while (iter.hasNext()) {
                    Cluster sp = iter.next();
                    if (sp.weight < p.MinWeight * (iteration + 1)) {
                        iter.remove();
                    }
                }
            } else {
                while (it.hasNext()) {
                    Cluster sp = iter.next();
                    if (sp.weight == 0) {
                        iter.remove();
                    }
                }
            }

            //seedPoints.r
            // update seeds
            //for (SamplePoint seed : seedPoints) {
            //  seed.clearValues();
            //}
            for (Cluster sp : points) {
                if (sp.seed != null) {
                    sp.seed.addPoint(sp);
                }
            }
            for (Cluster seed : seedPoints) {
                seed.divByWeight();
            }
        }
        return seedPoints;
    }

    public void showResult(ArrayList<Integer> list, String path){
        File[] files = new File(path).listFiles();
        for(int i = 0; i < list.size(); i++){
            ImagePlus bmp = new ImagePlus(files[list.get(i)].getAbsolutePath());
                bmp.show();
        }
    }
}
