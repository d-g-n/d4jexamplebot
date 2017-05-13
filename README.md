# d4jexamplebot
A basic click-to-run bot using D4J.

## Import project
![IntelliJ import step 1](http://i.imgur.com/CggB7lG.png)

![IntelliJ import step 2](http://i.imgur.com/zZqiu4n.png)

If you get this view with no SDK to choose from, click on the green plus
sign and select your jdk folder, you should end up with this:

![IntelliJ SDK view](http://i.imgur.com/sgSGIr4.png)

## How to run

Import the project as a maven project into your IDE of choice.

Once imported, either build it as a jar or run it from your IDE. When
unning the bot, the first argument of the exec must be your token.

When running as a jar: `java -jar builtjar.jar TOKENHERE`

When running from an IDE:

IntelliJ:

![IntelliJ Example](http://i.imgur.com/qkjwvie.png)

Eclipse:

![Eclipse Example](http://i.imgur.com/v0mLql6.png)


## How to build

A fatjar can be built through maven simply by running the `mvn package`
goal in whatever manner your IDE requires.
