package com.example.fast_pdr;
import java.util.ArrayList;
import com.example.fast_pdr.SensorData;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.OutOfRangeException;
import Jama.Matrix;
import android.util.Log;
import java.util.Arrays;
import java.util.Comparator;


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

    boolean initial = true;

    boolean accel_flag = false;
    boolean mag_flag = false;
    boolean gyro_flag = false;
    boolean location_flag = false;

    int last_index = 0;
    double last_time = 0.0;

    boolean process_flag = false;


    // 设置一个大缓冲区，在完成时间戳对齐之后，将数据存储到这个缓冲区中
    // 用于存储最终的数据
    Matrix Buffer = new Matrix(100, 10); // 1000行，10列 包含时间戳，加速度，陀螺仪，磁强计

    public Datalist() {
    
    }

    public void printdata() {
        Log.d("accel_Data", Arrays.deepToString(accel_array.getArray()));
        Log.d("mag_Data", Arrays.deepToString(mag_array.getArray()));
        Log.d("gyro_Data", Arrays.deepToString(gyro_array.getArray()));
    }

    public void printBuffer() {
        Log.d("Buffer", Arrays.deepToString(Buffer.getArray()));
    }

    public void pushtoBuffer(int last_index, double last_time)
    {
        

        // 防止buffer溢出   
        if(last_index + accel_array.getRowDimension() >= 1000)
        {
            // 整体向前移动
            for(int i = 0; i < 1000 - accel_array.getRowDimension(); i++)
            {
                for(int j = 0; j < 10; j++)
                {
                    Buffer.set(i, j, Buffer.get(i + accel_array.getRowDimension(), j));
                }
            }


        }
        
        
        // 首先寻找第一列的时间戳中值为last_time的位置
        int index = 0;
        for(int i = 0; i < accel_array.getRowDimension(); i++)
        {
            if(accel_array.get(i, 0) == last_time)
            {
                index = i;
                break;
            }
        }

        // 从index开始，将数据存储到Buffer中
        for(int i = index + 1; i < accel_array.getRowDimension(); i++)
        {   
            int b_index = last_index + i - index;
            Buffer.set(b_index, 0, accel_array.get(i, 0));
            Buffer.set(b_index, 1, accel_array.get(i, 1));
            Buffer.set(b_index, 2, accel_array.get(i, 2));
            Buffer.set(b_index, 3, accel_array.get(i, 3));
            Buffer.set(b_index, 4, gyro_array.get(i, 1));
            Buffer.set(b_index, 5, gyro_array.get(i, 2));
            Buffer.set(b_index, 6, gyro_array.get(i, 3));
            Buffer.set(b_index, 7, mag_array.get(i, 1));
            Buffer.set(b_index, 8, mag_array.get(i, 2));
            Buffer.set(b_index, 9, mag_array.get(i, 3));
        }

        // 更新last_index和last_time
        last_index = last_index + accel_array.getRowDimension() - 1 - index;
        last_time = accel_array.get(accel_array.getRowDimension() - 1, 0);
        
        process_flag = true;


        
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

    /********************************************************************************************
     * This method synchronizes the accelerometer, magnetometer, and gyroscope data.
     * Author : lzy
     * It uses linear interpolation to align the timestamps of the magnetometer and gyroscope data with the accelerometer data.
     * If a timestamp from the accelerometer data is outside the range of the magnetometer or gyroscope data, it performs linear extrapolation.
     * It also performs a window smoothing operation on the data.
     * The synchronized data is stored in a buffer.
     *********************************************************************************************/

    public void synchronize()
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
        double[] accel_timestamp = accel_array.getMatrix(0, accel_count - 1, 0, 0).getColumnPackedCopy();

        
        
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

        int i = 0;
        int windowSize = 3;
        // 对于每一个accel的时间戳，找到对应的mag和gyro的数据

        for(i = 0; i < accel_timestamp.length; i++)
        {
            double accel_x_value;
            double accel_y_value;
            double accel_z_value;

            double mag_x_value;
            double mag_y_value;
            double mag_z_value;

            double gyro_x_value;
            double gyro_y_value;
            double gyro_z_value;

            if (initial == false)
            {
                if(accel_timestamp[i] == last_time)
                {
                    last_index = i;
                }

            }

            try {
                mag_x_value = mag_x_interpolator.value(accel_timestamp[i]);
                mag_y_value = mag_y_interpolator.value(accel_timestamp[i]);
                mag_z_value = mag_z_interpolator.value(accel_timestamp[i]);
        
                gyro_x_value = gyro_x_interpolator.value(accel_timestamp[i]);
                gyro_y_value = gyro_y_interpolator.value(accel_timestamp[i]);
                gyro_z_value = gyro_z_interpolator.value(accel_timestamp[i]);
            } catch (OutOfRangeException e) {
                // 在定义域之外，进行线性外推
                double slope, intercept;
        
                // 对于mag_x
                slope = (mag_x[mag_x.length - 1] - mag_x[mag_x.length - 2]) / (mag_time[mag_time.length - 1] - mag_time[mag_time.length - 2]);
                intercept = mag_x[mag_x.length - 1] - slope * mag_time[mag_time.length - 1];
                mag_x_value = slope * accel_timestamp[i] + intercept;
        
                // 对于mag_y
                slope = (mag_y[mag_y.length - 1] - mag_y[mag_y.length - 2]) / (mag_time[mag_time.length - 1] - mag_time[mag_time.length - 2]);
                intercept = mag_y[mag_y.length - 1] - slope * mag_time[mag_time.length - 1];
                mag_y_value = slope * accel_timestamp[i] + intercept;
        
                // 对于mag_z
                slope = (mag_z[mag_z.length - 1] - mag_z[mag_z.length - 2]) / (mag_time[mag_time.length - 1] - mag_time[mag_time.length - 2]);
                intercept = mag_z[mag_z.length - 1] - slope * mag_time[mag_time.length - 1];
                mag_z_value = slope * accel_timestamp[i] + intercept;
        
                // 对于gyro_x
                slope = (gyro_x[gyro_x.length - 1] - gyro_x[gyro_x.length - 2]) / (gyro_time[gyro_time.length - 1] - gyro_time[gyro_time.length - 2]);
                intercept = gyro_x[gyro_x.length - 1] - slope * gyro_time[gyro_time.length - 1];
                gyro_x_value = slope * accel_timestamp[i] + intercept;
        
                // 对于gyro_y
                slope = (gyro_y[gyro_y.length - 1] - gyro_y[gyro_y.length - 2]) / (gyro_time[gyro_time.length - 1] - gyro_time[gyro_time.length - 2]);
                intercept = gyro_y[gyro_y.length - 1] - slope * gyro_time[gyro_time.length - 1];
                gyro_y_value = slope * accel_timestamp[i] + intercept;
        
                // 对于gyro_z
                slope = (gyro_z[gyro_z.length - 1] - gyro_z[gyro_z.length - 2]) / (gyro_time[gyro_time.length - 1] - gyro_time[gyro_time.length - 2]);
                intercept = gyro_z[gyro_z.length - 1] - slope * gyro_time[gyro_time.length - 1];
                gyro_z_value = slope * accel_timestamp[i] + intercept;
            }
            
           
            

            // 窗口平滑，窗口大小为3，取该点以及前面两个点的平均值
            if (i >= windowSize - 1)
            {
                double sum_accel_x = 0;
                double sum_accel_y = 0;
                double sum_accel_z = 0;
                double sum_gyro_x = 0;
                double sum_gyro_y = 0;
                double sum_gyro_z = 0;
                double sum_mag_x = 0;
                double sum_mag_y = 0;
                double sum_mag_z = 0;

                for  (int j = i - windowSize + 1; j <= i; j++)
                {
                    sum_accel_x += accel_array.get(j, 1);
                    sum_accel_y += accel_array.get(j, 2);
                    sum_accel_z += accel_array.get(j, 3);
                    sum_gyro_x += gyro_array.get(j, 1);
                    sum_gyro_y += gyro_array.get(j, 2);
                    sum_gyro_z += gyro_array.get(j, 3);
                    sum_mag_x += mag_array.get(j, 1);
                    sum_mag_y += mag_array.get(j, 2);
                    sum_mag_z += mag_array.get(j, 3);
                }

                accel_x_value = sum_accel_x / windowSize;
                accel_y_value = sum_accel_y / windowSize;
                accel_z_value = sum_accel_z / windowSize;

                gyro_x_value = sum_gyro_x / windowSize;
                gyro_y_value = sum_gyro_y / windowSize;
                gyro_z_value = sum_gyro_z / windowSize;

                mag_x_value = sum_mag_x / windowSize;
                mag_y_value = sum_mag_y / windowSize;
                mag_z_value = sum_mag_z / windowSize;

            
            }
            else{

                accel_x_value = accel_array.get(i, 1);
                accel_y_value = accel_array.get(i, 2);
                accel_z_value = accel_array.get(i, 3);
                
            }
            
            // 将这些数据存储到原来的数组中
            accel_array.set(i, 1, accel_x_value);
            accel_array.set(i, 2, accel_y_value);
            accel_array.set(i, 3, accel_z_value);
            
            
            gyro_array.set(i, 0, (accel_timestamp[i]));
            gyro_array.set(i, 1, gyro_x_value);
            gyro_array.set(i, 2, gyro_y_value);
            gyro_array.set(i, 3, gyro_z_value);

            mag_array.set(i, 0, (accel_timestamp[i]));
            mag_array.set(i, 1, mag_x_value);
            mag_array.set(i, 2, mag_y_value);
            mag_array.set(i, 3, mag_z_value);



            
            // 将数据存储到Buffer中
            Buffer.set(i, 0, accel_timestamp[i]);
            Buffer.set(i, 1, accel_x_value);
            Buffer.set(i, 2, accel_y_value);
            Buffer.set(i, 3, accel_z_value);
            Buffer.set(i, 4, gyro_x_value);
            Buffer.set(i, 5, gyro_y_value);
            Buffer.set(i, 6, gyro_z_value);
            Buffer.set(i, 7, mag_x_value);
            Buffer.set(i, 8, mag_y_value);
            Buffer.set(i, 9, mag_z_value);
            
            

            


        }

        if(initial)
        {
            last_index = i - 1;
            last_time = accel_timestamp[i - 1];
            initial = false;
        
        }
        
        last_time = accel_timestamp[i - 1];
        
        
        process_flag = true;
            
        printBuffer();
        Log.d("last_index", Integer.toString(last_index));
        Log.d("last_time", Double.toString(last_time));
        



        


        
    }

    // 设置一个函数，持续监控flag的值，当所有的flag都为true时，进行数据处理
    public void checkFlag()
    {
        if(accel_flag && mag_flag && gyro_flag)
        {
           
            try
            {
                synchronize();
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
    
    public void addAccelData(double timestamp, double x, double y, double z, int mode)
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

    public void addMagData(double timestamp, double x, double y, double z, int mode)
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

    public void addLocationData(double timestamp, double x, double y, double z) 
    {
        // 创建一个新的传感器数据对象
        SensorData sensorData = new SensorData(x, y, z, timestamp);
        // 将新的传感器数据对象添加到数组中
        Location_DataList.add(sensorData);
    }   

    public void addGyroData(double timestamp, double x, double y, double z, int mode)
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
