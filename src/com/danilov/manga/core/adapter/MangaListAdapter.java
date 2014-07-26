package com.danilov.manga.core.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.httpimage.HttpImageManager;
import com.danilov.manga.R;
import com.danilov.manga.core.model.Manga;
import com.danilov.manga.core.util.ServiceContainer;

import java.util.List;

/**
 * Created by Semyon Danilov on 18.05.2014.
 */
public class MangaListAdapter extends ArrayAdapter<Manga> {

    private final String TAG = "MangaListAdapter";

    private final int resourceId;
    private int sizeOfImage;
    private PopupButtonClickListener popupButtonClickListener;

    public MangaListAdapter(final Context context, final int resource, final List<Manga> objects) {
        this(context, resource, objects, null);
    }

    public MangaListAdapter(final Context context, final int resource, final List<Manga> objects, final PopupButtonClickListener popupButtonClickListener) {
        super(context, resource, objects);
        resourceId = resource;
        sizeOfImage = context.getResources().getDimensionPixelSize(R.dimen.manga_list_image_height);
        this.popupButtonClickListener = popupButtonClickListener;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resourceId, parent, false); //attaching to parent == (false), because it attaching later by android
        }
        Manga manga = getItem(position);
        Object tag = view.getTag();
        final MangaViewBag viewBag;

        Log.d(TAG, "Position = " + position);
        Log.d(TAG, "Has convertView = " + (convertView != null));

        if (tag != null && tag instanceof MangaViewBag) {
            viewBag = (MangaViewBag) tag;
        } else {
            viewBag = new MangaViewBag();
            TextView titleView = (TextView) view.findViewById(R.id.manga_title);
            ImageView coverView = (ImageView) view.findViewById(R.id.manga_cover);
            ImageButton popupButton = (ImageButton) view.findViewById(R.id.popup_button);
            viewBag.titleView = titleView;
            viewBag.coverView = coverView;
            viewBag.popupButton = popupButton;
            view.setTag(viewBag);
        }
        viewBag.titleView.setText(manga.getTitle());
        if (popupButtonClickListener != null) {
            viewBag.popupButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View v) {
                    popupButtonClickListener.onPopupButtonClick(viewBag.popupButton, position);
                }

            });
        }
        HttpImageManager httpImageManager = ServiceContainer.getService(HttpImageManager.class);
        Uri coverUri = Uri.parse(manga.getCoverUri());
        HttpImageManager.LoadRequest request = HttpImageManager.LoadRequest.obtain(coverUri, viewBag.coverView, sizeOfImage);
        Bitmap bitmap = httpImageManager.loadImage(request);
        if (bitmap != null) {
            viewBag.coverView.setImageBitmap(bitmap);
        }
        return view;
    }

    public class MangaViewBag {
        protected TextView titleView;
        protected ImageView coverView;
        protected ImageButton popupButton;
        //TODO: add everything else
    }

    public interface PopupButtonClickListener {

        void onPopupButtonClick(final View popupButton, final int listPosition);

    }

}