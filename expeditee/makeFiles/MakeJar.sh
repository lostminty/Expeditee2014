rm -f ../bin/org/expeditee/*/*Test.class
rm -f ../bin/org/expeditee/*/*Test$*.class
jar cvfm ../releases/Expeditee.jar Manifest.txt -C ../bin org


echo ""
echo "*****"
echo "* This assumes you have freshly compiled Expeditee."
echo "* If not, run COMPILE-EVERYTHING.sh first!"
echo "*****"
