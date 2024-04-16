package com.example.fast_pdr;
import java.util.ArrayList;
import com.example.fast_pdr.SensorData;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import Jama.Matrix;
import android.util.Log;
import java.util.Arrays;


public class Datalist {
    
    public ArrayList<SensorData> accel_DataList = new ArrayList<SensorData>();
    public ArrayList<SensorData> mag_DataList = new ArrayList<SensorData>();
    public ArrayList<SensorData> Location_DataList = new ArrayList<SensorData>();
    public ArrayList<SensorData> gyro_DataList = new ArrayList<SensorData>();

    // 每个类型的数据都设立一个数组，用于存储数据
    Matrix accel_array = new Matrix(40, 4);
    Matrix mag_array = new Matrix(40, 4);
    Matrix gyro_array = new Matrix(40, 4);
    Matrix location_array = new Matrix(40, 4);

    

    boolean accel_flag = false;
    boolean mag_flag = false;
    boolean gyro_flag = false;
    boolean location_flag = false;

    public Datalist() {
    
    }

    public void printdata() {
        Log.d("accel_Data", Arrays.deepToString(accel_array.getArray()));
        Log.d("mag_Data", Arrays.deepToString(mag_array.getArray()));
        Log.d("gyro_Data", Arrays.deepToString(gyro_array.getArray()));
    }

    public int getarrayfromlist(ArrayList<SensorData> list, Matrix array)
    {
        // 将list中的数据转换为Matrix
        for(int i = 0; i < list.size(); i++)
        {   
            array.set(i, 0, list.get(i).getTimestamp());
            array.set(i, 1, list.get(i).getX());
            array.set(i, 2, list.get(i).getY());
            array.set(i, 3, list.get(i).getZ());
        }

        return list.size();
    }

    public void process()
    {
        // 对于所有的accel的timestamp，在mag和gyro中对该时间戳进行插值

        // 创建一个插值器
        LinearInterpolator interpolator = new LinearInterpolator();

        // 转换list为array
        int accel_count = getarrayfromlist(accel_DataList, accel_array);
        int mag_count = getarrayfromlist(mag_DataList, mag_array);
        int gyro_count = getarrayfromlist(gyro_DataList, gyro_array);

        // 创建一个插值函数，对于gyro和mag的x，y，z分别进行插值
        // 对于所有的accel的timestamp，在mag和gyro中对该时间戳进行插值

       // ...
        double[] accel_timestamp = accel_array.getMatrix(0, accel_array.getRowDimension() - 1, 0, 0).getColumnPackedCopy();

        double[] mag_time = mag_array.getMatrix(0, mag_count - 1, 0, 0).getColumnPackedCopy();
        double[] mag_x = mag_array.getMatrix(0, mag_count - 1, 1, 1).getColumnPackedCopy();
        double[] mag_y = mag_array.getMatrix(0, mag_count - 1, 2, 2).getColumnPackedCopy();
        double[] mag_z = mag_array.getMatrix(0, mag_count - 1, 3, 3).getColumnPackedCopy();

        double[] gyro_time = gyro_array.getMatrix(0, gyro_count - 1, 0, 0).getColumnPackedCopy();
        double[] gyro_x = gyro_array.getMatrix(0, gyro_count - 1, 1, 1).getColumnPackedCopy();
        double[] gyro_y = gyro_array.getMatrix(0, gyro_count - 1, 2, 2).getColumnPackedCopy();
        double[] gyro_z = gyro_array.getMatrix(0, gyro_count - 1, 3, 3).getColumnPackedCopy();

        PolynomialSplineFunction mag_x_interpolator = interpolator.interpolate(mag_time, mag_x);
        PolynomialSplineFunction mag_y_interpolator = interpolator.interpolate(mag_time, mag_y);
        PolynomialSplineFunction mag_z_interpolator = interpolator.interpolate(mag_time, mag_z);

        PolynomialSplineFunction gyro_x_interpolator = interpolator.interpolate(gyro_time, gyro_x);
        PolynomialSplineFunction gyro_y_interpolator = interpolator.interpolate(gyro_time, gyro_y);
        PolynomialSplineFunction gyro_z_interpolator = interpolator.interpolate(gyro_time, gyro_z);

        // 对于每一个accel的时间戳，找到对应的mag和gyro的数据

        for(int i = 0; i < accel_timestamp.length; i++)
        {
            double mag_x_value = mag_x_interpolator.value(accel_timestamp[i]);
            double mag_y_value = mag_y_interpolator.value(accel_timestamp[i]);
            double mag_z_value = mag_z_interpolator.value(accel_timestamp[i]);

            double gyro_x_value = gyro_x_interpolator.value(accel_timestamp[i]);
            double gyro_y_value = gyro_y_interpolator.value(accel_timestamp[i]);
            double gyro_z_value = gyro_z_interpolator.value(accel_timestamp[i]);

            // 将这些数据存储到原来的数组中
            gyro_array.set(i, 1, (accel_timestamp[i]));
            gyro_array.set(i, 1, gyro_x_value);
            gyro_array.set(i, 2, gyro_y_value);
            gyro_array.set(i, 3, gyro_z_value);

            mag_array.set(i, 1, (accel_timestamp[i]));
            mag_array.set(i, 1, mag_x_value);
            mag_array.set(i, 2, mag_y_value);
            mag_array.set(i, 3, mag_z_value);




        }

        printdata();

        


        
    }

    // 设置一个函数，持续监控flag的值，当所有的flag都为true时，进行数据处理
    public void checkFlag()
    {
        if(accel_flag && mag_flag && gyro_flag)
        {
           
            try
            {
                process();
            }
            catch(Throwable t)
            {
                printdata();
            }
            
            
            // 重置flag
            accel_flag = false;
            mag_flag = false;
            gyro_flag = false;
            location_flag = false;
        }
    }
    
    public void addAccelData(long timestamp, double x, double y, double z, int mode)
    {
        // 如果是静态
        if(mode == 1)
        {
            // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            accel_DataList.add(sensorData);
        }

        // 如果是动态
        if(mode == 2)
        {
            // 检查内部数据的数量
            int size = accel_DataList.size();
            if(size < 40)
            {
                // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            accel_DataList.add(sensorData);
            }
            else
            {
                // 创建一个新的传感器数据对象
                SensorData sensorData = new SensorData(x, y, z, timestamp);
                // 将新的传感器数据对象添加到数组中
                accel_DataList.add(sensorData);
                // 删除第一个元素
                accel_DataList.remove(0);


            }

            accel_flag = true;
        }
    }

    public void addMagData(long timestamp, double x, double y, double z, int mode)
    {
        // 如果是静态
        if(mode == 1)
        {
            // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            mag_DataList.add(sensorData);
        }

        // 如果是动态
        if(mode == 2)
        {
            // 检查内部数据的数量
            int size = mag_DataList.size();
            if(size < 40)
            {
                // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            mag_DataList.add(sensorData);
            }
            else
            {
                // 创建一个新的传感器数据对象
                SensorData sensorData = new SensorData(x, y, z, timestamp);
                // 将新的传感器数据对象添加到数组中
                mag_DataList.add(sensorData);
                // 删除第一个元素
                mag_DataList.remove(0);
            }

            mag_flag = true;
        }
    }

    public void addLocationData(long timestamp, double x, double y, double z) 
    {
        // 创建一个新的传感器数据对象
        SensorData sensorData = new SensorData(x, y, z, timestamp);
        // 将新的传感器数据对象添加到数组中
        Location_DataList.add(sensorData);
    }   

    public void addGyroData(long timestamp, double x, double y, double z, int mode)
    {
        // 如果是静态
        if(mode == 1)
        {
            // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            gyro_DataList.add(sensorData);
        }

        // 如果是动态
        if(mode == 2)
        {
            // 检查内部数据的数量
            int size = gyro_DataList.size();
            if(size < 40)
            {
                // 创建一个新的传感器数据对象
            SensorData sensorData = new SensorData(x, y, z, timestamp);
            // 将新的传感器数据对象添加到数组中
            gyro_DataList.add(sensorData);
            }
            else
            {
                // 创建一个新的传感器数据对象
                SensorData sensorData = new SensorData(x, y, z, timestamp);
                // 将新的传感器数据对象添加到数组中
                gyro_DataList.add(sensorData);
                // 删除第一个元素
                gyro_DataList.remove(0);
            }

            gyro_flag = true;
        }
    
    }






}
