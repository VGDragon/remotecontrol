package interfaces

interface TaskInterface {
    val nextTask: TaskInterface?
    val taskName: String
    var taskThread: Thread?
    val taskThreadLock: Any
    val startedFrom: String
    fun start()
    fun stop()
}