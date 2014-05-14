@echo off
setlocal enabledelayedexpansion

color 0A
set startdir=%CD%
rem pushd "%CD%"
CD /D "%~dp0"
set EXPLANG=en

if "%EXPMODE%" == "" set EXPMODE=local

::  -------- Run the Expeditee Browser --------

:: This script must be run from within the directory in which it lives
if exist gli.bat goto start
    if "%EXPLANG%" == "en" echo This script must be run from the directory in which it resides.
    if "%EXPLANG%" == "es" echo Este gui¢n deber  ejecutarse desde el directorio en el que reside.
    if "%EXPLANG%" == "fr" echo Ce script doit ˆtre ex‚cut‚ … partir du r‚pertoire dans lequel il se trouve.
    if "%EXPLANG%" == "ru" echo â®â áªà¨¯â ¤®«¦¥­ ¡ëâì ¢§ïâ ¨§ ¤¨à¥ªâ®à¨¨, ¢ ª®â®à®© ®­ à á¯®«®¦¥­
    goto exit
	
:start
if "%OS%" == "Windows_NT" goto progName
    :: Invoke a new command processor to ensure there's enough environment space
    if "%1" == "Second" goto progName
		command /E:2048 /C %0 Second %1 %2 %3 %4 %5 %6 %7 %8 %9
		shift
		goto done

:progName
if not "%PROGNAME%" == "" goto findExpeditee
	:: otherwise PROGNAME was not set, so default to the Expeditee Browser program
	if "%EXPLANG%" == "es" set PROGNAME=Navegador Expeditee
	if "%EXPLANG%" == "fr" set PROGNAME=Navigateur Expeditee
	if "%EXPLANG%" == "ru" set PROGNAME=âÒÁÕÚÅÒ Expeditee
	:: if the PROGNAME is still not set, then set the language to English
	if "%PROGNAME%" == "" set PROGNAME=Expeditee Browser


if "%PROGABBR%" == "" set PROGABBR=Expeditee
if "%PROGNAME_EN%" == "" set PROGNAME_EN=Expeditee Browser

:: Now need to work out the _VERSION, EXPEDITEE_HOME (and if GS3, then GSDL3SRCHOME and GSDL3HOME)
:findExpeditee
call findexp.bat
if "%EXPEDITEE_HOME%" == "" goto exit

:checkUserPermissions
	echo.
	echo Checking if the Expeditee frameset directory is writable ...
	(echo This is a temporary file. It is safe to delete it. > "!EXPEDITEE_HOME!\collect\testing.tmp" ) 2>nul
	if exist "%EXPEDITEE_HOME%\collect\testing.tmp" goto deleteTempFile 
	if "%1" == "Elevated" goto printWarning
	echo ... FAILED
	echo The %PROGNAME% cannot write to the collection directory (!EXPEDITEE_HOME!\collect)
	echo Requesting elevated status to become admin user to continue.
	"%EXPEDITEE_HOME%\bin\windows\gstart.exe" %0 Elevated %1 %2 %3 %4 %5 %6 %7 %8 %9
    goto done
	
:printWarning
	echo ... FAILED
	echo The %PROGNAME% cannot write to the log directory (!EXPEDITEE_HOME!\collect). 
	echo Attempting to continue without permissions.
	goto shiftElevated

:deleteTempFile
	echo ... OK
	del "%EXPEDITEE_HOME%\collect\testing.tmp"

:shiftElevated
:: Shift "Elevated" (one of our own internal command words) out of the way if present
:: so the command-line is as it was when the user initiated the command
	if "%1" == "Elevated" shift

:: Make sure we're in the GLI folder, even if located outside a GS installation
CD /D "%~dp0"

:: Need to find Java. If found, JAVA_EXECUTABLE will be set
call findjava.bat
if "%JAVA_EXECUTABLE%" == "" goto exit


:runExpeditee


if not "%EXPEDITEE_HOME%" == "" (
    echo EXPEDITEE_HOME:
    echo !EXPEDITEE_HOME!
    echo.
)


:: ---- Finally, run Expeditee ----
if "%EXPLANG%" == "en" echo Running the %PROGNAME%...
if "%EXPLANG%" == "es" echo Ejecutando la %PROGNAME%...
if "%EXPLANG%" == "fr" echo Ex‚cution de %PROGNAME%
if "%EXPLANG%" == "ru" echo ’¥ªãé¨© ¡¨¡«¨ %PROGNAME%...

:: -Xms32M          To set minimum memory
:: -Xmx32M          To set maximum memory
:: -verbose:gc      To set garbage collection messages
:: -Xincgc          For incremental garbage collection
:: -Xprof           Function call profiling
:: -Xloggc:<file>   Write garbage collection log


:: "%JAVA_EXECUTABLE%" -cp classes/;GLI.jar;lib/apache.jar;lib/qfslib.jar;lib/rsyntaxtextarea.jar org.greenstone.gatherer.GathererProg -gsdl "%EXPEDITEE_HOME%" -gsdlos %GSDLOS% -gsdl3 "%GSDL3HOME%" -gsdl3src "%GSDL3SRCHOME%" %1 %2 %3 %4 %5 %6 %7 %8 %9

set jars=jars\JEP.jar;jars\JFreeCharts.jar;jars\activation.jar;jars\cobra.jar;jars\ext;jars\iText-2.1.3.jar;jars\jazzy-core.jar;jars\jcommon-1.0.13.jar;jars\js.jar;jars\lobo-pub.jar;jars\lobo.jar;jars\mail.jar;jars\xercesImpl.jar

"%JAVA_EXECUTABLE%" -classpath %jars%;releases\Expeditee.jar org.expeditee.gui.Browser %1 %2 %3 %4 %5 %6 %7 %8 %9


:finRun
    if "%EXPLANG%" == "en" echo Done.
    if "%EXPLANG%" == "es" echo Hecho.
    if "%EXPLANG%" == "fr" echo Termin‚.
    if "%EXPLANG%" == "ru" echo ‚ë¯®«­¥­®.
    goto done


:exit
echo.
pause
color 07
rem popd

:done
:: ---- Clean up ----
set JAVA_EXECUTABLE=
set EXPMODE=
set PROGNAME=
set PROGNAME_EN=
set PROGFULLNAME=
set PROGABBR=
color 07
rem popd
cd "%startdir%"
set startdir=

endlocal
