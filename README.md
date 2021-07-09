# SlimView

A super-fast and simple directory-based image viewer and browser for Windows, macOS and Linux.

![Screenshot](https://github.com/antikmozib/SlimView/blob/master/screenshot.png?raw=true)

<h1>Building</h1>

**Tested on Windows with Java 11 only**

_Java, Maven and [JavaFX jmods](https://openjfx.io/openjfx-docs/) (required for producing jlink images) must be set as environment variables. Alternatively, use an IDE like IntelliJ IDEA to import as a Maven project._

`clone` the repository, `cd` into the root directory and execute `mvn clean`.

Three Windows batch files are provided to ease building and running:

* `build.bat` and `run.bat`: Builds and runs the program using Maven.
* `jlink-run.bat`: Builds the program using Maven, produces a native jlink image and uses that to run the program.
