package by.ghoncharko.donationtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

@Component
@EnableScheduling
public class DonationPoller {

    private static final Logger log = LoggerFactory.getLogger(DonationPoller.class);

    private final DonationAlertsClient da;
    private final ActionExecutor executor;
    private final DonationActionsProperties props;
    private final SecureRandom random = new SecureRandom();

    public DonationPoller(DonationAlertsClient da,
                          ActionExecutor executor,
                          DonationActionsProperties props) {
        this.da = da;
        this.executor = executor;
        this.props = props;
    }


    @Scheduled(fixedDelayString = "${donations.pollIntervalMs:4000}")
    public void tick() {
        da.fetchNew()
                .flatMapMany(reactor.core.publisher.Flux::fromIterable)
                .concatMap(d -> Mono.fromRunnable(() -> handleDonation(d)))
                .onErrorContinue((e, o) -> {
                    log.error("Ошибка при обработке доната {}: {}", o, e.toString(), e);
                })
                .blockLast();
    }

    private void handleDonation(DonationAlertsClient.Donation donation) {
        log.info("Новый донат: id={}, сумма={} {}, сообщение='{}'",
                donation.id(), donation.amount(), donation.currency(), donation.message());

        List<ActionType> pool = switch (props.getMode()) {
            case RANDOM -> props.getEnabled();
            case RULES -> actionsByRules(new BigDecimal(String.valueOf(donation.amount())));
        };

        if (pool == null || pool.isEmpty()) {
            log.info("Подходящих действий не найдено (mode={}, amount={})", props.getMode(), donation.amount());
            return;
        }

        ActionType chosen = pool.get(random.nextInt(pool.size()));
        executor.run(chosen);
    }

    private List<ActionType> actionsByRules(BigDecimal amount) {
        if (props.getRules() == null) return List.of();
        return props.getRules().stream()
                .filter(r -> inRange(amount, r.getMin(), r.getMax()))
                .findFirst()
                .map(DonationActionsProperties.Rule::getActions)
                .orElse(List.of());
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        boolean geMin = (min == null) || (value.compareTo(min) >= 0);
        boolean leMax = (max == null) || (value.compareTo(max) <= 0);
        return geMin && leMax;
    }
}
