package com.example.impulso.share;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.core.content.FileProvider;

import com.example.impulso.R;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ShareCardRenderer {
    private ShareCardRenderer() {
    }

    public static Bitmap render(Context context, String text, String author, boolean vertical) {
        int width = 1080;
        int height = vertical ? 1920 : 1080;
        int margin = 96;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.parseColor("#F8F6FC"));
        Paint gold = new Paint(Paint.ANTI_ALIAS_FLAG);
        gold.setColor(Color.parseColor("#C49A45"));
        canvas.drawRect(margin, margin, margin + 72, margin + 8, gold);

        int textWidth = width - margin * 2;
        int authorSpace = 180;
        int brandSpace = 160;
        int maxHeight = height - margin * 2 - 40 - authorSpace - brandSpace;
        TextPaint quotePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        quotePaint.setColor(Color.parseColor("#292334"));
        quotePaint.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        int size = vertical ? 64 : 52;
        StaticLayout layout = null;
        while (size >= 28) {
            quotePaint.setTextSize(size);
            layout = StaticLayout.Builder.obtain(text, 0, text.length(), quotePaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(8, 1f)
                    .build();
            if (layout.getHeight() <= maxHeight) {
                break;
            }
            size -= 2;
        }
        canvas.save();
        canvas.translate(margin, margin + 40);
        if (layout != null) {
            layout.draw(canvas);
        }
        canvas.restore();
        int quoteBottom = margin + 40 + (layout == null ? 0 : layout.getHeight());

        TextPaint authorPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        authorPaint.setColor(Color.parseColor("#292334"));
        authorPaint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        authorPaint.setTextSize(36);
        canvas.drawText(author, margin, quoteBottom + 72, authorPaint);

        TextPaint brand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        brand.setColor(Color.parseColor("#4E348F"));
        brand.setTextSize(32);
        brand.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        int logoSize = 96;
        int brandBaseline = height - margin;
        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_digital_rider);
        if (logo != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true);
            canvas.drawBitmap(scaled, margin, brandBaseline - logoSize + 8, null);
            if (scaled != logo) {
                scaled.recycle();
            }
        }
        canvas.drawText("Digital Rider", margin + logoSize + 24, brandBaseline - 28, brand);
        TextPaint seal = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        seal.setColor(Color.parseColor("#6E6578"));
        seal.setTextSize(22);
        canvas.drawText(context.getString(com.example.impulso.R.string.seal), margin, brandBaseline - logoSize - 12, seal);
        return bitmap;
    }

    public static File writePng(Context context, Bitmap bitmap) throws IOException {
        File dir = new File(context.getCacheDir(), "share");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("cache");
        }
        long cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L;
        File[] old = dir.listFiles();
        if (old != null) {
            for (File file : old) {
                if (file.lastModified() < cutoff) {
                    file.delete();
                }
            }
        }
        File out = new File(dir, "impulso-card-" + System.currentTimeMillis() + ".png");
        try (FileOutputStream stream = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        return out;
    }

    public static Uri uri(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }
}
