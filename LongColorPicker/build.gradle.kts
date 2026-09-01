plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

group = "com.longx.intelligent.android.lib.longcolorpicker"
version = "1.0"
val manualBuildTime = "2026 年 9 月 1 日"

val generatedSourcesDir = layout.projectDirectory.dir("src/main/java")

val generateBuildInfoTask = tasks.register("generateBuildInfo") {
    inputs.property("version", project.version.toString())
    inputs.property("buildTime", manualBuildTime)
    outputs.dir(generatedSourcesDir)
    doLast {
        val outDir = generatedSourcesDir.asFile
        val packageDir = File(outDir, "com/longx/intelligent/android/lib/longcolorpicker/_build")
        if (!packageDir.exists()) {
            packageDir.mkdirs()
        }
        File(packageDir, "BuildInfo.java").writeText("""
            package com.longx.intelligent.android.lib.longcolorpicker._build;

            public class BuildInfo {
                public static final String VERSION = "${project.version}";
                public static final String BUILD_TIME = "$manualBuildTime";
            }
        """.trimIndent())
    }
}

tasks.named("preBuild") {
    dependsOn(generateBuildInfoTask)
}

android {
    namespace = "com.longx.intelligent.android.lib.longcolorpicker"
    compileSdk = 35

    defaultConfig {
        minSdk = 16

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.longx.intelligent.android.lib.longcolorpicker"
                artifactId = "long-color-picker"
                version = "1.0"
            }
        }
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat)
    implementation(libs.material)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenRelease") {
                from(components["release"])
                groupId = "com.longx.intelligent.android.lib.longcolorpicker"
                artifactId = "long-color-picker"
                version = "1.0"
            }
        }
    }
}