# FisherMail
cross-platform e-mail client  
(only for Gmail just yet)

## system specific\*

[win-32](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win32.zip),
[win-64](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-win64.zip)

[linux-32](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin32.zip),
[linux-64](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-lin64.zip)

[macOs](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail-macOs.zip)

\**includes Java SE Runtime Environment 8*

## cross platform 
__*`Java 8 or newer with JavaFX required`*__

[FisherMail.jar](http://speederpan.uk.to/jenkins/job/PROD%20mail-client-distrib/lastSuccessfulBuild/artifact/mail-client-distrib/distrib/FisherMail.jar)

### windows
download and install the [latest version of Java](https://www.java.com/en/download/)  
start FisherMail with a double-click on file FisherMail.jar

### ubuntu
install Java and JavaFx  
`sudo apt install default-jre openjfx`

start FisherMail with command  
`java --module-path /usr/share/openjfx/lib --add-modules=javafx.web,javafx.swing -jar FisherMail.jar`

## continuous integration
latest build available in [Jenkins](http://speederpan.uk.to/jenkins/job/DEV%20mail-client-distrib)
