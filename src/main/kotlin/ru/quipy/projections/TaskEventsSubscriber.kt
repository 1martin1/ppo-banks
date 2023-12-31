package ru.quipy.projections

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import ru.quipy.api.TaskAggregate
import ru.quipy.api.TaskCreatedEvent
import ru.quipy.config.Services
import ru.quipy.logic.addTask
import ru.quipy.streams.AggregateSubscriptionsManager
import javax.annotation.PostConstruct

@Service
class TasksEventsSubscriber {

    val logger: Logger = LoggerFactory.getLogger(TasksEventsSubscriber::class.java)

    @Autowired
    lateinit var subscriptionsManager: AggregateSubscriptionsManager
//    @Autowired
//    lateinit var taskProjection: TaskProjection
    @Autowired
    lateinit var services: Services
    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @PostConstruct
    fun init() {
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`("task-event-listener")), Update.update("readIndex", 0) ,"event-stream-read-index")
        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").`is`("task-event-listener")), Update.update("version", 0) ,"event-stream-read-index")
        subscriptionsManager.createSubscriber(TaskAggregate::class, "task-event-listener") {

            `when`(TaskCreatedEvent::class) { event ->

                println("Task Cr")
                logger.info("Task created: {}", event.taskName)
                services.taskProjection.addTask(event.projectId)

                services.projectEsService.update(event.projectId) {it.addTask(event, services)}
            }
        }
    }
}