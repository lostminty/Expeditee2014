@echo off

setlocal enabledelayedexpansion

pushd "%CD%"
CD /D "%~dp0"
set EXPLANG=en

:: ASCII art generated at:
::   http://patorjk.com/software/taag/
:: Font = Epic
::
echo  ______           ______  ______  ______  __________________ ______  ______
echo (  ___ \^|\     /^|(  ___ )(  ___ \(  __  \ \__   __/\__   __/(  ___ \(  ___ \
echo ^| (   \/( \   / )^| (   )^|^| (   \/^| (  \  )   ) (      ) (   ^| (   \/^| (   \/
echo ^| (__    \ (_) / ^| (___)^|^| (_    ^| ^|   ) ^|   ^| ^|      ^| ^|   ^| (__   ^| (__   
echo ^|  __)    ) _ (  ^|  ____)^|  __)  ^| ^|   ^| ^|   ^| ^|      ^| ^|   ^|  __)  ^|  __)  
echo ^| (      / ( ) \ ^| (     ^| (     ^| ^|   ) ^|   ^| ^|      ^| ^|   ^| (     ^| (     
echo ^| (___/\( /   \ )^| )     ^| (___/\^| (__/  )___) (___   ^| ^|   ^| (___/\^| (___/\
echo (______/^|/     \^|^|/      (______/(______/ \_______/   )_(   (______/(______/
echo.
echo (C) 2013, University of Waikato, New Zealand
echo.
echo.
echo.

if "!EXPEDITEE_HOME!" == "" goto start
if "!EXPEDITEE_HOME!" == "!CD!" if not "!EXPOS!" == "" (
	echo Your environment is already set up for Expeditee
	goto done
)

:start
if "!OS!" == "Windows_NT" goto WinNT
if "!OS!" == "" goto Win95
if "!EXPLANG!" == "en" echo Setup failed - your PATH has not been set
if "!EXPLANG!" == "es" echo No se pudo realizar la configuraci¢n - no se ha establecido la RUTA.
if "!EXPLANG!" == "fr" echo Ech‚c de l'installation - votre variable PATH n'a pas ‚t‚ ajust‚e
if "!EXPLANG!" == "ru" echo “áâ ­®¢ª  ­¥ ã¤ « áì - “’œ ­¥ ¡ë« ãáâ ­®¢«¥­
goto End

:WinNT
set EXPEDITEE_HOME=%CD%
set EXPOS=windows

set PATH=!EXPEDITEE_HOME!\windows;!EXPEDITEE_HOME!\script;!PATH!

set EXP_CP_SET=yes
goto Success

:Win95
if "%1" == "SetEnv" goto Win95Env
REM We'll invoke a second copy of the command processor to make
REM sure there's enough environment space
COMMAND /E:2048 /K %0 SetEnv
goto End

:Win95Env
set EXPEDITEE_HOME=%CD%
set EXPOS=windows

set PATH=!EXPEDITEE_HOME!\windows;!EXPEDITEE_HOME!\script;!PATH!

set EXP_CP_SET=yes
goto Success


:Success
if "!EXPLANG!" == "en" echo.
if "!EXPLANG!" == "en" echo Your environment has successfully been set up to run Expeditee.
if "!EXPLANG!" == "en" echo Note that these settings will only have effect within this MS-DOS
if "!EXPLANG!" == "en" echo session. You will therefore need to rerun expeditee-setup.bat if you want
if "!EXPLANG!" == "en" echo to run Expeditee programs from a different MS-DOS session.
if "!EXPLANG!" == "en" echo.

if "!EXPLANG!" == "es" echo.
if "!EXPLANG!" == "es" echo Su ambiente ha sido configurado para correr los programas Expeditee.
if "!EXPLANG!" == "es" echo Recuerde que estos ajustes £nicamente tendr n efecto dentro de esta sesi¢n
if "!EXPLANG!" == "es" echo MS-DOS. Por lo tanto deber  ejecutar nuevamente expeditee-setup.bat si desea
if "!EXPLANG!" == "es" echo correr los programas de Expeditee desde una sesi¢n MS-DOS diferente.
if "!EXPLANG!" == "es" echo.

if "!EXPLANG!" == "fr" echo.
if "!EXPLANG!" == "fr" echo Votre environnement a ‚t‚ configu‚re avec succŠs pour ex‚cuter Expeditee
if "!EXPLANG!" == "fr" echo Notez que ces paramŠtrages n'auront d'effet que dans cette session MS-DOS.
if "!EXPLANG!" == "fr" echo Vous devrez par cons‚quent r‚ex‚cuter expeditee-setup.bat si vous voulez faire
if "!EXPLANG!" == "fr" echo lancer des programmes Expeditee dans une autre session MS-DOS.
if "!EXPLANG!" == "fr" echo.

if "!EXPLANG!" == "ru" echo.
if "!EXPLANG!" == "ru" echo ‚ è¥ ®ªàã¦¥­¨¥ ¡ë«® ãá¯¥è­® ­ áâà®¥­®, çâ®¡ë ãáâ ­®¢¨âì Expeditee Ž¡à â¨â¥
if "!EXPLANG!" == "ru" echo ¢­¨¬ ­¨¥, çâ® íâ¨ ­ §­ ç¥­¨ï ¡ã¤ãâ â®«ìª® ¨¬¥âì íää¥ªâ ¢ ¯à¥¤¥« å íâ®£® MS DOS
if "!EXPLANG!" == "ru" echo á¥áá¨ï. ‚ë ¡ã¤¥â¥ ¯®íâ®¬ã ¤®«¦­ë ¯®¢â®à­® ã¯à ¢«ïâì expeditee-setup.bat, ¥á«¨ ‚ë å®â¨â¥
if "!EXPLANG!" == "ru" echo ã¯à ¢«ïâì ¯à®£à ¬¬ ¬¨ ‡¥«ñ­ëå ¨§¢¥à¦¥­­ëå ¯®à®¤ ®â à §«¨ç­®© á¥áá¨¨ MS DOS.
if "!EXPLANG!" == "ru" echo.

:End
endlocal & set PATH=%PATH%& set EXPEDITEE_HOME=%EXPEDITEE_HOME%& set EXPOS=%EXPOS%

setlocal enabledelayedexpansion

if exist "%EXPEDITEE_HOME%\local" (
  if exist "!EXPEDITEE_HOME!\local\setup.bat" (
    echo.
    echo Running !EXPEDITEE_HOME!\local\setup.bat
    cd "!EXPEDITEE_HOME!\local"
    call setup.bat 
    cd "!EXPEDITEE_HOME!"
  )
)

:: Consider the equivlent converted to DOS syntax??
:: which >/dev/null java 2>&1 



:done
popd
endlocal & set PATH=%PATH%& set EXPEDITEE_HOME=%EXPEDITEE_HOME%& set EXPOS=%EXPOS%

:: if [ $? != 0 ] ; then
::   echo "Unable to find a Java runtime" >2
:: else 
::  echo "-----"
::  echo "To run the Expeditee Browser, type:"
::  echo "  expeditee"
::  echo ""
::fi
