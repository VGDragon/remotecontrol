cd /D "%~dp0"
:: look if remotecontrol.jar.update exists
if exist remotecontrol.jar.update (
    :: delete old remotecontrol.jar
    del remotecontrol.jar
    :: rename remotecontrol.jar.update to remotecontrol.jar
    ren remotecontrol.jar.update remotecontrol.jar
)
java -jar remotecontrol.jar