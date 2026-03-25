import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * D{0-1}背包问题求解器
 * 功能：数据读取、性价比排序、动态规划求解、TXT导出、散点图绘制、GUI界面
 * 修复所有编译错误 + 无libpng警告
 */
public class Main extends JFrame {

    // ==================== 数据模型 ====================
    static class ItemSet {
        private final int w1, v1, w2, v2, w3, v3;
        private final double ratio;

        public ItemSet(int w1, int v1, int w2, int v2, int w3, int v3) {
            this.w1 = w1; this.v1 = v1;
            this.w2 = w2; this.v2 = v2;
            this.w3 = w3; this.v3 = v3;
            this.ratio = (w3 == 0) ? 0 : (double) v3 / w3;
        }

        public int getW1() { return w1; } public int getV1() { return v1; }
        public int getW2() { return w2; } public int getV2() { return v2; }
        public int getW3() { return w3; } public int getV3() { return v3; }
        public double getRatio() { return ratio; }
    }

    static class SolutionResult {
        private final int maxValue;
        private final long solveTimeMs;
        public SolutionResult(int maxValue, long solveTimeMs) {
            this.maxValue = maxValue;
            this.solveTimeMs = solveTimeMs;
        }
        public int getMaxValue() { return maxValue; }
        public long getSolveTimeMs() { return solveTimeMs; }
    }

    // ==================== 数据读取 ====================
    static class DataReader {
        static class DataBundle {
            final List<ItemSet> itemSets;
            final int capacity;
            public DataBundle(List<ItemSet> itemSets, int capacity) {
                this.itemSets = itemSets;
                this.capacity = capacity;
            }
        }

        public static DataBundle readFromFile(String filePath) throws IOException {
            List<ItemSet> itemSets = new ArrayList<>();
            int capacity = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                int lineNum = 0;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split("\\s+");
                    if (lineNum == 0) {
                        capacity = Integer.parseInt(parts[1]);
                    } else {
                        int w1 = Integer.parseInt(parts[0]); int v1 = Integer.parseInt(parts[1]);
                        int w2 = Integer.parseInt(parts[2]); int v2 = Integer.parseInt(parts[3]);
                        int w3 = Integer.parseInt(parts[4]); int v3 = Integer.parseInt(parts[5]);
                        itemSets.add(new ItemSet(w1, v1, w2, v2, w3, v3));
                    }
                    lineNum++;
                }
            }
            return new DataBundle(itemSets, capacity);
        }
    }

    // ==================== 数据排序 ====================
    static class DataSorter {
        public static void sortByRatioDesc(List<ItemSet> items) {
            if (items == null) return;
            items.sort(Comparator.comparingDouble(ItemSet::getRatio).reversed());
        }
    }

    // ==================== 动态规划算法 ====================
    static class DynamicProgrammingSolver {
        public static SolutionResult solveWithTimer(List<ItemSet> items, int capacity) {
            long startTime = System.currentTimeMillis();
            int maxVal = solve(items, capacity);
            return new SolutionResult(maxVal, System.currentTimeMillis() - startTime);
        }
        private static int solve(List<ItemSet> items, int capacity) {
            int n = items.size();
            int[][] dp = new int[n + 1][capacity + 1];
            for (int i = 1; i <= n; i++) {
                ItemSet item = items.get(i - 1);
                for (int w = 0; w <= capacity; w++) {
                    dp[i][w] = dp[i - 1][w];
                    if (w >= item.getW1()) dp[i][w] = Math.max(dp[i][w], dp[i-1][w-item.getW1()] + item.getV1());
                    if (w >= item.getW2()) dp[i][w] = Math.max(dp[i][w], dp[i-1][w-item.getW2()] + item.getV2());
                    if (w >= item.getW3()) dp[i][w] = Math.max(dp[i][w], dp[i-1][w-item.getW3()] + item.getV3());
                }
            }
            return dp[n][capacity];
        }
    }

    // ==================== 结果导出 ====================
    static class ResultExporter {
        public static void exportToTxt(String outputPath, SolutionResult result, int itemCount, int capacity, boolean isSorted) throws IOException {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
                bw.write("========== D{0-1} 背包问题求解结果 =========="); bw.newLine();
                bw.write(String.format("输入规模：%d 个项集，背包容量 %d", itemCount, capacity)); bw.newLine();
                bw.write("数据处理：" + (isSorted ? "已按性价比(V3/W3)非递增排序" : "未排序")); bw.newLine();
                bw.write("----------------------------------------"); bw.newLine();
                bw.write(String.format("最优解（最大价值）：%d", result.getMaxValue())); bw.newLine();
                bw.write(String.format("求解耗时：%d 毫秒", result.getSolveTimeMs())); bw.newLine();
                bw.write("========================================");
            }
        }
    }

    // ==================== 散点图绘制（纯Swing） ====================
    class ScatterChartPanel extends JPanel {
        private final List<ItemSet> items;
        public ScatterChartPanel(List<ItemSet> items) {
            this.items = items;
            setBackground(Color.WHITE);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padding = 50;
            int width = getWidth() - 2 * padding;
            int height = getHeight() - 2 * padding;

            g2d.drawLine(padding, padding, padding, padding + height);
            g2d.drawLine(padding, padding + height, padding + width, padding + height);
            g2d.drawString("重量(横轴)", padding + width/2, padding + height + 30);
            g2d.drawString("价值(纵轴)", 10, padding + height/2);

            g2d.setColor(Color.RED);
            for (ItemSet item : items) {
                drawPoint(g2d, padding, width, height, item.getW1(), item.getV1());
                drawPoint(g2d, padding, width, height, item.getW2(), item.getV2());
                drawPoint(g2d, padding, width, height, item.getW3(), item.getV3());
            }
        }
        private void drawPoint(Graphics2D g2d, int padding, int w, int h, int weight, int value) {
            int x = padding + (weight * w) / currentCapacity;
            int y = padding + h - (value * h) / currentCapacity;
            g2d.fillOval(x - 3, y - 3, 6, 6);
        }
    }

    // ==================== GUI界面 ====================
    private List<ItemSet> currentData = null;
    private int currentCapacity = 0;
    private SolutionResult currentResult = null;
    private boolean dataSorted = false;

    private JTextArea logArea;
    private JLabel lblCapacity, lblResult, lblTime;
    private JButton btnSort, btnSolve, btnExport, btnChart;

    public Main() {
        setTitle("D{0-1} 背包问题求解器");
        setSize(750, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnLoad = new JButton("1. 读取数据文件");
        btnSort = new JButton("2. 按性价比排序");
        btnChart = new JButton("3. 绘制散点图");
        btnSolve = new JButton("4. 开始求解");
        btnExport = new JButton("5. 导出结果");

        btnSort.setEnabled(false);
        btnChart.setEnabled(false);
        btnSolve.setEnabled(false);
        btnExport.setEnabled(false);

        topPanel.add(btnLoad);
        topPanel.add(btnSort);
        topPanel.add(btnChart);
        topPanel.add(btnSolve);
        topPanel.add(btnExport);

        JPanel statusPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statusPanel.setBorder(BorderFactory.createTitledBorder("状态信息"));
        statusPanel.add(new JLabel("📦 背包容量："));
        lblCapacity = new JLabel("未加载");
        lblCapacity.setFont(new Font("Dialog", Font.BOLD, 14));
        statusPanel.add(lblCapacity);

        statusPanel.add(new JLabel("🏆 最大价值："));
        lblResult = new JLabel("---");
        lblResult.setFont(new Font("Dialog", Font.BOLD, 18));
        lblResult.setForeground(new Color(220, 20, 60));
        statusPanel.add(lblResult);

        statusPanel.add(new JLabel("⏱️ 求解耗时："));
        lblTime = new JLabel("--- ms");
        lblTime.setFont(new Font("Dialog", Font.BOLD, 14));
        statusPanel.add(lblTime);

        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("操作日志"));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(statusPanel, BorderLayout.CENTER);
        mainPanel.add(logScroll, BorderLayout.SOUTH);
        add(mainPanel);

        btnLoad.addActionListener(e -> loadFile());
        btnSort.addActionListener(e -> sortData());
        btnChart.addActionListener(this::showChart);
        btnSolve.addActionListener(e -> solve());
        btnExport.addActionListener(e -> exportFile());

        log("系统启动成功 ✅");
    }

    private void log(String msg) {
        logArea.append(String.format("[%tT] %s%n", System.currentTimeMillis(), msg));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // 读取文件（修复类型转换）
    private void loadFile() {
        String projectDir = System.getProperty("user.dir");
        // 修复：Object 强转为 String
        String userInput = (String) JOptionPane.showInputDialog(this,
                "输入文件路径（默认input.txt）：","读取数据",
                JOptionPane.PLAIN_MESSAGE,null,null,"input.txt");
        if (userInput == null || userInput.isBlank()) return;

        File file = new File(userInput.trim());
        if (!file.exists()) file = new File(projectDir, userInput.trim());

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,"文件不存在！","错误",JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DataReader.DataBundle bundle = DataReader.readFromFile(file.getAbsolutePath());
            currentData = bundle.itemSets;
            currentCapacity = bundle.capacity;
            currentResult = null;
            dataSorted = false;

            lblCapacity.setText(String.valueOf(currentCapacity));
            btnSort.setEnabled(true);
            btnChart.setEnabled(true);
            btnSolve.setEnabled(true);
            log("读取成功：" + currentData.size() + " 个项集");
        } catch (Exception ex) {
            log("读取失败：" + ex.getMessage());
        }
    }

    // 排序
    private void sortData() {
        DataSorter.sortByRatioDesc(currentData);
        dataSorted = true;
        log("排序完成 ✅");
    }

    // 显示散点图
    private void showChart(ActionEvent e) {
        JFrame frame = new JFrame("重量-价值散点图");
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(this);
        frame.add(new ScatterChartPanel(currentData));
        frame.setVisible(true);
        log("散点图已打开 ✅");
    }

    // 求解
    private void solve() {
        log("开始求解...");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<SolutionResult, Void>() {
            @Override
            protected SolutionResult doInBackground() {
                return DynamicProgrammingSolver.solveWithTimer(currentData, currentCapacity);
            }
            @Override
            protected void done() {
                try {
                    currentResult = get();
                    lblResult.setText(String.valueOf(currentResult.getMaxValue()));
                    lblTime.setText(currentResult.getSolveTimeMs() + " ms");
                    btnExport.setEnabled(true);
                    log("求解完成！最大价值：" + currentResult.getMaxValue());
                } catch (Exception ex) {
                    log("求解失败");
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    // 导出文件（修复类型转换）
    private void exportFile() {
        // 修复：Object 强转为 String
        String path = (String) JOptionPane.showInputDialog(this,"保存文件名：","导出结果",
                JOptionPane.PLAIN_MESSAGE,null,null,"result.txt");
        if (path == null || path.isBlank()) return;

        try {
            ResultExporter.exportToTxt(path, currentResult, currentData.size(), currentCapacity, dataSorted);
            log("导出成功：" + path);
            JOptionPane.showMessageDialog(this,"导出成功 ✅");
        } catch (IOException ex) {
            log("导出失败");
        }
    }

    // ==================== 主函数 ====================
    public static void main(String[] args) {
        // 禁用libpng警告
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("java2d.noddraw", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }
}