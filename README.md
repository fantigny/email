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

## portable (zip)\*

### windows  
[win32](https://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.zip),
[win64](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.zip)

### linux  
[linux32](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.zip),
[linux64](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.zip)

### macOs  
[macOs](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-macOs.zip)

\**shipped with Java runtime from Oracle*

## installable\*

### windows  
[win32](https://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.exe),
[win64](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.exe)

### debian flavors  
[deb32](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.deb),
[deb64](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.deb)

### readhat flavors  
[rpm32](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.rpm),
[rpm64](http://speederpan.uk.to/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.rpm)

\**shipped with Java runtime from Oracle*

## continuous integration
latest build available in [Jenkins](https://speederpan.uk.to/jenkins/job/mail-client-distrib%20(continuous))
