package flicker.example.an.flickersearch.di;

import android.app.Application;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;
import flicker.example.an.flickersearch.FlickerApp;

@Component(modules = {AndroidSupportInjectionModule.class, AppModule.class, BuildersModule.class})
public interface ApplicationComponent {
    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);
        ApplicationComponent build();
    }
    void inject(FlickerApp app);
}
