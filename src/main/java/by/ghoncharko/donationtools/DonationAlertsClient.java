package by.ghoncharko.donationtools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DonationAlertsClient {

    private static final Logger log = LoggerFactory.getLogger(DonationAlertsClient.class);

    private final WebClient web;
    private final AtomicLong lastSeenId = new AtomicLong(0);

    public record Donation(long id, double amount, String currency, String message) {}
    public record DonationsResp(List<Donation> data) {}

    public DonationAlertsClient(
            @Value("${donationalerts.token}") String token
    ) {
        this.web = WebClient.builder()
                .baseUrl("https://www.donationalerts.com/api/v1/alerts")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    public Mono<List<Donation>> fetchNew() {
        return web.get().uri("/donations")
                .retrieve()
                .bodyToMono(DonationsResp.class)
                .map(resp -> {
                    long prev = lastSeenId.get();
                    List<Donation> all = resp.data();

                    if (all == null || all.isEmpty()) {
                        log.debug("Нет новых донатов в ответе API");
                        return List.of();
                    }

                    all.forEach(d -> log.info("Получен донат [id={}, amount={}, currency={}, message='{}']",
                            d.id(), d.amount(), d.currency(), d.message()));

                    long max = all.stream().mapToLong(Donation::id).max().orElse(prev);

                    if (prev == 0 && max > 0) {
                        lastSeenId.set(max);
                        log.debug("Инициализация lastSeenId = {}", max);
                        return List.of();
                    }

                    var fresh = all.stream().filter(d -> d.id() > prev).toList();
                    if (!fresh.isEmpty()) {
                        log.info("Новых донатов: {}", fresh.size());
                        fresh.forEach(d -> log.info("НОВЫЙ донат [id={}, amount={}, currency={}, message='{}']",
                                d.id(), d.amount(), d.currency(), d.message()));
                    } else {
                        log.debug("Новых донатов нет");
                    }

                    if (max > prev) lastSeenId.set(max);

                    return fresh;
                });
    }
}
