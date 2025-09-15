plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 代码质量检查插件
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

android {
    namespace = "com.example.calculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.calculator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 向量drawable支持
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 发布版本签名配置（可选）
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Kotlin编译器选项
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Lint配置
    lint {
        abortOnError = false
        checkReleaseBuilds = true
        htmlReport = true
        xmlReport = true
        htmlOutput = file("$project.buildDir/reports/lint/lint.html")
        xmlOutput = file("$project.buildDir/reports/lint/lint.xml")
        baseline = file("lint-baseline.xml")

        // 禁用一些不必要的检查
        disable += setOf(
            "TypographyFractions",
            "TypographyQuotes",
            "ObsoleteLintCustomCheck"
        )
    }

    // 测试选项配置
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }
}

// Detekt配置
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

dependencies {
    // 核心Android依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)

    // Compose BOM和相关依赖
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // 导航组件（如果需要）
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // 数据持久化
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 单元测试依赖
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.4")

    // Android Instrumentation测试
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest")

    // Debug依赖
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation("androidx.compose.ui:ui-tooling")

    // 静态分析工具
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.4")

    // 性能监控（可选）
    implementation("androidx.tracing:tracing:1.2.0")

    // 内存泄漏检测（仅Debug版本）
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// 任务配置
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "17"
}

// 自定义任务：运行所有质量检查
tasks.register("qualityCheck") {
    dependsOn("lint", "detekt", "testDebugUnitTest")
    group = "verification"
    description = "运行所有代码质量检查"
}
