

which javac >/dev/null 2>&1 

if [ $? != 0 ] ; then
  export JAVA_HOME="/cygdrive/c/Program Files/Java/jdk1.8.0"
  export PATH=$JAVA_HOME/bin/:$PATH

  echo "+ Set JAVA_HOME to $JAVA_HOME and updated path"

  cygpath 2>/dev/null
  if [ $? != 0 ] ; then
    echo "Detected runing in Cygwin environment => Changing JAVA_HOME to Windows native format"
    export JAVA_HOME=`cygpath -w "$JAVA_HOME"`
  fi
fi


