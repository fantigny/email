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
```
sudo apt install default-jre openjfx
```  
+ start FisherMail with command
```
java \
--module-path /usr/share/openjfx/lib \
--add-modules=javafx.swing,javafx.web \
--add-exports=javafx.web/com.sun.webkit.network=ALL-UNNAMED \
--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
-Djdk.gtk.version=2.2 \
-jar FisherMail.jar
```

## system specific\*

windows install [win-32.exe](https://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.exe),
[win-64.exe](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.exe)

windows portable [win-32.zip](https://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.zip),
[win-64.zip](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.zip)

macOs app [macOs-64.zip](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-macOs.zip)

debian package [linux-32.deb](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.deb),
[linux-64.deb](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.deb)

rpm package [linux-32.rpm](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.rpm),
[linux-64.rpm](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.rpm)

linux portable [linux-32.zip](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.zip),
[linux-64.zip](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.zip)

\**shipped with Java runtime from Oracle*

## continuous integration
latest build available in [Jenkins](https://speederpan.uk.to/jenkins/job/mail-client-distrib%20(continuous))
