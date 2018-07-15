package flicker.example.an.flickersearch.di;

import android.app.Application;
import android.content.Context;

import com.google.gson.Gson;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import flicker.example.an.flickersearch.HttpConfig;
import flicker.example.an.flickersearch.data.api.FlickrDataSource;
import flicker.example.an.flickersearch.data.api.UrlProvider;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
public class AppModule {

    
    @Provides
    public Context provideContext(Application application){
        return application;
    }

    
    @Provides
    public OkHttpClient provideOkHttpClient(Context context){
        int cacheSize = 10 * 1024 * 1024; // 10 MiB
        Cache cache = new Cache(context.getCacheDir(), cacheSize);
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(logging)
                .build();
    }

    
    @Provides
    public UrlProvider provideUrlProvider(){
        return new UrlProvider(HttpConfig.SCHEME, HttpConfig.HOST);
    }

    
    @Provides
    public Gson provideGson(){
        return new Gson();
    }

    
    @Provides
    public FlickrDataSource provideRemoteDataSource(OkHttpClient okHttpClient, UrlProvider urlProvider, Gson gson){
        return new FlickrDataSource(okHttpClient, urlProvider, gson);
    }


}
