call mvn clean package

echo.
echo Making directories
echo.
mkdir target\win

echo.
echo Copying notice
echo.
copy notice.txt target\win\notice.txt

echo.
echo Making new jlink image
echo.
jlink --module-path "%PATH_TO_FX_MODS%" --add-modules javafx.controls,javafx.graphics,java.base,java.desktop,javafx.fxml,javafx.base,javafx.swing,java.se,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.crypto.mscapi --output target\win\jlink-image

echo.
echo Done.