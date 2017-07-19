package ru.gdgkazan.simpleweather.screen.weatherlist;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ru.gdgkazan.simpleweather.R;
import ru.gdgkazan.simpleweather.model.City;
import ru.gdgkazan.simpleweather.screen.general.LoadingDialog;
import ru.gdgkazan.simpleweather.screen.general.LoadingView;
import ru.gdgkazan.simpleweather.screen.general.SimpleDividerItemDecoration;
import ru.gdgkazan.simpleweather.screen.weather.WeatherActivity;
import ru.gdgkazan.simpleweather.screen.weather.WeatherLoader;

/**
 * @author Artur Vasilov
 */
public class WeatherListActivity extends AppCompatActivity implements CitiesAdapter.OnItemClick, SwipeRefreshLayout.OnRefreshListener {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;

    @BindView(R.id.empty)
    View mEmptyView;

    private CitiesAdapter mAdapter;

    private LoadingView mLoadingView;

    //Список городов с загруженной инфой о погоде
    private List<City> loadedCity = new ArrayList<>();
    private List<City> cities;

    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, false));
        mAdapter = new CitiesAdapter(getInitialCities(), this);
        mRecyclerView.setAdapter(mAdapter);
        mLoadingView = LoadingDialog.view(getSupportFragmentManager());

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeColors(Color.RED, Color.GREEN, Color.BLUE, Color.CYAN);

        cities = getInitialCities();

        load(cities, false);
    }

    @Override
    public void onRefresh() {
        load(cities, true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 4000);
    }

    private class WeatherCallbacks implements LoaderManager.LoaderCallbacks<City> {

        private City city;
        private String cityName;

        public WeatherCallbacks(City city, String cityName) {
            this.city = city;
            this.cityName = cityName;
        }

        @Override
        public Loader<City> onCreateLoader(int id, Bundle args) {
            //создаем для каждого города Loader
            if (id <= cities.size()) {
                return new WeatherLoader(WeatherListActivity.this, cityName);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<City> loader, City city) {
            showWather(city);
        }

        @Override
        public void onLoaderReset(Loader<City> loader) {

        }
    }

    private void loadWeather(boolean restart, City city, String cityName, Integer id) {
        mLoadingView.showLoadingIndicator();
        LoaderManager.LoaderCallbacks<City> callbacks = new WeatherCallbacks(city, cityName);
        if (restart) {
            getSupportLoaderManager().restartLoader(id, Bundle.EMPTY, callbacks);
        } else {
            getSupportLoaderManager().initLoader(id, Bundle.EMPTY, callbacks);
        }
    }

    private void load(List<City> cities, boolean restart) {
        for (int i = 0; i < cities.size(); i++) {
            String cityName = cities.get(i).getName();
            loadWeather(restart, cities.get(i), cityName, i + 1);
        }
    }

    @Override
    public void onItemClick(@NonNull City city) {
        startActivity(WeatherActivity.makeIntent(this, city.getName()));
    }

    @NonNull
    private List<City> getInitialCities() {
        List<City> cities = new ArrayList<>();
        String[] initialCities = getResources().getStringArray(R.array.initial_cities);
        for (String city : initialCities) {
            cities.add(new City(city));
        }
        return cities;
    }

    private List<City> sortAllCities(List<City> cities) {
        Collections.sort(cities, new Comparator<City>() {
            @Override
            public int compare(City t0, City t1) {
                return t0.getName().compareTo(t1.getName());
            }
        });
        return cities;
    }

    private void showError() {
        mLoadingView.hideLoadingIndicator();
        Snackbar snackbar = Snackbar.make(mRecyclerView, "Error loading weather", Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> load(cities, true));
        snackbar.setDuration(4000);
        snackbar.show();
    }

    private void showWather(@Nullable City city) {
        if (city == null || city.getMain() == null || city.getWeather() == null || city.getWind() == null) {
            showError();
            return;
        }

        loadedCity.add(city);
        if (loadedCity.size() >= cities.size()) {
            mLoadingView.hideLoadingIndicator();
            sortAllCities(loadedCity);
            mAdapter.changeDataSet(loadedCity);
            loadedCity.clear();
        }
    }
}
