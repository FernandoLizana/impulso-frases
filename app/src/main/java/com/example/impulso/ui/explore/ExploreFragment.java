package com.example.impulso.ui.explore;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.databinding.FragmentExploreBinding;
import com.example.impulso.domain.Categories;
import com.example.impulso.domain.GoalToneMapping;
import com.example.impulso.domain.Goals;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.domain.Tones;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.detail.QuoteDetailActivity;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreFragment extends Fragment {
    private FragmentExploreBinding binding;
    private QuoteAdapter adapter;
    private String category = "";
    private String tone = "";
    private String goal = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExploreBinding.inflate(inflater, container, false);
        if (savedInstanceState != null) {
            category = savedInstanceState.getString("category", "");
            tone = savedInstanceState.getString("tone", "");
            goal = savedInstanceState.getString("goal", "");
        }
        adapter = new QuoteAdapter(row -> {
            Intent intent = new Intent(requireContext(), QuoteDetailActivity.class);
            intent.putExtra(QuoteDetailActivity.EXTRA_ID, row.id);
            intent.putExtra(QuoteDetailActivity.EXTRA_SOURCE, row.source);
            startActivity(intent);
        });
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        addChip("", getString(R.string.filter_all));
        for (String id : Categories.IDS) {
            addChip(id, Labels.category(requireContext(), id));
        }
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.buttonFilters.setOnClickListener(v -> showFilters());
        render();
        return binding.getRoot();
    }

    private void addChip(String id, String label) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(id.equals(category));
        chip.setEnsureMinTouchTargetSize(true);
        chip.setOnClickListener(v -> {
            category = id;
            for (int i = 0; i < binding.chipsCategory.getChildCount(); i++) {
                View child = binding.chipsCategory.getChildAt(i);
                if (child instanceof Chip other) {
                    other.setChecked(other == chip);
                }
            }
            render();
        });
        binding.chipsCategory.addView(chip);
    }

    private void showFilters() {
        RadioGroup tones = new RadioGroup(requireContext());
        RadioGroup goals = new RadioGroup(requireContext());
        addRadio(tones, "", getString(R.string.filter_all), tone);
        for (String id : Tones.IDS) {
            addRadio(tones, id, Labels.tone(requireContext(), id), tone);
        }
        addRadio(goals, "", getString(R.string.filter_all), goal);
        for (String id : Goals.IDS) {
            addRadio(goals, id, Labels.goal(requireContext(), id), goal);
        }
        android.widget.LinearLayout box = new android.widget.LinearLayout(requireContext());
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(48, 24, 48, 0);
        box.addView(goals);
        box.addView(tones);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.filters_title)
                .setView(box)
                .setPositiveButton(R.string.apply, (d, w) -> {
                    goal = selected(goals);
                    tone = selected(tones);
                    render();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addRadio(RadioGroup group, String id, String label, String current) {
        android.widget.RadioButton button = new android.widget.RadioButton(requireContext());
        button.setText(label);
        button.setTag(id);
        button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
        group.addView(button);
        if (id.equals(current)) {
            button.setChecked(true);
        }
    }

    private String selected(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) {
            return "";
        }
        View view = group.findViewById(id);
        return view == null || view.getTag() == null ? "" : String.valueOf(view.getTag());
    }

    private void render() {
        if (binding == null) {
            return;
        }
        String query = String.valueOf(binding.inputSearch.getText()).trim().toLowerCase(Locale.ROOT);
        List<String> preferred = GoalToneMapping.categoriesFor(goal);
        List<QuoteAdapter.Row> rows = new ArrayList<>();
        for (Quote quote : ImpulsoApplication.from(requireContext()).catalog().quotes()) {
            if (!category.isEmpty() && !category.equals(quote.category)) {
                continue;
            }
            if (!tone.isEmpty() && !tone.equals(quote.tone)) {
                continue;
            }
            if (!preferred.isEmpty() && (quote.category == null || !preferred.contains(quote.category))) {
                continue;
            }
            String haystack = (quote.text + " " + quote.author).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !haystack.contains(query)) {
                continue;
            }
            rows.add(new QuoteAdapter.Row(
                    quote.id,
                    QuoteRefs.CATALOG,
                    quote.text,
                    quote.author,
                    Labels.category(requireContext(), quote.category)
            ));
        }
        adapter.submit(rows);
        binding.textEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("category", category);
        outState.putString("tone", tone);
        outState.putString("goal", goal);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
