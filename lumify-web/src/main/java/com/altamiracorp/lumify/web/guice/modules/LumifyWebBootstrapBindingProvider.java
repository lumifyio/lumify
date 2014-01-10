/*
 * Copyright 2014 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.altamiracorp.lumify.web.guice.modules;

import com.altamiracorp.lumify.core.bootstrap.BootstrapBindingProvider;
import com.altamiracorp.lumify.core.bootstrap.BootstrapUtils;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.google.inject.Binder;

/**
 * This Provider configures the Guice bindings required by the Lumify web application.
 */
public class LumifyWebBootstrapBindingProvider implements BootstrapBindingProvider {
    @Override
    public void addBindings(final Binder binder, final Configuration configuration) {
        Class<? extends AuthenticationProvider> authProviderClass =
                BootstrapUtils.getConfiguredClass(configuration, Configuration.AUTHENTICATION_PROVIDER, true);
        binder.bind(AuthenticationProvider.class).to(authProviderClass);
    }
}
