package com.example.fast_pdr;
import static java.lang.Math.*;

public class GJC2WGS {
    private static final double earthR = 6378137.0;

    private static boolean outOfChina(double lat, double lng) {
        return !(72.004 <= lng && lng <= 137.8347 && 0.8293 <= lat && lat <= 55.8271);
    }
    public static double[] transform(double x, double y) {
        double xy = x * y;
        double absX = sqrt(abs(x));
        double xPi = x * PI;
        double yPi = y * PI;
        double d = 20.0 * sin(6.0 * xPi) + 20.0 * sin(2.0 * xPi);

        double lat = d;
        double lng = d;

        lat += 20.0 * sin(yPi) + 40.0 * sin(yPi / 3.0);
        lng += 20.0 * sin(xPi) + 40.0 * sin(xPi / 3.0);

        lat += 160.0 * sin(yPi / 12.0) + 320 * sin(yPi / 30.0);
        lng += 150.0 * sin(xPi / 12.0) + 300.0 * sin(xPi / 30.0);

        lat *= 2.0 / 3.0;
        lng *= 2.0 / 3.0;

        lat += -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * xy + 0.2 * absX;
        lng += 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * xy + 0.1 * absX;

        return new double[]{lat, lng};
    }

    private static double[] delta(double lat, double lng) {

        double ee = 0.00669342162296594323;
        double[] dBL = transform(lng - 105.0, lat - 35.0);
        double dLat = dBL[0], dLng = dBL[1];
        double radLat = lat / 180.0 * PI;
        double magic = sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = sqrt(magic);
        dLat = (dLat * 180.0) / ((earthR * (1 - ee)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (earthR / sqrtMagic * cos(radLat) * PI);
        return new double[]{dLat, dLng};
    }

    public static double[] gcj2wgs_exact(double gcjLat, double gcjLng) {
        double initDelta = 0.01;
        double threshold = 0.000001;
        double dLat = initDelta, dLng = initDelta;
        double mLat = gcjLat - dLat, mLng = gcjLng - dLng;
        double pLat = gcjLat + dLat, pLng = gcjLng + dLng;
        double wgsLat = gcjLat, wgsLng = gcjLng;
        for (int i = 0; i < 30; i++) {
            wgsLat = (mLat + pLat) / 2;
            wgsLng = (mLng + pLng) / 2;
            double[] tmp = wgs2gcj(wgsLat, wgsLng);
            dLat = tmp[0] - gcjLat;
            dLng = tmp[1] - gcjLng;
            if (abs(dLat) < threshold && abs(dLng) < threshold)
                return new double[]{wgsLat, wgsLng};
            if (dLat > 0) pLat = wgsLat; else mLat = wgsLat;
            if (dLng > 0) pLng = wgsLng; else mLng = wgsLng;
        }
        return new double[]{wgsLat, wgsLng};
    }

    public static double[] wgs2gcj(double wgsLat, double wgsLng) {
        if (outOfChina(wgsLat, wgsLng)) {
            return new double[]{wgsLat, wgsLng};
        } else {
            double[] d = delta(wgsLat, wgsLng);
            return new double[]{wgsLat + d[0], wgsLng + d[1]};
        }
    }
}
