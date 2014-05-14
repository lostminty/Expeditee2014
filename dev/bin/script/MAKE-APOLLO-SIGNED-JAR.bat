@echo off


cd releases/apollo

if exist ApolloSignedApplet.jar (
  del ApolloSignedApplet.jar
)


echo greenstone| jarsigner ^
             -keystore appletstore ^
             -signedjar ApolloSignedApplet.jar ^
       ApolloApplet.jar privateKey 2> NUL

cd ..\..

