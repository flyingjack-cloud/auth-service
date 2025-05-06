@echo on
setlocal enabledelayedexpansion

:: Define variables (modify according to your needs)
set DOCKER_IMAGE_NAME=flyingjack-auth-service
set DEFAULT_TAG=latest
set DOCKER_REGISTRY=registry.wms.com:8443/cloud
set DOCKERFILE_PATH=.
set MVN_PROFILE=beta


echo Current Maven profile: %MVN_PROFILE%

:: 1. Ask whether to skip tests
set /p SKIP_TESTS=Step 1: Skip tests? (y/n)
if /i "%SKIP_TESTS%"=="y" (
    set MVN_SKIP_TESTS=-DskipTests
    echo [INFO] Tests will be skipped...
) else (
    set MVN_SKIP_TESTS=
    echo [INFO] Tests will be executed...
)

:: 2. Execute mvn clean package
echo Step 2: Running mvn clean package...
call mvn clean package -P %MVN_PROFILE% %MVN_SKIP_TESTS%
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b 1
)

:: 3. Execute docker build
echo Step 3: Building Docker image...
docker build -t %DOCKER_IMAGE_NAME%:%DEFAULT_TAG% %DOCKERFILE_PATH%
if %errorlevel% neq 0 (
    echo [ERROR] Docker build failed!
    exit /b 1
)

:: 4.0 Ask whether to upload image
set /p UPLOAD_CHOICE=Step 4.0: Upload image to registry? (y/n)
if /i not "%UPLOAD_CHOICE%"=="y" (
    echo [INFO] Image upload skipped. Script completed.
    exit /b 0
)

:: 4.1 Tag remote image
echo Current image tag: %DEFAULT_TAG%
set /p CUSTOM_TAG=Step 4.1: Enter custom tag (leave blank for default %DEFAULT_TAG%):
if "%CUSTOM_TAG%"=="" (
    set FINAL_TAG=%DEFAULT_TAG%
) else (
    set FINAL_TAG=%CUSTOM_TAG%
)

:: 4.2 Execute docker tag
echo Step 4.2: Tagging Docker image...
set TAGGED_IMAGE=%DOCKER_REGISTRY%/%DOCKER_IMAGE_NAME%:%FINAL_TAG%
docker tag %DOCKER_IMAGE_NAME%:%DEFAULT_TAG% %TAGGED_IMAGE%
if %errorlevel% neq 0 (
    echo [ERROR] Docker tag failed!
    exit /b 1
)

:: 5 Push image to registry
echo Step 5: Pushing Docker image...
docker push %TAGGED_IMAGE%
if %errorlevel% neq 0 (
    echo [ERROR] Docker push failed!
    exit /b 1
)

:: 10. Print final result
echo [SUCCESS] Operation completed!
echo Image successfully uploaded to: %TAGGED_IMAGE%

endlocal