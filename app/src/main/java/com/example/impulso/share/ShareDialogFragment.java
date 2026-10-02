package com.example.impulso.share;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.databinding.DialogShareBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;

public class ShareDialogFragment extends DialogFragment {
    private static final String TEXT = "text";
    private static final String AUTHOR = "author";
    private DialogShareBinding binding;
    private Bitmap preview;
    private boolean vertical;

    public static void show(FragmentManager manager, String text, String author) {
        ShareDialogFragment fragment = new ShareDialogFragment();
        Bundle args = new Bundle();
        args.putString(TEXT, text);
        args.putString(AUTHOR, author);
        fragment.setArguments(args);
        fragment.show(manager, "share");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogShareBinding.inflate(inflater, container, false);
        String text = requireArguments().getString(TEXT, "");
        String author = requireArguments().getString(AUTHOR, "");
        binding.buttonCopy.setOnClickListener(v -> copy(text, author));
        binding.buttonText.setOnClickListener(v -> shareText(text, author));
        binding.buttonSquare.setOnClickListener(v -> render(text, author, false));
        binding.buttonVertical.setOnClickListener(v -> render(text, author, true));
        binding.buttonSendImage.setOnClickListener(v -> shareImage(text, author));
        binding.buttonQr.setOnClickListener(v -> showQr(text));
        binding.buttonNfc.setOnClickListener(v -> NfcTags.write(requireActivity(), text));
        render(text, author, false);
        return binding.getRoot();
    }

    private void render(String text, String author, boolean asVertical) {
        vertical = asVertical;
        Context appContext = requireContext().getApplicationContext();
        AppDatabase.IO.execute(() -> {
            Bitmap bitmap = ShareCardRenderer.render(appContext, text, author, asVertical);
            if (!isAdded()) {
                bitmap.recycle();
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    bitmap.recycle();
                    return;
                }
                if (preview != null) {
                    preview.recycle();
                }
                preview = bitmap;
                binding.preview.setImageBitmap(bitmap);
            });
        });
    }

    private void showQr(String text) {
        try {
            Bitmap qr = QrCodes.encode(text);
            if (qr == null) {
                Snackbar.make(binding.getRoot(), R.string.qr_too_big, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (preview != null) {
                preview.recycle();
            }
            preview = qr;
            binding.preview.setImageBitmap(qr);
        } catch (Exception ignored) {
            Snackbar.make(binding.getRoot(), R.string.qr_too_big, Snackbar.LENGTH_LONG).show();
        }
    }

    private void copy(String text, String author) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("frase", text + "\n— " + author));
        }
        Snackbar.make(binding.getRoot(), R.string.copied, Snackbar.LENGTH_SHORT).show();
    }

    private void shareText(String text, String author) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text + "\n— " + author);
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser)));
    }

    private void shareImage(String text, String author) {
        AppDatabase.IO.execute(() -> {
            try {
                Context appContext = requireContext().getApplicationContext();
                Bitmap bitmap = preview != null ? preview : ShareCardRenderer.render(appContext, text, author, vertical);
                File file = ShareCardRenderer.writePng(requireContext().getApplicationContext(), bitmap);
                Uri uri = ShareCardRenderer.uri(requireContext(), file);
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("image/png");
                    send.putExtra(Intent.EXTRA_STREAM, uri);
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    send.setClipData(ClipData.newRawUri("card", uri));
                    startActivity(Intent.createChooser(send, getString(R.string.share_chooser)));
                });
            } catch (Exception ignored) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Snackbar.make(binding.getRoot(), R.string.tts_failed, Snackbar.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (preview != null) {
            binding.preview.setImageDrawable(null);
            preview.recycle();
            preview = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
