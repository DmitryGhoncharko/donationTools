package by.ghoncharko.donationtools;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "donations")
public class DonationActionsProperties {

    public enum Mode { RANDOM, RULES }

    /** RANDOM или RULES */
    private Mode mode = Mode.RANDOM;

    /** период опроса для @Scheduled (перекинем сюда и используем в @Scheduled) */
    private long pollIntervalMs = 4000;

    /** какие действия вообще разрешены в RANDOM-режиме */
    private List<ActionType> enabled = List.of(ActionType.WASD, ActionType.MOUSE, ActionType.G, ActionType.DIM);

    /** правила для RULES-режима */
    private List<Rule> rules = List.of();

    @Data
    public static class Rule {
        /** нижняя граница включительно (nullable = без нижней) */
        private BigDecimal min;
        /** верхняя граница включительно (nullable = без верхней) */
        private BigDecimal max;
        /** из этих действий выберем случайное */
        private List<ActionType> actions;
    }
}
