/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module com.davils.kollect.core {
    requires transitive kotlin.stdlib;
    requires transitive com.davils.kore.core;
    requires transitive com.davils.konfetti.core;
    requires transitive kotlinx.coroutines.core;
    requires java.management;
    requires jdk.management;

    exports com.davils.kollect.core;
    exports com.davils.kollect.core.binder;
    exports com.davils.kollect.core.declare;
    exports com.davils.kollect.core.event;
    exports com.davils.kollect.core.export;
    exports com.davils.kollect.core.exposition;
    exports com.davils.kollect.core.filter;
    exports com.davils.kollect.core.health;
    exports com.davils.kollect.core.health.rule;
    exports com.davils.kollect.core.jvm;
    exports com.davils.kollect.core.limit;
    exports com.davils.kollect.core.meter;
    exports com.davils.kollect.core.name;
    exports com.davils.kollect.core.otlp;
    exports com.davils.kollect.core.snapshot;
    exports com.davils.kollect.core.trace;
}
