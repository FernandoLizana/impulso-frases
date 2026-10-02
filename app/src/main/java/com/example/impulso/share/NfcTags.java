package com.example.impulso.share;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.widget.Toast;

import com.example.impulso.R;

import java.nio.charset.StandardCharsets;

public final class NfcTags {
    private NfcTags() {
    }

    public static void write(Activity activity, String text) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        if (adapter == null) {
            Toast.makeText(activity, R.string.nfc_missing, Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(activity, R.string.nfc_ready, Toast.LENGTH_LONG).show();
        adapter.enableReaderMode(activity, tag -> writeTag(activity, adapter, tag, text),
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B, null);
    }

    private static void writeTag(Activity activity, NfcAdapter adapter, Tag tag, String text) {
        boolean ok = false;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                byte[] language = "es".getBytes(StandardCharsets.US_ASCII);
                byte[] body = (QrCodes.PREFIX + text).getBytes(StandardCharsets.UTF_8);
                byte[] payload = new byte[1 + language.length + body.length];
                payload[0] = (byte) language.length;
                System.arraycopy(language, 0, payload, 1, language.length);
                System.arraycopy(body, 0, payload, 1 + language.length, body.length);
                NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
                ndef.connect();
                ndef.writeNdefMessage(new NdefMessage(new NdefRecord[]{record}));
                ndef.close();
                ok = true;
            }
        } catch (Exception ignored) {
            ok = false;
        }
        boolean written = ok;
        activity.runOnUiThread(() -> {
            adapter.disableReaderMode(activity);
            Toast.makeText(activity, written ? R.string.nfc_written : R.string.nfc_failed, Toast.LENGTH_LONG).show();
        });
    }
}
