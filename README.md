# FisherMail
cross-platform e-mail client  
(only for Gmail just yet)

## cross platform 
__*`Java 8 or newer with JavaFX required`*__

[FisherMail.jar](https://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail.jar)

### windows
+ download and install the [latest version](https://www.java.com/en/download/) of Java  
+ double-click on downloaded file `FisherMail.jar`

### macOs
+ download and install the [latest version](https://www.java.com/en/download/) of Java  
+ double-click on downloaded file `FisherMail.jar`

### ubuntu
+ install Java and JavaFx  
`sudo apt install default-jre openjfx`  
+ start FisherMail with command
```
java \
--module-path /usr/share/openjfx/lib \
--add-modules=javafx.swing,javafx.web \
--add-exports=javafx.web/com.sun.webkit.network=ALL-UNNAMED \
-Djdk.gtk.version=2 \
-jar FisherMail.jar
```

## system specific\*

[win-32](https://speederpan.uk.to/jenkins/job/REL%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.zip),
[win-64](http://speederpan.uk.to/jenkins/job/REL%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.zip)

[macOs-64](http://speederpan.uk.to/jenkins/job/REL%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-macOs.zip)

[linux-32](http://speederpan.uk.to/jenkins/job/REL%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.zip),
[linux-64](http://speederpan.uk.to/jenkins/job/REL%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.zip)

\**shipped with Java SE Runtime Environment*

## continuous integration
latest build available in [Jenkins](http://speederpan.uk.to/jenkins/job/mail-client-distrib)
