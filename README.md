# Genshin Inventory Scanner for Android

![GitHub commit activity](https://img.shields.io/github/commit-activity/m/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub last commit](https://img.shields.io/github/last-commit/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub issues](https://img.shields.io/github/issues/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub pull requests](https://img.shields.io/github/issues-pr/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub](https://img.shields.io/github/license/steve1316/genshin-inventory-scanner-android?logo=GitHub)

This mobile application serves as a native way to scan your inventory in [GOOD (Genshin Open Object Description)](https://frzyc.github.io/genshin-optimizer/#/doc) format for websites like [Genshin Optimizer](https://frzyc.github.io/genshin-optimizer/) to parse data from for the mobile video game, Genshin Impact.

# Features

-   [x] Scan for the following data on the Inventory screen:
    -   [x] Weapons including Level, Refinement, and Ascension.
    -   [x] Artifacts including Level, Main Stat, and Sub Stats.
    -   [x] Materials including Character Development Items.
    -   [ ] Characters including their Level, Ascension, Constellations, and Talents.

# Requirements

1. [Android Device or Emulator (Nougat 7.0+)](https://developer.android.com/about/versions)
    1. Tested emulator was Bluestacks 5 with the following settings:
        - P64 (Beta)
        - 1080x1920 (Portrait Mode as emulators do not have a way to tell the bot that it rotated.)
        - 240 DPI
        - 4+ GB of Memory

# Instructions

1. Download the latest .apk from the Releases section on the right side of this page right under the About section and then install the application.
2. TODO

# Technologies used

1. [MediaProjection - Used to obtain full screenshots](https://developer.android.com/reference/android/media/projection/MediaProjection)
2. [AccessibilityService - Used to dispatch gestures like tapping and scrolling](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
3. [OpenCV Android 4.5.1 - Used to template match](https://opencv.org/releases/)
4. [Tesseract4Android 2.1.1 - For performing OCR on the screen](https://github.com/adaptech-cz/Tesseract4Android)
5. [AppUpdater 2.7 - For automatically checking and notifying the user for new app updates](https://github.com/javiersantos/AppUpdater)
