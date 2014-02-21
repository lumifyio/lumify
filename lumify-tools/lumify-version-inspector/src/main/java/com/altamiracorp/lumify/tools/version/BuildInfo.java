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
package com.altamiracorp.lumify.tools.version;

import java.util.Map;

/**
 * This class provides a simple API for accessing the
 * properties of a Lumify component as defined in the
 * generated ${project.artifactId}-build.properties file.
 */
public final class BuildInfo {
    /**
     * The build date key.
     */
    private static final String BUILD_DATE = "build.date";

    /**
     * The build timestamp key.
     */
    private static final String BUILD_TIMESTAMP = "build.timestamp";

    /**
     * The build user key.
     */
    private static final String BUILD_USER = "build.user";

    /**
     * The build OS key.
     */
    private static final String BUILD_OS = "build.os";

    /**
     * The build OS version key.
     */
    private static final String BUILD_OS_VERSION = "build.os.version";

    /**
     * The build OS architecture key.
     */
    private static final String BUILD_OS_ARCH = "build.os.arch";

    /**
     * The build JVM version key.
     */
    private static final String BUILD_JVM_VERSION = "build.jvm.version";

    /**
     * The build JVM vendor key.
     */
    private static final String BUILD_JVM_VENDOR = "build.jvm.vendor";

    /**
     * The build Maven version.
     */
    private static final String BUILD_MAVEN_VERSION = "build.maven.version";

    private final String date;

    private final String timestamp;

    private final String user;

    private final String osName;

    private final String osVersion;

    private final String osArch;

    private final String jvmVersion;

    private final String jvmVendor;

    private final String mavenVersion;

    /**
     * Create a new BuildInfo.
     * @param props the properties
     */
    public BuildInfo(final Map<String, String> props) {
        date = props.get(BUILD_DATE);
        timestamp = props.get(BUILD_TIMESTAMP);
        user = props.get(BUILD_USER);
        osName = props.get(BUILD_OS);
        osVersion = props.get(BUILD_OS_VERSION);
        osArch = props.get(BUILD_OS_ARCH);
        jvmVersion = props.get(BUILD_JVM_VERSION);
        jvmVendor = props.get(BUILD_JVM_VENDOR);
        mavenVersion = props.get(BUILD_MAVEN_VERSION);
    }

    public String getDate() {
        return date;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    public String getJvmVendor() {
        return jvmVendor;
    }

    public String getMavenVersion() {
        return mavenVersion;
    }

    public String osSpec() {
        return String.format("%s %s (%s)", osName, osVersion, osArch);
    }

    public String jvmSpec() {
        return String.format("%s (%s)", jvmVersion, jvmVendor);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.date != null ? this.date.hashCode() : 0);
        hash = 89 * hash + (this.timestamp != null ? this.timestamp.hashCode() : 0);
        hash = 89 * hash + (this.user != null ? this.user.hashCode() : 0);
        hash = 89 * hash + (this.osName != null ? this.osName.hashCode() : 0);
        hash = 89 * hash + (this.osVersion != null ? this.osVersion.hashCode() : 0);
        hash = 89 * hash + (this.osArch != null ? this.osArch.hashCode() : 0);
        hash = 89 * hash + (this.jvmVersion != null ? this.jvmVersion.hashCode() : 0);
        hash = 89 * hash + (this.jvmVendor != null ? this.jvmVendor.hashCode() : 0);
        hash = 89 * hash + (this.mavenVersion != null ? this.mavenVersion.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BuildInfo other = (BuildInfo) obj;
        if ((this.date == null) ? (other.date != null) : !this.date.equals(other.date)) {
            return false;
        }
        if ((this.timestamp == null) ? (other.timestamp != null) : !this.timestamp.equals(other.timestamp)) {
            return false;
        }
        if ((this.user == null) ? (other.user != null) : !this.user.equals(other.user)) {
            return false;
        }
        if ((this.osName == null) ? (other.osName != null) : !this.osName.equals(other.osName)) {
            return false;
        }
        if ((this.osVersion == null) ? (other.osVersion != null) : !this.osVersion.equals(other.osVersion)) {
            return false;
        }
        if ((this.osArch == null) ? (other.osArch != null) : !this.osArch.equals(other.osArch)) {
            return false;
        }
        if ((this.jvmVersion == null) ? (other.jvmVersion != null) : !this.jvmVersion.equals(other.jvmVersion)) {
            return false;
        }
        if ((this.jvmVendor == null) ? (other.jvmVendor != null) : !this.jvmVendor.equals(other.jvmVendor)) {
            return false;
        }
        if ((this.mavenVersion == null) ? (other.mavenVersion != null) : !this.mavenVersion.equals(other.mavenVersion)) {
            return false;
        }
        return true;
    }
}
