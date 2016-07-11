package orbital.com.foodsearch.Activities;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import orbital.com.foodsearch.DAO.BingImageDAO;
import orbital.com.foodsearch.Fragments.SearchBarFragment;
import orbital.com.foodsearch.Fragments.SearchResultsFragment;
import orbital.com.foodsearch.Helpers.BingOcr;
import orbital.com.foodsearch.Helpers.BingSearch;
import orbital.com.foodsearch.Helpers.BingTranslate;
import orbital.com.foodsearch.Helpers.ImageInsights;
import orbital.com.foodsearch.Models.ImageInsightsPOJO.ImageInsightsResponse;
import orbital.com.foodsearch.Models.ImageSearchPOJO.ImageSearchResponse;
import orbital.com.foodsearch.Models.ImageSearchPOJO.ImageValue;
import orbital.com.foodsearch.Models.OcrPOJO.BingOcrResponse;
import orbital.com.foodsearch.Models.OcrPOJO.Line;
import orbital.com.foodsearch.R;
import orbital.com.foodsearch.Utils.AnimUtils;
import orbital.com.foodsearch.Utils.ImageUtils;
import orbital.com.foodsearch.Utils.NetworkUtils;
import orbital.com.foodsearch.Views.DrawableView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OcrActivity extends AppCompatActivity implements SearchResultsFragment.OnFragmentInteractionListener {
    public static final int IMAGE_COUNT = 1;
    private static final String LOG_TAG = "FOODIES";
    private static final String SAVED_FILE_PATH = "SAVEDFILEPATH";
    private static final String SEARCH_FRAGMENT_TAG = "SEARCHFRAGMENT";
    private static final String SEARCH_BAR_TAG = "SEARCHBARTAG";
    private static String mTranslatedText = null;
    private static AsyncTask<Void, Void, String> translateTask;
    private DatabaseReference database;
    private final FragmentManager FRAGMENT_MANAGER = getSupportFragmentManager();
    private boolean animating = false;
    private int containerTransY;
    private int searchBarTrans;
    private String filePath = null;
    private List<ImageValue> mImageValues = null;
    private static BingImageDAO imgDAO = null;
    private static ImageSearchResponse searchResponse;
    private static Boolean imageExist = false;
    private static ImageSearchResponse searchResponseDB;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (filePath != null) {
            outState.putString(SAVED_FILE_PATH, filePath);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey(SAVED_FILE_PATH)) {
            filePath = savedInstanceState.getString(SAVED_FILE_PATH);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr_exp);
        if (filePath == null) {
            filePath = getIntent().getStringExtra("filePath");
        }
        database = FirebaseDatabase.getInstance().getReference();
        imgDAO = new BingImageDAO();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_ocr));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView imgView = (ImageView) findViewById(R.id.previewImageView2);
        Picasso.with(this).load("file://" + filePath)
                //.placeholder(R.color.black_overlay)
                .memoryPolicy(MemoryPolicy.NO_CACHE)
                .resize(30, 44)
                .into(imgView);
        startOcrService();
        initializeDrawView();
        setupSearchContainer();
        setupSearchBar();
    }

    private void setupSearchBar() {
        final FrameLayout searchBarContainer = (FrameLayout) findViewById(R.id.search_bar_container);
        ViewTreeObserver vto = searchBarContainer.getViewTreeObserver();
        // To move searchBarContainer out of the screen
        // Done using onPreDrawListener so as to get the correct measured height
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                searchBarContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                searchBarTrans = 2 * searchBarContainer.getHeight();
                searchBarContainer.setTranslationY(searchBarTrans);
                android.support.v4.app.FragmentTransaction ft = FRAGMENT_MANAGER.beginTransaction();
                ft.replace(R.id.search_bar_container, SearchBarFragment.newInstance(searchBarTrans), SEARCH_BAR_TAG);
                ft.commit();
                return true;
            }
        });
    }

    private void setupSearchContainer() {
        final View rootView = findViewById(R.id.activity_ocr_exp);
        final FrameLayout recyclerContainer = (FrameLayout) findViewById(R.id.search_frag_container);
        ViewTreeObserver vto = recyclerContainer.getViewTreeObserver();
        // To move recyclerContainer out of the screen
        // Done using onPreDrawListener so as to get the correct measured height
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                recyclerContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                containerTransY = rootView.getHeight();
                recyclerContainer.setTranslationY(containerTransY);
                android.support.v4.app.FragmentTransaction ft = FRAGMENT_MANAGER.beginTransaction();
                ft.replace(R.id.search_frag_container, new SearchResultsFragment(), SEARCH_FRAGMENT_TAG);
                ft.commit();
                return true;
            }
        });
    }

    private void initializeDrawView() {
        final DrawableView drawableView = (DrawableView) findViewById(R.id.drawable_view);
        drawableView.setOnTouchListener(new DrawableTouchListener(this, findViewById(R.id.activity_ocr_exp)));
    }

    private void startOcrService() {
        if (NetworkUtils.isNetworkAvailable(this) && NetworkUtils.isOnline()) {
            bingOcrConnect();
        } else {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_ocr_exp),
                    R.string.internet_error_text, Snackbar.LENGTH_LONG);
            snackbar.setAction(R.string.retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startOcrService();
                }
            });
            snackbar.show();
        }
    }

    /**
     * Method that compresses the image before sending it to BingOcrService
     */
    private void bingOcrConnect() {
        View root = findViewById(R.id.activity_ocr_exp);
        CompressAsyncTask compressTask = new CompressAsyncTask(this, root) {
            @Override
            protected void onPostExecute(byte[] compressedImage) {
                // When compressTask is done, invoke dispatchCall for POST call
                Call<BingOcrResponse> call = BingOcr.buildCall(compressedImage);
                // Enqueue the method to the call and wait for callback (Asynchronous call)
                call.enqueue(new OcrCallback(findViewById(R.id.activity_ocr_exp), filePath));
                // After call is dispatched, load compress image into preview
                ImageView previewImageView2 = (ImageView) findViewById(R.id.previewImageView2);
                Picasso.with(mContext).load("file://" + filePath)
                        .noPlaceholder()
                        .fit()
                        .memoryPolicy(MemoryPolicy.NO_CACHE)
                        .into(previewImageView2);
            }
        };
        compressTask.execute(filePath);
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.search_frag_container).getTranslationY() != 0) {
            super.onBackPressed();
        } else {
            closeSearch(findViewById(R.id.activity_ocr_exp));
        }
    }


    public void closeSearch(View view) {
        View rootView = findViewById(R.id.activity_ocr_exp);
        Button searchButton = (Button) rootView.findViewById(R.id.start_search);
        searchButton.setEnabled(true);
        AnimUtils.containerSlideDown(rootView,
                new AnimListener(rootView),
                containerTransY);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void searchImageResponse (final String searchParam) {
        database.child("images").child(searchParam).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                searchResponseDB = dataSnapshot.getValue(ImageSearchResponse.class);
                //initialize the imagesearch again
                if(searchResponseDB == null) {
                    enqueueSearch(searchParam);
                    translate(searchParam);
                }
                else {
                    imageExist = true;
                    SearchResultsFragment searchFragment = (SearchResultsFragment) FRAGMENT_MANAGER.findFragmentByTag(SEARCH_FRAGMENT_TAG);
                    searchFragment.updateRecyclerList(searchResponseDB.getImageValues());
                    for(ImageValue imgVal : searchResponseDB.getImageValues()) {
                        searchImageInsights(imgVal);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "getUser:onCancelled", databaseError.toException());
                enqueueSearch(searchParam);
                translate(searchParam);
            }
        });
    }

    /*
    There is a possibility that there may exist 2 different images with the same image insight.
    This method is called after onresponse for imagesearch callback in order to prevent duplicates from being
    persisted into the database. Flag used is imageExists
     */
    private void searchImageInsights(final ImageValue imgVal) {
        String insightsToken = imgVal.getImageInsightsToken();
        final View rootView = findViewById(R.id.activity_ocr_exp);
        database.child("imageinsights").child(insightsToken).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ImageInsightsResponse imgInsights = dataSnapshot.getValue(ImageInsightsResponse.class);
                //initialize the image insights search again
                if(imgInsights == null) {
                    enqueueImageInsightSearch(imgVal);
                }
                else {
                    imgVal.setInsightsResponse(imgInsights);
                    ImageInsightCallback.count++;
                    if(ImageInsightCallback.count < IMAGE_COUNT) {
                        return;
                    }
                    if(!imageExist) {
                        new CompleteTask(rootView, FRAGMENT_MANAGER)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    else {
                        SearchResultsFragment searchFragment = (SearchResultsFragment) FRAGMENT_MANAGER.findFragmentByTag(SEARCH_FRAGMENT_TAG);
                        searchFragment.finalizeRecycler(searchResponseDB.getTranslatedQuery());
                        ProgressBar progressBar = (ProgressBar)rootView.findViewById(R.id.search_progress);
                        progressBar.setVisibility(View.GONE);
                        AnimUtils.containerSlideUp(rootView);
                        imageExist = false;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "getUser:onCancelled", databaseError.toException());
                enqueueImageInsightSearch(imgVal);
            }
        });
    }
    /**
     * This method creates a search call based on the input param and enqueues it
     *
     * @param searchParam parameter string to be searched for
     */
    private void enqueueSearch(String searchParam) {
        AnimUtils.darkenOverlay(findViewById(R.id.drawable_overlay),
                AnimUtils.DURATION_SLOW);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.search_progress);
        progressBar.setVisibility(View.VISIBLE);
        Log.d(LOG_TAG, "Search String: " + searchParam);
        BingSearch bingImg = new BingSearch(searchParam);
        Call<ImageSearchResponse> call = bingImg.buildCall();
        call.enqueue(new ImageSearchCallback(findViewById(R.id.activity_ocr_exp), FRAGMENT_MANAGER, searchParam));
    }

    /**
     * Method for translating searched text
     */
    private void translate(final String searchParam) {
        mTranslatedText = searchParam;
        translateTask = new translateTask(searchParam).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * This method creates a imageinsight search call based on the input imageValues list and enqueues it
     *
     * @param imgVal image values list to be used as param for insightsToken call
     *                    Multiple calls.
     */
    private void enqueueImageInsightSearch(ImageValue imgVal) {

        ImageInsights imageInsights = new ImageInsights(imgVal.getImageInsightsToken(), "");
        Call<ImageInsightsResponse> call = imageInsights.buildCall();
        call.enqueue(new ImageInsightCallback(findViewById(R.id.activity_ocr_exp),
                FRAGMENT_MANAGER,
                imgVal));

    }

    /**
     * Callback class for ImageInsightCallback.
     */
    private static class ImageInsightCallback implements Callback<ImageInsightsResponse> {
        public static volatile int count = 0;
        private View rootView = null;
        private FragmentManager fm = null;
        private ImageValue imageValue = null;
        ImageInsightCallback(View rootView, FragmentManager fm, ImageValue imageValue) {
            this.rootView = rootView;
            this.fm = fm;
            this.imageValue = imageValue;
        }

        @Override
        public void onResponse(Call<ImageInsightsResponse> call, Response<ImageInsightsResponse> response) {
            ImageInsightsResponse insightsResponse = response.body();
            imageValue.setInsightsResponse(insightsResponse);
            //persist to DB
            imgDAO.persistImageInsight(insightsResponse);
            count++;
            if (count < IMAGE_COUNT) {
                return;
            }
            count = 0;
            //pseudo pause of current thread lol.
            new CompleteTask(rootView, fm)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void onFailure(Call<ImageInsightsResponse> call, Throwable t) {
            if (count < IMAGE_COUNT) {
                count++;
                return;
            }
            count = 0;
            Log.e(LOG_TAG, t.getMessage());
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.search_progress);
            progressBar.setVisibility(View.GONE);
            AnimUtils.brightenOverlay(rootView.findViewById(R.id.drawable_overlay),
                    AnimUtils.DURATION_SLOW);
            Snackbar.make(rootView.findViewById(R.id.activity_ocr_exp), R.string.insights_search_fail,
                    Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Callback class for ImageSearchResponse, handling of POST responses is done here
     */
    private class ImageSearchCallback implements Callback<ImageSearchResponse> {
        private View rootView = null;
        private FragmentManager fm = null;
        private String searchParam = null;

        public ImageSearchCallback(View rootView, FragmentManager fm, String searchParam) {
            this.rootView = rootView;
            this.fm = fm;
            this.searchParam = searchParam;
        }

        @Override
        public void onResponse(Call<ImageSearchResponse> call, Response<ImageSearchResponse> response) {
            searchResponse = response.body();
            if (searchResponse == null) {
                try {
                    Log.e(LOG_TAG, response.errorBody().string());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            searchResponse.setSearchQuery(searchParam);
            List<ImageValue> imageValues = searchResponse.getImageValues();
            // image search results received, now enqueueImageInsightSearch with received value
            SearchResultsFragment searchFragment = (SearchResultsFragment) fm.findFragmentByTag(SEARCH_FRAGMENT_TAG);
            if (!imageValues.isEmpty()) {
                searchFragment.updateRecyclerList(imageValues);
                for(ImageValue imgVal : imageValues) {
                    searchImageInsights(imgVal);
                }
            } else {
                ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.search_progress);
                progressBar.setVisibility(View.GONE);
                FrameLayout drawableOverlay = (FrameLayout) rootView.findViewById(R.id.drawable_overlay);
                AnimUtils.brightenOverlay(drawableOverlay, AnimUtils.DURATION_SLOW);
                Snackbar.make(rootView, R.string.no_image_found, Snackbar.LENGTH_LONG).show();
            }

        }

        @Override
        public void onFailure(Call<ImageSearchResponse> call, Throwable t) {
            Log.e(LOG_TAG, t.getMessage());
            Snackbar.make(rootView.findViewById(R.id.activity_ocr_exp), R.string.image_search_fail,
                    Snackbar.LENGTH_LONG)
                    .show();
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.search_progress);
            progressBar.setVisibility(View.GONE);
            FrameLayout drawableOverlay = (FrameLayout) rootView.findViewById(R.id.drawable_overlay);
            AnimUtils.brightenOverlay(drawableOverlay, AnimUtils.DURATION_SLOW);
            Snackbar.make(rootView, R.string.no_image_found, Snackbar.LENGTH_LONG).show();
        }
    }

    private static class CompleteTask extends AsyncTask<Void, Void, Void> {
        View rootView = null;
        FragmentManager fm = null;

        CompleteTask(View rootView, FragmentManager fm) {
            this.rootView = rootView;
            this.fm = fm;
        }
        @Override
        protected Void doInBackground(Void... params) {
            while (!translateTask.getStatus().equals(Status.FINISHED)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SearchResultsFragment searchFragment = (SearchResultsFragment) fm.findFragmentByTag(SEARCH_FRAGMENT_TAG);
            if (!imageExist) {
                //persist search result to DB
                searchResponse.setTranslatedQuery(mTranslatedText);
                imgDAO.persistImage(searchResponse);
                searchFragment.finalizeRecycler(mTranslatedText);
            }
            //in the case where the database has the image but doesn't have the imageinsights.
            else {
                searchFragment.finalizeRecycler(searchResponseDB.getTranslatedQuery());
            }
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.search_progress);
            progressBar.setVisibility(View.GONE);
            AnimUtils.containerSlideUp(rootView);
            imageExist = false;
        }
    }

    private class translateTask extends AsyncTask<Void, Void, String> {
        String searchParam;

        translateTask(String searchParam) {
            this.searchParam = searchParam;
        }

        @Override
        protected String doInBackground(Void... params) {
            return BingTranslate.getTranslatedText(searchParam);
        }

        @Override
        protected void onPostExecute(String result) {
            mTranslatedText = result;
            super.onPostExecute(result);
        }
    }

    /**
     * Callback class that implements callback method to be used for when a
     * response is received from server
     */
    private class OcrCallback implements Callback<BingOcrResponse> {
        private View rootView = null;
        private String mFilePath = null;
        private DrawableView mDrawableView = null;

        private OcrCallback(View rootView, String filePath) {
            this.rootView = rootView;
            mFilePath = filePath;
            mDrawableView = (DrawableView) rootView.findViewById(R.id.drawable_view);
        }

        @Override
        public void onResponse(Call<BingOcrResponse> call, Response<BingOcrResponse> response) {
            BingOcrResponse bingOcrResponse = response.body();
            List<Line> lines = bingOcrResponse.getAllLines();
            mDrawableView.drawBoxes(rootView, mFilePath, lines,
                    bingOcrResponse.getTextAngle().floatValue(),
                    bingOcrResponse.getLanguage());
            AnimUtils.brightenOverlay(findViewById(R.id.drawable_overlay),
                    AnimUtils.DURATION_VERY_FAST);
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
            AnimUtils.fadeOut(progressBar, AnimUtils.DURATION_VERY_FAST);
        }

        @Override
        public void onFailure(Call<BingOcrResponse> call, Throwable t) {
            AnimUtils.brightenOverlay(findViewById(R.id.drawable_overlay),
                    AnimUtils.DURATION_VERY_FAST);
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
            AnimUtils.fadeOut(progressBar, AnimUtils.DURATION_VERY_FAST);
            Snackbar.make(rootView.findViewById(R.id.activity_ocr_exp), R.string.ocr_call_fail,
                    Snackbar.LENGTH_LONG)
                    .show();
            Log.e(LOG_TAG, "POST Call Failed!" + t.getMessage());
        }
    }

    /**
     * This async task is used to compress the image to be sent in the
     * background thread using ImageUtils.compressImage
     */
    private class CompressAsyncTask extends AsyncTask<String, Integer, byte[]> {
        Context mContext = null;
        View mRootView = null;

        CompressAsyncTask(Context context, View rootView) {
            mContext = context;
            mRootView = rootView;
        }

        @Override
        protected byte[] doInBackground(String... params) {
            return ImageUtils.compressImage(params[0]);
        }
    }

    private class DrawableTouchListener implements View.OnTouchListener {
        private Context context;
        private View rootView;
        private DrawableView mDrawableView = null;

        public DrawableTouchListener(Context context, View rootView) {
            this.context = context;
            this.rootView = rootView;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (!animating) {
                closeSearch(v);
            }
            mDrawableView = (DrawableView) v;
            List<Rect> rects = mDrawableView.getmRects();
            int x = (int) event.getX();
            int y = (int) event.getY();
            for (int i = 0; i < rects.size(); i++) {
                Rect rect = rects.get(i);
                if (rect.contains(x, y)) {
                    selectRect(i);
                    break;
                }
            }
            return true;
        }

        private void selectRect(int i) {
            mDrawableView.updateSelection(i);
            // Display the string in a snackbar and allow for search
            final String searchParam = mDrawableView.getmLineTexts().get(i).toLowerCase();
            View searchBar = findViewById(R.id.search_bar_container);
            AnimUtils.showSearchBar(searchBar, searchParam);
        }
    }

    /**
     * Listener to set boolean value for animating so that we can track it
     */
    private class AnimListener implements Animator.AnimatorListener {
        View rootView = null;

        AnimListener(View rootView) {
            this.rootView = rootView;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            animating = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animating = false;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
