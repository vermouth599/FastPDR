package com.example.fast_pdr;
import java.util.ArrayList;
import java.util.List;
import com.example.fast_pdr.SensorData;
import java.lang.Math;
import java.lang.reflect.Array;
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

        

        initial_r = initial_r * 180 / Math.PI; //rad to deg
        initial_theta = initial_theta * 180 / Math.PI; //rad to deg



        

    }
   




    
}
