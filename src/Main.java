import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * D{0-1}背包问题主程序 (GUI稳定版)
 * 包含：文件读取、数据排序、动态规划求解、结果导出TXT
 */
public class Main extends JFrame {

    // ==================== 1. 数据模型类 ====================

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

    static class DataSorter {
        public static void sortByRatioDesc(List<ItemSet> items) {
            if (items == null) return;
            items.sort(Comparator.comparingDouble(ItemSet::getRatio).reversed());
        }
    }

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

    // ==================== 2. GUI 界面代码 ====================

    private List<ItemSet> currentData = null;
    private int currentCapacity = 0;
    private SolutionResult currentResult = null;
    private boolean dataSorted = false;

    private JTextArea logArea;
    private JLabel lblCapacity;
    private JLabel lblResult;
    private JLabel lblTime;
    private JButton btnSort;
    private JButton btnSolve;
    private JButton btnExport;

    public Main() {
        setTitle("D{0-1} 背包问题求解器");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- 顶部：按钮栏 ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnLoad = new JButton("1. 读取数据文件");
        btnSort = new JButton("2. 按性价比排序");
        btnSolve = new JButton("3. 开始求解");
        btnExport = new JButton("4. 导出结果");

        btnSort.setEnabled(false);
        btnSolve.setEnabled(false);
        btnExport.setEnabled(false);

        topPanel.add(btnLoad);
        topPanel.add(btnSort);
        topPanel.add(btnSolve);
        topPanel.add(btnExport);

        // --- 中部：状态显示 ---
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

        // --- 底部：日志 ---
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
        btnSolve.addActionListener(e -> solve());
        btnExport.addActionListener(e -> exportFile());

        log("系统启动成功。");
        log("提示：请确保 input.txt 在项目根目录下，或准备好文件的完整路径。");
    }

    private void log(String msg) {
        logArea.append(String.format("[%tT] %s%n", System.currentTimeMillis(), msg));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // --- 修复后的文件读取：使用输入框 ---
    private void loadFile() {
        String projectDir = System.getProperty("user.dir");
        String defaultPath = "input.txt";
        String helpMsg = "使用方法：\n" +
                "1. 如果文件在项目根目录，直接输入文件名：input.txt\n" +
                "2. 或者按住Shift右键文件，选择“复制为路径”粘贴到这里。\n\n" +
                "当前项目目录：\n" + projectDir;

        String userInput = (String) JOptionPane.showInputDialog(
                this,
                helpMsg,
                "读取数据文件",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultPath
        );

        if (userInput == null || userInput.trim().isEmpty()) return;

        String filePath = userInput.trim();
        File file = new File(filePath);

        // 智能补全：如果只写了文件名，自动去项目根目录找
        if (!file.exists() && !filePath.contains(File.separator)) {
            file = new File(projectDir, filePath);
        }

        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "找不到文件！\n尝试路径：" + file.getAbsolutePath(),
                    "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            log("正在读取文件：" + file.getName());
            DataReader.DataBundle bundle = DataReader.readFromFile(file.getAbsolutePath());
            currentData = bundle.itemSets;
            currentCapacity = bundle.capacity;
            currentResult = null;
            dataSorted = false;

            lblCapacity.setText(String.valueOf(currentCapacity));
            lblResult.setText("---");
            lblTime.setText("--- ms");

            btnSort.setEnabled(true);
            btnSolve.setEnabled(true);

            log(String.format("✅ 读取成功！共 %d 个项集，背包容量 %d。", currentData.size(), currentCapacity));
            log("下一步：请点击【2. 按性价比排序】或直接【3. 开始求解】。");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "读取失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("❌ 读取失败：" + ex.getMessage());
        }
    }

    // --- 修复后的文件导出：使用输入框 ---
    private void exportFile() {
        if (currentResult == null) return;

        String defaultPath = "result_output.txt";
        String userInput = (String) JOptionPane.showInputDialog(
                this,
                "请输入要保存的文件名：",
                "导出结果",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                defaultPath
        );

        if (userInput == null || userInput.trim().isEmpty()) return;

        String filePath = userInput.trim();
        // 如果没写绝对路径，默认保存在项目目录
        if (!filePath.contains(File.separator)) {
            filePath = System.getProperty("user.dir") + File.separator + filePath;
        }

        try {
            ResultExporter.exportToTxt(filePath, currentResult, currentData.size(), currentCapacity, dataSorted);
            log("✅ 结果已导出至：" + filePath);
            JOptionPane.showMessageDialog(this, "导出成功！\n保存路径：" + filePath, "提示", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "导出失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            log("❌ 导出失败：" + ex.getMessage());
        }
    }

    private void sortData() {
        if (currentData == null) return;
        log("正在按“物品3价值/重量比”进行非递增排序...");
        DataSorter.sortByRatioDesc(currentData);
        dataSorted = true;
        log("✅ 排序完成。");
    }

    private void solve() {
        if (currentData == null) return;
        log("开始动态规划求解...");
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
                    log(String.format("✅ 求解完成！最大价值：%d，耗时：%dms。",
                            currentResult.getMaxValue(), currentResult.getSolveTimeMs()));
                    log("下一步：请点击【4. 导出结果】保存文件。");
                } catch (Exception ex) {
                    log("❌ 求解出错：" + ex.getMessage());
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        }.execute();
    }

    // ==================== 3. 程序入口 ====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 强制设置为跨平台外观，避免Windows系统图标冲突
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Main().setVisible(true);
        });
    }
}