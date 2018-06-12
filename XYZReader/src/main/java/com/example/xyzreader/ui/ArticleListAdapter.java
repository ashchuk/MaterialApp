package com.example.xyzreader.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.xyzreader.data.Constants.EXTRA_STARTING_ARTICLE_POSITION;

public class ArticleListAdapter extends RecyclerView.Adapter<ArticleListAdapter.ViewHolder> {
    private Cursor mCursor;
    private Context mContext;
    private int mArticlePosition;

    public class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.thumbnail)
        DynamicHeightNetworkImageView thumbnailView;
        @BindView(R.id.article_title)
        TextView titleView;
        @BindView(R.id.article_subtitle)
        TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    public ArticleListAdapter(Context context, Cursor cursor) {
        mContext = context;
        mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_article, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(view1 -> {
            mArticlePosition = viewHolder.getAdapterPosition();

            ActivityOptionsCompat bundle = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        (Activity) mContext,
                        viewHolder.thumbnailView,
                        viewHolder.thumbnailView.getTransitionName()
                );
            }
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    ItemsContract.Items.buildItemUri(getItemId(viewHolder.getAdapterPosition())));
            intent.putExtra(EXTRA_STARTING_ARTICLE_POSITION, mArticlePosition);
            mContext.startActivity(intent, bundle.toBundle());
        });
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        mArticlePosition = position;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.thumbnailView.setTransitionName("transition_photo" + position);
        }
        holder.thumbnailView.setTag("transition_photo" + position);

        String title = mCursor.getString(ArticleLoader.Query.TITLE);
        String subtitle = DateUtils.getRelativeTimeSpanString(
                mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();
        String image = mCursor.getString(ArticleLoader.Query.THUMB_URL);
        holder.titleView.setText(title);
        holder.subtitleView.setText(subtitle);
        holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));

        ImageLoader loader = ImageLoaderHelper.getInstance(mContext).getImageLoader();
        holder.thumbnailView.setImageUrl(image, loader);
        loader.get(image, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                Bitmap bitmap = imageContainer.getBitmap();
                if (bitmap != null) {
                    Palette p = Palette.from(bitmap).generate();
                    int mMutedColor = p.getDarkMutedColor(0xFF424242);
                    holder.itemView.setBackgroundColor(mMutedColor);
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }
}