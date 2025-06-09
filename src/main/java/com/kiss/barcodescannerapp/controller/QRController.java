package com.kiss.barcodescannerapp.controller;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import org.imgscalr.Scalr;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class QRController {

    @PostMapping("/barcode-upload")
    public ResponseEntity<?> uploadQrImage(@RequestParam("qrImage") MultipartFile file) {
        try {
            BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
            BufferedImage scaledImage = Scalr.resize(bufferedImage, 600); // resize nếu ảnh quá to

            LuminanceSource source = new BufferedImageLuminanceSource(scaledImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Cấu hình đọc nhiều loại mã
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Arrays.asList(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.ITF,
                    BarcodeFormat.PDF_417,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC
            ));

            MultiFormatReader multiFormatReader = new MultiFormatReader();
            multiFormatReader.setHints(hints);

            GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(multiFormatReader);
            Result[] results = reader.decodeMultiple(bitmap, hints);

            if (results == null || results.length == 0) {
                return ResponseEntity.badRequest().body("Không tìm thấy mã vạch trong ảnh");
            }

            List<Map<String, String>> codes = Arrays.stream(results).map(r -> {
                Map<String, String> m = new HashMap<>();
                m.put("text", r.getText());
                m.put("format", r.getBarcodeFormat().toString());
                return m;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("count", codes.size());
            response.put("codes", codes);

            return ResponseEntity.ok(response);

        } catch (NotFoundException e) {
            return ResponseEntity.badRequest().body("Không tìm thấy mã vạch trong ảnh");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý ảnh");
        }
    }
}