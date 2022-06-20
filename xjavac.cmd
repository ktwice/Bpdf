:: compile .java to .class
@SET CMDPATH=%~dp0
@SET JARPATH="%CMDPATH%pdfbox-app-3.0.0-alpha3.jar"
javac -encoding UTF-8 -cp %JARPATH% %1
