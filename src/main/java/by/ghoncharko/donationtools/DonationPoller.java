// by/ghoncharko/donationtools/DonationPoller.java
package by.ghoncharko.donationtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.TaskScheduler;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DonationPoller {

    private static final Logger log = LoggerFactory.getLogger(DonationPoller.class);

    private final DonationAlertsClient da;
    private final ActionExecutor executor;
    private final DonationActionsProperties props;
    private final TaskScheduler scheduler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> future;
    private final Random random = new Random();

    public DonationPoller(DonationAlertsClient da,
                          ActionExecutor executor,
                          DonationActionsProperties props,
                          TaskScheduler scheduler) {
        this.da = da;
        this.executor = executor;
        this.props = props;
        this.scheduler = scheduler;
    }

    /** запустить с текущим pollIntervalMs */
    public synchronized void start() {
        if (running.get()) return;
        long delay = Math.max(500, props.getPollIntervalMs());
        future = scheduler.scheduleAtFixedRate(this::safeTick, Duration.ofMillis(delay));
        running.set(true);
        log.info("DonationPoller запущен, интервал {} ms", delay);
    }

    /** остановить */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        running.set(false);
        log.info("DonationPoller остановлен");
    }

    /** перезапустить при смене интервала */
    public synchronized void restartIfRunning() {
        if (running.get()) {
            stop();
            start();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void safeTick() {
        try {
            tick();
        } catch (Throwable t) {
            log.error("Ошибка в tick(): {}", t.toString(), t);
        }
    }

    private void tick() {
        da.fetchNew()
                .flatMapMany(reactor.core.publisher.Flux::fromIterable)
                .concatMap(d -> Mono.fromRunnable(() -> handleDonation(d)))
                .onErrorContinue((e, o) -> log.error("Ошибка при обработке доната {}: {}", o, e.toString(), e))
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
