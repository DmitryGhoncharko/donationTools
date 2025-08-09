package by.ghoncharko.donationtools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class DonationPoller {

    private final DonationAlertsClient da;
    private final ActionService act;
    private final Map<String, String> triggers;
    private final NavigableMap<Integer, List<String>> thresholds;
    private final String customKeys;
    private final int customKeysRepeat;

    public DonationPoller(
            DonationAlertsClient da,
            ActionService act,
            @Value("${actions.triggers}") String triggersStr,
            @Value("${actions.thresholds}") String thresholdsStr,
            @Value("${actions.customKeys}") String customKeys,
            @Value("${actions.customKeysRepeat}") int customKeysRepeat
    ) {
        this.da = da;
        this.act = act;
        this.triggers = parseMap(triggersStr);
        this.thresholds = parseThresholds(thresholdsStr);
        this.customKeys = customKeys;
        this.customKeysRepeat = customKeysRepeat;
    }

    @Scheduled(fixedDelayString = "${donationalerts.pollIntervalMs}")
    public void tick() {
        da.fetchNew().flatMapMany(list -> reactor.core.publisher.Flux.fromIterable(list))
                .concatMap(d -> Mono.fromRunnable(() -> handleDonation(d.amount(), d.message())))
                .onErrorContinue((e, o) -> e.printStackTrace())
                .blockLast();
    }

    private void handleDonation(double amount, String message) {
        String msg = Optional.ofNullable(message).orElse("").toLowerCase();

        for (var entry : triggers.entrySet()) {
            if (msg.contains(entry.getKey().toLowerCase())) {
                runAction(entry.getValue());
                return;
            }
        }

        for (var entry : thresholds.descendingMap().entrySet()) {
            if (amount >= entry.getKey()) {
                entry.getValue().forEach(this::runAction);
                return;
            }
        }
    }

    private void runAction(String actionName) {
        switch (actionName) {
            case "randomMouse" -> act.randomMouseMoveAndClicks(6, 2);
            case "pressG3" -> act.pressKeys("g", 3, 150);
            case "pressWASD" -> act.pressKeys("wasd", 1, 120);
            case "dark2s" -> act.setBrightnessTemporary(0.0, 2000);
            case "dark3s" -> act.setBrightnessTemporary(0.0, 3000);
            case "customKeys" -> act.pressKeys(customKeys, customKeysRepeat, 150);
        }
    }

    private Map<String, String> parseMap(String str) {
        return Arrays.stream(str.split(";"))
                .map(s -> s.split(":"))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }

    private NavigableMap<Integer, List<String>> parseThresholds(String str) {
        NavigableMap<Integer, List<String>> map = new TreeMap<>();
        for (String part : str.split(";")) {
            String[] arr = part.split(":");
            if (arr.length == 2) {
                int amount = Integer.parseInt(arr[0]);
                List<String> acts = Arrays.asList(arr[1].split(","));
                map.put(amount, acts);
            }
        }
        return map;
    }
}
