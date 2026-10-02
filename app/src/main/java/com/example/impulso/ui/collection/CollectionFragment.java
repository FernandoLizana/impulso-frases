package com.example.impulso.ui.collection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.impulso.R;
import com.example.impulso.databinding.FragmentCollectionBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class CollectionFragment extends Fragment {
    private FragmentCollectionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCollectionBinding.inflate(inflater, container, false);
        binding.pager.setAdapter(new Pages(this));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> {
            int title = switch (position) {
                case 1 -> R.string.tab_mine;
                case 2 -> R.string.tab_recent;
                default -> R.string.tab_favorites;
            };
            tab.setText(title);
        }).attach();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private static final class Pages extends FragmentStateAdapter {
        Pages(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            String mode = switch (position) {
                case 1 -> QuotePageFragment.MINE;
                case 2 -> QuotePageFragment.RECENT;
                default -> QuotePageFragment.FAVORITES;
            };
            return QuotePageFragment.create(mode);
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
