#
# Auto creates the manifest file needed for the Applet version of Apollo
#

argc=$#
if [ $argc != "1" ] ; then
  echo "Usage: $0 <manifest-filename>"
  exit -1
fi

# Write header
echo "Manifest-Version: SVN-Developer" > $1
echo "Created-By: Brook Novak and David Bainbridge" >> $1
echo "Main-Class: org.apollo.ApolloSystemApplet" >> $1
echo "Permissions: all-permissions" >> $1
echo "Application-Name: Apollo" >> $1

