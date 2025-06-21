plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}


android {
    namespace = "com.example.nknote"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nknote"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        //enable view binding to use xml layout files(view) in compose
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }


}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    //compose navigation
    val nav_version = "2.7.7"
    implementation ("androidx.navigation:navigation-compose:$nav_version")
    /*
    *rich text editor
    * https://github.com/wasabeef/richeditor-android
     */
    implementation ("jp.wasabeef:richeditor-android:2.0.0")

    //Image loader glide
    val glideComposeVersion = "1.0.0-beta01"
    implementation("com.github.bumptech.glide:compose:$glideComposeVersion")

    /*
    * Picture picking frame
    * https://github.com/LuckSiege/PictureSelector/tree/version_component
     */
    implementation("io.github.lucksiege:pictureselector:v3.11.2")
    implementation("io.github.lucksiege:compress:v3.11.2")

    implementation("com.github.stfalcon-studio:StfalconImageViewer:v1.0.1")

    //view binding
    implementation ("androidx.compose.ui:ui-viewbinding")

    //FlowRow
    implementation("com.google.accompanist:accompanist-flowlayout:0.32.0")

    //Date picker
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")

    //Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    //Gson
    implementation ("com.google.code.gson:gson:2.11.0")

    //FileOperator
    implementation ("com.github.javakam:file.core:3.9.8@aar")      //Core library required
    implementation ("com.github.javakam:file.selector:3.9.8@aar")  //File selector
    implementation ("com.github.javakam:file.compressor:3.9.8@aar")//Image compression, based on Luban

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}