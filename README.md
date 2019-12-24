# FisherMail
cross-platform e-mail client  
(only for Gmail just yet)

## downloads
| usage            | download                             |
|------------------|--------------------------------------|
| cross-platform   | [FisherMail.jar](#cross-platform)    |
| macOs app        | [FisherMail-macOs.zip](#macos-1)     |
| windows install  | [FisherMail-win64.exe](#install)     |
| windows portable | [FisherMail-win64.zip](#portable)    |
| linux deb        | [FisherMail-lin64.deb](#deb-install) |
| linux rpm        | [FisherMail-lin64.rpm](#rpm-install) |
| linux portable   | [FisherMail-lin64.zip](#portable-1)  |  

## cross-platform 
__*`Java 8 or newer with JavaFX required`*__

| download | [FisherMail.jar](https://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail.jar) |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------|

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
-client \
-jar FisherMail.jar
```

## macOs  
__*`shipped with Java runtime from Oracle`*__

| download | [FisherMail-macOs.zip](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-macOs.zip) |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|

## windows  
__*`shipped with Java runtime from Oracle`*__
### install
| download | [FisherMail-win64.exe](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.exe)  |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
|          | [FisherMail-win32.exe](https://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.exe) |
### portable
| download | [FisherMail-win64.zip](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.zip)  |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
|          | [FisherMail-win32.zip](https://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.zip) |

## linux  
__*`shipped with Java runtime from Oracle`*__
### deb install
| download | [FisherMail-lin64.deb](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.deb) |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|          | [FisherMail-lin32.deb](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.deb) |
### rpm install
| download | [FisherMail-lin64.rpm](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.rpm) |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|          | [FisherMail-lin32.rpm](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.rpm) |
### portable
| download | [FisherMail-lin64.zip](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.zip) |
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|          | [FisherMail-lin32.zip](http://speederpan.com/jenkins/job/mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.zip) |


## continuous integration
latest build available in [Jenkins](https://speederpan.com/jenkins/job/mail-client-distrib%20(continuous))
