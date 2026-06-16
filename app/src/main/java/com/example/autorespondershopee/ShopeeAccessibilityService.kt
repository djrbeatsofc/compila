package com.example.autorespondershopee

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.text.Normalizer
import java.util.Locale

class ShopeeAccessibilityService : AccessibilityService() {
    private enum class FlowStep {
        FIND_RESPONDER,
        WAIT_REPLY_FIELD,
        WAIT_SEND_BUTTON
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = FlowStep.FIND_RESPONDER
    private var isStepScheduled = false
    private var missCount = 0
    private var lastStatus = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateStatus("Serviço AutoResponderShopee conectado.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isShopeePackage(event.packageName)) {
            return
        }

        val delayMillis = when (currentStep) {
            FlowStep.FIND_RESPONDER -> 300L
            FlowStep.WAIT_REPLY_FIELD -> 800L
            FlowStep.WAIT_SEND_BUTTON -> 500L
        }
        scheduleStep(delayMillis)
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        isStepScheduled = false
        updateStatus("Serviço interrompido.")
    }

    private fun scheduleStep(delayMillis: Long) {
        if (isStepScheduled) {
            return
        }

        isStepScheduled = true
        handler.postDelayed({
            isStepScheduled = false
            runCurrentStep()
        }, delayMillis)
    }

    private fun runCurrentStep() {
        val rootNode = rootInActiveWindow
        if (rootNode == null || !isShopeePackage(rootNode.packageName)) {
            updateStatus("Tela da Shopee não encontrada.")
            return
        }

        when (currentStep) {
            FlowStep.FIND_RESPONDER -> clickFirstResponder(rootNode)
            FlowStep.WAIT_REPLY_FIELD -> fillReplyField(rootNode)
            FlowStep.WAIT_SEND_BUTTON -> clickSendButton(rootNode)
        }
    }

    private fun clickFirstResponder(rootNode: AccessibilityNodeInfo) {
        val responderNode = findResponderNode(rootNode)
        val clicked = responderNode?.let { clickNodeOrClickableParent(it) } == true

        if (clicked) {
            missCount = 0
            currentStep = FlowStep.WAIT_REPLY_FIELD
            updateStatus("Botão Responder acionado.")
            scheduleStep(1_000L)
        } else {
            handleMissingElement("Botão Responder não encontrado.", FlowStep.FIND_RESPONDER)
        }
    }

    private fun fillReplyField(rootNode: AccessibilityNodeInfo) {
        val replyField = findReplyField(rootNode)
        val filled = replyField?.let { setNodeText(it, REPLY_MESSAGE) } == true

        if (filled) {
            missCount = 0
            currentStep = FlowStep.WAIT_SEND_BUTTON
            updateStatus("Campo de resposta preenchido.")
            scheduleStep(600L)
        } else {
            handleMissingElement("Campo Sua Resposta... não encontrado.", FlowStep.WAIT_REPLY_FIELD)
        }
    }

    private fun clickSendButton(rootNode: AccessibilityNodeInfo) {
        val sendNode = findSendNode(rootNode)
        val clicked = sendNode?.let { clickNodeOrClickableParent(it) } == true

        if (clicked) {
            missCount = 0
            currentStep = FlowStep.FIND_RESPONDER
            updateStatus("Botão ENVIAR acionado.")
            scheduleStep(1_200L)
        } else {
            handleMissingElement("Botão ENVIAR não encontrado.", FlowStep.WAIT_SEND_BUTTON)
        }
    }

    private fun handleMissingElement(message: String, stepToKeep: FlowStep) {
        missCount += 1
        currentStep = stepToKeep
        updateStatus(message)

        if (missCount <= MAX_MISSES) {
            scheduleStep(700L)
        } else {
            currentStep = FlowStep.FIND_RESPONDER
            missCount = 0
            updateStatus("$message Fluxo reiniciado.")
        }
    }

    private fun findResponderNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFirstDepthFirst(rootNode) { node ->
            node.isVisibleToUser &&
                hasTextOrDescription(node, RESPONDER_TEXT) &&
                hasClickableNode(node) &&
                isLikelyButtonOrClickableText(node)
        } ?: findByTextFallback(rootNode, RESPONDER_TEXT) { node ->
            hasClickableNode(node) && isLikelyButtonOrClickableText(node)
        }
    }

    private fun findReplyField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFirstDepthFirst(rootNode) { node ->
            node.isVisibleToUser &&
                node.isEnabled &&
                isTextInput(node) &&
                textDescriptionOrHintContains(node, REPLY_HINT)
        } ?: findByTextFallback(rootNode, REPLY_HINT) { node ->
            node.isEnabled && isTextInput(node)
        } ?: findSingleVisibleEditableField(rootNode)
    }

    private fun findSendNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findFirstDepthFirst(rootNode) { node ->
            node.isVisibleToUser &&
                hasTextOrDescription(node, SEND_TEXT) &&
                hasClickableNode(node) &&
                isLikelyButtonOrClickableText(node)
        } ?: findByTextFallback(rootNode, SEND_TEXT) { node ->
            hasClickableNode(node) && isLikelyButtonOrClickableText(node)
        }
    }

    private fun findFirstDepthFirst(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(node)) {
            return node
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val result = findFirstDepthFirst(child, predicate)
            if (result != null) {
                return result
            }
        }

        return null
    }

    private fun findByTextFallback(
        rootNode: AccessibilityNodeInfo,
        text: String,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        return rootNode
            .findAccessibilityNodeInfosByText(text)
            .firstOrNull { it.isVisibleToUser && predicate(it) }
    }

    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }

        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findSingleVisibleEditableField(
        rootNode: AccessibilityNodeInfo
    ): AccessibilityNodeInfo? {
        val editableFields = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(rootNode, editableFields) { node ->
            node.isVisibleToUser && node.isEnabled && isTextInput(node)
        }

        return editableFields.singleOrNull()
    }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        result: MutableList<AccessibilityNodeInfo>,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ) {
        if (predicate(node)) {
            result.add(node)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectNodes(child, result, predicate)
        }
    }

    private fun clickNodeOrClickableParent(node: AccessibilityNodeInfo): Boolean {
        val clickableNode = findClickableNode(node) ?: return false
        return clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun hasClickableNode(node: AccessibilityNodeInfo): Boolean {
        return findClickableNode(node) != null
    }

    private fun findClickableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var currentNode: AccessibilityNodeInfo? = node

        repeat(MAX_PARENT_DEPTH) {
            val candidate = currentNode ?: return null
            if (candidate.isClickable && candidate.isEnabled && candidate.isVisibleToUser) {
                return candidate
            }
            currentNode = candidate.parent
        }

        return null
    }

    private fun isTextInput(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString().orEmpty()
        return node.isEditable || className.contains("EditText", ignoreCase = true)
    }

    private fun isLikelyButtonOrClickableText(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString().orEmpty()
        return node.isClickable ||
            className.contains("Button", ignoreCase = true) ||
            className.contains("TextView", ignoreCase = true) ||
            className.contains("View", ignoreCase = true)
    }

    private fun hasTextOrDescription(node: AccessibilityNodeInfo, expected: String): Boolean {
        val expectedNormalized = normalize(expected)
        return normalize(node.text?.toString()) == expectedNormalized ||
            normalize(node.contentDescription?.toString()) == expectedNormalized
    }

    private fun textDescriptionOrHintContains(
        node: AccessibilityNodeInfo,
        expected: String
    ): Boolean {
        val expectedNormalized = normalize(expected).removeSuffix("...")
        val values = listOfNotNull(
            node.text?.toString(),
            node.contentDescription?.toString(),
            node.hintText?.toString(),
            node.viewIdResourceName,
            node.className?.toString()
        )

        return values.any { normalize(it).contains(expectedNormalized) }
    }

    private fun isShopeePackage(packageName: CharSequence?): Boolean {
        return packageName
            ?.toString()
            ?.lowercase(Locale.ROOT)
            ?.contains("shopee") == true
    }

    private fun normalize(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        val withoutAccents = Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

        return withoutAccents
            .replace('…', '.')
            .trim()
            .lowercase(Locale.ROOT)
    }

    private fun updateStatus(message: String) {
        Log.d(TAG, message)
        if (message != lastStatus) {
            lastStatus = message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "AutoResponderShopee"
        private const val RESPONDER_TEXT = "Responder"
        private const val REPLY_HINT = "Sua Resposta..."
        private const val SEND_TEXT = "ENVIAR"
        private const val MAX_MISSES = 8
        private const val MAX_PARENT_DEPTH = 8
        private const val REPLY_MESSAGE =
            "Olá! Muito obrigado pela sua avaliação 😊 Ficamos felizes com sua compra e esperamos te atender novamente em breve!"
    }
}
