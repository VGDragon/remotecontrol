import androidx.compose.runtime.Composable
import messages.tasks.MessageStartTaskScript
import tasks.TaskStartScript

class AddingTaskPlaces {
    @Composable
    fun addingTask(){
        // storing a class in a variable and create a class object from it
        // TODO: GUI task information
        App()
        val daskDirectory = TaskStartScript
        val MessageStartTaskScript = MessageStartTaskScript
        TaskFunctions.convertToTaskMessageType("", "")
        TaskFunctions.getTaskFromGuiData("", "", mutableMapOf())
        TaskFunctions.entryTypeList()

    }
}