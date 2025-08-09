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


    private Mode mode = Mode.RANDOM;


    private long pollIntervalMs = 4000;


    private List<ActionType> enabled = List.of(ActionType.WASD, ActionType.MOUSE, ActionType.G, ActionType.DIM);


    private List<Rule> rules = List.of();

    @Data
    public static class Rule {
        private BigDecimal min;
        private BigDecimal max;
        private List<ActionType> actions;
    }
}
