package com.example.fast_pdr;
import java.util.ArrayList;
import java.util.List;
import com.example.fast_pdr.SensorData;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.lang.Math;
import java.lang.reflect.Array;
import Jama.Matrix;
import org.apache.commons.math3.complex.Quaternion;
import org.locationtech.proj4j.*;
import com.example.fast_pdr.CanvasView;
import com.example.fast_pdr.MapManager;

public class PDR {

    // Create a Handler for the main thread
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private MapManager mMapManager;
    // 
    private CanvasView canvasView;

    private LineChartHelper mLineChart;

    public PDR(CanvasView canvasView,MapManager mapManager)
    {
        this.canvasView = canvasView;
        this.mMapManager = mapManager;
    }
    
    // 定义一个常量,武汉地区磁偏角
    public static final double D_CONSTANT = -5.0;
    
    private double accel_x;
    private double accel_y;
    private double accel_z;
    private double gyro_x;
    private double gyro_y;
    private double gyro_z;
    private double mag_x;
    private double mag_y;
    private double mag_z;
    private long timestamp;
    



    // 初始变量
    public double initial_r, initial_theta; // 初始水平姿态角
    public double initial_B = 0.0, initial_L = 0.0, initial_H = 0.0; // 初始位置
    public double Phi_m; // 真北方角度 deg

    // dt
    public double dt = 0.0;

    // 四元数
    public Quaternion q_now = new Quaternion(1, 0, 0, 0);
    public Quaternion q_last = new Quaternion(1, 0, 0, 0);

    // 旋转矩阵
    public Matrix C_n_b = new Matrix(3,3);

    // 加速度计修正量
    public Matrix V = new Matrix(3,1);

    // 误差补偿参数,用于控制陀螺仪补偿的各项误差
    public double omega = 1.5;
    public double beta = 2.146/omega;
    public double kp = 2.0 * beta;
    public double ki = beta * beta; 

    // 补偿相关矩阵
    public Matrix e = new Matrix(3,1);
    public Matrix e_int = new Matrix(3,1);

    // initial
    boolean initial = true;

    // 用于统计步到了第几步
    public int step_index = 0;

    // 步长
    public double step_length_ = 0.6;

    // 位置
    public double X_ = 0.0;
    public double Y_ = 0.0;

    // 位置列表
    public List<Double> X_list = new ArrayList<Double>();
    public List<Double> Y_list = new ArrayList<Double>();
    public List<Double> yaw_list = new ArrayList<Double>();
    public List<Double> time_List= new ArrayList<Double>();

    // 控制
    public boolean usemap = false;
    public boolean fliter_6 = false;

    public void clear()
    {
        X_list.clear();
        Y_list.clear();
        yaw_list.clear();
        time_List.clear();
        step_index = 0;
        X_ = 0.0;
        Y_ = 0.0;
        q_now = new Quaternion(1, 0, 0, 0);
        q_last = new Quaternion(1, 0, 0, 0);
        initial = true;
    }


    /********************************************************
     * 初始化函数，计算加速度和磁场数据的平均值，并计算初始的角度。
     * Author:lzy
     * @param accel_DataList 加速度数据列表
     * @param mag_DataList 磁场数据列表
     * @param Location_DataList 初始经纬度列表
     *********************************************************/
    public void Initialize(ArrayList<SensorData> accel_DataList, ArrayList<SensorData> mag_DataList, ArrayList<SensorData> Location_DataList)
    {
        // 取平均值
        // 加速度
        double acc_x = 0, acc_y = 0, acc_z = 0;
        for (int i = 0; i < accel_DataList.size(); i++)
        {
            acc_x += accel_DataList.get(i).getX();
            acc_y += accel_DataList.get(i).getY();
            acc_z += accel_DataList.get(i).getZ();
        }
        acc_x /= accel_DataList.size();
        acc_y /= accel_DataList.size();
        acc_z /= accel_DataList.size();

        initial_r = Math.atan2(-acc_x, -acc_z);//rad
        initial_theta = Math.atan2(acc_x, Math.sqrt(acc_y * acc_y + acc_z * acc_z));//rad

        double mag_x_ = 0, mag_y_ = 0, mag_z_ = 0;
        // 磁强计
        for (int i = 0; i < mag_DataList.size(); i++)
        {
            mag_x_ += mag_DataList.get(i).getX();
            mag_y_ += mag_DataList.get(i).getY();
            mag_z_ += mag_DataList.get(i).getZ();
        }
        mag_x_ /= mag_DataList.size();
        mag_y_ /= mag_DataList.size();
        mag_z_ /= mag_DataList.size();

        double m_x = mag_x_ * Math.cos(initial_theta) + mag_y_ * Math.sin(initial_theta)
                * Math.sin(initial_r) + mag_z_ * Math.sin(initial_theta) * Math.cos(initial_r);
        double m_y = mag_y_ * Math.cos(initial_r) - mag_z_ * Math.sin(initial_r);

        double Phi_ = -Math.atan2(m_y, m_x);//rad


        Phi_ = Phi_ * 180 / Math.PI;//rad2deg

        Phi_m = Phi_ + D_CONSTANT;//deg
        yaw_list.add(Phi_m);

        double B = 0, L = 0, H = 0;

        if (Location_DataList.size() != 0)
        {
            
        
        
            // 位置取平均值
            for (int i = 0; i < Location_DataList.size(); i++)
            {
                B += Location_DataList.get(i).getX();
                L += Location_DataList.get(i).getY();
                H += Location_DataList.get(i).getZ();
            }

            B /= Location_DataList.size();
            L /= Location_DataList.size();
            H /= Location_DataList.size();

            initial_B = B;
            initial_L = L;
            initial_H = H;
        }

        Phi_m = Phi_m * Math.PI / 180; //deg to rad
        
        // 转换为四元数
        q_now = fromEuler(initial_r, initial_theta, Phi_m);

    

        initial_r = initial_r * 180 / Math.PI; //rad to deg
        initial_theta = initial_theta * 180 / Math.PI; //rad to deg
        Phi_m = Phi_m * 180 / Math.PI; //rad to deg

        X_ = 0.0;
        Y_ = 0.0;
        X_list.add(X_);
        Y_list.add(Y_);



        canvasView.drawPoint((float)X_,(float)Y_);
        if (usemap == true) {
            mMapManager.setcentralpoint(initial_B, initial_L);//TODO:改成INITIAL_BL，仅为测试用
//            mMapManager.testMoveToPoint();
        }

       


        

    }

    // 从角度转换为四元数
    public Quaternion fromEuler(double roll, double pitch, double yaw) // rad,rad,rad
    {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        double w = cy * cp * cr + sy * sp * sr;
        double x = cy * cp * sr - sy * sp * cr;
        double y = sy * cp * sr + cy * sp * cr;
        double z = sy * cp * cr - cy * sp * sr;

        return new Quaternion(w, x, y, z).normalize();
    }

    // 从四元数到旋转矩阵
    public Matrix quaternion2Rotation(Quaternion q)
    {
        double w = q.getQ0();
        double x = q.getQ1();
        double y = q.getQ2();
        double z = q.getQ3();

        double[][] R = new double[3][3];
        R[0][0] = w*w + x*x - y*y - z*z;
        R[0][1] = 2 * (x*y + w*z);
        R[0][2] = 2 * (x*z - w*y);

        R[1][0] = 2 * (x*y - w*z);
        R[1][1] = w*w - x*x + y*y - z*z;
        R[1][2] = 2 * (y*z + w*x);

        R[2][0] = 2 * (x*z + w*y);
        R[2][1] = 2 * (y*z - w*x);
        R[2][2] = w*w - x*x - y*y + z*z;

        return new Matrix(R);
    }

    // 从main函数中获取数据
    public void from_main(ArrayList steplist, ArrayList lengthlist, int last_index, double last_time , Matrix Buff, int buff_count)
    {
        // Buff - 0 - timestamp - 1 2 3 - accel - 4 5 6 - gyro - 7 8 9 - mag

        int i;
       
       if (initial == true)
       {
           for (i = 0; i < last_index; i++)
           {
               q_last = q_now;
               C_n_b = quaternion2Rotation(q_now);
               double[][] M_1_ = {{0}, {0}, {-1}};
               Matrix M_1 = new Matrix(M_1_);
               
               // 计算加速度计的修正量
               V = C_n_b.times(M_1);

               double[][] e_acc_ = {{Buff.get(i, 2) * V.get(2, 0) - Buff.get(i, 3) * V.get(1, 0)},
                                    {Buff.get(i, 3) * V.get(0, 0) - Buff.get(i, 1) * V.get(2, 0)},
                                    {Buff.get(i, 1) * V.get(1, 0) - Buff.get(i, 2) * V.get(0, 0)}};
               Matrix e_acc = new Matrix(e_acc_);
               
               // 计算磁力计的修正量

               double mag_1 = Buff.get(i, 7)/100; // uT -> Gauss
               double mag_2 = Buff.get(i, 8)/100; // uT -> Gauss
               double mag_3 = Buff.get(i, 9)/100; // uT -> Gauss

               double[][] M_2_ = {{mag_1}, {mag_2}, {mag_3}};
               Matrix M_2 = new Matrix(M_2_);
               Matrix mag_dili = C_n_b.transpose().times(M_2);

               double [][] M_3_ = {{Math.sqrt(mag_dili.get(0, 0) * mag_dili.get(0, 0) + mag_dili.get(1, 0) * mag_dili.get(1, 0))},
                                   {0},
                                   {mag_dili.get(2, 0)}};
               Matrix M_3 = new Matrix(M_3_);

               Matrix mag_jiti = C_n_b.times(M_3);

               double[][] e_mag_ = {{mag_2 * mag_jiti.get(2, 0) - mag_3 * mag_jiti.get(1, 0)},
                                    {mag_3 * mag_jiti.get(0, 0) - mag_1 * mag_jiti.get(2, 0)},
                                    {mag_1 * mag_jiti.get(1, 0) - mag_2 * mag_jiti.get(0, 0)}};
                
               
               Matrix e_mag = new Matrix(e_mag_);
               
               
               if (fliter_6 == true)
               {
                e = e_acc;
               }
               else
               {
               
               e = e_acc.plus(e_mag);
               }
               

               
                   
               dt = Buff.get(i+1, 0) - Buff.get(i, 0); // ms
               dt = dt / 1000.0; // s

                
               e_int = e_int.plus(e.times(dt));
               
                                   
              
               
               if (step_index < steplist.size() && Buff.get(i, 0) == ((Double) steplist.get(step_index)).doubleValue())
               {
                // 更新
                   step_length_ = (Double) lengthlist.get(step_index);
                   location_update(q_now);
                   step_index++;
                   Log.d("位置更新", String.valueOf(Buff.get(i, 0)));
               }
               q_now = quaternion_update(q_now, Buff.get(i, 4), Buff.get(i, 5), Buff.get(i, 6), dt);
               // 获取yaw
                double[] euler = fromQuaternion(q_now);
                double yaw = euler[2];//rad
                yaw = yaw * 180 / Math.PI;//rad2deg
                yaw_list.add(yaw);
                time_List.add(Buff.get(i, 0));
           }
              initial = false;
       }
       else
       {
           for (i = last_index; i < buff_count - 1; i++)
           {    
                // 更新姿态
               q_last = q_now;
               
               C_n_b = quaternion2Rotation(q_now);
               double[][] M_1_ = {{0}, {0}, {-1}};
               Matrix M_1 = new Matrix(M_1_);
               
               // 计算加速度计的修正量
               V = C_n_b.times(M_1);

               double[][] e_acc_ = {{Buff.get(i, 2) * V.get(2, 0) - Buff.get(i, 3) * V.get(1, 0)},
                                    {Buff.get(i, 3) * V.get(0, 0) - Buff.get(i, 1) * V.get(2, 0)},
                                    {Buff.get(i, 1) * V.get(1, 0) - Buff.get(i, 2) * V.get(0, 0)}};
               Matrix e_acc = new Matrix(e_acc_);
               
               // 计算磁力计的修正量
               double mag_1 = Buff.get(i, 7)/100; // uT -> Gauss
               double mag_2 = Buff.get(i, 8)/100; // uT -> Gauss
               double mag_3 = Buff.get(i, 9)/100; // uT -> Gauss

               double[][] M_2_ = {{mag_1}, {mag_2}, {mag_3}};
               Matrix M_2 = new Matrix(M_2_);
               Matrix mag_dili = C_n_b.transpose().times(M_2);

               double [][] M_3_ = {{Math.sqrt(mag_dili.get(0, 0) * mag_dili.get(0, 0) + mag_dili.get(1, 0) * mag_dili.get(1, 0))},
                                   {0},
                                   {mag_dili.get(2, 0)}};
               Matrix M_3 = new Matrix(M_3_);

               Matrix mag_jiti = C_n_b.times(M_3);

               double[][] e_mag_ = {{mag_2 * mag_jiti.get(2, 0) - mag_3 * mag_jiti.get(1, 0)},
                                    {mag_3 * mag_jiti.get(0, 0) - mag_1 * mag_jiti.get(2, 0)},
                                    {mag_1 * mag_jiti.get(1, 0) - mag_2 * mag_jiti.get(0, 0)}};
                
               Matrix e_mag = new Matrix(e_mag_);              

               if (fliter_6 == true)
               {
                e = e_acc;
               }
               else
               {
               
               e = e_acc.plus(e_mag);
               }
            

               dt = Buff.get(i+1, 0) - Buff.get(i, 0); // ms
               dt = dt / 1000.0; // s
    
    

               e_int = e_int.plus(e.times(dt));

              
               if (step_index < steplist.size() && Buff.get(i, 0) == ((Double) steplist.get(step_index)).doubleValue())
               {

                // 更新
                   step_length_ = (double) lengthlist.get(step_index);
                   location_update(q_now);
                   step_index++;
                   Log.d("位置更新", String.valueOf(Buff.get(i, 0)));
               }
               q_now = quaternion_update(q_now, Buff.get(i, 4), Buff.get(i, 5), Buff.get(i, 6), dt);
                // 获取yaw
                 double[] euler = fromQuaternion(q_now);
                 double yaw = euler[2];//rad
                 yaw = yaw * 180 / Math.PI;//rad2deg
                 yaw_list.add(yaw);
                 time_List.add(Buff.get(i, 0));
           }
           int a = 0;

        }



    }

    // 四元数更新
    public Quaternion quaternion_update(Quaternion q_now, double gyro_x, double gyro_y, double gyro_z, double dt)
    {   // 计算陀螺仪的改正量
        gyro_x = gyro_x + kp * e.get(0, 0) + ki * e_int.get(0, 0);
        gyro_y = gyro_y + kp * e.get(1, 0) + ki * e_int.get(1, 0);
        gyro_z = gyro_z + kp * e.get(2, 0) + ki * e_int.get(2, 0);

        // 计算更新的四元数
        double q0 = q_now.getQ0() + (-q_now.getQ1() * gyro_x - q_now.getQ2() * gyro_y - q_now.getQ3() * gyro_z) * dt / 2;
        double q1 = q_now.getQ1() + (q_now.getQ0() * gyro_x + q_now.getQ2() * gyro_z - q_now.getQ3() * gyro_y) * dt / 2;
        double q2 = q_now.getQ2() + (q_now.getQ0() * gyro_y - q_now.getQ1() * gyro_z + q_now.getQ3() * gyro_x) * dt / 2;
        double q3 = q_now.getQ3() + (q_now.getQ0() * gyro_z + q_now.getQ1() * gyro_y - q_now.getQ2() * gyro_x) * dt / 2;
        
        return new Quaternion(q0, q1, q2, q3).normalize();
        
    }

    // 从四元数到欧拉角
    public double[] fromQuaternion(Quaternion q)
    {
        double w = q.getQ0();
        double x = q.getQ1();
        double y = q.getQ2();
        double z = q.getQ3();

        double roll = Math.atan2(2.0 * (w * x + y * z), 1.0 - 2.0 * (x * x + y * y));
        if(roll < 0)
        {
            roll = roll + 2.0 * Math.PI;
        }
        double pitch = Math.asin(2.0 * (w * y - z * x));
        double yaw = Math.atan2(2.0 * (w * z + x * y), 1.0 - 2.0 * (y * y + z * z));

        return new double[]{roll, pitch, yaw}; // rad,rad,rad
    }
    
    // 位置更新，只更新xy平面
    public void location_update(Quaternion q_now)
    {   
        double[] euler = fromQuaternion(q_now);
        double yaw = euler[2];//rad

        // 由于安卓的轴与算法的轴不一样，yaw需要取反
        yaw = -yaw;
         X_ = X_ + step_length_ * Math.cos(yaw);
         Y_ = Y_ + step_length_ * Math.sin(yaw);
//        X_ = X_ + 0.6 * Math.cos(yaw);
//        Y_ = Y_ + 0.6 * Math.sin(yaw);
        canvasView.drawPoint((float)X_,(float)Y_);
        X_list.add(X_); // North - m
        Y_list.add(Y_); // East - m

        double B_ = 0.0;
        double L_ = 0.0;
        if (usemap == true) {
            // 转换坐标
            double[] BL;
            BL = xyToBL(X_, Y_, initial_B, initial_L);
             B_ =  BL[0];
             L_ =  BL[1];
             Log.d("B_", String.valueOf(B_));
             Log.d("L_", String.valueOf(L_));
             
        }

        // Create new final variables
        final double finalB_ = B_;
        final double finalL_ = L_;

        if (usemap == true) {
            // Use the Handler to move to point on the main thread
            mainHandler.post(new Runnable() {
                @Override
                public void run() {

                    mMapManager.moveToPoint(finalB_, finalL_); //TODO:验证这里是不是对的
                }
            });
        }

      
    }

    /**
     * This method is used to convert plane coordinates (dX, dY) to geographic coordinates (latitude, longitude).
     *
     * @param dX The northward coordinate relative to the origin in the plane coordinate system, in meters.
     * @param dY The eastward coordinate relative to the origin in the plane coordinate system, in meters.
     * @param B0 The latitude of the origin, in degrees.
     * @param L0 The longitude of the origin, in degrees.
     * @return An array of two doubles. The first element is the latitude (B) and the second element is the longitude (L).
     */

    public double[] xyToBL(double dX, double dY, double B0, double L0) // m m deg deg
    {  

        
        String projString = "+proj=tmerc +lat_0=" + B0 + " +lon_0=" + L0 + " +k=1 +x_0=500000 +y_0=0 +ellps=WGS84 +units=m +no_defs";
        
        // Create coordinate reference systems
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem srcCRS = factory.createFromName("EPSG:4326");
        CoordinateReferenceSystem dstCRS = factory.createFromParameters(null, projString);
    
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = ctFactory.createTransform(srcCRS, dstCRS);

        // Define the original coordinates
        ProjCoordinate srcCoord = new ProjCoordinate(L0, B0);

        ProjCoordinate dstCoord = new ProjCoordinate();
        transform.transform(srcCoord, dstCoord);

        Log.d("dstCoord", " B: " + dstCoord.x + " L: " + dstCoord.y); // x - 东方向  y - 北方向

        double X_0 = dstCoord.x; //坐标轴原点的东方向值，理应为500000.0
        double Y_0 = dstCoord.y; //坐标轴原点的北方向值，理应为0.0

        assert (X_0 == 500000.0);
        assert (Y_0 == 0.0);

        double X = X_0 + dX;
        double Y = Y_0 + dY;

        // 逆转换
        ProjCoordinate srcCoord_ = new ProjCoordinate();//地理坐标系
        ProjCoordinate dstCoord_ = new ProjCoordinate(X, Y);//高斯坐标系

        // Gauss-Kruger projection inverse calculation
        transform = ctFactory.createTransform(dstCRS, srcCRS);

        transform.transform(dstCoord_, srcCoord_);

        Log.d("srcCoord_", " B: " + srcCoord_.y + " L: " + srcCoord_.x); // x - 经度  y - 纬度


        return new double[]{srcCoord_.y,  srcCoord_.x}; // B L
    
    }

    // 计算TDR
    public double TDR(ArrayList Length_list)
    {
        // 计算总长度
        double total_length = 0.0;
        for (int i = 0; i < Length_list.size(); i++)
        {
            total_length += (double) Length_list.get(i);
        }
        // 计算闭合差
        double a = X_list.get(0);
        double b = Y_list.get(0);
        double c = X_list.get(X_list.size() - 1);
        double d = Y_list.get(Y_list.size() - 1);

        double close_error = Math.sqrt((a - c) * (a - c) + (b - d) * (b - d));

        return close_error / total_length;

    }
    


  

    
    


   




    
}
