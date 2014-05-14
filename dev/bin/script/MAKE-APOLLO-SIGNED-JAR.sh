#!/bin/bash


cd releases/apollo

/bin/rm -f ApolloSignedApplet.jar




if [ ! -e appletstore ] ; then
 echo "****"
 echo "* keytool check"
 echo "****"
 echo "* No 'appletstore' file detected"
 echo "* Please provide the following information to create one: "
 echo "*===="

 keytool -genkey -alias privateKey -keystore appletstore -storepass greenstone

 keytool -selfcert -keystore appletstore -alias privateKey

 echo "*===="
fi


#echo "greenstone" \
#  | jarsigner -keystore .greenstonestore \
#              -signedjar ApolloSignedApplet.jar \
#      Apollo.jar privateKey 2> /dev/null

echo greenstone| \
  jarsigner -keystore appletstore \
            -signedjar ApolloSignedApplet.jar \
    ApolloApplet.jar privateKey 2> /dev/null

cd ../..

