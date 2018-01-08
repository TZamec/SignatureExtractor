/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import ij.ImagePlus;

/**
 *
 * @author TOm
 */
public class Cluster {

    public static final int C_X = 0, C_Y = 1, C_L = 2, C_a = 3, C_b = 4, C_contrast = 5, C_entropy = 6;

    public boolean remove = false;
    public int weight = 0;
    public Double minDist = 0.0;
    public Cluster seed = null;

    // Values represents {X, Y, L, a, b, C, E}
    public float[] Values = null;
    private static final int mDimension = 7;

    public int R = 0;
    public int G = 0;
    public int B = 0;

    public Cluster(ImagePlus bmp, int[][][] GSM, ExtractorParameters p, PointF point) {

        Values = new float[mDimension];
        Values[C_X] = point.x;
        Values[C_Y] = point.y;

        int x = (int) (point.x * bmp.getWidth());
        int y = (int) (point.y * bmp.getHeight());
        R = GSM[x][y][1];
        G = GSM[x][y][2];
        B = GSM[x][y][3];

        computeAndNormalizeLabValues();

        computeAndNormalizeContrastEntropy(bmp, GSM, (int) (point.x * bmp.getWidth()), (int) (point.y * bmp.getHeight()), p.TextureRadius);
    }

    public double squareDistance(Cluster sp) {
        double result = 0.0, h = 0.0;
        for (int i = 0; i < mDimension; i++) {
            h = Values[i] - sp.Values[i];
            result += h * h;
        }
        return result;
    }

    public void clearValues() {
        R = 0;
        G = 0;
        B = 0;
        for (int i = 0; i < mDimension; i++) {
            Values[i] = 0.0f;
        }
    }

    public void addPoint(Cluster sp) {
        R += sp.R;
        G += sp.G;
        B += sp.B;
        for (int i = 0; i < mDimension; i++) {
            Values[i] += sp.Values[i];
        }
    }

    public void divByWeight() {
        R /= weight;
        G /= weight;
        B /= weight;
        for (int i = 0; i < mDimension; i++) {
            Values[i] /= weight;
        }
    }

    // Converts RGB to CIE Lab color space
    public static double divFactor = 50.0;

    private void computeAndNormalizeLabValues() {
        // normalize red, green, blue values
        double rLinear = (double) R / 255.0;
        double gLinear = (double) G / 255.0;
        double bLinear = (double) B / 255.0;

        // convert to a sRGB form
        double r = (rLinear > 0.04045) ? Math.pow((rLinear + 0.055) / (1.055), 2.2) : (rLinear / 12.92);
        double g = (gLinear > 0.04045) ? Math.pow((gLinear + 0.055) / (1.055), 2.2) : (gLinear / 12.92);
        double b = (bLinear > 0.04045) ? Math.pow((bLinear + 0.055) / (1.055), 2.2) : (bLinear / 12.92);

        // convert to CIE XYZ
        double x, y, z;
        x = r * 0.4124 + g * 0.3576 + b * 0.1805;
        y = r * 0.2126 + g * 0.7152 + b * 0.0722;
        z = r * 0.0193 + g * 0.1192 + b * 0.9505;

        // CIEXYZ D65 = new CIEXYZ(0.9505, 1.0, 1.0890);
        Values[C_L] = (float) (116.0 * fxyz(y / 1.0) - 16);
        Values[C_a] = (float) (500.0 * (fxyz(x / 0.9505) - fxyz(y / 1.0)));
        Values[C_b] = (float) (200.0 * (fxyz(y / 1.0) - fxyz(z / 1.0890)));

        // Normalize to [0, 1] using divFactor obtained empirically
        Values[C_L] /= 2 * divFactor;
        Values[C_a] /= divFactor;
        Values[C_b] /= divFactor;
    }

    // XYZ to L*a*b* transformation function.
    private static float fxyz(double t) {
        return (float) ((t > 0.008856) ? Math.pow(t, (1.0 / 3.0)) : (7.787 * t + 16.0 / 116.0));
    }

    private void computeAndNormalizeContrastEntropy(ImagePlus bmp, int[][][] GSM, int x, int y, int o) {
        int lx = Math.max(0, x - o);
        int ly = Math.max(0, y - o);
        int rx = Math.min(bmp.getWidth() - 1, x + o);
        int ry = Math.min(bmp.getHeight() - 1, y + o);

        int gridSize = 16;
        int gridSizeSquare = gridSize * gridSize;
        int[] IM = new int[gridSizeSquare];
        for (int i = 0; i < gridSizeSquare; i++) {
            IM[i] = 0;
        }

        // compute incidence matrix
        double count = 0;
        for (int i = lx; i < rx; i++) {
            for (int j = ly; j < ry; j++) {
                count += 4.0;
                IM[GSM[i][j][0] * gridSize + GSM[i][j + 1][0]] += 1;
                IM[GSM[i][j][0] * gridSize + GSM[i + 1][j][0]] += 1;
                IM[GSM[i][j][0] * gridSize + GSM[i + 1][j + 1][0]] += 1;
                IM[GSM[i + 1][j][0] * gridSize + GSM[i][j + 1][0]] += 1;
            }
        }

        Values[C_contrast] = 0; // contrast
        Values[C_entropy] = 0; // entropy                

        // evaluate contrast and entropy from the incidence matrix
        double a, b;
        for (int i = 0; i < gridSize; i++) {
            if (IM[i * gridSize + i] != 0) {
                Values[6] -= (IM[i * gridSize + i] / count) * Math.log(IM[i * gridSize + i] / count);
            }
            // Values[5] += Math.Pow(i - i, 2) * IM[i, i] / c;

            for (int j = i + 1; j < gridSize; j++) {
                a = IM[i * gridSize + j] + IM[j * gridSize + i];
                if (a > 0.0) {
                    b = i - j;
                    Values[5] += b * b * a / count;
                    Values[6] -= (a / count) * Math.log(a / count);
                }
            }
        }

        // normalize contrast and entropy, 25.0 and 4.0 obtained empirically
        Values[C_contrast] /= 25.0;
        Values[C_entropy] /= 4.0;
    }
}
