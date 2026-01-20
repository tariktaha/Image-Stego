package org.example.stego;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;

public class StegoQualityControl {

    @FunctionalInterface
    public interface EmbedFn {
        BufferedImage embed(BufferedImage cover, String message) throws Exception;
    }

    @FunctionalInterface
    public interface ExtractFn {
        String extract(BufferedImage stego) throws Exception;
    }

    public static boolean embedWriteReadExtractOrMoveFailed(
            BufferedImage cover,
            String message,
            String format,
            Path validOutPath,
            Path failedDir,
            EmbedFn embedFn,
            ExtractFn extractFn
    ) throws Exception {

        Files.createDirectories(validOutPath.getParent());
        Files.createDirectories(failedDir);

        // 1) embed
        BufferedImage stego = embedFn.embed(cover, message);

        // 2) write
        ImageIO.write(stego, format, validOutPath.toFile());

        // 3) reread
        BufferedImage reread = ImageIO.read(validOutPath.toFile());
        if (reread == null) {
            moveToFailed(validOutPath, failedDir, "unreadable");
            return false;
        }

        // 4) extract + verify
        try {
            String extracted = extractFn.extract(reread);
            if (!message.equals(extracted)) {
                moveToFailed(validOutPath, failedDir, "mismatch");
                return false;
            }
            return true;
        } catch (Exception ex) {
            moveToFailed(validOutPath, failedDir, "extract_fail");
            return false;
        }
    }

    private static void moveToFailed(Path validOutPath, Path failedDir, String reason) throws Exception {
        String fileName = validOutPath.getFileName().toString();
        Path failedPath = failedDir.resolve(reason + "_" + fileName);
        Files.move(validOutPath, failedPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
