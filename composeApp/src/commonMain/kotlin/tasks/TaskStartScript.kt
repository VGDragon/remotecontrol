package tasks

import GlobalVariables
import connection.client.WebsocketConnectionClient
import interfaces.TaskInterface
import java.io.File

class TaskStartScript(
    val scriptName: String,
    val websocketConnectionClient: WebsocketConnectionClient,
    override val nextTask: TaskInterface?,
    override val startedFrom: String
) : TaskInterface {
    override val taskName: String = TaskStartScript.taskName
    override var taskThread: Thread? = null
    override val taskThreadLock = Object()

    override fun start() {
        synchronized(taskThreadLock) {
            if (taskThread != null) {
                return
            }
            taskThread = Thread {
                var process: ProcessBuilder = ProcessBuilder()
                try {
                    val scriptFile = File(GlobalVariables.scriptFolder(), scriptName)
                    val osName = GlobalVariables.pcOS()
                    if (osName == OsType.WINDOWS) {
                        process = process.command("cmd", "/c", "\"${scriptFile.absolutePath}\"")
                    } else if (osName == OsType.LINUX) {
                        process = process.command("/bin/bash", "-c", "\"${scriptFile.absolutePath}\"")
                    } else if (osName == OsType.MAC) {
                        //process = Runtime.getRuntime()
                        //    .exec("cmd /c start cmd.exe /K \"cd C:\\Users\\james\\Documents\\GitHub\\kotlin-websocket-bridge\\scripts && ${scriptName}.bat\"")
                        println("Mac OS not supported")
                    } else {
                        println("Unknown OS")
                    }
                    val process_temp = process.start()
                    while (process_temp.isAlive) {
                        Thread.sleep(100)
                    }

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                println(scriptName + "was started")
                if (nextTask != null) {
                    nextTask.start()
                }
                println(scriptName + " finished")
                websocketConnectionClient.removeTask(this)
            }
            websocketConnectionClient.addTask(this)
            taskThread!!.start()
        }
        println("Running StartScriptTask")
    }

    override fun stop() {
        synchronized(taskThreadLock) {
            if (taskThread == null) {
                return
            }
            taskThread!!.interrupt()
            taskThread!!.join()
            taskThread = null
        }
        println("Stopping StartScriptTask")
        websocketConnectionClient.removeTask(this)
    }
    companion object {
        val taskName: String = "StartScrip"
    }
}