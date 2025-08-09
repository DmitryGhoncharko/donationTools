package by.ghoncharko.donationtools;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActionExecutor {
    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);
    private final ActionService act;

    public void run(ActionType action) {
        switch (action) {
            case WASD -> {
                log.info("Действие: Нажимаем WASD x3");
                act.pressKeys("wasd", 1, 120);
                act.pressKeys("wasd", 1, 120);
                act.pressKeys("wasd", 1, 120);
            }
            case MOUSE -> {
                log.info("Действие: Двигаем мышь x3");
                act.randomMouseMoveAndClicks(6, 2);
                act.randomMouseMoveAndClicks(6, 2);
                act.randomMouseMoveAndClicks(6, 2);
            }
            case G -> {
                log.info("Действие: Нажимаем G x3");
                act.pressKeys("g", 1, 150);
                act.pressKeys("g", 1, 150);
                act.pressKeys("g", 1, 150);
            }
            case DIM -> {
                log.info("Действие: Гасим экран на 3 сек");
                act.setBrightnessTemporary(0.0, 3000);
            }
        }
    }
}
