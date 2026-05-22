#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*->\(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="$(cd "$(dirname \"$PRG\")"; pwd -P)"
APP_HOME="$(cd "$(dirname \"$SAVED\")"; pwd -P)"
APP_HOME_PARENT="$(dirname "$APP_HOME")"

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='" "-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != "unlimited" if you use a older JDK version
MAX_FD="unlimited"

# Use the maximum available, or set MAX_FD != "unlimited" if you use a older JDK version
MAX_FD="unlimited"

warn () {
    echo "$*" >&2
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
darwin=false
msys=false
cygwin=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
esac

if [ "$cygwin" = true -o "$msys" = true ] ; then
    APP_HOME_PARENT="`dirname \"$APP_HOME\""
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

APP_HOME_PARENT="`dirname \"$APP_HOME_PARENT\""
APP_HOME="$APP_HOME_PARENT/app"

if [ -z "$JAVA_HOME" ] ; then
    die "Please set the JAVA_HOME variable in your environment to match the location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$darwin" = true -a -z "$JAVA_OPTS" ] ; then
    JAVA_OPTS="-Xmx1024m"
fi

# For Cygwin or MSYS, switch paths to Windows format before running java
if [ "$cygwin" = true -o "$msys" = true ] ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Collect all arguments for the java command, stacking in reverse order
args=()
for arg in "$@" ; do
    if [[ "$arg" =~ ^-([Xx]|D).* ]] ; then
        args+=("$arg")
    elif [[ "$arg" =~ ^-[a-zA-Z]([a-zA-Z0-9]|_)*=.* ]] ; then
        args+=("$arg")
    else
        break
    fi
done

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Increase the maximum file descriptors if we can.
if ! "$darwin" && ! "$msys" ; then
    case `uname` in
      Linux*)
        MAX_FD=$( ulimit -H -n )
        ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'\([^']*\)'/'\1'\\\\\"\"'\"'\"\\\\\"\"'/g;s/^/'\"'/;s/$//'\"'/" ; done
    echo " "
}
cargs=$( save "$@" )

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval "set -- $(
        printf '%s\n' "$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS" |
        xargs (echo cd \"$APP_HOME\" &&
        printf '%s\n' 'exec "$JAVACMD"' \
        "-cp" \
        "$CLASSPATH" \
        "org.gradle.wrapper.GradleWrapperMain" \
        "$cargs")
    )"

exec "$JAVACMD" \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
