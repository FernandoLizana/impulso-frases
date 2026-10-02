package com.example.impulso.ui.explore;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.impulso.databinding.ItemQuoteBinding;

import java.util.ArrayList;
import java.util.List;

public class QuoteAdapter extends RecyclerView.Adapter<QuoteAdapter.Holder> {
    public static final class Row {
        public final String id;
        public final String source;
        public final String text;
        public final String author;
        public final String category;

        public Row(String id, String source, String text, String author, String category) {
            this.id = id;
            this.source = source;
            this.text = text;
            this.author = author;
            this.category = category;
        }
    }

    public interface OnOpen {
        void open(Row row);
    }

    private final OnOpen onOpen;
    private final List<Row> rows = new ArrayList<>();

    public QuoteAdapter(OnOpen onOpen) {
        this.onOpen = onOpen;
    }

    public void submit(List<Row> next) {
        rows.clear();
        rows.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemQuoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = rows.get(position);
        holder.binding.textCategory.setText(row.category);
        holder.binding.textQuote.setText(row.text);
        holder.binding.textAuthor.setText(row.author);
        holder.binding.getRoot().setOnClickListener(v -> onOpen.open(row));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemQuoteBinding binding;

        Holder(ItemQuoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
