#!/bin/bash
echo FisherMail is starting...
execPath="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
$execPath/openjdk/bin/java \
	--module-path $execPath/openjfx/lib \
	--add-modules=javafx.swing,javafx.web \
	--add-exports=javafx.web/com.sun.webkit.network=ALL-UNNAMED \
	--add-exports=javafx.base/com.sun.javafx=ALL-UNNAMED \
	-Djdk.gtk.version=2.2 \
	-client \
	-jar \
	$execPath/FisherMail.jar \
		0</dev/null \
		1>/dev/null \
		2>/dev/null \
		&
