@echo off
cd /d "%~dp0"

echo Compiling LibrarySystem...
javac -encoding UTF-8 -cp ".;lib/*" -d bin src\model\*.java src\view\*.java src\exception\*.java

if errorlevel 1 (
    echo.
    echo Compile failed.
    pause
    exit /b
)

echo.
echo Running LibrarySystem...
java -cp "bin;lib/*" view.EntryFrame

pause