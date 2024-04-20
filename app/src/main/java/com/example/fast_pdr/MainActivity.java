package com.example.fast_pdr;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.TextView;



import com.example.fast_pdr.FileIO;
import com.example.fast_pdr.LineChartHelper;
import com.example.fast_pdr.SensorData;
import com.example.fast_pdr.PDR;
import com.example.fast_pdr.Datalist;
import com.example.fast_pdr.StepInfo;
import com.example.fast_pdr.StepDetectorHandler;
import com.example.fast_pdr.StepDetectorListener;
import java.util.ArrayList;
import com.example.fast_pdr.CanvasView;

//import javax.xml.crypto.Data;

import android.widget.Button;
import android.view.View;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.github.mikephil.charting.charts.LineChart;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class MainActivity  extends AppCompatActivity {

    private CanvasView customCanvas;
    private SensorManager sensorManager;
    
    
    private SensorEventListener accelerometerEventListener;
    private Sensor accelerometerSensor;
    
    private SensorEventListener gyroscopeEventListener;
    private Sensor gyroscopeSensor;

    private SensorEventListener magneticFieldEventListener;
    private Sensor magneticFieldSensor;

    private StepDetectorHandler stepDetectorSensor;

    private LocationManager locationManager;
    private LocationListener locationListener;

    private TextView accelerationTextView;
    private TextView gyroTextView;
    private TextView magneticFieldTextView;

    private TextView LocationTextView;

    private TextView StepTextView;

    // 创建FileIO对象
    private FileIO fileIO;

    // 创建PDR对象
    private PDR pdr;

    private Handler handler = new Handler();
    
    // 创建一个单线程的ExecutorService
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // todo: 将所有耗时的操作放到后台线程中执行

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 在后台线程中执行耗时的操作
                        pdrdata.synchronize();
                    } catch (Exception e) {
                        pdrdata.printdata();
                        e.printStackTrace();
                    }
                }
            });

            handler.postDelayed(this, 400);
        }
    };
    


    private long startTime; // 记录开始时间
    int mode;

    // 创建传感器数据缓冲区
    Datalist pdrdata = new Datalist();
    



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // 调用父类的方法
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户接受了请求，你的应用程序现在可以写入外部存储
            } else {
                // 用户拒绝了请求，你的应用程序不能写入外部存储
            }
        }
    }

    public void updateTextView(String text) {

        StepTextView.setText(text);
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accelerationTextView = findViewById(R.id.accelerationTextView);
        gyroTextView = findViewById((R.id.gyroTextView));
        magneticFieldTextView = findViewById(R.id.magneticTextView);
        LocationTextView = findViewById(R.id.LocationTextView);
        StepTextView = findViewById(R.id.StepTextView);
        Button startButton = findViewById(R.id.startButton); // 获取开始按钮的引用
        Button stopButton = findViewById(R.id.stopButton); // 获取停止按钮的引用
        Button initializeButton = findViewById(R.id.initializeButton);

        LineChart Accel_chart = (LineChart) findViewById(R.id.accel_chart);
        LineChartHelper charthelper = new LineChartHelper(Accel_chart);
        charthelper.settitle("加速度传感器数据");

        LineChart Gyro_chart = (LineChart) findViewById(R.id.gyro_chart);
        LineChartHelper charthelper2 = new LineChartHelper(Gyro_chart);
        charthelper2.settitle("陀螺仪传感器数据");

        LineChart Mag_chart = (LineChart) findViewById(R.id.mag_chart);
        LineChartHelper charthelper3 = new LineChartHelper(Mag_chart);
        charthelper3.settitle("磁场传感器数据");

        customCanvas = (CanvasView) findViewById(R.id.Canvas);
        customCanvas.drawPoint(5, 5);

        // 检查外部存储是否可用
        // 检查是否已经有了写入外部存储的权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有，那么请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        // 初始化FileIO对象
        fileIO = new FileIO(this);
        fileIO.setDirTime();

        pdr = new PDR(customCanvas);
        // 获取初始位置




        
        // 获取传感器管理器
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // 获取加速度传感器
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // 获取陀螺仪传感器
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // 获取磁场传感器
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // 获取计步传感器
        stepDetectorSensor = new StepDetectorHandler(this);
        //stepDetectorSensor.setStepListener(this);
        stepDetectorSensor.SensorChangeListener(new StepInfo());

        String fileName = "data.txt";

        // 创建传感器事件监听器
        accelerometerEventListener   = new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // 处理传感器数据变化事件
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    double accelx = event.values[0]; // x轴方向上的加速度
                    double accely = event.values[1]; // y轴方向上的加速度
                    double accelz = event.values[2]; // z轴方向上的加速度

                    if (startTime == 0) {
                        // 记录第一个传感器事件的时间戳
                        startTime = event.timestamp;
                    }
                    
                    
                    // 获取传感器回调的时间戳
                    long timestamp = event.timestamp;
                    
                    //long timestamp = System.currentTimeMillis();

                    long elapsedTime = timestamp - startTime;

                    // 转换为毫秒，类型转换为double

                    double elapsedTime_ = elapsedTime / 1000000.0;

                    stepDetectorSensor.from_accel(elapsedTime_,accelx,accely,accelz);

                    // 更新UI界面，显示加速度数据
                    String accelerationText = "2 "+ elapsedTime_ + " " + accelx + " " + accely + " " + accelz;
                    accelerationTextView.setText(accelerationText);

                    // 存储传感器数据到缓冲区
                    pdrdata.addAccelData(elapsedTime_,accelx, accely, accelz, mode);
                    

                    
                    charthelper.addEntry(accelx, accely, accelz);
                    
                    
                    // 将传感器数据写入文件
                    String content = accelerationText + "\n";
                    fileIO.writeToFile(fileName, content);



                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // 传感器精度变化时的回调方法
            }

        };

        gyroscopeEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    double gyroX = event.values[0]; // x轴方向上的角速度
                    double gyroY = event.values[1]; // y轴方向上的角速度
                    double gyroZ = event.values[2]; // z轴方向上的角速度

                    if (startTime == 0) {
                        // 记录第一个传感器事件的时间戳
                        startTime = event.timestamp;
                    }
                    
                    long timestamp = event.timestamp;
                    
                    // long timestamp = System.currentTimeMillis();

                    long elapsedTime = timestamp - startTime; 

                    double elapsedTime_ = elapsedTime / 1000000.0;
                    // 将陀螺仪传感器数据合并为一个字符串
                    String gyroscopeData = "1 "+ elapsedTime_ + " " + gyroX + " " + gyroY + " " + gyroZ;
                    gyroTextView.setText(gyroscopeData);
                    
                    // 存储传感器数据到缓冲区
                    pdrdata.addGyroData(elapsedTime_, gyroX, gyroY, gyroZ, mode);

                    charthelper2.addEntry(gyroX, gyroY, gyroZ);

                    String content = gyroscopeData + "\n";
                    fileIO.writeToFile(fileName, content);



                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        // 创建磁场传感器事件监听器
        magneticFieldEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    double magneticX = event.values[0]; // x轴方向上的磁场强度
                    double magneticY = event.values[1]; // y轴方向上的磁场强度
                    double magneticZ = event.values[2]; // z轴方向上的磁场强度

                    if (startTime == 0) {
                        // 记录第一个传感器事件的时间戳
                        startTime = event.timestamp;
                    }
                    
                    
                    long timestamp = event.timestamp;
                    // long timestamp = System.currentTimeMillis();

                    long elapsedTime = timestamp - startTime; //单位：毫秒

                    double elapsedTime_ = elapsedTime / 1000000.0;
                    // 将磁场传感器数据合并为一个字符串
                    String magneticFieldData = "3 "+ elapsedTime_ + " " + magneticX + " " + magneticY + " " + magneticZ;
                    magneticFieldTextView.setText(magneticFieldData);
                    
                    // 存储传感器数据到缓冲区
                    pdrdata.addMagData(elapsedTime_, magneticX, magneticY, magneticZ, mode);

                    charthelper3.addEntry(magneticX, magneticY, magneticZ);

                    String content = magneticFieldData + "\n";
                    fileIO.writeToFile(fileName, content);

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        // 首先获取GPS位置
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
           @Override
           public void onLocationChanged(Location location) {
                
               
            
               double timestamp = 0.0;

               

               
               // 获取位置信息
               double latitude = location.getLatitude();
               double longitude = location.getLongitude();
               double altitude = location.getAltitude();

               pdrdata.addLocationData(timestamp, latitude, longitude, altitude);

               
                
           }

           // 提供一个返回位置信息的接口
           
           
        };

        
        // 在按钮的点击事件中注册传感器监听器
        startButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                startTime = 0;
                mode = 2;
                sensorManager.registerListener(accelerometerEventListener, accelerometerSensor,SensorManager.SENSOR_DELAY_GAME);
                sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
                sensorManager.registerListener(magneticFieldEventListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
                handler.postDelayed(runnable, 400);
            }
        });

        // 在停止按钮的点击事件中注销传感器监听器
        stopButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                sensorManager.unregisterListener(accelerometerEventListener);
                sensorManager.unregisterListener(gyroscopeEventListener);
                sensorManager.unregisterListener(magneticFieldEventListener);
                // 停止Handler
                handler.removeCallbacks(runnable);
                pdrdata.printBuffer();
                // pdrdata.printdata();
                // sensorManager.unregisterListener(stepDetectorListener);
            }
        });

        initializeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                // 创建一个AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("请保持静止五秒")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // 用户点击OK按钮后，继续进行后续的逻辑操作
                                startTime = 0;
                                mode = 1;

                                // 首先注册GPS位置监听器
                                // 检查是否有访问位置信息的权限
                                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // 如果没有权限，请求用户授予权限
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                                } else {
                                    // 如果有权限，请求位置更新
                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                                }
                                sensorManager.registerListener(accelerometerEventListener, accelerometerSensor,SensorManager.SENSOR_DELAY_GAME);
                                sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
                                sensorManager.registerListener(magneticFieldEventListener, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
                                
                            
                                // 创建一个Handler来延迟执行代码
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 5秒后，注销监听器
                                        sensorManager.unregisterListener(accelerometerEventListener);
                                        sensorManager.unregisterListener(gyroscopeEventListener);
                                        sensorManager.unregisterListener(magneticFieldEventListener);
                                        locationManager.removeUpdates(locationListener);
                                        pdr.Initialize(pdrdata.accel_DataList, pdrdata.mag_DataList, pdrdata.Location_DataList);
                                        String locationText = "r: " + pdr.initial_r + " theta: " + pdr.initial_theta + "\n 真北角: " + pdr.Phi_m + "\n B: " + pdr.initial_B + " L: " + pdr.initial_L + " H: " + pdr.initial_H;
                                        LocationTextView.setText(locationText);
                                        // 清除内存
                                        pdrdata.accel_DataList.clear();
                                        pdrdata.gyro_DataList.clear();
                                        pdrdata.mag_DataList.clear();
                                        pdrdata.Location_DataList.clear();
                                        
                                    }
                                }, 5000);  // 延迟5秒执行


                                
                            

                            }
                        });
                // 创建AlertDialog对象并显示
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });
        
    
    
    }



}









