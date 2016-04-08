package com.danilov.supermanga.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.httpimage.HttpImageManager;
import com.danilov.supermanga.R;
import com.danilov.supermanga.activity.MangaInfoActivity;
import com.danilov.supermanga.activity.MangaViewerActivity;
import com.danilov.supermanga.core.adapter.BaseGridAdapter;
import com.danilov.supermanga.core.adapter.ItemClickListener;
import com.danilov.supermanga.core.application.MangaApplication;
import com.danilov.supermanga.core.database.DatabaseAccessException;
import com.danilov.supermanga.core.database.HistoryDAO;
import com.danilov.supermanga.core.model.HistoryElement;
import com.danilov.supermanga.core.model.LocalManga;
import com.danilov.supermanga.core.model.Manga;
import com.danilov.supermanga.core.service.LocalImageManager;
import com.danilov.supermanga.core.util.Constants;
import com.danilov.supermanga.core.util.ServiceContainer;
import com.danilov.supermanga.core.util.Utils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Semyon Danilov on 07.10.2014.
 */
public class HistoryMangaFragment extends BaseFragmentNative {

    private static final String TAG = "HistoryMangaFragment";

    @Bind(R.id.recycler_view)
    public RecyclerView gridView;

    @Bind(R.id.history_progress_bar)
    public ProgressBar historyProgressBar;

    @Inject
    public LocalImageManager localImageManager = null;

    @Inject
    public HttpImageManager httpImageManager = null;

    private HistoryMangaAdapter adapter = null;
    private HistoryDAO historyDAO = null;

    private int sizeOfImage;

    private ItemClickListener<HistoryItemHolder> listener = new ItemClickListener<HistoryItemHolder>() {

        @Override
        public void onItemClick(final int position, final HistoryItemHolder viewHolder) {
            HistoryElement historyElement = adapter.getHistoryElements().get(position);
            Intent intent = new Intent(getActivity(), MangaViewerActivity.class);
            intent.putExtra(Constants.MANGA_PARCEL_KEY, historyElement.getManga());
            intent.putExtra(Constants.FROM_PAGE_KEY, historyElement.getPage());
            intent.putExtra(Constants.FROM_CHAPTER_KEY, historyElement.getChapter());
            intent.putExtra(Constants.SHOW_ONLINE, historyElement.isOnline());
            startActivity(intent);
        }

        @Override
        public boolean onItemLongClick(final int position, final HistoryItemHolder viewHolder) {
            ImageView imageView = viewHolder.mangaCover;
            Manga manga = adapter.getHistoryElements().get(position).getManga();

            Intent intent = new Intent(getActivity().getApplicationContext(), MangaInfoActivity.class);

            int[] onScreenLocation = new int[2];
            imageView.getLocationOnScreen(onScreenLocation);

            intent.putExtra(MangaInfoActivity.EXTRA_LEFT, onScreenLocation[0]);
            intent.putExtra(MangaInfoActivity.EXTRA_TOP, onScreenLocation[1]);
            intent.putExtra(MangaInfoActivity.EXTRA_WIDTH, imageView.getWidth());
            intent.putExtra(MangaInfoActivity.EXTRA_HEIGHT, imageView.getHeight());
            intent.putExtra(MangaInfoActivity.EXTRA_HEIGHT, imageView.getHeight());

            intent.putExtra(Constants.MANGA_PARCEL_KEY, manga);
            startActivity(intent);

            getActivity().overridePendingTransition(0, 0);
            return true;
        }
    };

    public HistoryMangaFragment() {
        MangaApplication.get().applicationComponent().inject(this);
    }

    public static HistoryMangaFragment newInstance() {
        return new HistoryMangaFragment();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.manga_history_fragment, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ButterKnife.bind(this, view);
        historyDAO = ServiceContainer.getService(HistoryDAO.class);
        sizeOfImage = getActivity().getResources().getDimensionPixelSize(R.dimen.grid_item_height);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadHistory();
    }

    private void loadHistory() {
        if (adapter != null) {
            adapter.clearList();
        }
        historyProgressBar.setVisibility(View.VISIBLE);
        final Context context = getActivity();
        Thread thread = new Thread() {
            @Override
            public void run() {
                boolean _success = true;
                String _error = null;
                try {
                    List<HistoryElement> history = getHistorySorted();
                    if (!history.isEmpty()) {
                        adapter = new HistoryMangaAdapter(gridView, history);
                        adapter.setClickListener(listener);
                    }
                } catch (DatabaseAccessException e) {
                    _success = false;
                    _error = e.getMessage();
                    Log.e(TAG, "Failed to get history: " + _error);
                }
                final boolean success = _success;
                final String error = _error;
                handler.post(() -> {
                    historyProgressBar.setVisibility(View.INVISIBLE);
                    if (success) {
                        gridView.setAdapter(adapter);
                    } else {
                        String formedError = Utils.stringResource(getActivity(), R.string.p_failed_to_show_loaded);
                        Utils.showToast(getActivity(), formedError + error);
                    }
                });
            }

        };
        thread.start();
    }

    private void removeHistory(final HistoryElement historyElement) {
        try {
            historyDAO.deleteManga(historyElement.getManga(), historyElement.isOnline());
            loadHistory();
        } catch (DatabaseAccessException e) {
            e.printStackTrace();
            Utils.showToast(getActivity(), getActivity().getString(R.string.e_failed_remove_history) + e.getMessage());
        }
    }

    @NonNull
    private List<HistoryElement> getHistorySorted() throws DatabaseAccessException {
        List<HistoryElement> history = historyDAO.getMangaHistory();
        Collections.sort(history, (l, r) -> {
            Date lDate = l.getDate();
            Date rDate = r.getDate();
            if (lDate.equals(rDate)) {
                return 0;
            }
            return lDate.after(rDate) ? -1 : 1;
        });
        return history;
    }

    private class HistoryMangaAdapter extends BaseGridAdapter<HistoryItemHolder> {

        private List<HistoryElement> history = null;

        public HistoryMangaAdapter(@NonNull final RecyclerView recyclerView, final List<HistoryElement> history) {
            super(recyclerView);
            this.history = history;
        }

        public List<HistoryElement> getHistoryElements() {
            return history;
        }


        public void clearList() {
            if (history != null) {
                history.clear();
                notifyDataSetChanged();
            }
        }

        @Override
        public HistoryItemHolder newViewHolder(final ViewGroup parent, final int viewType) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View view = inflater.inflate(R.layout.history_grid_item, parent, false);
            return new HistoryItemHolder(view);
        }


        @Override
        public void onBindViewHolder(final HistoryItemHolder holder, final int position) {
            final HistoryElement historyElement = history.get(position);
            holder.discardButton.setOnClickListener(view1 -> removeHistory(historyElement));
            Manga manga = historyElement.getManga();
            holder.mangaTitle.setText(manga.getTitle());

            if (!historyElement.isOnline()) {
                holder.isOnline.setVisibility(View.INVISIBLE);
                LocalManga localManga = (LocalManga) manga;
                String mangaUri = localManga.getLocalUri();
                Bitmap bitmap = localImageManager.loadBitmap(holder.mangaCover, mangaUri + "/cover", sizeOfImage);
                if (bitmap != null) {
                    holder.mangaCover.setImageBitmap(bitmap);
                }
            } else {
                holder.isOnline.setVisibility(View.VISIBLE);
                if (manga.getCoverUri() != null) {
                    Uri coverUri = Uri.parse(manga.getCoverUri());
                    HttpImageManager.LoadRequest request = HttpImageManager.LoadRequest.obtain(coverUri, holder.mangaCover, manga.getRepository().getEngine().getRequestPreprocessor(), sizeOfImage);
                    Bitmap bitmap = httpImageManager.loadImage(request);
                    if (bitmap != null) {
                        holder.mangaCover.setImageBitmap(bitmap);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

    }

    private class HistoryItemHolder extends RecyclerView.ViewHolder {

        public ImageView mangaCover;
        public TextView mangaTitle;
        public ImageButton discardButton;
        private View isOnline;

        public HistoryItemHolder(final View itemView) {
            super(itemView);
            mangaCover = (ImageView) itemView.findViewById(R.id.manga_cover);
            mangaTitle = (TextView) itemView.findViewById(R.id.manga_title);
            discardButton = (ImageButton) itemView.findViewById(R.id.discard_button);
            isOnline = itemView.findViewById(R.id.is_online);
        }

    }

}