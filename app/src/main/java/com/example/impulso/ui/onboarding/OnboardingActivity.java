package com.example.impulso.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.impulso.R;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.ActivityOnboardingBinding;
import com.example.impulso.domain.Goals;
import com.example.impulso.domain.Tones;
import com.example.impulso.domain.UserQuoteRules;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.SystemBarPadding;
import com.example.impulso.ui.detail.QuoteDetailActivity;
import com.example.impulso.ui.main.MainActivity;
import com.example.impulso.ui.welcome.WelcomeActivity;

public class OnboardingActivity extends AppCompatActivity {
    private ActivityOnboardingBinding binding;
    private AppPreferences prefs;
    private int step;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.impulso.ui.Themes.apply(this);
        super.onCreate(savedInstanceState);
        prefs = new AppPreferences(this);
        if (prefs.isOnboardingComplete()) {
            openMain();
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarPadding.apply(binding.getRoot());
        fillGroup(binding.groupGoal, Goals.IDS, true);
        fillGroup(binding.groupTone, Tones.IDS, false);
        step = prefs.onboardingStep();
        binding.inputName.setText(prefs.draftName());
        check(binding.groupGoal, prefs.draftGoal());
        check(binding.groupTone, prefs.draftTone());
        binding.buttonBack.setOnClickListener(v -> back());
        binding.buttonNext.setOnClickListener(v -> next());
        showStep();
    }

    private void fillGroup(RadioGroup group, String[] ids, boolean goals) {
        for (String id : ids) {
            RadioButton button = new RadioButton(this);
            button.setText(goals ? Labels.goal(this, id) : Labels.tone(this, id));
            button.setTag(id);
            button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
            group.addView(button);
        }
    }

    private void check(RadioGroup group, String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (id.equals(child.getTag())) {
                group.check(child.getId());
            }
        }
    }

    private void showStep() {
        binding.textStep.setText(getString(R.string.onboarding_step, step + 1));
        binding.nameLayout.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        binding.textNameHelp.setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        binding.groupGoal.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        binding.groupTone.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (step == 0) {
            binding.textTitle.setText(R.string.onboarding_name_title);
        } else if (step == 1) {
            binding.textTitle.setText(R.string.question_goal);
        } else {
            binding.textTitle.setText(R.string.question_tone);
        }
        binding.buttonNext.setText(step == 2 ? R.string.onboarding_finish : R.string.onboarding_next);
        binding.buttonNext.setOnClickListener(v -> next());
        persistDraft();
    }

    private void advanceFromName() {
        step = 1;
        showStep();
    }

    private void next() {
        if (step == 0) {
            advanceFromName();
            return;
        }
        if (step == 1) {
            if (selected(binding.groupGoal) == null) {
                Toast.makeText(this, R.string.onboarding_pick, Toast.LENGTH_SHORT).show();
                return;
            }
            step = 2;
            showStep();
            return;
        }
        String tone = selected(binding.groupTone);
        String goal = selected(binding.groupGoal);
        if (tone == null || goal == null) {
            Toast.makeText(this, R.string.onboarding_pick, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = UserQuoteRules.normalize(String.valueOf(binding.inputName.getText()));
        prefs.completeOnboarding(name, goal, tone);
        openMain();
    }

    private void back() {
        persistDraft();
        if (step == 0) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        step--;
        showStep();
    }

    private void persistDraft() {
        if (binding == null) {
            return;
        }
        prefs.saveDraft(
                UserQuoteRules.normalize(String.valueOf(binding.inputName.getText())),
                selected(binding.groupGoal) == null ? "" : selected(binding.groupGoal),
                selected(binding.groupTone) == null ? "" : selected(binding.groupTone),
                step
        );
    }

    private String selected(RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) {
            return null;
        }
        View view = group.findViewById(id);
        return view == null ? null : String.valueOf(view.getTag());
    }

    private void openMain() {
        String pendingId = prefs.pendingQuoteId();
        String pendingSource = prefs.pendingQuoteSource();
        prefs.clearPendingQuote();
        startActivity(new Intent(this, MainActivity.class));
        if (pendingId != null && !pendingId.isEmpty()) {
            Intent detail = new Intent(this, QuoteDetailActivity.class);
            detail.putExtra(QuoteDetailActivity.EXTRA_ID, pendingId);
            detail.putExtra(QuoteDetailActivity.EXTRA_SOURCE, pendingSource);
            startActivity(detail);
        }
        finish();
    }

    @Override
    protected void onPause() {
        persistDraft();
        super.onPause();
    }
}
