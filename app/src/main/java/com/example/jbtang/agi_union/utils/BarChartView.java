package com.example.jbtang.agi_union.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jbtang on 12/12/2015.
 */
public class BarChartView {

    private static int margins[] = new int[]{30, 70, 40, 70};
    private static String[] titles = new String[]{"能量"};
    private List<int[]> values = new ArrayList<>();
    private static int[] colors = new int[]{Color.RED};
    private XYMultipleSeriesRenderer renderer;
    private Context mContext;
    private String mTitle;
    private List<String> option;

    public BarChartView(Context context) {
        this.mContext = context;
        this.renderer = new XYMultipleSeriesRenderer();
    }

    public void initData(int[] puschList, List<String> option, String title) {
        values.add(puschList);
        mTitle = title;
        this.option = option;
    }

    public View getBarChartView() {
        buildBarRenderer();
        setChartSettings(renderer, mTitle, "", "", 0, 6, 0, 100, Color.BLACK, Color.BLACK);
        renderer.getSeriesRendererAt(0).setDisplayBoundingPoints(true);
        //renderer.getSeriesRendererAt(1).setDisplayBoundingPoints(true);
        int size = option.size();
        for (int i = 0; i < size; i++) {
            renderer.addXTextLabel(i, option.get(i));
        }
        renderer.setMargins(margins);
        renderer.setMarginsColor(0x00ffffff);
        renderer.setPanEnabled(false, false);
        renderer.setZoomEnabled(true, true);// 设置x，y方向都不可以放大或缩�?
        renderer.setZoomRate(1.0f);
        renderer.setInScroll(false);
        renderer.setBackgroundColor(0x00ffffff);
        renderer.setApplyBackgroundColor(false);
        renderer.setShowGrid(true);
        renderer.setYLabelsPadding(30);
        renderer.setYLabelsColor(0, Color.BLACK);
        renderer.setXLabelsColor(Color.BLACK);
        View view = ChartFactory.getBarChartView(mContext, buildBarDataset(titles, values), renderer, BarChart.Type.DEFAULT); // Type.STACKED
        view.setBackgroundColor(0x00ffffff);
        return view;
    }

    private XYMultipleSeriesDataset buildBarDataset(String[] titles, List<int[]> values) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        for (int i = 0; i < titles.length; i++) {
            CategorySeries series = new CategorySeries(titles[i]);
            int[] v = values.get(i);
            int seriesLength = v.length;
            for (int k = 0; k < seriesLength; k++) {
                series.add(v[k]);
            }
            dataset.addSeries(series.toXYSeries());
        }
        return dataset;
    }

    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle, String yTitle,
                                    double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
        renderer.setChartTitle(title);//设置柱图名称
        renderer.setXTitle(xTitle);//设置X轴名称
        renderer.setYTitle(yTitle);//设置Y轴名称
        renderer.setXAxisMin(xMin);
        renderer.setXAxisMax(xMax);//设置X轴的最大值
        renderer.setYAxisMin(yMin);
        renderer.setYAxisMax(yMax);//设置Y轴的最大值
        renderer.setAxesColor(axesColor);
        renderer.setLabelsColor(labelsColor);
        renderer.setXLabels(0);//设置X轴显示的刻度标签的个数
        renderer.setYLabels(0);
        renderer.setLabelsTextSize(20);// 设置轴标签文本大小
        renderer.setYLabelsAlign(Paint.Align.RIGHT);
        renderer.setXLabelsAlign(Paint.Align.CENTER);
        // renderer.setXLabelsColor(0xff000000);//设置X轴上的字体颜�?
        // renderer.setYLabelsColor(0,0xff000000);//设置Y轴上的字体颜�?

    }

    /*
     * 初始化柱子风�?
     */
    protected void buildBarRenderer() {
        if (null == renderer) {
            return;
        }
        renderer.setBarWidth(30);//柱宽
        renderer.setBarSpacing(20);
        renderer.setAxisTitleTextSize(16);// 设置坐标轴标题文本大小
        renderer.setChartTitleTextSize(30);// 设置图表标题文本大小
        renderer.setLabelsTextSize(15);// 设置轴标签文本大小
        renderer.setLegendTextSize(25); // 设置图例文本大小
        for (int i = 0; i < colors.length; i++) {
            XYSeriesRenderer xyr = new XYSeriesRenderer();
            xyr.setChartValuesTextAlign(Paint.Align.RIGHT);
            xyr.setChartValuesTextSize(20);//柱上文本大小
            xyr.setDisplayChartValues(true);
            xyr.setColor(colors[i]);
            renderer.addSeriesRenderer(xyr);
        }
    }
}
