/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
plugins {
    idea
    kotlin("jvm") version "1.3.61"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir("src/main/kotlin")

        dependencies {
            implementation(kotlin("stdlib"))
        }
    }
}