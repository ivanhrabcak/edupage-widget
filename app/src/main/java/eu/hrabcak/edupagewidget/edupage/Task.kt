package eu.hrabcak.edupagewidget.edupage

import java.lang.Thread.UncaughtExceptionHandler
import kotlin.concurrent.thread

class Task(
    private val target: Thread
) {
    private var shouldContinueExecution = true
    private val afterCompletion: MutableList<Runnable> = mutableListOf()
    private var errorHandler: ((Throwable) -> Unit)? = null

    init {
        target.uncaughtExceptionHandler = UncaughtExceptionHandler { _, e ->
            shouldContinueExecution = false
            errorHandler?.let { it(e) }
        }
    }

    private val task: Thread = thread(false) {
        target.start()
        target.join()

        if (!shouldContinueExecution) {
            return@thread
        }

        for (nextTask in afterCompletion) {
            try {
                nextTask.run()
            } catch (e: Exception) {
                errorHandler?.let { it(e) }
            }
        }
    }

    fun then(runnable: Runnable): Task {
        afterCompletion.add(runnable)

        return this
    }

    fun onError(handler: (Throwable) -> Unit): Task {
        errorHandler = handler

        return this
    }

    fun start() {
        task.start()
    }
}