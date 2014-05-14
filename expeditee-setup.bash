
# Script written to be compatible with a variety of 'sh' derived shells: 
#   ash, bash, dash ...

explang=""
# encoding inputs and outputs
exptoenc=""
expfromenc=""

# see if the shell has any language environment variables set
# see locale(7) manpage for this ordering.
if test ! -z "$LC_ALL" ; then
  explang="$LC_ALL"
elif test ! -z "$LANG"; then
  explang="$LANG"
fi


# note... all our output strings have the charset hard-coded, but
# people may be using a different encoding in their terminal. LANG
# strings look like "en_NZ.UTF-8".

# Determine the requested output encoding
case $explang in
  *.*)
    exptoenc=`echo $explang | sed 's/.*\.//'`
    ;;
esac

# Our French and Spanish strings are in 'iso-8859-1' (Latin 1) encoding; 
# Russian is the Cyrillic alphabet encoding, 'KOI8-R'
case $explang in
  fr*|FR*)
    explang=fr
    expfromenc="iso-8859-1"
  ;;
  es*|ES*)
    explang=es
    expfromenc="iso-8859-1"
  ;;
  ru*|RU*)
    explang=ru
    expfromenc="koi8r"
  ;;
  *) # default
    explang=en
    expfromenc="iso-8859-1"
  ;;
esac  

# "iconv" is the program for converting text between encodings.
expiconv=`which iconv 2>/dev/null`
if test $? -ne 0 || test ! -x "$expiconv" || test -z "$expfromenc" || test -z "$exptoenc"; then
# we can't convert encodings from some reason
  expiconv="cat"
else
# add the encodings
  expiconv="$expiconv -f $expfromenc -t $exptoenc"
fi


# ASCII art generated at:
#   http://patorjk.com/software/taag/
# Font = Epic
#
echo " ______           ______  ______  ______  __________________ ______  ______ "
echo "(  ___ \|\     /|(  ___ )(  ___ \(  __  \ \__   __/\__   __/(  ___ \(  ___ \\"
echo "| (   \/( \   / )| (   )|| (   \/| (  \  )   ) (      ) (   | (   \/| (   \/"
echo "| (_     \ (_) / | (___)|| (_    | |   ) |   | |      | |   | (_    | (_    "
echo "|  __)    ) _ (  |  ____)|  __)  | |   | |   | |      | |   |  __)  |  __)  "
echo "| (      / ( ) \ | (     | (     | |   ) |   | |      | |   | (     | (     "
echo "| (___/\( /   \ )| )     | (___/\| (__/  )___) (___   | |   | (___/\| (___/\\"
echo "(______/|/     \||/      (______/(______/ \_______/   )_(   (______/(______/"
echo
echo "(C) 2013, University of Waikato, New Zealand"
echo
echo
echo

# make sure we are sourced, and not run

if test "$0" != "`echo $0 | sed s/expeditee-setup\.bash//`" ; then
# if $0 contains "expeditee-setup.bash" we've been run... $0 is shellname if sourced.
# One exception is zsh has an option to set it temporarily to the script name
  if test -z "$ZSH_NAME" ; then
  # we aren't using zsh
  expeditee_not_sourced=true
  fi
fi

if test -n "$expeditee_not_sourced" ; then
  case "$explang" in
 "es")
eval $expiconv <<EOF
      Error: Asegúrese de compilar este guión, no de ejecutarlo. P. ej.:
         $ source expeditee-setup.bash
      o
         $ . ./expeditee-setup.bash
      no
         $ ./expeditee-setup.bash
EOF
  ;;
  "fr")
eval $expiconv <<EOF
      Erreur: Assurez-vous de "sourcer" le script, plutôt que de l'exécuter. Ex:
         $ source expeditee-setup.bash
      ou
         $ . ./expeditee-setup.bash
      pas
         $ ./expeditee-setup.bash
EOF
  ;;
  "ru")
eval $expiconv <<EOF
      ïÛÉÂËÁ: õÄÏÓÔÏ×ÅÒØÔÅÓØ × ÉÓÔÏÞÎÉËÅ ÜÔÏÇÏ ÓËÒÉÐÔÁ. îÅ ÉÓÐÏÌÎÑÊÔÅ ÅÇÏ.
      îÁÐÒÉÍÅÒ:
         $ source expeditee-setup.bash
      ÉÌÉ
         $ . ./expeditee-setup.bash
      ÎÅÔ
         $ ./expeditee-setup.bash
EOF
  ;;
  *)
eval $expiconv <<EOF
	Error: Make sure you source this script, not execute it. Eg:
		$ source expeditee-setup.bash
	or
		$ . ./expeditee-setup.bash
	not
		$ ./expeditee-setup.bash
EOF
  ;;
  esac
elif test -n "$EXPEDITEE_HOME" ; then
  case "$explang" in
  "es")
    echo '¡Su ambiente ya está listo para Expeditee!' | eval $expiconv
  ;;
  "fr")
    echo 'Votre environnement est déjà préparé pour Expeditee!' | eval $expiconv
 ;;
  "ru")
    echo '÷ÁÛÅ ÏËÒÕÖÅÎÉÅ ÕÖÅ ÎÁÓÔÒÏÅÎÏ ÄÌÑ Expeditee!' | eval $expiconv
  ;;
  *)
    echo 'Your environment is already set up for Expeditee!'
  ;;
  esac
elif test ! -f expeditee-setup.bash ; then
  case "$explang" in
    "es")
      echo 'Usted debe compilar el guión desde el interior del directorio de inicio' | eval $expiconv
  ;;
    "fr")
      echo 'Vous devez trouver la source du script dans le répertoire de base de Expeditee' | eval $expiconv
  ;;
    "ru")
      echo '÷ÁÍ ÎÅÏÂÈÏÄÉÍ ÉÓÔÏÞÎÉË ÓËÒÉÐÔÁ ÉÚ ÂÁÚÏ×ÏÊ ÄÉÒÅËÔÏÒÉÉ Expeditee' | eval $expiconv
  ;;
   *)
      echo 'You must source the script from within the Expeditee home directory'
  ;;
  esac
else
  EXPEDITEE_HOME=`pwd`
  export EXPEDITEE_HOME

  if test "x$EXPOS" = "x" ; then
    EXPOS=`uname -s | tr 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' 'abcdefghijklmnopqrstuvwxyz'`
    # check for running sh/bash under cygwin
    if test "`echo $EXPOS | sed 's/cygwin//'`" != "$EXPOS" ;
    then
      EXPOS=windows
    fi
  fi
  export EXPOS

  # check for running sh/bash under mingw
  if test "`echo $EXPOS | sed 's/mingw//'`" != "$EXPOS" ;
  then
    EXPOS=windows
  fi
  export EXPOS

  # Establish cpu architecture
  # 32-bit or 64-bit?
  UNAME_HW_MACH=`uname -m`

  # Following test came from VirtualBox's Guest Additions autostart.bash
  # (adapted for use in Expeditee)
  case "$UNAME_HW_MACH" in
    i[3456789]86|x86|i86pc)
      EXPARCH='32bit'
      ;;
    x86_64|amd64|AMD64)
      EXPARCH='64bit'
      ;;
    *)
      echo "Unknown architecture: $UNAME_HW_MACH"
      ;;
  esac

  # Only want non-trival EXPARCH value set if there is evidence of
  # the installed bin (lib, ...) directories using linux32, linux64
  # (otherwise probably looking at an SVN compiled up version for single OS)
  if test ! -d "$EXPEDITEE_HOME/$EXPOS/$EXPARCH" ;
  then 
    EXPARCH=""
  fi

  export EXPARCH

  PATH=$EXPEDITEE_HOME/script:$EXPEDITEE_HOME/$EXPOS/$EXPARCH:$PATH
  export PATH
  
#  if test "$EXPOS" = "linux" ; then
#      LD_LIBRARY_PATH="$EXPEDITEE_HOME/lib/$EXPOS$EXPARCH:$LD_LIBRARY_PATH"
#	  export LD_LIBRARY_PATH
#  elif test "$EXPOS" = "darwin" ; then
#      DYLD_LIBRARY_PATH="$EXPEDITEE_HOME/lib/$EXPOS$EXPARCH:$DYLD_LIBRARY_PATH"
#      export DYLD_LIBRARY_PATH
#  fi
 
#  MANPATH=$MANPATH:$EXPEDITEE_HOME/man
#  export MANPATH

  case "$explang" in
    "es")
      echo 'Su ambiente ha sido configurado para correr los programas Expeditee.' | eval $expiconv
    ;;
    "fr")
      echo 'Votre environnement a été configuére avec succès pour exécuter Expeditee' | eval $expiconv
    ;;
    "ru")
      echo '÷ÁÛÅ ÏËÒÕÖÅÎÉÅ ÂÙÌÏ ÕÓÐÅÛÎÏ ÎÁÓÔÒÏÅÎÏ, ÞÔÏÂÙ ÕÓÔÁÎÏ×ÉÔØ Expeditee' | eval $expiconv
    ;;
    *)
      echo 'Your environment has successfully been set up to run Expeditee'
    ;;
  esac
fi
unset expeditee_not_sourced
unset expiconv
unset expfromenc
unset exptoenc

if test -f local-setup.bash ; then
    echo "+ Sourcing local-setup.bash"
    . ./local-setup.bash 
fi

#which java >/dev/null 2>&1 
#
#if [ $? != 0 ] ; then
#  echo "Unable to find a Java runtime" >2
#else 
#  echo "-----"
#  echo "To run the Expeditee Browser, type:"
#  echo "  expeditee"
#  echo ""
#fi

