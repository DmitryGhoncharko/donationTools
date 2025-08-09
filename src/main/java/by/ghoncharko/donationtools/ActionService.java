package by.ghoncharko.donationtools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Service
public class ActionService {

    private final Robot robot;
    private final double defaultBrightness;
    private final boolean useNirCmd;
    private final String nirCmdPath;

    private static final Map<Character, Integer> KEYMAP = new HashMap<>();
    static {
        KEYMAP.put('w', KeyEvent.VK_W);
        KEYMAP.put('a', KeyEvent.VK_A);
        KEYMAP.put('s', KeyEvent.VK_S);
        KEYMAP.put('d', KeyEvent.VK_D);
        KEYMAP.put('g', KeyEvent.VK_G);
    }

    public ActionService(
            @Value("${actions.defaultBrightness}") double defaultBrightness,
            @Value("${platform.windows.useNirCmd}") boolean useNirCmd,
            @Value("${platform.windows.nirCmdPath}") String nirCmdPath
    ) throws AWTException {
        this.robot = new Robot();
        this.defaultBrightness = defaultBrightness;
        this.useNirCmd = useNirCmd;
        this.nirCmdPath = nirCmdPath;
        robot.setAutoDelay(30);
    }

    public void pressKeys(String keys, int repeat, int delayMs) {
        for (int r = 0; r < repeat; r++) {
            for (char c : keys.toLowerCase().toCharArray()) {
                Integer code = KEYMAP.get(c);
                if (code != null) {
                    robot.keyPress(code);
                    robot.delay(40);
                    robot.keyRelease(code);
                    robot.delay(delayMs);
                }
            }
        }
    }

    public void randomMouseMoveAndClicks(int moves, int clicks) {
        SecureRandom rnd = new SecureRandom();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        for (int i = 0; i < moves; i++) {
            int x = rnd.nextInt(screen.width);
            int y = rnd.nextInt(screen.height);
            robot.mouseMove(x, y);
            robot.delay(100 + rnd.nextInt(150));
        }
        for (int i = 0; i < clicks; i++) {
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(120);
        }
    }

    public void setBrightnessTemporary(double value01, int sleepMs) {
        setBrightness(value01);
        try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
        setBrightness(defaultBrightness);
    }

    public void setBrightness(double value01) {
        try {
            if (useNirCmd) {
                int percent = (int)(value01 * 100);
                new ProcessBuilder(nirCmdPath, "setbrightness", String.valueOf(percent)).start().waitFor();
            } else {
                int percent = (int)(value01 * 100);
                String ps = "$b=(Get-WmiObject -Namespace root/WMI -Class WmiMonitorBrightnessMethods);$b.WmiSetBrightness(1," + percent + ")";
                new ProcessBuilder("powershell", "-Command", ps).start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
