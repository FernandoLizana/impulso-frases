package com.example.impulso.ui.collection;

import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.databinding.DialogQuoteBinding;
import com.example.impulso.domain.Categories;
import com.example.impulso.domain.UserQuoteRules;
import com.example.impulso.share.ShareDialogFragment;
import com.example.impulso.ui.Labels;

public final class QuoteEditor {
    private QuoteEditor() {
    }

    public static void show(FragmentActivity activity, UserQuoteEntity existing, Runnable onChanged) {
        DialogQuoteBinding binding = DialogQuoteBinding.inflate(LayoutInflater.from(activity));
        String[] ids = new String[Categories.IDS.length + 1];
        String[] labels = new String[ids.length];
        ids[0] = "";
        labels[0] = activity.getString(R.string.category_none);
        for (int i = 0; i < Categories.IDS.length; i++) {
            ids[i + 1] = Categories.IDS[i];
            labels[i + 1] = Labels.category(activity, Categories.IDS[i]);
        }
        binding.spinnerCategory.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels));
        if (existing != null) {
            binding.inputText.setText(existing.text);
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(existing.category)) {
                    binding.spinnerCategory.setSelection(i);
                }
            }
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(existing == null ? R.string.add_quote : R.string.edit_quote)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        if (existing != null) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.delete_quote), (d, w) -> confirmDelete(activity, existing.id, onChanged));
        } else {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.share_image), (d, w) -> {
            });
        }
        dialog.setOnShowListener(shown -> {
            if (existing == null && dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    String text = UserQuoteRules.normalize(String.valueOf(binding.inputText.getText()));
                    if (!UserQuoteRules.isValid(text)) {
                        Toast.makeText(activity, R.string.quote_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ShareDialogFragment.show(activity.getSupportFragmentManager(), text, activity.getString(R.string.your_quote));
                });
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = UserQuoteRules.normalize(String.valueOf(binding.inputText.getText()));
            if (!UserQuoteRules.isValid(text)) {
                Toast.makeText(activity, R.string.quote_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            String category = ids[binding.spinnerCategory.getSelectedItemPosition()];
            String id = existing == null ? null : existing.id;
            AppDatabase.IO.execute(() -> {
                ImpulsoApplication.from(activity).userData().saveUserQuoteSync(id, text, category);
                activity.runOnUiThread(() -> {
                    dialog.dismiss();
                    if (onChanged != null) {
                        onChanged.run();
                    }
                });
            });
            });
        });
        dialog.show();
    }

    private static void confirmDelete(FragmentActivity activity, String id, Runnable onChanged) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.delete_confirm)
                .setMessage(R.string.delete_message)
                .setPositiveButton(R.string.delete_all_confirm, (d, w) ->
                        ImpulsoApplication.from(activity).userData().deleteUserQuote(id, () -> activity.runOnUiThread(() -> {
                            if (onChanged != null) {
                                onChanged.run();
                            }
                        })))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
