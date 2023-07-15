#!/bin/bash
if [ $EUID != 0 ]; then
    echo "This script must be run with root privileges."
    sudo -H -E "$0" "$@"
    exit $?
fi

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PATH_TO_AGENT="$SCRIPT_DIR/target/bitwig-docked-plugins-0.1-SNAPSHOT.jar"

# This script starts Bitwig Studio with our modifications. It intercepts the call to the real program and modifies the JVM arguments as needed to allow for our Java agent.

# In a normal startup, the .desktop file used by the user will call /usr/bin/bitwig-studio which is a symlink to /opt/bitwig-studio/bitwig-studio.
# /opt/bitwig-studio/bitwig-studio sets up some environment variables and then calls /opt/bitwig-studio/bin/BitwigStudio which is actually just
# a Java Virtual Machine that's been renamed.

# What we want to do is intercept the call to the JVM and append some new arguments, to include our java agent.
# We will do this by temporarily overwriting /opt/bitwig-studio/bin/BitwigStudio for the duration of the bootup process.

#
# Overwrite the stage two loader.
#

# if /opt/bitwig-studio/bin/BitwigStudio is not a bash script, save an extra backup to /opt/bitwig-studio/bin/BitwigStudio.bak.bak
if [[ $(file /opt/bitwig-studio/bin/BitwigStudio) == *"symbolic link to ../lib/jre/bin/java"* ]]; then
    cp /opt/bitwig-studio/bin/BitwigStudio /opt/bitwig-studio/bin/BitwigStudio.bak.bak
fi

mv /opt/bitwig-studio/bin/BitwigStudio /opt/bitwig-studio/bin/BitwigStudio.bak

cleanup() {
  echo "start-bitwig.sh: Restoring original /opt/bitwig-studio/bin/BitwigStudio loader"
  mv /opt/bitwig-studio/bin/BitwigStudio.bak /opt/bitwig-studio/bin/BitwigStudio
}
trap cleanup EXIT INT TERM



# Create the script using a HEREDOC
cat << EOF > /opt/bitwig-studio/bin/BitwigStudio
#!/bin/bash
# This script starts Bitwig Studio with our modifications. It replaces the call to the packaged JVM, with our own, and modifies the JVM arguments as needed to allow for our Java agent.

# The JVM shipped with Bitwig has been stripped of the java.instrument module, so we must use our own JVM.
# For Bitwig Studio 4.3.10 on Ubuntu, BWS uses (OpenJDK Java 17.0.7) and strips some features. I have downloaded a non-stripped version from the internet and used that instead.

# Capture the original arguments to the JVM
ORIGINAL_ARGS="\$@"

# Modify the following:
# -Xverify:none is required for VisualVM profiler to work (when debugging)
# -javaagent: for our code to run inside BWS
MODIFIED_ARGS="-Xverify:none -javaagent:$PATH_TO_AGENT \$ORIGINAL_ARGS"

# echo "[hooked] new args: /usr/lib/jvm/java-17-openjdk-amd64/bin/java \$MODIFIED_ARGS"

# Start bitwig proper
exec "/usr/lib/jvm/java-17-openjdk-amd64/bin/java" \$MODIFIED_ARGS
EOF


# Set the permissions so the script can be executed
chmod +x /opt/bitwig-studio/bin/BitwigStudio


# Runs the bootstrap script. This will call /opt/bitwig-studio/bin/BitwigStudio which is our bash script
sudo -u "$SUDO_USER" -H -E BITWIG_DEBUG_PORT=8000 bitwig-studio
