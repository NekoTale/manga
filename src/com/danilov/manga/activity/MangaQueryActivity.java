package com.danilov.manga.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import com.danilov.manga.R;
import com.danilov.manga.core.adapter.MangaListAdapter;
import com.danilov.manga.core.model.Manga;
import com.danilov.manga.core.repository.ReadmangaEngine;
import com.danilov.manga.core.repository.RepositoryEngine;
import com.danilov.manga.core.util.Constants;

import java.util.List;

/**
 * Created by Semyon Danilov on 26.07.2014.
 */
public class MangaQueryActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private EditText query;
    private ListView searchResultsView;

    private View brand;

    private MangaListAdapter adapter = null;

    private List<Manga> foundManga = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manga_query_activity);
        query = (EditText) findViewById(R.id.query);
        searchResultsView = (ListView) findViewById(R.id.search_results);
        brand = findViewById(R.id.brand_container);
        ImageButton btn = (ImageButton) findViewById(R.id.search_button);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        QueryTask task = new QueryTask();
        String textQuery = query.getText().toString();
        task.execute(textQuery);
    }

    private class QueryTask extends AsyncTask<String, Void, List<Manga>> {

        @Override
        protected void onPreExecute() {
            hideBrand();
        }

        @Override
        protected List<Manga> doInBackground(final String... params) {
            if (params == null || params.length < 1) {
                return null;
            }
            RepositoryEngine repositoryEngine = new ReadmangaEngine();
            final List<Manga> mangaList = repositoryEngine.queryRepository(params[0]);
            return mangaList;
        }

        @Override
        protected void onPostExecute(final List<Manga> foundManga) {
            if (foundManga == null) {
                return;
            }
            MangaQueryActivity.this.foundManga = foundManga;
            adapter = new MangaListAdapter(MangaQueryActivity.this, R.layout.manga_list_item, foundManga);
            searchResultsView.setAdapter(adapter);
            searchResultsView.setOnItemClickListener(MangaQueryActivity.this);
        }

    }


    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int i, final long l) {
        Manga manga = adapter.getItem(i);
        Intent intent = new Intent(this, MangaInfoActivity.class);
        intent.putExtra(Constants.MANGA_PARCEL_KEY, manga);
        startActivity(intent);
    }

    private void hideBrand() {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setDuration(1000);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(final Animation animation) {

            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                brand.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {

            }

        });
        brand.startAnimation(fadeOut);
    }

}