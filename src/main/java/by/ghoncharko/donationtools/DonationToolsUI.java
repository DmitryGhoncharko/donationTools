package by.ghoncharko.donationtools;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DonationToolsUI {

    private final DonationActionsProperties props;
    private final DonationPoller poller;
    private JTextField tokenField;

    private JFrame frame;
    private JComboBox<DonationActionsProperties.Mode> modeCombo;
    private JSpinner pollSpinner;
    private final Map<ActionType, JCheckBox> enabledChecks = new EnumMap<>(ActionType.class);
    private JTable rulesTable;
    private RulesTableModel rulesModel;


    private JSpinner defaultBrightnessSpinner;
    private JCheckBox useNircmdCheck;
    private JTextField nircmdPathField;
    private final TokenStore tokenStore;

    public DonationToolsUI(DonationActionsProperties props, DonationPoller poller,TokenStore tokenStore) {
        this.props = props;
        this.poller = poller;
        this.tokenStore = tokenStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void show() {
        SwingUtilities.invokeLater(this::buildAndShow);
    }

    private void buildAndShow() {
        frame = new JFrame("Donation Tools – Settings");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(760, 560);
        frame.setLocationRelativeTo(null);

        var root = new JPanel(new BorderLayout(10,10));
        root.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        frame.setContentPane(root);


        var top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Mode:"));
        modeCombo = new JComboBox<>(DonationActionsProperties.Mode.values());
        modeCombo.setSelectedItem(props.getMode());
        top.add(modeCombo);

        top.add(new JLabel("Poll interval, ms:"));
        pollSpinner = new JSpinner(new SpinnerNumberModel(Math.max(500, props.getPollIntervalMs()), 200, 60000, 100));
        top.add(pollSpinner);

        JButton btnStart = new JButton("Start");
        btnStart.addActionListener(e -> poller.start());
        JButton btnStop = new JButton("Stop");
        btnStop.addActionListener(e -> poller.stop());

        top.add(btnStart);
        top.add(btnStop);

        JLabel runState = new JLabel(poller.isRunning() ? "RUNNING" : "STOPPED");
        Timer t = new Timer(500, e -> runState.setText(poller.isRunning() ? "RUNNING" : "STOPPED"));
        t.start();
        runState.setForeground(new Color(0,128,0));
        top.add(new JLabel("State:"));
        top.add(runState);

        root.add(top, BorderLayout.NORTH);


        var tabs = new JTabbedPane();


        tabs.add("Random Enabled", buildRandomPanel());


        tabs.add("Rules", buildRulesPanel());


        tabs.add("Extra", buildExtraPanel());

        root.add(tabs, BorderLayout.CENTER);


        var bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnApply = new JButton("Apply");
        btnApply.addActionListener(e -> applyToProps());
        JButton btnApplyAndRestart = new JButton("Apply & Restart Poller");
        btnApplyAndRestart.addActionListener(e -> {
            applyToProps();
            poller.restartIfRunning();
        });
        bottom.add(btnApply);
        bottom.add(btnApplyAndRestart);
        root.add(bottom, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private JPanel buildRandomPanel() {
        var panel = new JPanel(new GridLayout(0, 2, 8, 8));
        for (ActionType a : ActionType.values()) {
            JCheckBox cb = new JCheckBox(a.name());
            cb.setSelected(props.getEnabled() != null && props.getEnabled().contains(a));
            enabledChecks.put(a, cb);
            panel.add(cb);
        }
        var wrap = new JPanel(new BorderLayout());
        wrap.add(new JLabel("В RANDOM будут только отмеченные действия:"), BorderLayout.NORTH);
        wrap.add(panel, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildRulesPanel() {
        rulesModel = new RulesTableModel(props.getRules());
        rulesTable = new JTable(rulesModel);
        rulesTable.setFillsViewportHeight(true);

        var buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add rule");
        add.addActionListener(e -> rulesModel.addEmpty());
        JButton remove = new JButton("Remove selected");
        remove.addActionListener(e -> rulesModel.removeAt(rulesTable.getSelectedRow()));
        buttons.add(add);
        buttons.add(remove);

        var wrap = new JPanel(new BorderLayout(8,8));
        wrap.add(new JLabel("RULES: первая подходящая по сумме запись, actions перечисляются через запятую (WASD,MOUSE,G,DIM)."), BorderLayout.NORTH);
        wrap.add(new JScrollPane(rulesTable), BorderLayout.CENTER);
        wrap.add(buttons, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildExtraPanel() {
        var panel = new JPanel(new GridBagLayout());
        var gc = new GridBagConstraints();
        gc.insets = new Insets(5,5,5,5);
        gc.anchor = GridBagConstraints.WEST;

        int r = 0;

        gc.gridx=0; gc.gridy=r; panel.add(new JLabel("DonationAlerts token:"), gc);
        tokenField = new JTextField(30);
        tokenField.setText(tokenStore.getToken());
        gc.gridx=1; panel.add(tokenField, gc);
        r++;

        gc.gridx=0; gc.gridy=r; panel.add(new JLabel("Default brightness (0..1):"), gc);
        defaultBrightnessSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 1.0, 0.05));
        gc.gridx=1; panel.add(defaultBrightnessSpinner, gc);
        r++;

        gc.gridx=0; gc.gridy=r; panel.add(new JLabel("Use NirCmd (Windows):"), gc);
        useNircmdCheck = new JCheckBox();
        gc.gridx=1; panel.add(useNircmdCheck, gc);
        r++;

        gc.gridx=0; gc.gridy=r; panel.add(new JLabel("NirCmd path:"), gc);
        nircmdPathField = new JTextField(28);
        gc.gridx=1; panel.add(nircmdPathField, gc);
        r++;

        var note = new JLabel("<html><i>Замечание: эти параметры читаются ActionService при старте приложения. Меняются с рестартом.</i></html>");
        gc.gridx=0; gc.gridy=r; gc.gridwidth=2;
        panel.add(note, gc);

        return panel;
    }


    private void applyToProps() {

        props.setMode((DonationActionsProperties.Mode) modeCombo.getSelectedItem());


        long interval = ((Number) pollSpinner.getValue()).longValue();
        props.setPollIntervalMs(interval);


        List<ActionType> enabled = new ArrayList<>();
        enabledChecks.forEach((a, cb) -> { if (cb.isSelected()) enabled.add(a); });
        props.setEnabled(enabled);


        props.setRules(rulesModel.getRules());

        String newToken = tokenField.getText();
        tokenStore.setToken(newToken);
        try {
            tokenStore.save();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Не удалось сохранить токен: " + ex.getMessage(),
                    "Ошибка сохранения", JOptionPane.ERROR_MESSAGE);
            return;
        }


        poller.restartIfRunning();

        JOptionPane.showMessageDialog(frame, "Настройки применены.", "OK", JOptionPane.INFORMATION_MESSAGE);
    }
}
