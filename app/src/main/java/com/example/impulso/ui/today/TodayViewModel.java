package com.example.impulso.ui.today;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.data.db.DailyStateEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.data.repo.UserDataRepository;
import com.example.impulso.domain.QuietHours;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.domain.QuoteSelector;
import com.example.impulso.domain.TensionQuote;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TodayViewModel extends AndroidViewModel {
    private static final String BROWSING = "browsing_id";
    private static final String RECORDED = "recorded_id";
    private final SavedStateHandle state;
    private final MutableLiveData<Ui> ui = new MutableLiveData<>();
    private int generation;
    private boolean pickAnother;

    public TodayViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        this.state = state;
    }

    public LiveData<Ui> ui() {
        return ui;
    }

    public void refresh() {
        boolean another = pickAnother;
        pickAnother = false;
        int gen = ++generation;
        ImpulsoApplication app = (ImpulsoApplication) getApplication();
        AppPreferences prefs = new AppPreferences(app);
        app.userData().io(() -> {
            Ui model = build(app, prefs, another);
            if (gen == generation) {
                ui.postValue(model);
            }
        });
    }

    public void showAnother() {
        pickAnother = true;
        refresh();
    }

    public void backToDaily() {
        state.remove(BROWSING);
        refresh();
    }

    public void setAction(String status) {
        String date = LocalDate.now(ZoneId.systemDefault()).toString();
        ((ImpulsoApplication) getApplication()).userData().setAction(date, status);
        refresh();
    }

    public void toggleFavorite() {
        Ui current = ui.getValue();
        if (current == null || current.cardId == null || current.cardId.isEmpty()) {
            return;
        }
        ((ImpulsoApplication) getApplication()).userData()
                .toggleFavorite(current.cardSource, current.cardId, ignored -> refresh());
    }

    private Ui build(ImpulsoApplication app, AppPreferences prefs, boolean another) {
        UserDataRepository repo = app.userData();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String date = today.toString();
        DailyStateEntity row = repo.findDailySync(date);
        boolean missingFromCatalog = row != null
                && row.quoteId != null
                && !row.quoteId.isEmpty()
                && app.catalog().find(row.quoteId) == null;
        if (row == null || row.quoteId == null || row.quoteId.isEmpty() || missingFromCatalog) {
            if (missingFromCatalog) {
                row.actionStatus = "none";
            }
            Quote selected = QuoteSelector.select(
                    app.catalog().quotes(),
                    prefs.goal(),
                    prefs.tone(),
                    repo.recentIdsSync(),
                    null,
                    today.toEpochDay(),
                    List.of(),
                    prefs.deck()
            );
            if (selected != null) {
                if (row == null) {
                    row = new DailyStateEntity();
                    row.date = date;
                    row.actionStatus = "none";
                }
                row.quoteId = selected.id;
                row.source = QuoteRefs.CATALOG;
                repo.saveDailySync(row);
            }
        }
        Quote daily = row == null ? null : app.catalog().find(row.quoteId);
        if (another) {
            Quote extra = QuoteSelector.select(
                    app.catalog().quotes(),
                    prefs.goal(),
                    prefs.tone(),
                    repo.recentIdsSync(),
                    daily == null ? null : daily.id,
                    System.nanoTime(),
                    List.of(),
                    prefs.deck()
            );
            if (extra != null) {
                state.set(BROWSING, extra.id);
            }
        }
        String browsingId = state.get(BROWSING);
        Quote card = browsingId == null ? daily : app.catalog().find(browsingId);
        if (browsingId != null && card == null) {
            state.remove(BROWSING);
            card = daily;
            browsingId = null;
        }
        Ui model = new Ui();
        model.browsing = browsingId != null;
        model.hasCard = card != null;
        if (card != null) {
            model.cardId = card.id;
            model.cardSource = QuoteRefs.CATALOG;
            model.cardText = card.text;
            model.cardAuthor = card.author;
            model.cardCategory = card.category;
            model.cardFavorite = repo.isFavoriteSync(QuoteRefs.CATALOG, card.id);
            String recorded = state.get(RECORDED);
            if (!card.id.equals(recorded)) {
                repo.recordView(QuoteRefs.CATALOG, card.id);
                state.set(RECORDED, card.id);
            }
        }
        model.hasDaily = daily != null;
        if (daily != null) {
            model.dailyReflection = daily.reflection;
            model.dailyMicro = daily.microAction;
        }
        model.actionStatus = row == null || row.actionStatus == null ? "none" : row.actionStatus;
        model.days = week(repo, today);
        model.cardEnglish = card == null || card.english == null ? "" : card.english;
        model.placeNote = row == null || row.placeNote == null ? "" : row.placeNote;
        QuietHours quiet = QuietHours.ofMinutes(prefs.quietEnabled(), prefs.quietStartMin(), prefs.quietEndMin());
        model.quietNow = quiet.contains(LocalTime.now());
        if (daily != null) {
            Quote tension = TensionQuote.pick(app.catalog().quotes(), daily, prefs.tone(), today.toEpochDay());
            if (tension != null) {
                model.tensionId = tension.id;
                model.tensionText = tension.text;
                model.tensionAuthor = tension.author;
            }
        }
        long anchorDay = prefs.anchorDay();
        if (anchorDay >= 0 && today.toEpochDay() - anchorDay < 7) {
            Quote anchor = app.catalog().find(prefs.anchorId());
            if (anchor != null && (daily == null || !anchor.id.equals(daily.id))) {
                model.showAnchor = true;
                model.anchorText = anchor.text;
                model.anchorAuthor = anchor.author;
            }
        }
        if (!date.equals(prefs.echoDate())) {
            DailyStateEntity past = repo.findDailySync(today.minusDays(7).toString());
            if (past != null && "done".equals(past.actionStatus)) {
                Quote echo = app.catalog().find(past.quoteId);
                if (echo != null) {
                    model.showEcho = true;
                    model.echoText = echo.text;
                    model.echoAuthor = echo.author;
                    prefs.setEchoDate(date);
                }
            }
        }
        return model;
    }

    public void setAnchorFromDaily() {
        Ui current = ui.getValue();
        if (current == null || !current.hasDaily) {
            return;
        }
        ImpulsoApplication app = (ImpulsoApplication) getApplication();
        String date = LocalDate.now(ZoneId.systemDefault()).toString();
        // The daily id is the persisted one, not the browsing card.
        app.userData().io(() -> {
            DailyStateEntity stored = app.userData().findDailySync(date);
            if (stored != null && stored.quoteId != null) {
                new AppPreferences(app).setAnchor(stored.quoteId, LocalDate.now(ZoneId.systemDefault()).toEpochDay());
            }
            refresh();
        });
    }

    public void setPlace(String note) {
        String date = LocalDate.now(ZoneId.systemDefault()).toString();
        ImpulsoApplication app = (ImpulsoApplication) getApplication();
        app.userData().io(() -> {
            DailyStateEntity stored = app.userData().findDailySync(date);
            if (stored != null) {
                stored.placeNote = note == null ? "" : note.trim();
                app.userData().saveDailySync(stored);
            }
        });
    }

    private List<DayRow> week(UserDataRepository repo, LocalDate today) {
        List<DailyStateEntity> rows = repo.daysSync(today.minusDays(6).toString(), today.toString());
        Map<String, String> byDate = new HashMap<>();
        for (DailyStateEntity row : rows) {
            byDate.put(row.date, row.actionStatus);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE d MMM", new Locale("es"));
        List<DayRow> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            DayRow row = new DayRow();
            row.label = date.format(formatter);
            String status = byDate.get(date.toString());
            row.status = status == null || status.isEmpty() ? "none" : status;
            days.add(row);
        }
        return days;
    }

    public static final class Ui {
        public boolean browsing;
        public boolean hasCard;
        public boolean hasDaily;
        public boolean cardFavorite;
        public String cardId = "";
        public String cardSource = QuoteRefs.CATALOG;
        public String cardText = "";
        public String cardAuthor = "";
        public String cardCategory = "";
        public String dailyReflection = "";
        public String dailyMicro = "";
        public String actionStatus = "none";
        public List<DayRow> days = List.of();
        public String cardEnglish = "";
        public String placeNote = "";
        public boolean quietNow;
        public boolean showAnchor;
        public String anchorText = "";
        public String anchorAuthor = "";
        public boolean showEcho;
        public String echoText = "";
        public String echoAuthor = "";
        public String tensionId = "";
        public String tensionText = "";
        public String tensionAuthor = "";
    }

    public static final class DayRow {
        public String label;
        public String status;
    }
}
