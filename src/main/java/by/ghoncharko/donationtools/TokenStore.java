package by.ghoncharko.donationtools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class TokenStore {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    private volatile String token;


    private final Path storePath = Path.of(System.getProperty("user.home"), ".donationtools.properties");

    public TokenStore(@Value("${donationalerts.token:}") String initial) {
        String fromFile = loadFromFile();
        this.token = (fromFile != null && !fromFile.isBlank()) ? fromFile : initial;
    }

    public String getToken() {
        lock.readLock().lock();
        try {
            return token;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setToken(String newToken) {
        lock.writeLock().lock();
        try {
            this.token = Objects.requireNonNullElse(newToken, "").trim();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void save() {
        lock.readLock().lock();
        try {
            Properties p = new Properties();
            p.setProperty("donationalerts.token", token == null ? "" : token);
            if (!Files.exists(storePath.getParent())) {
                Files.createDirectories(storePath.getParent());
            }
            try (OutputStream os = Files.newOutputStream(storePath)) {
                p.store(os, "DonationTools saved settings");
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить токен в " + storePath, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    private String loadFromFile() {
        if (!Files.exists(storePath)) return null;
        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(storePath)) {
            p.load(is);
            return p.getProperty("donationalerts.token");
        } catch (IOException e) {
            return null;
        }
    }
}

