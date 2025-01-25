import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    private JLabel welcomeLabel, promptLabel, groupMembersLabel, groupMembersTitleLabel;
    private JButton nextButton;

    public Main() {
        super("Welcome Page");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        // ========== LEFT PANEL (map or image) ==========
        JPanel mapPanel = new JPanel(new BorderLayout());
        JLabel imageLabel = new JLabel(new ImageIcon("img-map.png"));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        mapPanel.add(imageLabel, BorderLayout.CENTER);
        add(mapPanel, BorderLayout.CENTER);

        // ========== RIGHT PANEL (red background) ==========
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(139, 0, 0)); // dark red
        rightPanel.setPreferredSize(new Dimension(300, getHeight()));
        add(rightPanel, BorderLayout.EAST);

        // ========== Labels ==========
        welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 30));
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Load & scale the traffic light icon ---
        ImageIcon trafficIcon = new ImageIcon("traffic-light.png"); // Adjust path as needed
        Image scaledImg = trafficIcon.getImage().getScaledInstance(60, 80, Image.SCALE_SMOOTH);
        trafficIcon = new ImageIcon(scaledImg);
        welcomeLabel.setIcon(trafficIcon);
        welcomeLabel.setIconTextGap(10);
        welcomeLabel.setVerticalAlignment(JLabel.TOP);
        welcomeLabel.setVerticalTextPosition(JLabel.BOTTOM);

        // -- Short introduction prompt --
        promptLabel = new JLabel("<html>Welcome to the Traffic Simulation App!</html>");
        promptLabel.setForeground(Color.WHITE);
        promptLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        promptLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // -- Label before group members --
        groupMembersTitleLabel = new JLabel("Group Members:");
        groupMembersTitleLabel.setForeground(Color.WHITE);
        groupMembersTitleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        groupMembersTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // -- Names of the group members --
        groupMembersLabel = new JLabel("<html>Abram, Alfaro<br>Damaso, Edriana Joy<br>Estonilo, Julius Evan<br>Xavier, Mikhail Gabriel</html>");
        groupMembersLabel.setForeground(Color.WHITE);
        groupMembersLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        groupMembersLabel.setHorizontalAlignment(SwingConstants.CENTER);
        groupMembersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ========== Next button ==========
        nextButton = new JButton("Next →");
        nextButton.setFont(new Font("Arial", Font.BOLD, 18));
        nextButton.setBackground(new Color(255, 255, 255));
        nextButton.setOpaque(true);
        nextButton.setContentAreaFilled(true);
        nextButton.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        
        Dimension fixedSize = new Dimension(180, 50);
        nextButton.setPreferredSize(fixedSize);
        nextButton.setMinimumSize(fixedSize);
        nextButton.setMaximumSize(fixedSize);
        nextButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // -- Placeholder action listener for next button --
        nextButton.addActionListener(_ -> {
            // Close the current window
            Main.this.dispose();

            // Launch the ImageZoomAndPan simulation
            SwingUtilities.invokeLater(() -> ImageZoomAndPan.main(null));
        });

        // ========== Add components to the RIGHT PANEL ==========
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(welcomeLabel);
        rightPanel.add(Box.createVerticalStrut(80));
        rightPanel.add(promptLabel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(groupMembersTitleLabel);
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(groupMembersLabel);
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