package com.example.studentqrscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import androidx.core.content.ContextCompat;

import com.example.studentqrscanner.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Generator za custom QR kodove sa stilovima, bojama i logotipima
 */
public class CustomQrGenerator {

    public enum QrStyle {
        // Student stilovi - Plava kao prva (default)
        STUDENT_BLUE("Plava", "#1976D2", "#E3F2FD", "#2196F3", true),
        STUDENT_PURPLE("Ljubičasta", "#7B1FA2", "#F3E5F5", "#9C27B0", true),
        STUDENT_PINK("Roze", "#C2185B", "#FCE4EC", "#E91E63", true),
        STUDENT_INDIGO("Indigo", "#303F9F", "#E8EAF6", "#3F51B5", true),
        STUDENT_TEAL("Teal", "#00796B", "#E0F2F1", "#009688", true),
        STUDENT_RED("Crvena", "#C62828", "#FFEBEE", "#F44336", true),

        // Profesor stilovi - Plava kao prva (default)
        LECTURE_BLUE("Plava", "#1976D2", "#E3F2FD", "#2196F3", false),
        LECTURE_GREEN("Zelena", "#388E3C", "#E8F5E9", "#4CAF50", false),
        LECTURE_ORANGE("Narančasta", "#F57C00", "#FFF3E0", "#FF9800", false),
        LECTURE_CYAN("Cijan", "#0097A7", "#E0F7FA", "#00BCD4", false),
        LECTURE_DEEP_PURPLE("Tamno ljubičasta", "#512DA8", "#EDE7F6", "#673AB7", false),

        DEFAULT_BLACK("Crna", "#000000", "#FFFFFF", "#424242", true);

        private final String displayName;
        private final String foreground;
        private final String background;
        private final String border;
        private final boolean forStudent;

        QrStyle(String displayName, String foreground, String background, String border, boolean forStudent) {
            this.displayName = displayName;
            this.foreground = foreground;
            this.background = background;
            this.border = border;
            this.forStudent = forStudent;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getForegroundColor() {
            return Color.parseColor(foreground);
        }

        public int getBackgroundColor() {
            return Color.parseColor(background);
        }

        public int getBorderColor() {
            return Color.parseColor(border);
        }

        public boolean isForStudent() {
            return forStudent;
        }

        public static QrStyle[] getStudentStyles() {
            return new QrStyle[]{
                    STUDENT_BLUE, STUDENT_PURPLE, STUDENT_PINK,
                    STUDENT_INDIGO, STUDENT_TEAL, STUDENT_RED,
                    DEFAULT_BLACK
            };
        }

        public static QrStyle[] getProfessorStyles() {
            return new QrStyle[]{
                    LECTURE_BLUE, LECTURE_GREEN, LECTURE_ORANGE,
                    LECTURE_CYAN, LECTURE_DEEP_PURPLE, DEFAULT_BLACK
            };
        }
    }

    private final Context context;
    private static final int QR_SIZE = 600;
    private static final int LOGO_SIZE = 100;
    private static final int BORDER_SIZE = 80;
    private static final int CORNER_RADIUS = 30;

    public CustomQrGenerator(Context context) {
        this.context = context;
    }

    /**
     * Generiše custom QR kod sa odabranim stilom
     */
    public Bitmap generateStyledQr(String content, QrStyle style, String labelText) {
        try {
            // 1. Generiši osnovni QR kod
            Bitmap qrBitmap = generateBasicQr(content, style);

            // 2. Dodaj logo u centar
            Bitmap qrWithLogo = addLogoToCenter(qrBitmap, style);

            // 3. Dodaj border i label
            Bitmap finalBitmap = addBorderAndLabel(qrWithLogo, style, labelText);

            return finalBitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generiše osnovni QR kod sa custom bojama
     */
    private Bitmap generateBasicQr(String content, QrStyle style) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Dobavi boje za odabrani stil direktno iz enum-a
        int foregroundColor = style.getForegroundColor();
        int backgroundColor = style.getBackgroundColor();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? foregroundColor : backgroundColor);
            }
        }

        return bitmap;
    }

    /**
     * Dodaje logo u centar QR koda
     */
    private Bitmap addLogoToCenter(Bitmap qrBitmap, QrStyle style) {
        Bitmap result = qrBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        // Kreiraj logo
        Bitmap logo = createStyledLogo(style);

        if (logo != null) {
            // Pozicioniraj logo u centar
            int left = (qrBitmap.getWidth() - LOGO_SIZE) / 2;
            int top = (qrBitmap.getHeight() - LOGO_SIZE) / 2;

            // Nacrtaj bijeli background za logo (za bolju vidljivost)
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.WHITE);
            bgPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(
                    new RectF(left - 10, top - 10, left + LOGO_SIZE + 10, top + LOGO_SIZE + 10),
                    15, 15, bgPaint
            );

            // Nacrtaj logo
            Rect destRect = new Rect(left, top, left + LOGO_SIZE, top + LOGO_SIZE);
            canvas.drawBitmap(logo, null, destRect, null);
        }

        return result;
    }

    /**
     * Dodaje border i labelu oko QR koda
     */
    private Bitmap addBorderAndLabel(Bitmap qrBitmap, QrStyle style, String labelText) {
        int totalWidth = qrBitmap.getWidth() + (BORDER_SIZE * 2);
        int totalHeight = qrBitmap.getHeight() + (BORDER_SIZE * 2) + 120; // Extra za labelu

        Bitmap result = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // Background sa zaobljenim uglovima
        Paint bgPaint = new Paint();
        bgPaint.setColor(style.getBorderColor());
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setAntiAlias(true);

        RectF bgRect = new RectF(0, 0, totalWidth, totalHeight);
        canvas.drawRoundRect(bgRect, CORNER_RADIUS, CORNER_RADIUS, bgPaint);

        // Nacrtaj QR kod
        canvas.drawBitmap(qrBitmap, BORDER_SIZE, BORDER_SIZE, null);

        // Nacrtaj labelu na dnu
        if (labelText != null && !labelText.isEmpty()) {
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(42);
            textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.CENTER);

            float textY = qrBitmap.getHeight() + BORDER_SIZE + 80;
            canvas.drawText(labelText, totalWidth / 2f, textY, textPaint);
        }

        return result;
    }

    /**
     * Kreira stilizovani logo za centar QR koda
     */
    private Bitmap createStyledLogo(QrStyle style) {
        Bitmap logo = Bitmap.createBitmap(LOGO_SIZE, LOGO_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(logo);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        if (style.isForStudent()) {
            // Ikona studenta (glava + knjiga)
            paint.setColor(style.getBorderColor());
            canvas.drawCircle(LOGO_SIZE / 2f, LOGO_SIZE / 3f, 25, paint);
            canvas.drawRoundRect(
                    new RectF(LOGO_SIZE / 4f, LOGO_SIZE / 2f, 3 * LOGO_SIZE / 4f, 5 * LOGO_SIZE / 6f),
                    5, 5, paint
            );
        } else {
            // Ikona predavanja (dokument sa linijama)
            paint.setColor(style.getBorderColor());

            // Vanjski okvir
            canvas.drawRoundRect(
                    new RectF(15, 15, LOGO_SIZE - 15, LOGO_SIZE - 15),
                    8, 8, paint
            );

            // Linije za tekst
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(4);
            canvas.drawLine(25, 35, LOGO_SIZE - 25, 35, paint);
            canvas.drawLine(25, 50, LOGO_SIZE - 25, 50, paint);
            canvas.drawLine(25, 65, LOGO_SIZE - 25, 65, paint);
        }

        return logo;
    }

    /**
     * Helper metoda za brzo generisanje QR koda za studenta
     */
    public static Bitmap generateStudentQr(Context context, String studentId, String index, String name) {
        String content = "studentId=" + studentId + "&index=" + index + "&name=" + name;
        CustomQrGenerator generator = new CustomQrGenerator(context);
        return generator.generateStyledQr(content, QrStyle.STUDENT_BLUE, "Student: " + index);
    }

    /**
     * Helper metoda za brzo generisanje QR koda za predavanje
     */
    public static Bitmap generateLectureQr(Context context, String predavanjeId, String naslov) {
        String content = "predavanjeId=" + predavanjeId;
        CustomQrGenerator generator = new CustomQrGenerator(context);
        return generator.generateStyledQr(content, QrStyle.LECTURE_BLUE, naslov);
    }
}
