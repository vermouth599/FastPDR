package com.example.fast_pdr;
import java.util.ArrayList;
import java.util.List;
import com.example.fast_pdr.SensorData;
import android.util.Log;
import java.lang.Math;
import java.lang.reflect.Array;
import Jama.Matrix;
import org.apache.commons.math3.complex.Quaternion;
import org.locationtech.proj4j.*;
import com.example.fast_pdr.CanvasView;

public class PDR {

    // 
    private CanvasView canvasView;

    public PDR(CanvasView canvasView)
    {
        this.canvasView = canvasView;
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
    private double step_length;

    // 初始变量
    public double initial_r, initial_theta; // 初始水平姿态角
    public double initial_B, initial_L, initial_H; // 初始位置
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
    public double omega = 0.0;
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
    public double step_length_ = 0.0;

    // 位置
    public double X_ = 0.0;
    public double Y_ = 0.0;


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

        double B = 0, L = 0, H = 0;

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

        Phi_m = Phi_m * Math.PI / 180; //deg to rad
        
        // 转换为四元数
        q_now = fromEuler(initial_r, initial_theta, Phi_m);

    

        initial_r = initial_r * 180 / Math.PI; //rad to deg
        initial_theta = initial_theta * 180 / Math.PI; //rad to deg
        Phi_m = Phi_m * 180 / Math.PI; //rad to deg

        X_ = 0.0;
        Y_ = 0.0;

        canvasView.drawPoint((int)X_, (int)Y_);

       


        

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
    public void from_main(ArrayList steplist, int last_index, double last_time , Matrix Buff)
    {
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
                                    {Buff.get(i, 1) * V.get(1, 0) - Buff.get(i, 22) * V.get(0, 0)}};
               Matrix e_acc = new Matrix(e_acc_);
               
               // 计算磁力计的修正量
               double[][] M_2_ = {{Buff.get(i,8)}, {Buff.get(i,9)}, {Buff.get(i,10)}};
               Matrix M_2 = new Matrix(M_2_);
               Matrix mag_dili = C_n_b.transpose().times(M_2);

               double [][] M_3_ = {{Math.sqrt(mag_dili.get(0, 0) * mag_dili.get(0, 0) + mag_dili.get(1, 0) * mag_dili.get(1, 0))},
                                   {0},
                                   {mag_dili.get(2, 0)}};
               Matrix M_3 = new Matrix(M_3_);

               Matrix mag_jiti = C_n_b.times(M_3);

               double[][] e_mag_ = {{Buff.get(i,9) * mag_dili.get(2, 0) - Buff.get(i,10) * mag_dili.get(1, 0)},
                                    {Buff.get(i,10) * mag_dili.get(0, 0) - Buff.get(i,8) * mag_dili.get(2, 0)},
                                    {Buff.get(i,8) * mag_dili.get(1, 0) - Buff.get(i,9) * mag_dili.get(0, 0)}};
                
               
               Matrix e_mag = new Matrix(e_mag_);
               e = e_acc.plus(e_mag);

               
                   
               dt = Buff.get(i+1, 0) - Buff.get(i, 0);
               e_int = e_int.plus(e.times(dt));
               
                                   
               q_now = quaternion_update(q_now, Buff.get(i, 4), Buff.get(i, 5), Buff.get(i, 6), dt);
               
               if (step_index < steplist.size() && Buff.get(i, 0) == ((Double) steplist.get(step_index)).doubleValue())
               {
                // 更新
                   location_update(q_now);
                   step_index++;
               }
               
           }
              initial = false;
       }
       else
       {
           for (i = last_index; i < Buff.getRowDimension()-1; i++)
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
                                    {Buff.get(i, 1) * V.get(1, 0) - Buff.get(i, 22) * V.get(0, 0)}};
               Matrix e_acc = new Matrix(e_acc_);
               
               // 计算磁力计的修正量
               double[][] M_2_ = {{Buff.get(i,8)}, {Buff.get(i,9)}, {Buff.get(i,10)}};
               Matrix M_2 = new Matrix(M_2_);
               Matrix mag_dili = C_n_b.transpose().times(M_2);

               double [][] M_3_ = {{Math.sqrt(mag_dili.get(0, 0) * mag_dili.get(0, 0) + mag_dili.get(1, 0) * mag_dili.get(1, 0))},
                                   {0},
                                   {mag_dili.get(2, 0)}};
               Matrix M_3 = new Matrix(M_3_);

               Matrix mag_jiti = C_n_b.times(M_3);

               double[][] e_mag_ = {{Buff.get(i,9) * mag_dili.get(2, 0) - Buff.get(i,10) * mag_dili.get(1, 0)},
                                    {Buff.get(i,10) * mag_dili.get(0, 0) - Buff.get(i,8) * mag_dili.get(2, 0)},
                                    {Buff.get(i,8) * mag_dili.get(1, 0) - Buff.get(i,9) * mag_dili.get(0, 0)}};
                
               Matrix e_mag = new Matrix(e_mag_);              
               e = e_acc.plus(e_mag);

               dt = Buff.get(i+1, 0) - Buff.get(i, 0);

               e_int = e_int.plus(e.times(dt));

               q_now = quaternion_update(q_now, Buff.get(i, 4), Buff.get(i, 5), Buff.get(i, 6), dt);
               if (step_index < steplist.size() && Buff.get(i, 0) == ((Double) steplist.get(step_index)).doubleValue())
               {

                // 更新
                   location_update(q_now);
                   step_index++;
               }

           }
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

        double roll = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
        if(roll < 0)
        {
            roll = roll + 2 * Math.PI;
        }
        double pitch = Math.asin(2 * (w * y - z * x));
        double yaw = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));

        return new double[]{roll, pitch, yaw}; // rad,rad,rad
    }
    
    // 位置更新，只更新xy平面
    public void location_update(Quaternion q_now)
    {   
        double[] euler = fromQuaternion(q_now);
        double yaw = euler[2];//rad
        X_ = X_ + step_length_ * Math.cos(yaw);
        Y_ = Y_ + step_length_ * Math.sin(yaw);
        canvasView.drawPoint((float)X_, (float)Y_);
      
    }

  

    
    


   




    
}
