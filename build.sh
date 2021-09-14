#!/bin/bash
set -e # exit on all errors

print_heading() {
    printf "\n>>>%4s$1%4s\n"
}

# modules which have a module descriptor and can be embedded into the custom jre
JLINK_MODULES=java.base,java.security.jgss,\
javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml

MAKE_EXE=false
MAKE_RELEASE=false
LAUNCH=false
LAUNCH_ARGS=''

while getopts "erls:" opt; do
    case $opt in
        e) # make executable
            MAKE_EXE=true;;

        r) # make setup
            MAKE_EXE=true
            MAKE_RELEASE=true
            ;;

        l) # launch
            MAKE_EXE=true
            LAUNCH=true
            ;;
        
        s) # supply launch arg
            MAKE_EXE=true
            LAUNCH=true
            LAUNCH_ARGS=$OPTARG
            ;;

        *) # invalid
            exit;;
    esac
done

print_heading "Executing Maven goals"
mvn -B -q clean package

print_heading "Preparing..."
mkdir target/bin
cp notice.txt target/bin

if [[ $MAKE_EXE == true ]]; then
    print_heading "Making jlink image"
    jlink --module-path target/modules \
        --add-modules $JLINK_MODULES \
        --strip-debug \
        --no-header-files \
        --no-man-pages \
        --compress 2 \
        --output target/bin/runtime

    print_heading "Copying dependencies"
    mkdir target/bin/modules
    for file in target/modules/common-*;            do cp "$file" target/bin/modules; done
    for file in target/modules/commons-*;           do cp "$file" target/bin/modules; done
    for file in target/modules/imageio-*;           do cp "$file" target/bin/modules; done
    cp target/modules/httpclient-4.5.13.jar         target/bin/modules
    cp target/modules/httpcore-4.4.13.jar           target/bin/modules
    cp target/modules/imgscalr-lib-4.2.jar          target/bin/modules
    cp target/modules/metadata-extractor-2.16.0.jar target/bin/modules
    cp target/modules/xmpcore-6.1.11.jar            target/bin/modules

    # all OSes

    print_heading "Making executable"
    cp target/slimview-1.0.7.jar target/bin/
    
    echo "#!/bin/bash" > target/bin/slimview.sh
    echo "runtime/bin/java -jar slimview-1.0.7.jar" '$1' >> target/bin/slimview.sh
    chmod +x target/bin/slimview.sh

    if [[ "$OSTYPE" == "cygwin"* ]]; then
        # Windows only
        launch4jc launch4j-config.xml
    fi
fi

if [[ $MAKE_RELEASE == true ]]; then
    # invoke InnoSetup

    if [[ "$OSTYPE" == "cygwin"* ]]; then
        print_heading "Building installer"
        iscc /Qp "installer/slimview.iss"   
    fi
fi

print_heading "Done."

if [[ $LAUNCH == true ]]; then
    print_heading "Launching..."
    # This jar will only execute when invoked using the custom jre
    # That's because bin/modules don't contain the modularized dependencies (e.g. JavaFX)    
    target/bin/runtime/bin/java -jar target/slimview-1.0.7.jar $LAUNCH_ARGS
fi
