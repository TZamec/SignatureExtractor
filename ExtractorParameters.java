/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



import java.util.ArrayList;

/**
 *
 * @author TOm
 */
public class ExtractorParameters {

    public ArrayList<PointF> xyPoints = null;
    public ArrayList<PointF> xySeedPoints = null;

    public int NumberOfSamplePoints = 2000;
    public int NumberOfSeeds = 500;

    public boolean OptimizeKMeans = true;
    public int IterationCount = 5;
    public double MinSquareDistance = 0.05;
    public double MinWeight = 10;

    public int TextureRadius = 3;

    public int NumberOfObjects = 500;
    public boolean ParallelProcessing = true;
}
