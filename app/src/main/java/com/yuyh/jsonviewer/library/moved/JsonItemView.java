package com.yuyh.jsonviewer.library.moved;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import moe.ore.txhook.R;

/**
 * Created by yuyuhang on 2017/11/29.
 */
public class JsonItemView extends LinearLayout {

    public static int TEXT_SIZE_DP = 12;

    private final Context mContext;

    private TextView mTvLeft, mTvRight;
    private ImageView mIvIcon;

    public JsonItemView(Context context) {
        this(context, null);
    }

    public JsonItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JsonItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        initView();
    }

    private void initView() {
        setOrientation(VERTICAL);

        int p2 = dp(2);

        LinearLayout row = new LinearLayout(mContext);
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(24));
        row.setPadding(p2, p2, p2, p2);

        mTvLeft = new TextView(mContext);
        mTvLeft.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mTvLeft.setTextColor(0xFF475569);
        mTvLeft.setTextSize(13);
        mTvLeft.setVisibility(GONE);

        mIvIcon = new AppCompatImageView(mContext);
        LayoutParams iconLp = new LayoutParams(dp(12), dp(12));
        iconLp.setMargins(dp(2), 0, dp(2), 0);
        mIvIcon.setLayoutParams(iconLp);
        mIvIcon.setAdjustViewBounds(true);
        mIvIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mIvIcon.setImageResource(R.drawable.jsonviewer_plus);
        mIvIcon.setColorFilter(0xFF94A3B8);
        mIvIcon.setContentDescription(getResources().getString(R.string.jsonViewer_icon_plus));
        mIvIcon.setVisibility(GONE);

        mTvRight = new TextView(mContext);
        LayoutParams rightLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rightLp.leftMargin = dp(2);
        mTvRight.setLayoutParams(rightLp);
        mTvRight.setTextColor(0xFF0F172A);
        mTvRight.setTextSize(13);
        mTvRight.setGravity(Gravity.CENTER_VERTICAL);
        mTvRight.setVisibility(GONE);

        row.addView(mTvLeft);
        row.addView(mIvIcon);
        row.addView(mTvRight);

        addView(row);
    }

    public void setTextSize(float textSizeDp) {
        if (textSizeDp < 12) {
            textSizeDp = 12;
        } else if (textSizeDp > 30) {
            textSizeDp = 30;
        }

        TEXT_SIZE_DP = (int) textSizeDp;

        mTvLeft.setTextSize(TEXT_SIZE_DP);
        mTvRight.setTextSize(TEXT_SIZE_DP);
        mTvRight.setTextColor(BaseJsonViewerAdapter.BRACES_COLOR);

        // align the vertically expand/collapse icon to the text
        int textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP, getResources().getDisplayMetrics());

        LayoutParams layoutParams = (LayoutParams) mIvIcon.getLayoutParams();
        layoutParams.height = textSize;
        layoutParams.width = textSize;
        layoutParams.topMargin = textSize / 5;

        mIvIcon.setLayoutParams(layoutParams);
    }

    public void setRightColor(int color) {
        mTvRight.setTextColor(color);
    }

    public void hideLeft() {
        mTvLeft.setVisibility(GONE);
    }

    public void showLeft(CharSequence text) {
        mTvLeft.setVisibility(VISIBLE);
        if (text != null) {
            mTvLeft.setText(text);
        }
    }

    public void hideRight() {
        mTvRight.setVisibility(GONE);
    }

    public void showRight(CharSequence text) {
        mTvRight.setVisibility(VISIBLE);
        if (text != null) {
            mTvRight.setText(text);
        }
    }

    public CharSequence getRightText() {
        return mTvRight.getText();
    }

    public void hideIcon() {
        mIvIcon.setVisibility(GONE);
    }

    public void showIcon(boolean isPlus) {
        mIvIcon.setVisibility(VISIBLE);
        mIvIcon.setImageResource(isPlus ? R.drawable.jsonviewer_plus : R.drawable.jsonviewer_minus);
        mIvIcon.setContentDescription(getResources().getString(isPlus ? R.string.jsonViewer_icon_plus : R.string.jsonViewer_icon_minus));
    }

    public void setIconClickListener(OnClickListener listener) {
        mIvIcon.setOnClickListener(listener);
    }

    public void addViewNoInvalidate(View child) {
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
            if (params == null) {
                throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
            }
        }
        addViewInLayout(child, -1, params);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
