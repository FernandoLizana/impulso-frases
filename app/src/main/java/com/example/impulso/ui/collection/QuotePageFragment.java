package com.example.impulso.ui.collection;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.FavoriteEntity;
import com.example.impulso.data.db.HistoryEntity;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.databinding.FragmentQuotePageBinding;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.detail.QuoteDetailActivity;
import com.example.impulso.ui.explore.QuoteAdapter;

import java.util.ArrayList;
import java.util.List;

public class QuotePageFragment extends Fragment {
    public static final String FAVORITES = "favorites";
    public static final String MINE = "mine";
    public static final String RECENT = "recent";

    private FragmentQuotePageBinding binding;
    private QuoteAdapter adapter;
    private String mode = FAVORITES;

    public static QuotePageFragment create(String mode) {
        QuotePageFragment fragment = new QuotePageFragment();
        Bundle args = new Bundle();
        args.putString("mode", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuotePageBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            mode = getArguments().getString("mode", FAVORITES);
        }
        adapter = new QuoteAdapter(row -> {
            Intent intent = new Intent(requireContext(), QuoteDetailActivity.class);
            intent.putExtra(QuoteDetailActivity.EXTRA_ID, row.id);
            intent.putExtra(QuoteDetailActivity.EXTRA_SOURCE, row.source);
            startActivity(intent);
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.buttonCreate.setVisibility(MINE.equals(mode) ? View.VISIBLE : View.GONE);
        binding.buttonCreate.setOnClickListener(v -> QuoteEditor.show(requireActivity(), null, () -> {
        }));
        ImpulsoApplication app = ImpulsoApplication.from(requireContext());
        if (FAVORITES.equals(mode)) {
            app.userData().observeFavorites().observe(getViewLifecycleOwner(), this::showFavorites);
        } else if (MINE.equals(mode)) {
            app.userData().observeUserQuotes().observe(getViewLifecycleOwner(), this::showMine);
        } else {
            app.userData().observeHistory().observe(getViewLifecycleOwner(), this::showRecent);
        }
        return binding.getRoot();
    }

    private void showFavorites(List<FavoriteEntity> favorites) {
        ImpulsoApplication app = ImpulsoApplication.from(requireContext());
        String empty = getString(R.string.empty_favorites);
        AppDatabase.IO.execute(() -> {
            List<QuoteAdapter.Row> rows = new ArrayList<>();
            for (FavoriteEntity favorite : favorites) {
                rows.add(resolve(app, favorite.source, favorite.quoteId));
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> publish(rows, empty));
            }
        });
    }

    private void showMine(List<UserQuoteEntity> quotes) {
        List<QuoteAdapter.Row> rows = new ArrayList<>();
        for (UserQuoteEntity quote : quotes) {
            rows.add(new QuoteAdapter.Row(
                    quote.id,
                    QuoteRefs.USER,
                    quote.text,
                    getString(R.string.your_quote),
                    Labels.category(requireContext(), quote.category)
            ));
        }
        publish(rows, getString(R.string.empty_mine));
    }

    private void showRecent(List<HistoryEntity> history) {
        ImpulsoApplication app = ImpulsoApplication.from(requireContext());
        String empty = getString(R.string.empty_recent);
        AppDatabase.IO.execute(() -> {
            List<QuoteAdapter.Row> rows = new ArrayList<>();
            for (HistoryEntity item : history) {
                rows.add(resolve(app, item.source, item.quoteId));
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> publish(rows, empty));
            }
        });
    }

    private QuoteAdapter.Row resolve(ImpulsoApplication app, String source, String id) {
        if (QuoteRefs.USER.equals(source)) {
            UserQuoteEntity own = null;
            List<UserQuoteEntity> all = app.userData().userQuotesSync();
            for (UserQuoteEntity quote : all) {
                if (quote.id.equals(id)) {
                    own = quote;
                    break;
                }
            }
            if (own == null) {
                return new QuoteAdapter.Row(id, source, getString(R.string.deleted_quote), "", "");
            }
            return new QuoteAdapter.Row(id, source, own.text, getString(R.string.your_quote), Labels.category(requireContext(), own.category));
        }
        Quote quote = app.catalog().find(id);
        if (quote == null) {
            return new QuoteAdapter.Row(id, source, getString(R.string.deleted_quote), "", "");
        }
        return new QuoteAdapter.Row(id, source, quote.text, quote.author, Labels.category(requireContext(), quote.category));
    }

    private void publish(List<QuoteAdapter.Row> rows, String empty) {
        if (binding == null) {
            return;
        }
        adapter.submit(rows);
        binding.textEmpty.setText(empty);
        binding.textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
