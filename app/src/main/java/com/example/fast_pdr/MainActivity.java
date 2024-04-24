package com.example.fast_pdr;


import androidx.annotation.NonNull;
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
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;


import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.MapsInitializer;
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
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;





public class MainActivity  extends AppCompatActivity {

    private MapManager mapManager;
    private CanvasView customCanvas;
    private SensorManager sensorManager;

    MapsInitializer mapini;

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

    private EditText threadValueEditText;

    private EditText intervalEditText;

    private EditText omegaEditText;

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
                        // 注意，需要使用PostInvalidate
                        pdrdata.synchronize();
                        pdr.from_main(stepDetectorSensor.stepList,stepDetectorSensor.lengthList,pdrdata.last_index,pdrdata.last_time,pdrdata.Buffer,pdrdata.buff_count);

                        
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
        Button clearButton = findViewById(R.id.clearButton);
        Button TDRButton = findViewById(R.id.TDRbutton);

        LineChart Accel_chart = (LineChart) findViewById(R.id.accel_chart);
        LineChartHelper charthelper = new LineChartHelper(Accel_chart);
        charthelper.settitle("加速度传感器数据");

        LineChart Gyro_chart = (LineChart) findViewById(R.id.gyro_chart);
        LineChartHelper charthelper2 = new LineChartHelper(Gyro_chart);
        charthelper2.settitle("陀螺仪传感器数据");

        LineChart Mag_chart = (LineChart) findViewById(R.id.mag_chart);
        LineChartHelper charthelper3 = new LineChartHelper(Mag_chart);
        charthelper3.settitle("磁场传感器数据");

        CanvasView myCanvasView = (CanvasView) findViewById(R.id.canvas_view);
        




        // 检查外部存储是否可用
        // 检查是否已经有了写入外部存储的权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 如果没有，那么请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        // 初始化FileIO对象
        fileIO = new FileIO(this);
        fileIO.setDirTime();



        mapini.updatePrivacyShow(this,true,true);
        mapini.updatePrivacyAgree(this,true);

        //获取地图控件引用
        mapManager = new MapManager(this, R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapManager.initializeMap(savedInstanceState);

        pdr = new PDR(myCanvasView,mapManager);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.radioButton1) {
                    pdr.inside = 1;
                } else if(checkedId == R.id.radioButton2) {
                    pdr.inside = 0;
                } 
            }
        });
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        
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


        threadValueEditText = (EditText)findViewById(R.id.stepDetectorThreadValue);
        intervalEditText = (EditText)findViewById(R.id.stepDetectorInterval);
        omegaEditText = (EditText)findViewById(R.id.omega_);

        threadValueEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start are about to be replaced by new text with length after.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start have just replaced old text that had length before.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This method is called to notify you that, somewhere within s, the text has
                // been changed.
                if (!s.toString().isEmpty()) {
                    float threadValue = Float.parseFloat(s.toString());
                    stepDetectorSensor.setThreadValue(threadValue);
                }
            }
        });

        intervalEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    float interval = Float.parseFloat(s.toString());
                    stepDetectorSensor.setInterval(interval);
                }
            }
        });

        omegaEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().isEmpty()) {
                    float omega = Float.parseFloat(s.toString());
                    pdr.omega = (double) omega;
                }
            }
        });

        

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

               

               
               if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                double altitude = location.hasAltitude() ? location.getAltitude() : Double.NaN;
            
                // Check if the values are valid
                if (latitude != 0.0 && longitude != 0.0 && !Double.isNaN(altitude)) {
                    pdrdata.addLocationData(timestamp, latitude, longitude, altitude);
                }
                }

               // 检查三个值是否为空


              

               
                
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
                // 把位置信息写入文件
                for (int i = 0; i < pdr.X_list.size(); i++) {
                    String content = " " +  pdr.X_list.get(i) + " " + pdr.Y_list.get(i) + " " +  "\n";
                    fileIO.writeToFile("location.txt", content);
                }
                // 把步长信息写入文件
                for (int i = 0; i < stepDetectorSensor.lengthList.size(); i++) {
                    String content = " " +  stepDetectorSensor.lengthList.get(i) + "\n";
                    fileIO.writeToFile("step_length.txt", content);
                }
                // 把yaw角信息写入文件
                for (int i = 0; i < pdr.yaw_list.size(); i++) {
                    String content = " " +  pdr.yaw_list.get(i) + "\n";
                    fileIO.writeToFile("yaw.txt", content);
                }  
                // 把步的时间信息写入文件
                for (int i = 0; i < stepDetectorSensor.stepList.size(); i++) {
                    String content = " " +  stepDetectorSensor.stepList.get(i) + "\n";
                    fileIO.writeToFile("step_time.txt", content);
                }
                // 把buff的时间信息写入文件
                for (int i = 0; i < pdr.time_List.size(); i++)
                {
                    String content = " " +  pdr.time_List.get(i) + "\n";
                    fileIO.writeToFile("buff_time.txt", content);
                }
                // 清理图
//                charthelper_XY.clearChart();
//                charthelper.clearChart();
//                charthelper2.clearChart();
//                charthelper3.clearChart();
                // pdrdata.printdata();
                // sensorManager.unregisterListener(stepDetectorListener);
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View v) {
                pdrdata.clear();
                pdr.clear();
                charthelper.clearChart();
                charthelper2.clearChart();
                charthelper3.clearChart();
                myCanvasView.clearCanvas();
                mapManager.clearMap();

            }
        });

        TDRButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                double TDR = pdr.TDR(stepDetectorSensor.lengthList);
                // 输出百分比
                String TDRText = "TDR: " + TDR/100 + "%";
                LocationTextView.setText(TDRText);
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

                                // 更新进度条，5秒后注销监听器，进度条到100%
                                progressBar.setProgress(0);
                                progressBar.setMax(5000);
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (int i = 0; i < 5000; i++) {
                                            try {
                                                Thread.sleep(1);
                                                progressBar.setProgress(i);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }).start();



                                // 首先注册GPS位置监听器
                                // 检查是否有访问位置信息的权限
                                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    // 如果没有权限，请求用户授予权限
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                                } else {
                                    // 如果有权限，请求位置更新
                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                                }
                                sensorManager.registerListener(accelerometerEventListener, accelerometerSensor,20000);
                                sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, 20000);
                                sensorManager.registerListener(magneticFieldEventListener, magneticFieldSensor, 20000);
                                
                            
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









