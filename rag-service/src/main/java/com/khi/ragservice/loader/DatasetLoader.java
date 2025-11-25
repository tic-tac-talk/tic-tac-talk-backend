package com.khi.ragservice.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.ragservice.entity.RagItem;
import com.khi.ragservice.repository.RagItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetLoader implements CommandLineRunner {

    private final RagItemRepository repo;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private static final boolean SEED_ENABLED = true;
    private static final String DATASET_DIR = "classpath:";
    private static final int LABEL_COUNT = 30;
    private static final boolean SKIP_IF_NOT_EMPTY = true;
    private static final boolean USE_FINGERPRINT = true;
    private static final boolean RESET_BEFORE_SEED = false;
    private static final int BATCH_SIZE = 1000;

    @Override
    public void run(String... args) throws Exception {
        if (!SEED_ENABLED) {
            log.info("[seed] disabled");
            return;
        }

        if (RESET_BEFORE_SEED) {
            truncateForReset(dataSource);
        }

        if (!RESET_BEFORE_SEED && SKIP_IF_NOT_EMPTY && repo.count() > 0) {
            log.info("[seed] rag_items not empty -> skip dataset seed");
            return;
        }

        // Load all 30 dataset files
        List<org.springframework.core.io.Resource> resources = new ArrayList<>();
        for (int labelId = 1; labelId <= LABEL_COUNT; labelId++) {
            String filename = String.format("dataset_label_%02d.txt", labelId);
            var resource = resourceLoader.getResource(DATASET_DIR + filename);
            if (!resource.exists()) {
                log.warn("[seed] dataset file not found: {}", filename);
                continue;
            }
            resources.add(resource);
        }

        if (resources.isEmpty()) {
            log.warn("[seed] no dataset files found");
            return;
        }

        String fingerprint = null;
        if (USE_FINGERPRINT) {
            fingerprint = calcCombinedSha256(resources);
            ensureSeedHistoryTable(dataSource);
            if (!RESET_BEFORE_SEED && isSeedAlreadyApplied(dataSource, fingerprint)) {
                log.info("[seed] same dataset fingerprint already applied -> skip seed");
                return;
            }
        }

        log.info("[seed] loading JSON datasets from {} files", resources.size());

        List<RagItem> batch = new ArrayList<>(BATCH_SIZE);
        long total = 0;

        for (org.springframework.core.io.Resource resource : resources) {
            try (var in = resource.getInputStream();
                    var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

                reader.mark(4096);
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    log.warn("[seed] dataset file is empty: {}", resource.getFilename());
                    continue;
                }
                String head = firstLine.stripLeading();
                reader.reset();

                if (head.startsWith("[")) {
                    String all = reader.lines().collect(Collectors.joining("\n"));
                    JsonNode arr = objectMapper.readTree(all);
                    if (!arr.isArray())
                        throw new IllegalArgumentException("dataset is not a JSON array");
                    for (JsonNode node : arr) {
                        total += processNode(node, batch);
                        if (batch.size() >= BATCH_SIZE)
                            flushBatch(batch);
                    }
                } else {
                    // NDJSON
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = stripBom(line).trim();
                        if (line.isEmpty())
                            continue;
                        JsonNode node = objectMapper.readTree(line);
                        total += processNode(node, batch);
                        if (batch.size() >= BATCH_SIZE)
                            flushBatch(batch);
                    }
                }
            } catch (Exception e) {
                log.error("[seed] error loading file {}: {}", resource.getFilename(), e.getMessage());
                throw e;
            }
        }

        if (!batch.isEmpty())
            flushBatch(batch);

        if (USE_FINGERPRINT && fingerprint != null) {
            upsertSeedHistory(dataSource, fingerprint);
        }

        log.info("[seed] JSON dataset load done. totalRecords={}", total);
    }

    private int processNode(JsonNode node, List<RagItem> batch) {
        Map<String, JsonNode> idx = normalizeKeys(node);

        int id = parseInt(req(idx, "id"), "id");
        String text = normalizeSpace(req(idx, "text").asText(""));
        if (text.isBlank()) {
            log.warn("[seed] skip row id={} due to blank text", id);
            return 0;
        }
        String label = req(idx, "label").asText("");
        short labelId = (short) parseInt(req(idx, "label_id"), "label_id");

        RagItem item = new RagItem();
        item.setId(id);
        item.setText(text);
        item.setLabel(label);
        item.setLabelId(labelId);

        batch.add(item);
        return 1;
    }

    private void flushBatch(List<RagItem> batch) {
        try {
            repo.saveAll(batch);
            log.info("[seed] inserted {} rows", batch.size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            batch.clear();
        }
    }

    private void truncateForReset(DataSource ds) {
        try (var con = ds.getConnection(); var st = con.createStatement()) {
            st.execute("TRUNCATE TABLE rag_items RESTART IDENTITY CASCADE");
            log.info("[seed] TRUNCATE rag_items done");
        } catch (Exception e) {
            log.warn("[seed] truncate failed: {}", e.toString());
        }
    }

    private void ensureSeedHistoryTable(DataSource ds) {
        try (var con = ds.getConnection(); var st = con.createStatement()) {
            st.execute("""
                        CREATE TABLE IF NOT EXISTS seed_history (
                          id SERIAL PRIMARY KEY,
                          fingerprint TEXT UNIQUE NOT NULL,
                          applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
                        )
                    """);
        } catch (Exception e) {
            log.warn("[seed] ensure seed_history failed: {}", e.toString());
        }
    }

    private boolean isSeedAlreadyApplied(DataSource ds, String fp) {
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con
                        .prepareStatement("SELECT 1 FROM seed_history WHERE fingerprint = ? LIMIT 1")) {
            ps.setString(1, fp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.warn("[seed] query seed_history failed: {}", e.toString());
            return false;
        }
    }

    private void upsertSeedHistory(DataSource ds, String fp) {
        try (Connection con = ds.getConnection();
                PreparedStatement ps = con.prepareStatement("""
                            INSERT INTO seed_history(fingerprint) VALUES (?)
                            ON CONFLICT (fingerprint) DO NOTHING
                        """)) {
            ps.setString(1, fp);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("[seed] upsert seed_history failed: {}", e.toString());
        }
    }

    private static String stripBom(String s) {
        return s == null ? null : s.replace("\uFEFF", "");
    }

    private static String normKey(String k) {
        if (k == null)
            return null;
        return k.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, JsonNode> normalizeKeys(JsonNode node) {
        Map<String, JsonNode> out = new LinkedHashMap<>();
        var it = node.fieldNames();
        while (it.hasNext()) {
            String k = it.next();
            out.putIfAbsent(normKey(k), node.get(k));
        }
        return out;
    }

    private static JsonNode req(Map<String, JsonNode> idx, String key) {
        JsonNode v = idx.get(normKey(key));
        if (v == null)
            throw new IllegalArgumentException("Missing field: " + key);
        return v;
    }

    private static int parseInt(JsonNode n, String keyName) {
        if (n == null || n.isNull())
            throw new IllegalArgumentException("Missing field: " + keyName);
        if (n.isInt() || n.isLong())
            return n.asInt();
        String s = n.asText();
        if (s == null || s.trim().isEmpty())
            throw new IllegalArgumentException("Empty field: " + keyName);
        return Integer.parseInt(s.trim());
    }

    private static String normalizeSpace(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ");
    }

    private String calcSha256(org.springframework.core.io.Resource res) {
        try (var in = res.getInputStream()) {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0)
                md.update(buf, 0, n);
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("[seed] fingerprint calc failed: {}", e.toString());
            return null;
        }
    }

    private String calcCombinedSha256(List<org.springframework.core.io.Resource> resources) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            for (org.springframework.core.io.Resource res : resources) {
                try (var in = res.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0)
                        md.update(buf, 0, n);
                }
            }
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("[seed] combined fingerprint calc failed: {}", e.toString());
            return null;
        }
    }
}
