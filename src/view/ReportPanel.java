package view;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import model.AdminDAO;

// 引入 JFreeChart
import org.jfree.chart.ChartFactory; 
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation; 
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class ReportPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public ReportPanel() {
        setLayout(new GridLayout(1, 2, 10, 10));
        setBackground(Color.WHITE);
        refreshChart(); // 初始化圖表
    }

    /**
     * 重新整理並繪製圖表的方法
     */
    public void refreshChart() {
        this.removeAll(); // 清空舊圖表

        AdminDAO adminDAO = new AdminDAO();
        Map<String, Integer> stats = adminDAO.getCategoryBorrowStats();

        if (stats.isEmpty()) {
            add(new JLabel("📊 目前尚無借閱紀錄，請先去借書後再來查看統計資料。", SwingConstants.CENTER));
        } else {
            // 準備資料
            DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                pieDataset.setValue(entry.getKey(), entry.getValue());
                barDataset.addValue(entry.getValue(), "借閱次數", entry.getKey());
            }

            // ==========================================
            // 🎨 製作圓餅圖
            // ==========================================
            JFreeChart pieChart = ChartFactory.createPieChart("各主題借閱佔比", pieDataset, true, true, false);
            styleChart(pieChart);
            PiePlot plot = (PiePlot) pieChart.getPlot();
            
            // ✨ 解決圓餅圖亂碼與美化背景
            plot.setLabelFont(new Font("Microsoft JhengHei", Font.BOLD, 14)); // 給圓餅圖旁邊的線條標籤設定中文字體
            plot.setBackgroundPaint(Color.WHITE); // 移除預設的灰色背景，改為純白
            plot.setOutlineVisible(false); // 隱藏圖表外框線，看起來更現代
            
            // 莫蘭迪色調
            if(!stats.isEmpty()){
                plot.setSectionPaint(stats.keySet().iterator().next(), new Color(100, 149, 237)); 
            }

            // ==========================================
            // 📊 製作柱狀圖
            // ==========================================
            JFreeChart barChart = ChartFactory.createBarChart("熱門借閱主題排行榜", "書籍主題", "借閱次數", barDataset, PlotOrientation.VERTICAL, false, true, false);
            styleChart(barChart);
            
            CategoryPlot barPlot = barChart.getCategoryPlot();
            
            // ✨ 解決柱狀圖 X 軸與 Y 軸的框框亂碼
            barPlot.getDomainAxis().setLabelFont(new Font("Microsoft JhengHei", Font.BOLD, 16)); // X軸 標題 (書籍主題)
            barPlot.getDomainAxis().setTickLabelFont(new Font("Microsoft JhengHei", Font.PLAIN, 14)); // X軸 項目名稱
            barPlot.getRangeAxis().setLabelFont(new Font("Microsoft JhengHei", Font.BOLD, 16)); // Y軸 標題 (借閱次數)
            barPlot.getRangeAxis().setTickLabelFont(new Font("Microsoft JhengHei", Font.PLAIN, 14)); // Y軸 數字
            
            BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
            renderer.setSeriesPaint(0, new Color(70, 130, 180)); // 莫蘭迪深藍
            renderer.setMaximumBarWidth(0.1); 
            renderer.setShadowVisible(false); // 關閉陰影
            
            barPlot.setBackgroundPaint(new Color(245, 245, 245));
            barPlot.setRangeGridlinePaint(Color.WHITE);

            add(new ChartPanel(pieChart));
            add(new ChartPanel(barChart));
        }
        this.revalidate();
        this.repaint();
    }

    private void styleChart(JFreeChart chart) {
        chart.getTitle().setFont(new Font("Microsoft JhengHei", Font.BOLD, 20));
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Microsoft JhengHei", Font.PLAIN, 14));
        }
        chart.setBackgroundPaint(Color.WHITE);
    }
}