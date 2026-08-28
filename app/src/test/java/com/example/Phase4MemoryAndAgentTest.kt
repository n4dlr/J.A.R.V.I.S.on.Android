package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.jarvis.agent.AgentExecutor
import com.example.jarvis.agent.AgentPlanner
import com.example.jarvis.automation.workflow.Workflow
import com.example.jarvis.automation.workflow.WorkflowAction
import com.example.jarvis.automation.workflow.WorkflowCondition
import com.example.jarvis.automation.workflow.WorkflowEngine
import com.example.jarvis.automation.workflow.WorkflowTrigger
import com.example.jarvis.context.ConversationContextManager
import com.example.jarvis.core.LowRamManager
import com.example.jarvis.data.local.JarvisDatabase
import com.example.jarvis.data.repository.JarvisRepositoryImpl
import com.example.jarvis.domain.model.StructuredIntent
import com.example.jarvis.domain.model.TaskLifecycleStatus
import com.example.jarvis.domain.model.IntentConfidence
import com.example.jarvis.memory.MemoryManager
import com.example.jarvis.rag.KnowledgeChunk
import com.example.jarvis.rag.KnowledgeSource
import com.example.jarvis.rag.LightweightRetriever
import com.example.jarvis.rag.RAGEngine
import com.example.jarvis.tools.ToolRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Phase4MemoryAndAgentTest {

    private lateinit var context: Context
    private lateinit var database: JarvisDatabase
    private lateinit var repository: JarvisRepositoryImpl
    private lateinit var memoryManager: MemoryManager
    private lateinit var lowRamManager: LowRamManager
    private lateinit var toolRegistry: ToolRegistry
    private lateinit var retriever: LightweightRetriever
    private lateinit var ragEngine: RAGEngine
    private lateinit var agentPlanner: AgentPlanner
    private lateinit var agentExecutor: AgentExecutor
    private lateinit var contextManager: ConversationContextManager
    private lateinit var workflowEngine: WorkflowEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = JarvisRepositoryImpl(database)
        lowRamManager = LowRamManager(context)
        memoryManager = MemoryManager(repository, lowRamManager)
        toolRegistry = ToolRegistry()
        retriever = LightweightRetriever()
        ragEngine = RAGEngine(repository, retriever)
        agentPlanner = AgentPlanner()
        agentExecutor = AgentExecutor(toolRegistry, memoryManager)
        contextManager = ConversationContextManager()
        workflowEngine = WorkflowEngine(toolRegistry, memoryManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `memory manager saves facts and allows querying and forgetting`() = runBlocking {
        // 1. Remember fact
        val fact = "Ev ünvanım Nizami küçəsi 45-dir"
        memoryManager.rememberFact(fact)

        // 2. Query fact
        val results = memoryManager.searchFacts("Nizami")
        assertEquals(1, results.size)
        assertTrue(results[0].value.contains("Nizami küçəsi"))

        // 3. Forget fact
        val deleted = memoryManager.forgetFact("Nizami")
        assertTrue(deleted)

        val afterDelete = memoryManager.searchFacts("Nizami")
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun `user preferences are saved and retrieved`() = runBlocking {
        memoryManager.setPreference("preferred_language", "az")
        val lang = memoryManager.getPreference("preferred_language")
        assertEquals("az", lang)
    }

    @Test
    fun `task lifecycle status transitions properly`() = runBlocking {
        val task = memoryManager.createTask("Diaqnostika", "Tam sistem yoxlanışı")
        assertEquals(TaskLifecycleStatus.PENDING, task.status)

        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.RUNNING)
        val runningTasks = memoryManager.getTasksByStatus(TaskLifecycleStatus.RUNNING)
        assertEquals(1, runningTasks.size)
        assertEquals(task.id, runningTasks[0].id)

        memoryManager.updateTaskStatus(task.id, TaskLifecycleStatus.COMPLETED)
        val completedTasks = memoryManager.getTasksByStatus(TaskLifecycleStatus.COMPLETED)
        assertEquals(1, completedTasks.size)
    }

    @Test
    fun `device state snapshots are recorded and retrieved`() = runBlocking {
        memoryManager.recordDeviceSnapshot(
            batteryPct = 85,
            isCharging = true,
            ramUsedPercent = 45,
            storageFreeGb = 15.4,
            networkType = "Wi-Fi"
        )

        val latest = memoryManager.getLatestDeviceSnapshot()
        assertNotNull(latest)
        assertEquals(85, latest?.batteryPct)
        assertTrue(latest?.isCharging == true)
        assertEquals("Wi-Fi", latest?.networkType)
    }

    @Test
    fun `lightweight retriever finds relevant built-in knowledge chunks`() {
        val results = retriever.search("telefonum niyə yavaş işləyir RAM necə təmizləyim")
        assertTrue(results.isNotEmpty())
        val topChunk = results.first()
        assertTrue(topChunk.score > 0f)
        assertTrue(topChunk.content.contains("RAM") || topChunk.content.contains("yavaş"))
    }

    @Test
    fun `rag engine retrieves dynamic user facts alongside built-in docs`() = runBlocking {
        memoryManager.rememberFact("Ofisin Wi-Fi şifrəsi: JarvisPass2026")
        val retrieved = ragEngine.retrieveRelevantContext("Wi-Fi şifrəsi")
        assertTrue(retrieved.isNotEmpty())
        val factChunk = retrieved.firstOrNull { it.content.contains("JarvisPass2026") }
        assertNotNull("Expected dynamically indexed fact chunk", factChunk)
    }

    @Test
    fun `agent planner creates multi-step diagnostic plan for slow device query`() {
        val query = "Telefonum niyə yavaşdır? Problem varsa yoxla"
        assertTrue(agentPlanner.shouldPlan(query))

        val plan = agentPlanner.createPlan(query)
        assertEquals(5, plan.steps.size)
        assertTrue(plan.steps.any { it.toolId == "GET_RAM" })
        assertTrue(plan.steps.any { it.toolId == "CPU_STATUS" })
        assertTrue(plan.steps.any { it.toolId == "GET_STORAGE" })
        assertTrue(plan.steps.any { it.toolId == "BATTERY_STATUS" })
    }

    @Test
    fun `agent executor runs multi-step plan, observes results, and synthesizes report`() = runBlocking {
        val plan = agentPlanner.createPlan("Telefonumu yoxla")
        val result = agentExecutor.executePlan(context, plan)

        assertTrue(result.isSuccessful)
        assertEquals(5, result.observations.size)
        assertTrue(result.summary.isNotBlank())
    }

    @Test
    fun `conversation context manager resolves multi-turn contextual queries`() {
        // Turn 1: User says "Wi-Fi-ni aç"
        val wifiIntent = StructuredIntent("WIFI_STATUS", "Wi-Fi-ni yoxla", "wifi yoxla", IntentConfidence.EXACT_DETERMINISTIC, isDeterministic = true)
        contextManager.updateContext(wifiIntent)
        assertEquals("WIFI", contextManager.getCurrentContext().lastTopic)

        // Turn 2: User says "İndi vəziyyətini yoxla"
        val resolved = contextManager.resolveContextualQuery("indi veziyyetini yoxla")
        assertNotNull(resolved)
        assertEquals("WIFI_STATUS", resolved?.intentId)

        // Turn 3: User says "parametrlərini aç"
        val resolvedSettings = contextManager.resolveContextualQuery("parametrlərini aç")
        assertNotNull(resolvedSettings)
        assertEquals("WIFI_SETTINGS", resolvedSettings?.intentId)
    }

    @Test
    fun `workflow engine registers and executes automated workflow`() = runBlocking {
        val workflow = Workflow(
            id = "wf_battery_check",
            name = "Batareya Yoxlanışı",
            trigger = WorkflowTrigger.ManualTrigger("Check Battery"),
            condition = WorkflowCondition.AlwaysTrue,
            action = WorkflowAction.ExecuteTool("BATTERY_STATUS")
        )
        workflowEngine.registerWorkflow(workflow)
        assertEquals(1, workflowEngine.getAllWorkflows().size)

        val result = workflowEngine.executeWorkflow(context, "wf_battery_check")
        assertNotNull(result)
        assertTrue(result?.isSuccess == true)
    }
}
