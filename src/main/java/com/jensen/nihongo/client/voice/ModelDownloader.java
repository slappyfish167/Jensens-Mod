package com.jensen.nihongo.client.voice;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.function.DoubleConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Downloads and unpacks the speech model (data only - no code is downloaded). */
final class ModelDownloader {
    private ModelDownloader() {}

    static void download(String url, Path targetDir, DoubleConsumer progress) throws IOException, InterruptedException {
        Files.createDirectories(targetDir);
        Path zip = targetDir.resolve("download.zip.part");
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        long total = response.headers().firstValueAsLong("content-length").orElse(-1L);
        try (InputStream in = response.body(); OutputStream out = Files.newOutputStream(zip)) {
            byte[] buf = new byte[64 * 1024];
            long done = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (total > 0) progress.accept(Math.min(0.99, (double) done / total));
            }
        }
        unzip(zip, targetDir);
        Files.deleteIfExists(zip);
        progress.accept(1.0);
    }

    private static void unzip(Path zip, Path targetDir) throws IOException {
        Path root = targetDir.toAbsolutePath().normalize();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                Path out = root.resolve(e.getName()).normalize();
                if (!out.startsWith(root)) throw new IOException("Unsafe path in model archive: " + e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
