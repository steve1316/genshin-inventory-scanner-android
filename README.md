# Genshin Inventory Scanner for Android

![GitHub commit activity](https://img.shields.io/github/commit-activity/m/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub last commit](https://img.shields.io/github/last-commit/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub issues](https://img.shields.io/github/issues/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub pull requests](https://img.shields.io/github/issues-pr/steve1316/genshin-inventory-scanner-android?logo=GitHub) ![GitHub](https://img.shields.io/github/license/steve1316/genshin-inventory-scanner-android?logo=GitHub)

This mobile application serves as a native way to scan your inventory in [GOOD (Genshin Open Object Description)](https://frzyc.github.io/genshin-optimizer/#/doc) format for websites like [Genshin Optimizer](https://frzyc.github.io/genshin-optimizer/) to parse data from for the mobile video game, Genshin Impact.

# Features

-   [x] Scan for the following data on the Inventory screen:
    -   [x] Weapons including Level, Refinement, and Ascension.
    -   [x] Artifacts including Level, Main Stat, and Sub Stats.
    -   [x] Materials including Character Development Items.
    -   [x] Characters including their Level, Ascension, Constellations, and Talents.
-   [x] Output all scanned data into a JSON file in [GOOD](https://frzyc.github.io/genshin-optimizer/#/doc) format to be used in other third-party websites for parsing and optimization purposes.

# Requirements

1. [Android Device or Emulator (Nougat 7.0+)](https://developer.android.com/about/versions)
    1. Tested emulator was Bluestacks 5 with the following settings:
        - P64 (Beta)
        - 1920x1080 (Landscape Mode as emulators do not have a way to tell the bot that it rotated.)
        - 240 DPI
        - 4+ GB of Memory
    2. If using a physical Android device:
        - Device used to test this application was at 2400x1080 in Landscape Mode. Width should be the key factor but I am currently unsure if the height is a factor or not.

# Instructions

1. Download the latest .apk from the Releases section on the right side of this page right under the About section and then install the application.
2. Head to the Settings page of the application. Fill out your Traveler character name if you want the bot to detect and scan the Traveler.
3. Enable any of the scan(s) and then head back to the Home page and press the `Start` button in order to start giving permissions for the MediaProjectionService and the AccessibilityService.
4. After permissions are set, head back to the Home page and then press the `Start` button again to display the floating overlay button.
5. Now head to the inventory page in the game by pressing on the "backpack" / "bag" icon near the top right of the screen.
    - It is very highly recommended to put the floating overlay button to the bottom right corner of the screen.
6. Finally, you can press on the floating overlay button to start. Log messages can be viewed afterwards back on the Home page after the process finishes including where the actual log file was saved to.

# Technologies used

1. [MediaProjection - Used to obtain full screenshots](https://developer.android.com/reference/android/media/projection/MediaProjection)
2. [AccessibilityService - Used to dispatch gestures like tapping and scrolling](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
3. [OpenCV Android 4.5.1 - Used to template match](https://opencv.org/releases/)
4. [Tesseract4Android 2.1.1 - For performing OCR on the screen](https://github.com/adaptech-cz/Tesseract4Android)
5. [AppUpdater 2.7 - For automatically checking and notifying the user for new app updates](https://github.com/javiersantos/AppUpdater)
