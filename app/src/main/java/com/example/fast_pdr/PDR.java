package com.example.fast_pdr;
import java.util.ArrayList;
import java.util.List;
import com.example.fast_pdr.SensorData;
import java.lang.Math;
import java.lang.reflect.Array;
import Jama.Matrix;
import org.apache.commons.math3.complex.Quaternion;

public class PDR {

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

    // 四元数
    public Quaternion q_now = new Quaternion(1, 0, 0, 0);
    public Quaternion q_last = new Quaternion(1, 0, 0, 0);

    // 旋转矩阵
    public Matrix C_n_b = new Matrix(3,3);

    // 加速度计修正量
    public double V_0 = 0;

    // 误差补偿参数,用于控制陀螺仪补偿的各项误差
    public double Beta = 0.0;

    // initial
    boolean initial = true;

    // 用于统计步到了第几步
    public int step_index = 0;


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

        // 转换为旋转矩阵
        C_n_b = quaternion2Rotation(q_now);

        initial_r = initial_r * 180 / Math.PI; //rad to deg
        initial_theta = initial_theta * 180 / Math.PI; //rad to deg
        Phi_m = Phi_m * 180 / Math.PI; //rad to deg


        

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
           for (i = 0; i <= last_index; i++)
           {
               q_last = q_now;
               q_now = quaternion_update(q_now);
               if (Buff(i, 0) == steplist(step_index))
               {
                // TODO:更新位置   
                step_index++;
               }
               
           }
              initial = false;
       }
       else
       {
           for (i = 0; i < Buff.getRowDimension(); i++)
           {
               q_last = q_now;
               q_now = quaternion_update(q_now);
               if (Buff(i, 0) == steplist(step_index))
               {

                //TODO:更新位置
                step_index++;
               }

           }
        }



    }

    // 四元数更新
    public void quaternion_update(Quaternion q)
    {
        //TODO: 补全四元数更新函数
    }


    
    


   




    
}
