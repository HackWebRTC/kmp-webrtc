@echo off

if "%~1"=="" (
    set "LIB_EXE=lib"
) else (
    set "LIB_EXE=%~1"
)

.\gradlew :kmp-webrtc:linkReleaseSharedMingwX64 && ^
copy /Y kmp-webrtc\build\bin\mingwX64\releaseShared\kmp_webrtc.dll libs\windows\x64\ && ^
copy /Y kmp-webrtc\build\bin\mingwX64\releaseShared\kmp_webrtc.def libs\windows\x64\ && ^
copy /Y kmp-webrtc\build\bin\mingwX64\releaseShared\kmp_webrtc_api.h libs\windows\x64\ && ^
cd kmp-webrtc\build\bin\mingwX64\releaseShared && ^
%LIB_EXE% /def:kmp_webrtc.def /machine:x64 /out:kmp_webrtc.lib && ^
copy /Y kmp_webrtc.lib ..\..\..\..\..\libs\windows\x64\
