package cn.linli.workorder;

import cn.linli.common.ApiException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Component
public class ImageStorage {
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private final Path root;

    public ImageStorage(@Value("${linli.storage.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public FileAsset store(String orderId, MultipartFile upload) {
        if (upload.isEmpty() || upload.getSize() > MAX_BYTES) { throw invalid("图片不能为空，且不得超过5MiB"); }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { throw new IllegalStateException("Image writes require a transaction"); }
        byte[] normalized;
        String format;
        try (var input = new MemoryCacheImageInputStream(upload.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) { throw invalid("文件不是有效的JPEG或PNG图片"); }
            ImageReader reader = readers.next();
            try {
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) { throw invalid("仅支持JPEG和PNG图片"); }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096 || (long)width*height > 16_000_000) {
                    throw invalid("图片尺寸不得超过4096×4096及1600万像素");
                }
                var image = reader.read(0);
                try (var output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(image, format, output)) { throw invalid("图片无法解码"); }
                    normalized = output.toByteArray();
                } finally { image.flush(); }
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw invalid("图片损坏或无法读取"); }
        if (normalized.length > MAX_BYTES) { throw invalid("规范化后的图片超过5MiB，请压缩后重试"); }
        String id = UUID.randomUUID().toString();
        String key = id + (format.equals("png") ? ".png" : ".jpg");
        Path target = resolve(key);
        try {
            Files.createDirectories(root);
            Files.createFile(target);
        } catch (IOException ex) { throw unavailable(); }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { Files.deleteIfExists(target); }
                    catch (IOException ex) { LoggerFactory.getLogger(ImageStorage.class).error("Could not clean rolled-back attachment {}", id); }
                }
            }
        });
        try { Files.write(target, normalized, StandardOpenOption.WRITE); }
        catch (IOException ex) { throw unavailable(); }
        return new FileAsset(id, orderId, key, format.equals("png") ? "image/png" : "image/jpeg", normalized.length);
    }

    public byte[] read(FileAsset asset) {
        try { return Files.readAllBytes(resolve(asset.storageKey())); }
        catch (IOException ex) { throw unavailable(); }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !key.matches("[0-9a-f-]{36}\\.(png|jpg)")) { throw unavailable(); }
        return path;
    }
    private static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST,"INVALID_IMAGE",message); }
    private static ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"STORAGE_UNAVAILABLE","附件存储暂不可用"); }
}
