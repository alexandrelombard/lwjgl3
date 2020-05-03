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

sourceSets {
    create("templates.assimp") {
        java {
            srcDirs("src/templates/kotlin")
        }
    }
}