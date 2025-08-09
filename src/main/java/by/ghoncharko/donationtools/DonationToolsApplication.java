package by.ghoncharko.donationtools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DonationToolsApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(DonationToolsApplication.class, args);
    }

}
