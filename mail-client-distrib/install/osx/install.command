clear
echo
echo
echo "installing FisherMail app:"

cd "`dirname "$0"`"

echo
echo "(1/2) copy..."
sudo cp -R FisherMail.app /Applications

echo
echo "(2/2) registration..."
sudo xattr -d com.apple.quarantine /Applications/FisherMail.app

echo
echo
