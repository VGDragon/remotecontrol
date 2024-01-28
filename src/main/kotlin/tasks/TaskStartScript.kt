package tasks

import GlobalVariables
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import java.io.File
import java.util.*

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
                var process: Process? = null
                try {
                    val scriptFile = File(GlobalVariables.scriptFolder, scriptName)
                    val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
                    if (osName.contains("windows")) {
                        process = Runtime.getRuntime()
                            .exec("cmd /c \"${scriptFile.absolutePath}\"")
                    } else if (osName.contains("linux")) {
                        process = Runtime.getRuntime()
                            .exec("bash -c \"${scriptFile.absolutePath}\"")
                    } else if (osName.contains("mac")) {
                        //process = Runtime.getRuntime()
                        //    .exec("cmd /c start cmd.exe /K \"cd C:\\Users\\james\\Documents\\GitHub\\kotlin-websocket-bridge\\scripts && ${scriptName}.bat\"")
                        println("Mac OS not supported")
                    } else {
                        println("Unknown OS")
                    }
                    process?.waitFor()

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    if (process != null) {
                        val currentTime = System.currentTimeMillis()
                        process.destroy()
                        while (System.currentTimeMillis() - currentTime < 5000) {
                            Thread.sleep(100)
                        }
                        process.destroyForcibly()
                    }
                }
                if (nextTask != null) {
                    nextTask.start()
                }
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