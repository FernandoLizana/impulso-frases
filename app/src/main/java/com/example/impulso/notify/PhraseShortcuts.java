package com.example.impulso.notify;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.ui.detail.QuoteDetailActivity;

import java.util.ArrayList;
import java.util.List;

/** Atajos dinámicos para que la búsqueda del teléfono encuentre frases propias. */
public final class PhraseShortcuts {
    private PhraseShortcuts() {
    }

    public static void publish(Context context) {
        List<UserQuoteEntity> own = ImpulsoApplication.from(context).userData().userQuotesSync();
        List<ShortcutInfoCompat> shortcuts = new ArrayList<>();
        int count = 0;
        for (UserQuoteEntity quote : own) {
            if (quote == null || quote.text == null || quote.text.isEmpty()) {
                continue;
            }
            Intent open = new Intent(context, QuoteDetailActivity.class);
            open.setAction(Intent.ACTION_VIEW);
            open.putExtra(QuoteDetailActivity.EXTRA_ID, quote.id);
            open.putExtra(QuoteDetailActivity.EXTRA_SOURCE, QuoteRefs.USER);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            String label = quote.text.length() > 28 ? quote.text.substring(0, 28) + "…" : quote.text;
            shortcuts.add(new ShortcutInfoCompat.Builder(context, "own-" + quote.id)
                    .setShortLabel(label)
                    .setLongLabel(quote.text)
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_logo))
                    .setIntent(open)
                    .build());
            count++;
            if (count == 3) {
                break;
            }
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts);
    }
}
