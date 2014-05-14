
. expeditee-setup.bash


#which javac  >/dev/null 2>&1

#if [ $? != 0 ] ; then
#  export JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.7.0_45"
#  export PATH=$JAVA_HOME/bin/:$PATH

#  echo "+ Set JAVA_HOME to $JAVA_HOME and updated path"

#  cygpath 2>/dev/null
#  if [ $? != 0 ] ; then
#    echo "Detected runing in Cygwin environment => Changing JAVA_HOME to Windows native format"
#    export JAVA_HOME=`cygpath -w "$JAVA_HOME"`
#  fi
#
#fi


expeditee_home=`pwd`;
export ANT_HOME="$expeditee_home/dev/packages/apache-ant-1.9.2"
export PATH="$ANT_HOME/bin/:$PATH"

echo "+ Set ANT_HOME to"
echo "+   $ANT_HOME"
echo "+ and updated path"

# Update path to include 'dev/bin/script'
export PATH="$expeditee_home/dev/bin/script:$PATH"

if test -e local-devel.bash ; then
    echo "+ Sourcing local-devel.bash"
    . ./local-devel.bash
fi


echo "-----"
echo "To compile Expeditee, type:"
echo ""
echo "  ant build"
echo ""
echo "To run the compiled version, type:"
echo ""
echo "  ant run"
echo "-----"


