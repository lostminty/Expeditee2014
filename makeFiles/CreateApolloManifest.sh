#
# Auto creates Apollos manifest ... keeps classpath up to date
# By Brook Novak
#

argc=$#
if [ $argc != "1" ] ; then
  echo "Usage: $0 <manifest-filename>"
  exit -1
fi

# Write header
echo "Manifest-Version: Dev-2013" > $1
echo "Created-By: Brook Novak and David Bainbridge" >> $1
echo "Main-Class: org.apollo.ApolloSystem" >> $1
echo "Permissions: all-permissions" >> $1
echo "Application-Name: Expeditee" >> $1

# Create classpath
CLPATH=" ";
for j in `ls -1 ../releases/jars | grep "\.jar"`;
 do
	CLPATH="$CLPATH jars/$j";
 done;
 
for j in `ls -1 ../jars_apollo | grep "\.jar"`;
 do
	CLPATH="$CLPATH jars_apollo/$j";
 done;

echo "Class-Path:$CLPATH" >> $1
