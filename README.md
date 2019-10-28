# sql-runner

Java tool to run SQL Stress tests, based on this tool: https://github.com/ErikEJ/SqlQueryStress.

To download binary versions, go to the "Releases" tab.

Screenshot:

![image](https://user-images.githubusercontent.com/1840605/67724924-629c7000-f9bf-11e9-8729-0f78bb8c9d82.png)

## Build from source

#### Run

To run it, just clone the repo and run the gradle task `jfxRun`:

    ./gradlew jfxRun
    
or if you are on windows:

    ./gradlew.bat jfxRun
    
#### Build

To build native binaries, use:

    ./gradlew jfxNative
