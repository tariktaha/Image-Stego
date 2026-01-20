package org.example;

import org.example.stego.StegoQualityControl;
import org.example.stego.dct.DctSteganography;
import org.example.stego.dwt.DwtSteganography;
import org.example.stego.lsb.LsbExtractor;
import org.example.stego.lsb.LsbSteganography;
import org.example.stego.lsb.LsbMatchingSteganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {

        try {
            // =========================
            // LOG: konsol + dosya
            // =========================
            Path logDir = Path.of("output_logs");
            Files.createDirectories(logDir);

            Path logPath = logDir.resolve("run.log");
            PrintStream fileOut = new PrintStream(
                    new FileOutputStream(logPath.toFile(), true),
                    true,
                    StandardCharsets.UTF_8
            );

            PrintStream consoleOut = System.out;

            // Hem konsola hem dosyaya yazan "tee"
            PrintStream tee = new PrintStream(consoleOut) {
                @Override public void println(String x) {
                    consoleOut.println(x);
                    fileOut.println(x);
                }
                @Override public void print(String s) {
                    consoleOut.print(s);
                    fileOut.print(s);
                }
            };

            System.setOut(tee);
            System.setErr(tee);

            System.out.println("\n===== RUN START =====");
            System.out.println("Log file: " + logPath.toAbsolutePath());

            Path coverDir = Path.of("input/cover");

            // ✅ FINAL dataset (sadece 4 yöntemde de VALID olanlar)
            Path outCoverDir = Path.of("output/cover");
            Path outLsbDir = Path.of("output/lsb");
            Path outLsbmDir = Path.of("output/lsb_matching");
            Path outDctDir = Path.of("output/dct");
            Path outDwtDir = Path.of("output/dwt");

            Files.createDirectories(outCoverDir);
            Files.createDirectories(outLsbDir);
            Files.createDirectories(outLsbmDir);
            Files.createDirectories(outDctDir);
            Files.createDirectories(outDwtDir);

            // ✅ tmp klasörü (önce buraya yazıp QC yapacağız)
            Path tmpRoot = Path.of("output/_tmp");
            Path tmpLsbDir = tmpRoot.resolve("lsb");
            Path tmpLsbmDir = tmpRoot.resolve("lsb_matching");
            Path tmpDctDir = tmpRoot.resolve("dct");
            Path tmpDwtDir = tmpRoot.resolve("dwt");

            Files.createDirectories(tmpLsbDir);
            Files.createDirectories(tmpLsbmDir);
            Files.createDirectories(tmpDctDir);
            Files.createDirectories(tmpDwtDir);

            // ✅ failed klasörleri
            Path failedLsbDir = Path.of("output_failed/lsb");
            Path failedLsbmDir = Path.of("output_failed/lsb_matching");
            Path failedDctDir = Path.of("output_failed/dct");
            Path failedDwtDir = Path.of("output_failed/dwt");

            Files.createDirectories(failedLsbDir);
            Files.createDirectories(failedLsbmDir);
            Files.createDirectories(failedDctDir);
            Files.createDirectories(failedDwtDir);

            int targetCommon = 400; // hedef: 4 yöntem de başarılı 400 cover

            AtomicInteger scanned = new AtomicInteger(0);
            AtomicInteger commonCount = new AtomicInteger(0);

            // =========================
            // FAIL sayaçları
            // =========================
            AtomicInteger lsbFail = new AtomicInteger(0);
            AtomicInteger lsbmFail = new AtomicInteger(0);
            AtomicInteger dctFail = new AtomicInteger(0);
            AtomicInteger dwtFail = new AtomicInteger(0);

            // =========================
            // RANDOM DOSYA SIRASI (her çalıştırmada farklı)
            // =========================
            List<Path> coverFiles = new ArrayList<>();
            try (Stream<Path> paths = Files.list(coverDir)) {
                paths.filter(Main::isImageFile).forEach(coverFiles::add);
            }

            // Her çalıştırmada farklı seed: nanoTime
            Collections.shuffle(coverFiles, new Random(System.nanoTime()));

            for (Path p : coverFiles) {

                if (commonCount.get() >= targetCommon) break;

                int idx = scanned.incrementAndGet();
                System.out.println("\n[" + idx + "] İşleniyor: " + p);

                BufferedImage cover;
                try {
                    cover = ImageIO.read(p.toFile());
                    if (cover == null) {
                        System.err.println("Görüntü okunamadı, atlanıyor: " + p);
                        continue;
                    }
                } catch (Exception e) {
                    System.err.println("Görüntü okunurken hata, atlanıyor: " + p);
                    continue;
                }

                String fileName = p.getFileName().toString();
                String baseName = fileName.replaceAll("\\.[^.]+$", "");

                // Mesajlar
                String lsbMsg = "Bu bir LSB test mesajıdır!";
                String lsbMatchMsg = "Bu bir LSB Matching test mesajıdır!";
                String dctMsg = "Bu bir DCT tabanlı stego testidir!";
                String dwtMsg = "Bu bir DWT (Haar) tabanlı stego testidir!";

                // ✅ önce TMP dosya yolları
                Path tmpLsb = tmpLsbDir.resolve(baseName + "_lsb.png");
                Path tmpLsbm = tmpLsbmDir.resolve(baseName + "_lsbm.png");
                Path tmpDct = tmpDctDir.resolve(baseName + "_dct.png");
                Path tmpDwt = tmpDwtDir.resolve(baseName + "_dwt.png");

                boolean okLsb = runQcBool(
                        "LSB",
                        cover, lsbMsg,
                        tmpLsb,
                        failedLsbDir,
                        LsbSteganography::embedMessage,
                        LsbExtractor::extractMessage
                );

                boolean okLsbm = runQcBool(
                        "LSB-M",
                        cover, lsbMatchMsg,
                        tmpLsbm,
                        failedLsbmDir,
                        LsbMatchingSteganography::embedMessageMatching,
                        LsbExtractor::extractMessage
                );

                boolean okDct = runQcBool(
                        "DCT",
                        cover, dctMsg,
                        tmpDct,
                        failedDctDir,
                        DctSteganography::embedMessageDct,
                        DctSteganography::extractMessageDct
                );

                boolean okDwt = runQcBool(
                        "DWT",
                        cover, dwtMsg,
                        tmpDwt,
                        failedDwtDir,
                        DwtSteganography::embedMessageDwt,
                        DwtSteganography::extractMessageDwt
                );

                // ✅ fail sayaçları (hangi yöntem fail oldu?)
                if (!okLsb)  lsbFail.incrementAndGet();
                if (!okLsbm) lsbmFail.incrementAndGet();
                if (!okDct)  dctFail.incrementAndGet();
                if (!okDwt)  dwtFail.incrementAndGet();

                boolean allOk = okLsb && okLsbm && okDct && okDwt;

                if (allOk) {
                    int k = commonCount.incrementAndGet();

                    // ✅ cover’ı output/cover içine PNG olarak yaz
                    Path outCoverPath = outCoverDir.resolve(baseName + ".png");
                    ImageIO.write(cover, "PNG", outCoverPath.toFile());

                    // ✅ tmp'den final output klasörlerine taşı
                    moveReplace(tmpLsb, outLsbDir.resolve(baseName + "_lsb.png"));
                    moveReplace(tmpLsbm, outLsbmDir.resolve(baseName + "_lsbm.png"));
                    moveReplace(tmpDct, outDctDir.resolve(baseName + "_dct.png"));
                    moveReplace(tmpDwt, outDwtDir.resolve(baseName + "_dwt.png"));

                    System.out.println("  ✅ COMMON VALID [" + k + "/" + targetCommon + "] -> " + baseName);

                } else {
                    // ✅ tmp'de kalanları sil (QC başarısızsa zaten failed'e taşınmış olabilir)
                    deleteIfExists(tmpLsb);
                    deleteIfExists(tmpLsbm);
                    deleteIfExists(tmpDct);
                    deleteIfExists(tmpDwt);

                    System.out.println("  ❌ Bu cover dataset'e alınmadı (4 yöntemden biri FAIL).");
                }
            }

            // =========================
            // ÖZET RAPOR
            // =========================
            System.out.println("\n=== BİTTİ ===");
            System.out.println("Taranan cover sayısı: " + scanned.get());
            System.out.println("Dataset'e giren (4 yöntem VALID) sayısı: " + commonCount.get());
            System.out.println("Filtered out: " + (scanned.get() - commonCount.get()));

            System.out.println("\nFail counts by method:");
            System.out.println("LSB   fail: " + lsbFail.get());
            System.out.println("LSB-M fail: " + lsbmFail.get());
            System.out.println("DCT   fail: " + dctFail.get());
            System.out.println("DWT   fail: " + dwtFail.get());

            System.out.println("\noutput/cover ve output/* klasörleri sadece VALID ortak set olacak.");
            System.out.println("output_failed/* altına taşınanlar eğitim datasetine dahil edilmemeli.");

            if (commonCount.get() < targetCommon) {
                System.out.println("\n⚠️ UYARI: Hedef 400’e ulaşılamadı.");
                System.out.println("Bu normal olabilir: DCT/DWT bazı cover’larda stabil çalışmayabilir.");
                System.out.println("Çözüm: Daha fazla cover ekle veya DCT/DWT parametrelerini (DELTA/Q vb.) ayarla.");
            }

            System.out.println("===== RUN END =====\n");

            // log dosyasını kapat
            fileOut.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean runQcBool(
            String tag,
            BufferedImage cover,
            String msg,
            Path validOut,
            Path failedDir,
            StegoQualityControl.EmbedFn embedFn,
            StegoQualityControl.ExtractFn extractFn
    ) {
        try {
            boolean ok = StegoQualityControl.embedWriteReadExtractOrMoveFailed(
                    cover, msg, "PNG", validOut, failedDir, embedFn, extractFn
            );
            if (ok) {
                System.out.println("  [" + tag + "] -> (VALID)");
            } else {
                System.err.println("  !!! [" + tag + "] failed -> moved to " + failedDir);
            }
            return ok;
        } catch (Exception e) {
            System.err.println("  !!! [" + tag + "] hata: " + e.getMessage());
            return false;
        }
    }

    private static boolean isImageFile(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static void moveReplace(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void deleteIfExists(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignore) {
        }
    }
}
