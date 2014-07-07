package io.lumify.translate;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import io.lumify.core.bootstrap.BootstrapBindingProvider;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.ClassUtil;

public class TranslateBootstrapBindingProvider implements BootstrapBindingProvider {
    private static final String CONFIG_TRANSLATOR_CLASS_NAME = "translate.translator";

    @Override
    public void addBindings(Binder binder, Configuration configuration) {
        String translatorClassName = configuration.get(CONFIG_TRANSLATOR_CLASS_NAME, NopTranslator.class.getName());
        try {
            Class<? extends Translator> translatorClass = ClassUtil.forName(translatorClassName);

            binder.bind(Translator.class)
                    .to(translatorClass)
                    .in(Scopes.SINGLETON);
        } catch (Exception ex) {
            throw new LumifyException("Could not bind translator: " + translatorClassName, ex);
        }
    }
}
