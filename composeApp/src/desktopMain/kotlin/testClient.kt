import connection.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskActionData
import filedata.TaskListData
import messages.tasks.MessageStartTaskScript


fun testingClientScript(){
    GlobalVariables.computerName = System.getenv("COMPUTERNAME")
    // storing a class in a variable and create a class object from it
    val applicationData = ApplicationData.fromFile()
    //val ws_server = WebsocketConnectionServer(applicationData)
    //ws_server.start()
    //Thread.sleep(3000)

    //val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    val ws_client = WebsocketConnectionClient(applicationData)
    //ws_client_exec.connectAndRegister(doJoin = true)
    ws_client.connectAndRegister(doJoin = false)


    //var messageStartTaskBaseConvertObject = messageStartTaskBaseConvertClass.first(functionVariableMap)
    //task list
    val taskListData = TaskListData()
    taskListData.taskActionDataList.add(TaskActionData(
        clientName = GlobalVariables.computerName + "_executable",
        taskName = "test",
        taskData = MessageStartTaskScript(
            type=MessageStartTaskScript.TYPE,
            clientTo=GlobalVariables.computerName + "_executable",
            scriptName = "testing.bat").toJson()))

    taskListData.taskActionDataList.add(TaskActionData(
        clientName = GlobalVariables.computerName + "_executable",
        taskName = "test",
        taskData = MessageStartTaskScript(
            type=MessageStartTaskScript.TYPE,
            clientTo=GlobalVariables.computerName + "_executable",
            scriptName = "testing.bat").toJson()))
    taskListData.taskActionDataList.add(TaskActionData(
        clientName = GlobalVariables.computerName + "1_executable",
        taskName = "test",
        taskData = MessageStartTaskScript(
            type=MessageStartTaskScript.TYPE,
            clientTo=GlobalVariables.computerName + "1_executable",
            scriptName = "testing.bat").toJson()))
    taskListData.startTaskList(ws_client)
    Thread.sleep(1000)
    ws_client.stopConnection()
}

fun main(args: Array<String>) {
    testingClientScript()
    //startServerWithGuiTest()
}
