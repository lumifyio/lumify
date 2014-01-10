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

package com.altamiracorp.lumify.core.bootstrap;

import com.altamiracorp.lumify.core.config.Configuration;
import com.google.inject.Binder;

/**
 * A BootstrapBindingProvider can add Guice bindings to the Lumify Bootstrap Module.
 * Implementations are automatically discovered by the Lumify Bootstrapper and will be
 * instantiated using an empty constructor.
 */
public interface BootstrapBindingProvider {
    /**
     * Add the bindings defined by this BootstrapBindingProvider to
     * the Lumify Bootstrap module.
     * @param binder the Binder that configures the Bootstrapper
     * @param configuration the Lumify Configuration
     */
    void addBindings(final Binder binder, final Configuration configuration);
}
