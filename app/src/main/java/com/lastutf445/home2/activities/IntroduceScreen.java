package com.lastutf445.home2.activities;

import android.os.Bundle;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.lastutf445.home2.R;
import com.lastutf445.home2.util.IntroFragment;

import java.util.ArrayList;

public class IntroduceScreen extends AppCompatActivity {

    private FragmentPagerAdapter adapter;
    private ViewPager introContent;
    private SeekBar introIndicator;

    private ArrayList<IntroFragment> fragments;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);
    }

    @Override
    protected void onStart() {
        super.onStart();

        fragments = new ArrayList<>();

        fragments.add(new IntroFragment(
                R.drawable.introIcon1, R.string.introTitle1, R.string.introDesc1
        ));

        fragments.add(new IntroFragment(
                R.drawable.introIcon2, R.string.introTitle2, R.string.introDesc2
        ));

        fragments.add(new IntroFragment(
                R.drawable.introIcon3, R.string.introTitle3, R.string.introDesc3
        ));

        fragments.add(new IntroFragment(
                R.drawable.introIcon4, R.string.introTitle4, R.string.introDesc4
        ));

        fragments.add(new IntroFragment(
                R.drawable.introIcon5, R.string.introTitle5, R.string.introDesc5
        ));

        fragments.add(new IntroFragment(
                R.drawable.introIcon6, R.string.introTitle6, R.string.introDesc6
        ));

        fragments.add(new IntroFragment(
                R.drawable.looks,
                R.string.introTitle7,
                R.string.introDesc1,
                R.layout.introduce_end
        ));

        adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }
        };

        introContent = findViewById(R.id.introContent);
        introIndicator = findViewById(R.id.introIndicator);
        introIndicator.setEnabled(false);

        introContent.setAdapter(adapter);
        introContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                introIndicator.setProgress(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }
}
