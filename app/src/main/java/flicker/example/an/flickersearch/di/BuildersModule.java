package flicker.example.an.flickersearch.di;

import dagger.Module;
import dagger.android.AndroidInjectionModule;
import dagger.android.ContributesAndroidInjector;
import flicker.example.an.flickersearch.view.SearchActivity;

/**
 * Binds all sub-components within the app.
 */
@Module(includes = AndroidInjectionModule.class)
public abstract class BuildersModule {

    @PerActivity
    @ContributesAndroidInjector(modules = {SearchViewModule.class})
    abstract SearchActivity bindSearchActivity();

}