package co.poynt.samples.dcatestapp.ui.adapters;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import co.poynt.samples.dcatestapp.ui.fragments.EmvTestsFragment;
import co.poynt.samples.dcatestapp.ui.fragments.MifareTestsFragment;
import co.poynt.samples.dcatestapp.ui.fragments.MiscTestsFragment;
import co.poynt.samples.dcatestapp.utils.IHelper;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private IHelper helper;
    public ViewPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
        this.helper = (IHelper) activity;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            default:
            case 0:
                return new MiscTestsFragment(helper);
            case 1:
                return new EmvTestsFragment(helper);
            case 2:
                return new MifareTestsFragment(helper);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}