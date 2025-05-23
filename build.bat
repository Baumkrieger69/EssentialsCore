@echo off
echo Building EssentialsCore v1.0.12...

:: Create build directories
echo Creating build directories...
mkdir build 2>nul
mkdir build\classes 2>nul
mkdir build\libs 2>nul

:: Check if Java is installed
echo Checking for Java...
where java
if %ERRORLEVEL% neq 0 (
  echo Java is not found in PATH. Please make sure Java is installed.
  exit /b 1
)

:: Copy resources to build directory
echo Copying resources...
xcopy /s /y src\main\resources build\classes\
if %ERRORLEVEL% neq 0 (
  echo Failed to copy resources.
  exit /b 1
)

:: Check if resources were copied
echo Checking resources...
dir build\classes

:: Create JAR file
echo Creating JAR file...
cd build\classes
jar cf ..\libs\EssentialsCore-1.0.12.jar *
if %ERRORLEVEL% neq 0 (
  echo Failed to create JAR file.
  cd ..\..
  exit /b 1
)
cd ..\..

:: Check if JAR was created
echo Checking JAR file...
dir build\libs

echo Build completed!
echo JAR file created at: build\libs\EssentialsCore-1.0.12.jar 