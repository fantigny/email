#!/bin/bash
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
$basedir/jre/bin/java -Djdk.gtk.version=2.2 -jar $basedir/FisherMail.jar
