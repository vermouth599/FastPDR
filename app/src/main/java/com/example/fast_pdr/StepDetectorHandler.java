package com.example.fast_pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import androidx.annotation.Nullable;
import com.example.fast_pdr.StepInfo;

import java.util.ArrayList;

public class StepDetectorHandler {
    
    //存放三轴数据
    private float[] oriValues = new float[3];
    private final int valueNum = 4;
    //用于存放计算阈值的波峰波谷差值
    private float[] tempValue = new float[valueNum];
    private int tempCount = 0;
    //是否上升的标志位
    private boolean isDirectionUp = false;
    //持续上升次数
    private int continueUpCount = 0;
    //上一点的持续上升的次数，为了记录波峰的上升次数
    private int continueUpFormerCount = 0;
    //上一点的状态，上升还是下降
    private boolean lastStatus = false;
    //波峰值
    private float peakOfWave = 0;
    //波谷值
    private float valleyOfWave = 0;
    //此次波峰的时间
    private double timeOfThisPeak = 0.0;
    //上次波峰的时间
    private double timeOfLastPeak = 0.0;
    //当前的时间
    private double timeOfNow = 0;
    //当前传感器的值
    private float gravityNew = 0;
    //上次传感器的值
    private float gravityOld = 0;
    //动态阈值需要动态的数据，这个值用于这些动态数据的阈值
    private final float initialValue = (float) 1.3;
    //初始阈值
    private float ThreadValue = (float) 1.0;

    private float interval = (float) 500;
    //private StepDetectorListener mStepListeners;

    public ArrayList stepList = new ArrayList();
    public ArrayList lengthList = new ArrayList();

    public double height = 1.85;

    private StepInfo mStepInfo;
    
    private MainActivity mainActivity;

    public void SensorChangeListener(StepInfo calorieInfo) {
        this.mStepInfo = calorieInfo;
    }
    
    public StepDetectorHandler(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

//    public void setStepListener(@Nullable StepDetectorListener stepListener) {
//        this.mStepListeners = stepListener;
//    }

     /*
     * 注册了G-Sensor后会一直调用这个函数
	 * 对三轴数据进行平方和开根号的处理
	 * 调用DetectorNewStep检测步子
	 * */
    
    public void from_accel(double elapsed_time, double x, double y, double z) {
        
        oriValues[0] = (float) x;
        oriValues[1] = (float) y;
        oriValues[2] = (float) z;

        
        gravityNew = (float) Math.sqrt(oriValues[0] * oriValues[0]
                + oriValues[1] * oriValues[1] + oriValues[2] * oriValues[2]);
        DetectorNewStep(gravityNew,elapsed_time);
    }


    // 从外部设置参数
    public void setThreadValue(float threadValue) {
        this.ThreadValue = threadValue;
    }

    public void setInterval(float interval) {
        this.interval = interval;
    }

 


     /*
     * 检测步子，并开始计步
     * 1.传入sersor中的数据
     * 2.如果检测到了波峰，并且符合时间差以及阈值的条件，则判定为1步
     * 3.符合时间差条件，波峰波谷差值大于initialValue，则将该差值纳入阈值的计算中
     * */
    public void DetectorNewStep(float values, double elapsedtime) {
        if (gravityOld == 0) {
            gravityOld = values;
        } else {
            if (DetectorPeak(values, gravityOld)) {
                timeOfLastPeak = timeOfThisPeak;
                timeOfNow = elapsedtime;
                if (timeOfNow - timeOfLastPeak >= interval
                        && (peakOfWave - valleyOfWave >= ThreadValue)) {
                    timeOfThisPeak = timeOfNow;
                    if (DetectorEffectCalculate()) {
                        // 记录数据
                        int frequency = mStepInfo.getFrequency();
                        mStepInfo.setFrequency(++frequency);
                       
                    }
                }

            }
        }
        gravityOld = values;
    }


    /*
     * 更新界面的处理，不涉及到算法
     * 一般在通知更新界面之前，增加下面处理，为了处理无效运动：
     * 1.连续记录10才开始计步
     * 2.例如记录的9步用户停住超过3秒，则前面的记录失效，下次从头开始
     * 3.连续记录了9步用户还在运动，之前的数据才有效
     * */
    private boolean DetectorEffectCalculate() {
        boolean isEffect = false;
        if (timeOfThisPeak - timeOfLastPeak < 2000) {
            stepList.add(timeOfThisPeak);
            calculateStepLengths();
            mainActivity.updateTextView("用户在 " + timeOfThisPeak + "走了一步");
            isEffect = stepList.size() >= 3;
        } else {
            stepList.clear();
        }
        return isEffect;
    }


    /*
     * 检测波峰
     * 以下四个条件判断为波峰：
     * 1.目前点为下降的趋势：isDirectionUp为false
     * 2.之前的点为上升的趋势：lastStatus为true
     * 3.到波峰为止，持续上升大于等于2次
     * 4.波峰值大于20
     * 记录波谷值
     * 1.观察波形图，可以发现在出现步子的地方，波谷的下一个就是波峰，有比较明显的特征以及差值
     * 2.所以要记录每次的波谷值，为了和下次的波峰做对比
     * */
    public boolean DetectorPeak(float newValue, float oldValue) {
        lastStatus = isDirectionUp;
        if (newValue >= oldValue) {
            isDirectionUp = true;
            continueUpCount++;
        } else {
            continueUpFormerCount = continueUpCount;
            continueUpCount = 0;
            isDirectionUp = false;
        }

        if (!isDirectionUp && lastStatus
                && (continueUpFormerCount >= 2 || oldValue >= 10

        )) {
            peakOfWave = oldValue;
            return true;
        } else if (!lastStatus && isDirectionUp) {
            valleyOfWave = oldValue;
            return false;
        } else {
            return false;
        }
    }

    public void calculateStepLengths()
    {
        // 获取steplist的size

        int size = stepList.size();
        if (size == 1)
        {
            lengthList.add(0.4);
        }
        else if (size == 2)
        {
            lengthList.add(0.75);
        }
        else
        {
            double ll_time = (Double) stepList.get(size - 3);
            double last_time = (Double) stepList.get(size - 2);
            double now_time = (Double) stepList.get(size - 1);
            double frequency = 1.0 / (0.8 * (now_time - last_time) 
            + 0.2 * (last_time - ll_time));

            double steplength = 0.7 + 0.371 * (height - 1.6) + 0.227 * (frequency - 1.79) * height / 1.6;

            lengthList.add(steplength);


        }
        

    }




}
