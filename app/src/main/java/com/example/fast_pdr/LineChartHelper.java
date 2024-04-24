package com.example.fast_pdr;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import android.graphics.Color;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.Description;

public class LineChartHelper {
    
    private LineChart chart;


    public LineChartHelper(LineChart chart) {
        this.chart = chart;
        initChart();
    }

    public void settitle(String title){
        Description description = new Description();
        description.setText(title);
        chart.setDescription(description);
    }

    

    private void initChart() {
        
        
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true); //是否可以缩放 x和y轴, 默认是true
        chart.setDrawGridBackground(false); //是否展示网格背景
        chart.setPinchZoom(true);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        chart.setData(data);

        chart.animateX(1500);
 
          //没有数据时显示的文字
        chart.setNoDataTextColor(Color.BLUE);               //没有数据时显示文字的颜色
        chart.setDrawGridBackground(false);                 //chart 绘图区后面的背景矩形将绘制
        
    }
    /**************************
     * 向图表中添加一个新的数据点。
     *
     * @param x X轴上的数据值
     * @param y Y轴上的数据值
     * @param z Z轴上的数据值
     **************************/

    public void addEntry(double x, double y, double z) {
        LineData data = chart.getData();

        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        ILineDataSet setX = data.getDataSetByIndex(0);
        ILineDataSet setY = data.getDataSetByIndex(1);
        ILineDataSet setZ = data.getDataSetByIndex(2);

        if (setX == null) {
            setX = createSet("X", Color.RED);
            data.addDataSet(setX);
        }

        if (setY == null) {
            setY = createSet("Y", Color.GREEN);
            data.addDataSet(setY);
        }

        if (setZ == null) {
            setZ = createSet("Z", Color.BLUE);
            data.addDataSet(setZ);
        }
        float float_x = (float) (x);
        float float_y = (float) (y);
        float float_z = (float) (z);
        
        data.addEntry(new Entry(setX.getEntryCount(), float_x), 0);
        data.addEntry(new Entry(setY.getEntryCount(), float_y), 1);
        data.addEntry(new Entry(setZ.getEntryCount(), float_z), 2);

        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(120);
        chart.moveViewToX(data.getEntryCount());
    





    
    }

    // X 为北向坐标，Y 为东向坐标
    public void drawCoords(double x, double y)
    {   LineData data = chart.getData();

        if (data == null) {
            // No data, create it
            data = new LineData();
            chart.setData(data);
        }
    
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
    
        if (set == null) {
            // No set, create it
            set = createSet("Label", Color.RED);
            data.addDataSet(set);
        }
    
        // Add a new entry (point) to the set
        data.addEntry(new Entry((float) y, (float) x), 0); // Note: y is x-coordinate, x is y-coordinate
    
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.postInvalidate();// refresh

//        // Move view to the last added point
//        chart.moveViewTo(data.getEntryCount(), (float) x, YAxis.AxisDependency.LEFT);
        

    }
        

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(color);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    public void clearChart() {
        chart.clear();
    }

}
