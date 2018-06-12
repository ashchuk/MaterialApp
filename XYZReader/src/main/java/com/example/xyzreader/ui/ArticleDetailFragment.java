package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.data.Constants.ARG_ITEM_ID;
import static com.example.xyzreader.data.Constants.EXTRA_CURRENT_ARTICLE_POSITION;
import static com.example.xyzreader.data.Constants.EXTRA_STARTING_ARTICLE_POSITION;

public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private int mCurrentPosition;
    private int mStartingPosition;

    @BindView(R.id.scroll_view)
    NestedScrollView mScrollView;
    @BindView(R.id.photo_container)
    View mPhotoContainerView;
    @BindView(R.id.thumbnail)
    ImageView mPhotoView;
    @Nullable
    @BindView(R.id.parallax_bar)
    AppBarLayout parallaxBar;
    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;
    @BindView(R.id.meta_bar)
    LinearLayout metaBar;
    @BindView(R.id.article_title)
    TextView titleView;
    @BindView(R.id.article_byline)
    TextView bylineView;
    @BindView(R.id.detail_toolbar)
    Toolbar detailToolbar;
    @BindView(R.id.article_body)
    TextView bodyView;

    public ArticleDetailFragment() { }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        arguments.putInt(EXTRA_CURRENT_ARTICLE_POSITION, position);
        arguments.putInt(EXTRA_STARTING_ARTICLE_POSITION, startingPosition);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        mStartingPosition = getArguments().getInt(EXTRA_STARTING_ARTICLE_POSITION);
        mCurrentPosition = getArguments().getInt(EXTRA_CURRENT_ARTICLE_POSITION);

        if (Build.VERSION.SDK_INT >= 21) {
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);

        ((AppCompatActivity) getActivity()).setSupportActionBar(detailToolbar);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar())
                .setDisplayShowTitleEnabled(false);

        bindViews();
        return mRootView;
    }

    public void startPostponedEnterTransition() {
        if (mCurrentPosition == mStartingPosition) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                        getActivity().startPostponedEnterTransition();
                        return true;
                    }
                });
            }
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        bylineView.setMovementMethod(new LinkMovementMethod());

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            String photo = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            final String title = mCursor.getString(ArticleLoader.Query.TITLE);
            final String time = Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)).toString();
            final String body = Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)).toString();
            titleView.setText(title);
            bylineView.setText(time);
            mPhotoView.setContentDescription(title + time);
            bodyView.setText(body);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPhotoView.setTransitionName("transition_photo" + String.valueOf(mCurrentPosition));
            }
            mShareFab.setOnClickListener(view -> startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                    .setType("text/plain")
                    .setText(title + "/n" + time + "/n" + body)
                    .getIntent(), getString(R.string.action_share))));

            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(photo, new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                startPostponedEnterTransition();
                            }

                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.from(bitmap).generate();
                                mMutedColor = p.getDarkMutedColor(0xFF424242);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                if (parallaxBar != null) {
                                    metaBar.setBackgroundColor(mMutedColor);
                                }
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText(R.string.empty_label);
            bylineView.setText(R.string.empty_label);
            bodyView.setText(R.string.empty_label);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, getString(R.string.error_message));
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Nullable
    ImageView getAlbumImage() {
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }
}
