package com.example.impulso.ui.cards;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.domain.Decks;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.Tones;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.Themes;

import java.util.ArrayList;
import java.util.List;

public class CardsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);
        ViewPager2 pager = findViewById(R.id.card_pager);
        AppDatabase.IO.execute(() -> {
            AppPreferences prefs = new AppPreferences(this);
            List<Quote> cards = new ArrayList<>();
            boolean spiritual = Tones.ESPIRITUAL.equals(prefs.tone());
            for (Quote quote : ImpulsoApplication.from(this).catalog().quotes()) {
                if (quote == null) {
                    continue;
                }
                if (!Decks.ALL.equals(prefs.deck()) && !Decks.OWN.equals(prefs.deck())
                        && !Decks.contains(prefs.deck(), quote.category)) {
                    continue;
                }
                if (spiritual != Tones.ESPIRITUAL.equals(quote.tone)) {
                    continue;
                }
                cards.add(quote);
            }
            runOnUiThread(() -> pager.setAdapter(new Adapter(cards)));
        });
        findViewById(R.id.card_close).setOnClickListener(v -> finish());
    }

    private final class Adapter extends androidx.recyclerview.widget.RecyclerView.Adapter<Holder> {
        private final List<Quote> cards;

        private Adapter(List<Quote> cards) {
            this.cards = cards;
        }

        @Override
        public Holder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewPager2.LayoutParams(
                    ViewPager2.LayoutParams.MATCH_PARENT, ViewPager2.LayoutParams.MATCH_PARENT));
            int pad = (int) (24 * getResources().getDisplayMetrics().density);
            view.setPadding(pad, pad, pad, pad);
            view.setGravity(android.view.Gravity.CENTER);
            view.setTextAppearance(com.example.impulso.R.style.TextAppearance_Impulso_Quote);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            Quote quote = cards.get(position);
            holder.text.setText(quote.text + "\n\n" + quote.author + "\n" + Labels.category(CardsActivity.this, quote.category));
        }

        @Override
        public int getItemCount() {
            return cards.size();
        }
    }

    private static final class Holder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        private final TextView text;

        private Holder(TextView itemView) {
            super(itemView);
            this.text = itemView;
        }
    }
}
