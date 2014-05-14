#
# Auto creates Expeditee's manifest ... keeps classpath up to date
# By Brook Novak
#

argc=$#
if [ $argc != "1" ] ; then
  echo "Usage: $0 <manifest-filename>"
  exit -1
fi

# Write header
echo "Manifest-Version: DEV2008" > $1
echo "Created-By: Rob Akscyn, David Bainbridge, Michael Walmsley, Brook Novak, Jonothan ..." >> $1
echo "Main-Class: org.expeditee.gui.Browser" >> $1

pushd ../releases

# Create classpath
CLPATH=" ";
for j in jars/*.jar jars/ext/*.jar ;
 do
	CLPATH="$CLPATH $j";
 done;

popd

echo "Class-Path:$CLPATH" >> $1
