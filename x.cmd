::
:: run Bpdf.class file
:: %1 pdf-filename for slicing / dir with such files
::
@SET CMDPATH=%~dp0
@SET JARPATH=%CMDPATH%pdfbox-app-3.0.0-alpha3.jar
if not exist %CMDPATH%Bpdf.class @call %CMDPATH%xjavac.cmd %CMDPATH%Bpdf.java
java -Dfile.encoding=UTF-8 -cp "%CMDPATH%;%JARPATH%" Bpdf %1

