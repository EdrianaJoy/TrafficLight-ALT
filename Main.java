import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    private JLabel welcomeLabel, promptLabel;
    private JButton nextButton;
    private JSpinner flowSpinner;

    public Main() {
        super("Traffic Flow Selection");

        // Optional: cross-platform LAF on macOS so custom colors appear fully
        // try {
        //     UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        // ========== LEFT PANEL (map or image) ==========
        JPanel mapPanel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(new ImageIcon("map.png")); // Adjust path if needed
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        mapPanel.add(imageLabel, BorderLayout.CENTER);
        add(mapPanel, BorderLayout.CENTER);

        // ========== RIGHT PANEL (red background) ==========
        JPanel rightPanel = new JPanel();
        // Use a BoxLayout (Y_AXIS) so we can center vertically + horizontally
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(139, 0, 0)); // dark red
        rightPanel.setPreferredSize(new Dimension(300, getHeight()));
        add(rightPanel, BorderLayout.EAST);

        // ========== Labels ==========
        welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 30));
        // Center horizontally in the BoxLayout
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Load & scale the traffic light icon, then attach to welcomeLabel ---
        ImageIcon trafficIcon = new ImageIcon("2.png"); // Adjust path as needed
        Image scaledImg = trafficIcon.getImage().getScaledInstance(60, 80, Image.SCALE_SMOOTH);
        trafficIcon = new ImageIcon(scaledImg);
        welcomeLabel.setIcon(trafficIcon);
        welcomeLabel.setIconTextGap(10); // space between icon & text

        promptLabel = new JLabel("<html>Please choose the <br>traffic flow of your choice</html>");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        promptLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // Center horizontally in the BoxLayout
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ========== Spinner (1–10), centered & no typing allowed ==========
        flowSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        flowSpinner.setFont(new Font("Arial", Font.BOLD, 18));
        Dimension fixedSize = new Dimension(180, 50);
        flowSpinner.setPreferredSize(fixedSize);
        flowSpinner.setMinimumSize(fixedSize);
        flowSpinner.setMaximumSize(fixedSize);
        flowSpinner.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Center the number and disable text editing
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) flowSpinner.getEditor();
        JFormattedTextField textField = editor.getTextField();
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setEditable(false);

        // ========== Next button ==========
        nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Arial", Font.BOLD, 18));
        nextButton.setPreferredSize(fixedSize);
        nextButton.setMinimumSize(fixedSize);
        nextButton.setMaximumSize(fixedSize);
        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Remove background so it blends with panel
        nextButton.setContentAreaFilled(false);
        nextButton.setOpaque(false);
        // If you want no border outline at all, uncomment:
        // nextButton.setBorderPainted(false);

        // On click, read the spinner value
        nextButton.addActionListener(e -> {
            int flowValue = (Integer) flowSpinner.getValue();
            JOptionPane.showMessageDialog(
                Main.this,
                "You chose flow level: " + flowValue,
                "Selection",
                JOptionPane.INFORMATION_MESSAGE
            );
        });

        // ========== Add components to the RIGHT PANEL ==========
        // Glue at the top to push everything down
        rightPanel.add(Box.createVerticalGlue());

        rightPanel.add(welcomeLabel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(promptLabel);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(flowSpinner);

        // Glue in the middle so Next button is near the bottom
        rightPanel.add(Box.createVerticalGlue());

        rightPanel.add(nextButton);
        rightPanel.add(Box.createVerticalStrut(30));

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
