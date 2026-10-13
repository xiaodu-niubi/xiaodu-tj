import { createHotContext as __vite__createHotContext } from "/@vite/client";import.meta.hot = __vite__createHotContext("/src/pages/aiAgent/index.vue");import { ref, onMounted, nextTick } from '/node_modules/.vite/deps/vue.js?v=44c78bf4'
import { ElMessage, ElMessageBox } from '/node_modules/.vite/deps/element-plus.js?v=44c78bf4'
import { useStreamChat } from '/src/composables/useStreamChat.js?t=1779174680687'
import {
  getHotExamples, getSessionMessages,
  getHistorySessions, deleteHistorySession, getTemplates
} from '/src/api/aigc.js?t=1779174680687'

// 侧边栏

const _sfc_main = {
  __name: 'index',
  setup(__props, { expose }) {
  expose();

const sidebarCollapsed = ref(false)

// 流式聊天 composable
const {
  sessionId: currentSessionId,
  messages,
  streaming,
  streamingText,
  ensureSession,
  sendMessage,
  abortStream,
  formatContent,
  scrollToBottom: composableScrollToBottom
} = useStreamChat()

// 会话相关
const currentSessionTitle = ref('')
const sessionDescribe = ref('')
const loadingMessages = ref(false)

// 输入
const inputText = ref('')
const inputRef = ref(null)

// 滚动适配器
const scrollToBottom = () => composableScrollToBottom(messagesContainer)

// 热门示例 & 模板
const hotExamples = ref([])
const templates = ref([])

// 历史会话
const historyGroups = ref([])

// 消息容器
const messagesContainer = ref(null)

onMounted(async () => {
  await loadHistory()
  await loadHotExamples()
  await loadTemplates()
})

// 加载热门示例
const loadHotExamples = async () => {
  try {
    const res = await getHotExamples(3)
    if (res.code === 200) {
      hotExamples.value = res.data || []
    }
  } catch (e) { /* ignore */ }
}

// 加载模板
const loadTemplates = async () => {
  try {
    const res = await getTemplates()
    if (res.code === 200 && res.data) {
      const tpl = res.data
      const list = []
      if (tpl.associationalWord) list.push({ label: '联想词', prompt: tpl.associationalWord })
      if (tpl.helpedWrite) list.push({ label: '帮我写', prompt: tpl.helpedWrite })
      if (tpl.continuedWrite) list.push({ label: '继续写', prompt: tpl.continuedWrite })
      if (tpl.polish) list.push({ label: '润色', prompt: tpl.polish })
      if (tpl.streamline) list.push({ label: '精简', prompt: tpl.streamline })
      templates.value = list
    }
  } catch (e) { /* ignore */ }
}

// 加载历史会话
const loadHistory = async () => {
  try {
    const res = await getHistorySessions()
    if (res.code === 200 && res.data) {
      const data = res.data
      const groups = []
      // data is Map<String, List<ChatSessionVO>>
      for (const [label, sessions] of Object.entries(data)) {
        if (sessions && sessions.length) {
          groups.push({ label, sessions })
        }
      }
      historyGroups.value = groups
    }
  } catch (e) { /* ignore */ }
}

// 判断是否是用户消息 (兼容字符串与枚举值)
const isUserMsg = (msg) => {
  return msg.type === 'USER' || msg.type === 'user' || msg.type === 1 || msg.type === '1'
}

// 新建会话
const handleNewSession = async () => {
  try {
    if (streaming.value) {
      abortStream()
    }
    messages.value = []
    streamingText.value = ''
    const session = await ensureSession(3)
    currentSessionTitle.value = session.title || '新对话'
    sessionDescribe.value = session.describe || ''
    hotExamples.value = session.examples || []
    await loadHistory()
    scrollToBottom()
  } catch (e) {
    ElMessage({ message: '创建会话失败', type: 'error' })
  }
}

// 切换会话
const switchSession = async (sessionId) => {
  if (sessionId === currentSessionId.value) return
  if (streaming.value) {
    handleStop()
  }
  currentSessionId.value = sessionId
  currentSessionTitle.value = ''
  sessionDescribe.value = ''
  messages.value = []
  streamingText.value = ''
  loadingMessages.value = true
  try {
    const res = await getSessionMessages(sessionId)
    if (res.code === 200 && res.data) {
      messages.value = res.data.map(m => ({
        type: m.type,
        content: m.content,
        params: m.params
      }))
    }
  } catch (e) {
    ElMessage({ message: '加载消息失败', type: 'error' })
  } finally {
    loadingMessages.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 删除会话
const handleDeleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定删除该会话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteHistorySession(sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = ''
      currentSessionTitle.value = ''
      sessionDescribe.value = ''
      messages.value = []
      streamingText.value = ''
    }
    await loadHistory()
    ElMessage({ message: '删除成功', type: 'success' })
  } catch (e) { /* cancelled or error */ }
}

// 发送示例/模板问题
const sendExample = (text) => {
  inputText.value = text
  handleSend()
}

// 发送消息
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || streaming.value) return
  inputText.value = ''

  if (!currentSessionId.value) {
    try {
      const session = await ensureSession(3)
      currentSessionTitle.value = session.title || '新对话'
      sessionDescribe.value = session.describe || ''
      hotExamples.value = session.examples || []
      await loadHistory()
    } catch (e) {
      ElMessage({ message: '创建会话失败', type: 'error' })
      return
    }
  }

  messages.value.push({ type: 'USER', content: text })
  scrollToBottom()
  streaming.value = true
  streamingText.value = ''

  await sendMessage(text)
  await loadHistory()
  scrollToBottom()
}

// 停止生成
const handleStop = () => {
  abortStream()
}

// 自动调整输入框高度
const autoResize = () => {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 150) + 'px'
  }
}

const __returned__ = { sidebarCollapsed, currentSessionId, messages, streaming, streamingText, ensureSession, sendMessage, abortStream, formatContent, composableScrollToBottom, currentSessionTitle, sessionDescribe, loadingMessages, inputText, inputRef, scrollToBottom, hotExamples, templates, historyGroups, messagesContainer, loadHotExamples, loadTemplates, loadHistory, isUserMsg, handleNewSession, switchSession, handleDeleteSession, sendExample, handleSend, handleStop, autoResize, ref, onMounted, nextTick, ElMessage, ElMessageBox, useStreamChat, getHotExamples, getSessionMessages, getHistorySessions, deleteHistorySession, getTemplates }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}
import { createCommentVNode as _createCommentVNode, createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, withModifiers as _withModifiers, normalizeClass as _normalizeClass, vModelText as _vModelText, withKeys as _withKeys, withDirectives as _withDirectives } from "/node_modules/.vite/deps/vue.js?v=44c78bf4"

const _hoisted_1 = { class: "ai-agent-wrapper" }
const _hoisted_2 = { class: "sidebar-header" }
const _hoisted_3 = /*#__PURE__*/_createElementVNode("span", { class: "sidebar-title" }, "AI 助手", -1 /* HOISTED */)
const _hoisted_4 = /*#__PURE__*/_createElementVNode("span", null, "+ 新对话", -1 /* HOISTED */)
const _hoisted_5 = [
  _hoisted_4
]
const _hoisted_6 = { class: "history-list" }
const _hoisted_7 = { class: "group-label" }
const _hoisted_8 = ["onClick"]
const _hoisted_9 = { class: "item-title" }
const _hoisted_10 = ["onClick"]
const _hoisted_11 = {
  key: 0,
  class: "empty-history"
}
const _hoisted_12 = { class: "chat-main" }
const _hoisted_13 = { class: "chat-header" }
const _hoisted_14 = { class: "chat-title" }
const _hoisted_15 = {
  class: "chat-messages",
  ref: "messagesContainer"
}
const _hoisted_16 = {
  key: 0,
  class: "welcome"
}
const _hoisted_17 = /*#__PURE__*/_createElementVNode("div", { class: "welcome-icon" }, "🤖", -1 /* HOISTED */)
const _hoisted_18 = /*#__PURE__*/_createElementVNode("p", null, "我可以帮你推荐课程、解答学习问题，请随时向我提问", -1 /* HOISTED */)
const _hoisted_19 = {
  key: 0,
  class: "hot-examples"
}
const _hoisted_20 = /*#__PURE__*/_createElementVNode("div", { class: "examples-title" }, "热门问题", -1 /* HOISTED */)
const _hoisted_21 = ["onClick"]
const _hoisted_22 = /*#__PURE__*/_createElementVNode("span", { class: "example-icon" }, "💬", -1 /* HOISTED */)
const _hoisted_23 = { class: "example-text" }
const _hoisted_24 = { class: "example-title" }
const _hoisted_25 = { class: "example-desc" }
const _hoisted_26 = {
  key: 0,
  class: "message-avatar"
}
const _hoisted_27 = ["innerHTML"]
const _hoisted_28 = {
  key: 0,
  class: "message-params"
}
const _hoisted_29 = /*#__PURE__*/_createElementVNode("div", { class: "params-label" }, "工具调用结果：", -1 /* HOISTED */)
const _hoisted_30 = { class: "param-key" }
const _hoisted_31 = { class: "param-val" }
const _hoisted_32 = {
  key: 1,
  class: "message-avatar user-avatar"
}
const _hoisted_33 = {
  key: 1,
  class: "message-row ai-row"
}
const _hoisted_34 = /*#__PURE__*/_createElementVNode("div", { class: "message-avatar" }, "🤖", -1 /* HOISTED */)
const _hoisted_35 = { class: "message-bubble ai-bubble" }
const _hoisted_36 = ["innerHTML"]
const _hoisted_37 = /*#__PURE__*/_createElementVNode("span", { class: "cursor-blink" }, "|", -1 /* HOISTED */)
const _hoisted_38 = {
  key: 2,
  class: "loading-msg"
}
const _hoisted_39 = { class: "chat-input-area" }
const _hoisted_40 = {
  key: 0,
  class: "template-bar"
}
const _hoisted_41 = ["onClick"]
const _hoisted_42 = { class: "input-wrapper" }
const _hoisted_43 = ["onKeydown"]
const _hoisted_44 = { class: "input-actions" }
const _hoisted_45 = ["disabled"]
const _hoisted_46 = /*#__PURE__*/_createElementVNode("div", { class: "input-disclaimer" }, "内容由AI生成，仅供参考", -1 /* HOISTED */)

function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return (_openBlock(), _createElementBlock("div", _hoisted_1, [
    _createCommentVNode(" 左侧历史记录 "),
    _createElementVNode("div", {
      class: _normalizeClass(["sidebar", { collapsed: $setup.sidebarCollapsed }])
    }, [
      _createElementVNode("div", _hoisted_2, [
        _hoisted_3,
        _createElementVNode("span", {
          class: "collapse-btn",
          onClick: _cache[0] || (_cache[0] = $event => ($setup.sidebarCollapsed = !$setup.sidebarCollapsed))
        }, _toDisplayString($setup.sidebarCollapsed ? '☰' : '✕'), 1 /* TEXT */)
      ]),
      _createElementVNode("div", {
        class: "new-chat-btn",
        onClick: $setup.handleNewSession
      }, _hoisted_5),
      _createElementVNode("div", _hoisted_6, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.historyGroups, (group) => {
          return (_openBlock(), _createElementBlock("div", {
            key: group.label,
            class: "history-group"
          }, [
            _createElementVNode("div", _hoisted_7, _toDisplayString(group.label), 1 /* TEXT */),
            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(group.sessions, (item) => {
              return (_openBlock(), _createElementBlock("div", {
                key: item.sessionId,
                class: _normalizeClass(["history-item", { active: $setup.currentSessionId === item.sessionId }]),
                onClick: $event => ($setup.switchSession(item.sessionId))
              }, [
                _createElementVNode("span", _hoisted_9, _toDisplayString(item.title || '新对话'), 1 /* TEXT */),
                _createElementVNode("span", {
                  class: "item-delete",
                  onClick: _withModifiers($event => ($setup.handleDeleteSession(item.sessionId)), ["stop"])
                }, "✕", 8 /* PROPS */, _hoisted_10)
              ], 10 /* CLASS, PROPS */, _hoisted_8))
            }), 128 /* KEYED_FRAGMENT */))
          ]))
        }), 128 /* KEYED_FRAGMENT */)),
        (!$setup.historyGroups.length)
          ? (_openBlock(), _createElementBlock("div", _hoisted_11, "暂无历史记录"))
          : _createCommentVNode("v-if", true)
      ])
    ], 2 /* CLASS */),
    _createCommentVNode(" 主聊天区域 "),
    _createElementVNode("div", _hoisted_12, [
      _createCommentVNode(" 顶部 "),
      _createElementVNode("div", _hoisted_13, [
        _createElementVNode("span", {
          class: "toggle-sidebar",
          onClick: _cache[1] || (_cache[1] = $event => ($setup.sidebarCollapsed = !$setup.sidebarCollapsed))
        }, "☰"),
        _createElementVNode("span", _hoisted_14, _toDisplayString($setup.currentSessionTitle || 'AI 智能助手'), 1 /* TEXT */)
      ]),
      _createCommentVNode(" 消息区域 "),
      _createElementVNode("div", _hoisted_15, [
        _createCommentVNode(" 欢迎页 & 空会话 "),
        (!$setup.currentSessionId && $setup.messages.length === 0)
          ? (_openBlock(), _createElementBlock("div", _hoisted_16, [
              _hoisted_17,
              _createElementVNode("h2", null, _toDisplayString($setup.sessionDescribe || '你好，我是天机AI助手'), 1 /* TEXT */),
              _hoisted_18,
              ($setup.hotExamples.length)
                ? (_openBlock(), _createElementBlock("div", _hoisted_19, [
                    _hoisted_20,
                    (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.hotExamples, (example, idx) => {
                      return (_openBlock(), _createElementBlock("div", {
                        key: idx,
                        class: "example-item",
                        onClick: $event => ($setup.sendExample(example.describe))
                      }, [
                        _hoisted_22,
                        _createElementVNode("div", _hoisted_23, [
                          _createElementVNode("div", _hoisted_24, _toDisplayString(example.title), 1 /* TEXT */),
                          _createElementVNode("div", _hoisted_25, _toDisplayString(example.describe), 1 /* TEXT */)
                        ])
                      ], 8 /* PROPS */, _hoisted_21))
                    }), 128 /* KEYED_FRAGMENT */))
                  ]))
                : _createCommentVNode("v-if", true)
            ]))
          : _createCommentVNode("v-if", true),
        _createCommentVNode(" 消息列表 "),
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.messages, (msg, idx) => {
          return (_openBlock(), _createElementBlock("div", {
            key: idx,
            class: _normalizeClass(["message-row", $setup.isUserMsg(msg) ? 'user-row' : 'ai-row'])
          }, [
            (!$setup.isUserMsg(msg))
              ? (_openBlock(), _createElementBlock("div", _hoisted_26, "🤖"))
              : _createCommentVNode("v-if", true),
            _createElementVNode("div", {
              class: _normalizeClass(["message-bubble", $setup.isUserMsg(msg) ? 'user-bubble' : 'ai-bubble'])
            }, [
              _createElementVNode("div", {
                class: "message-text",
                innerHTML: $setup.formatContent(msg.content)
              }, null, 8 /* PROPS */, _hoisted_27),
              (msg.params && Object.keys(msg.params).length)
                ? (_openBlock(), _createElementBlock("div", _hoisted_28, [
                    _hoisted_29,
                    (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(msg.params, (val, key) => {
                      return (_openBlock(), _createElementBlock("div", {
                        key: key,
                        class: "param-chip"
                      }, [
                        _createElementVNode("span", _hoisted_30, _toDisplayString(key), 1 /* TEXT */),
                        _createElementVNode("span", _hoisted_31, _toDisplayString(val), 1 /* TEXT */)
                      ]))
                    }), 128 /* KEYED_FRAGMENT */))
                  ]))
                : _createCommentVNode("v-if", true)
            ], 2 /* CLASS */),
            ($setup.isUserMsg(msg))
              ? (_openBlock(), _createElementBlock("div", _hoisted_32, "👤"))
              : _createCommentVNode("v-if", true)
          ], 2 /* CLASS */))
        }), 128 /* KEYED_FRAGMENT */)),
        _createCommentVNode(" 流式输出 "),
        ($setup.streaming)
          ? (_openBlock(), _createElementBlock("div", _hoisted_33, [
              _hoisted_34,
              _createElementVNode("div", _hoisted_35, [
                _createElementVNode("div", {
                  class: "message-text",
                  innerHTML: $setup.formatContent($setup.streamingText)
                }, null, 8 /* PROPS */, _hoisted_36),
                _hoisted_37
              ])
            ]))
          : _createCommentVNode("v-if", true),
        _createCommentVNode(" 加载中 "),
        ($setup.loadingMessages)
          ? (_openBlock(), _createElementBlock("div", _hoisted_38, "加载消息中..."))
          : _createCommentVNode("v-if", true)
      ], 512 /* NEED_PATCH */),
      _createCommentVNode(" 底部输入区 "),
      _createElementVNode("div", _hoisted_39, [
        _createCommentVNode(" 模板快捷操作 "),
        ($setup.templates.length)
          ? (_openBlock(), _createElementBlock("div", _hoisted_40, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.templates, (tpl, idx) => {
                return (_openBlock(), _createElementBlock("span", {
                  key: idx,
                  class: "template-chip",
                  onClick: $event => ($setup.sendExample(tpl.prompt))
                }, _toDisplayString(tpl.label), 9 /* TEXT, PROPS */, _hoisted_41))
              }), 128 /* KEYED_FRAGMENT */))
            ]))
          : _createCommentVNode("v-if", true),
        _createElementVNode("div", _hoisted_42, [
          _withDirectives(_createElementVNode("textarea", {
            ref: "inputRef",
            "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.inputText) = $event)),
            class: "chat-input",
            placeholder: "输入你的问题...",
            rows: "1",
            onKeydown: _withKeys(_withModifiers($setup.handleSend, ["exact","prevent"]), ["enter"]),
            onInput: $setup.autoResize
          }, null, 40 /* PROPS, HYDRATE_EVENTS */, _hoisted_43), [
            [_vModelText, $setup.inputText]
          ]),
          _createElementVNode("div", _hoisted_44, [
            ($setup.streaming)
              ? (_openBlock(), _createElementBlock("button", {
                  key: 0,
                  class: "btn-stop",
                  onClick: $setup.handleStop
                }, "停止生成"))
              : (_openBlock(), _createElementBlock("button", {
                  key: 1,
                  class: "btn-send",
                  disabled: !$setup.inputText.trim(),
                  onClick: $setup.handleSend
                }, "发送", 8 /* PROPS */, _hoisted_45))
          ])
        ]),
        _hoisted_46
      ])
    ])
  ]))
}

import "/src/pages/aiAgent/index.scss?vue&type=style&index=0&src=946cffa8&lang.scss"

_sfc_main.__hmrId = "946cffa8"
typeof __VUE_HMR_RUNTIME__ !== 'undefined' && __VUE_HMR_RUNTIME__.createRecord(_sfc_main.__hmrId, _sfc_main)
import.meta.hot.accept(({ default: updated, _rerender_only }) => {
  if (_rerender_only) {
    __VUE_HMR_RUNTIME__.rerender(updated.__hmrId, updated.render)
  } else {
    __VUE_HMR_RUNTIME__.reload(updated.__hmrId, updated)
  }
})
import _export_sfc from '/@id/plugin-vue:export-helper'
export default /*#__PURE__*/_export_sfc(_sfc_main, [['render',_sfc_render],['__file',"D:/AI/code/xdxt-javaai02/前端/tj-protal/src/pages/aiAgent/index.vue"]])
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIkQ6L0FJL2NvZGUvdGp4dC1qYXZhYWkwMi/liY3nq68vdGotcHJvdGFsL3NyYy9wYWdlcy9haUFnZW50L2luZGV4LnZ1ZSJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFnSkEsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN0RCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDM0QsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDeEQsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuQjtBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDOzs7Ozs7QUFUUTtBQVVkLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuQztBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2xCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDUCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDWCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNaLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDaEIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNoQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDZCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDZCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2hCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbkI7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ25DLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDL0IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNsQztBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNMLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDekIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzFCO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDeEU7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNaLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzNCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDekI7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzdCO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNQLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuQztBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN2QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3pCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN2QixDQUFDLENBQUM7QUFDRjtBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1QsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDdkMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDM0IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN4QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDTCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QixDQUFDO0FBQ0Q7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ25DLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDUCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNwQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN2QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDMUIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDckIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMzRixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQy9FLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDckYsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3BFLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDNUUsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM1QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDTCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QixDQUFDO0FBQ0Q7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNULENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2pDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDUCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMxQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN2QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMzQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDdkIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNqRCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzdELENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1QsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNQLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbEMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ0wsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDOUIsQ0FBQztBQUNEO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDeEIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM1QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDekYsQ0FBQztBQUNEO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNQLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN0QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzFCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbkIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ0wsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3ZCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDNUIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDMUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDdEQsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2xELENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDOUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3ZCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNwQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNmLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ25ELENBQUMsQ0FBQyxDQUFDO0FBQ0gsQ0FBQztBQUNEO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNQLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM1QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbEQsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDeEIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNoQixDQUFDLENBQUMsQ0FBQztBQUNILENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3BDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNoQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzVCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3JCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMxQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbkQsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDdkMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMzQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMzQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN4QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNULENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNMLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2YsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDbkQsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNiLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2pDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNwQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDcEIsQ0FBQyxDQUFDLENBQUM7QUFDSCxDQUFDO0FBQ0Q7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1AsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2xELENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDUCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuRCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUM5QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDN0IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDckIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDTixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDekMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQy9DLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2pDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3BDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNoQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3pCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzlCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNMLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN2QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNuRCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMxQyxDQUFDO0FBQ0Q7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNaLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDL0IsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDeEIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2QsQ0FBQztBQUNEO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNQLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNoQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3JDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN0QyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3RCO0FBQ0EsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2hDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ1QsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzVDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN4RCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3BELENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2hELENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDekIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2pCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNyRCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNaLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNMLENBQUMsQ0FBQyxDQUFDO0FBQ0g7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN0RCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNsQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUN4QixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDMUI7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3pCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ3JCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQ2xCLENBQUM7QUFDRDtBQUNBLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDUCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDMUIsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDZixDQUFDO0FBQ0Q7QUFDQSxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUNaLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMxQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztBQUMzQixDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7QUFDWCxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDO0FBQzVCLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FB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LEVBQWQsVUFBUTs7O01BRzFELDhCQUFjO01BQ2Qsb0JBb0NNLE9BcENOLFdBb0NNO1FBbkNKLCtCQUFlO1NBQ2lCLGdCQUFTLENBQUMsTUFBTTsyQkFBaEQsb0JBT00sT0FQTixXQU9NO2lDQU5KLG9CQUt1Qiw2QkFKQSxnQkFBUyxHQUF0QixHQUFHLEVBQUUsR0FBRztzQ0FEbEIsb0JBS3VCO2tCQUhwQixHQUFHLEVBQUUsR0FBRztrQkFDVCxLQUFLLEVBQUMsZUFBZTtrQkFDcEIsT0FBSyxhQUFFLGtCQUFXLENBQUMsR0FBRyxDQUFDLE1BQU07b0NBQzVCLEdBQUcsQ0FBQyxLQUFLOzs7O1FBR2Ysb0JBdUJNLE9BdkJOLFdBdUJNOzBCQXRCSixvQkFRWTtZQVBWLEdBQUcsRUFBQyxVQUFVO3lFQUNMLGdCQUFTO1lBQ2xCLEtBQUssRUFBQyxZQUFZO1lBQ2xCLFdBQVcsRUFBQyxXQUFXO1lBQ3ZCLElBQUksRUFBQyxHQUFHO1lBQ1AsU0FBTywyQkFBc0IsaUJBQVU7WUFDdkMsT0FBSyxFQUFFLGlCQUFVOzswQkFMVCxnQkFBUzs7VUFPcEIsb0JBWU0sT0FaTixXQVlNO2FBVkksZ0JBQVM7K0JBRGpCLG9CQUljOztrQkFGWixLQUFLLEVBQUMsVUFBVTtrQkFDZixPQUFLLEVBQUUsaUJBQVU7bUJBQ25CLE1BQUk7K0JBQ0wsb0JBS1k7O2tCQUhWLEtBQUssRUFBQyxVQUFVO2tCQUNmLFFBQVEsR0FBRyxnQkFBUyxDQUFDLElBQUk7a0JBQ3pCLE9BQUssRUFBRSxpQkFBVTttQkFDbkIsSUFBRTs7O1FBR1AsV0FBZ0QiLCJzb3VyY2VzQ29udGVudCI6WyI8dGVtcGxhdGU+XG4gIDxkaXYgY2xhc3M9XCJhaS1hZ2VudC13cmFwcGVyXCI+XG4gICAgPCEtLSDlt6bkvqfljoblj7LorrDlvZUgLS0+XG4gICAgPGRpdiBjbGFzcz1cInNpZGViYXJcIiA6Y2xhc3M9XCJ7IGNvbGxhcHNlZDogc2lkZWJhckNvbGxhcHNlZCB9XCI+XG4gICAgICA8ZGl2IGNsYXNzPVwic2lkZWJhci1oZWFkZXJcIj5cbiAgICAgICAgPHNwYW4gY2xhc3M9XCJzaWRlYmFyLXRpdGxlXCI+QUkg5Yqp5omLPC9zcGFuPlxuICAgICAgICA8c3BhbiBjbGFzcz1cImNvbGxhcHNlLWJ0blwiIEBjbGljaz1cInNpZGViYXJDb2xsYXBzZWQgPSAhc2lkZWJhckNvbGxhcHNlZFwiPlxuICAgICAgICAgIHt7IHNpZGViYXJDb2xsYXBzZWQgPyAn4piwJyA6ICfinJUnIH19XG4gICAgICAgIDwvc3Bhbj5cbiAgICAgIDwvZGl2PlxuICAgICAgPGRpdiBjbGFzcz1cIm5ldy1jaGF0LWJ0blwiIEBjbGljaz1cImhhbmRsZU5ld1Nlc3Npb25cIj5cbiAgICAgICAgPHNwYW4+KyDmlrDlr7nor508L3NwYW4+XG4gICAgICA8L2Rpdj5cbiAgICAgIDxkaXYgY2xhc3M9XCJoaXN0b3J5LWxpc3RcIj5cbiAgICAgICAgPGRpdlxuICAgICAgICAgIHYtZm9yPVwiZ3JvdXAgaW4gaGlzdG9yeUdyb3Vwc1wiXG4gICAgICAgICAgOmtleT1cImdyb3VwLmxhYmVsXCJcbiAgICAgICAgICBjbGFzcz1cImhpc3RvcnktZ3JvdXBcIlxuICAgICAgICA+XG4gICAgICAgICAgPGRpdiBjbGFzcz1cImdyb3VwLWxhYmVsXCI+e3sgZ3JvdXAubGFiZWwgfX08L2Rpdj5cbiAgICAgICAgICA8ZGl2XG4gICAgICAgICAgICB2LWZvcj1cIml0ZW0gaW4gZ3JvdXAuc2Vzc2lvbnNcIlxuICAgICAgICAgICAgOmtleT1cIml0ZW0uc2Vzc2lvbklkXCJcbiAgICAgICAgICAgIGNsYXNzPVwiaGlzdG9yeS1pdGVtXCJcbiAgICAgICAgICAgIDpjbGFzcz1cInsgYWN0aXZlOiBjdXJyZW50U2Vzc2lvbklkID09PSBpdGVtLnNlc3Npb25JZCB9XCJcbiAgICAgICAgICAgIEBjbGljaz1cInN3aXRjaFNlc3Npb24oaXRlbS5zZXNzaW9uSWQpXCJcbiAgICAgICAgICA+XG4gICAgICAgICAgICA8c3BhbiBjbGFzcz1cIml0ZW0tdGl0bGVcIj57eyBpdGVtLnRpdGxlIHx8ICfmlrDlr7nor50nIH19PC9zcGFuPlxuICAgICAgICAgICAgPHNwYW4gY2xhc3M9XCJpdGVtLWRlbGV0ZVwiIEBjbGljay5zdG9wPVwiaGFuZGxlRGVsZXRlU2Vzc2lvbihpdGVtLnNlc3Npb25JZClcIj7inJU8L3NwYW4+XG4gICAgICAgICAgPC9kaXY+XG4gICAgICAgIDwvZGl2PlxuICAgICAgICA8ZGl2IHYtaWY9XCIhaGlzdG9yeUdyb3Vwcy5sZW5ndGhcIiBjbGFzcz1cImVtcHR5LWhpc3RvcnlcIj7mmoLml6Dljoblj7LorrDlvZU8L2Rpdj5cbiAgICAgIDwvZGl2PlxuICAgIDwvZGl2PlxuXG4gICAgPCEtLSDkuLvogYrlpKnljLrln58gLS0+XG4gICAgPGRpdiBjbGFzcz1cImNoYXQtbWFpblwiPlxuICAgICAgPCEtLSDpobbpg6ggLS0+XG4gICAgICA8ZGl2IGNsYXNzPVwiY2hhdC1oZWFkZXJcIj5cbiAgICAgICAgPHNwYW4gY2xhc3M9XCJ0b2dnbGUtc2lkZWJhclwiIEBjbGljaz1cInNpZGViYXJDb2xsYXBzZWQgPSAhc2lkZWJhckNvbGxhcHNlZFwiPuKYsDwvc3Bhbj5cbiAgICAgICAgPHNwYW4gY2xhc3M9XCJjaGF0LXRpdGxlXCI+e3sgY3VycmVudFNlc3Npb25UaXRsZSB8fCAnQUkg5pm66IO95Yqp5omLJyB9fTwvc3Bhbj5cbiAgICAgIDwvZGl2PlxuXG4gICAgICA8IS0tIOa2iOaBr+WMuuWfnyAtLT5cbiAgICAgIDxkaXYgY2xhc3M9XCJjaGF0LW1lc3NhZ2VzXCIgcmVmPVwibWVzc2FnZXNDb250YWluZXJcIj5cbiAgICAgICAgPCEtLSDmrKLov47pobUgJiDnqbrkvJror50gLS0+XG4gICAgICAgIDxkaXYgdi1pZj1cIiFjdXJyZW50U2Vzc2lvbklkICYmIG1lc3NhZ2VzLmxlbmd0aCA9PT0gMFwiIGNsYXNzPVwid2VsY29tZVwiPlxuICAgICAgICAgIDxkaXYgY2xhc3M9XCJ3ZWxjb21lLWljb25cIj7wn6SWPC9kaXY+XG4gICAgICAgICAgPGgyPnt7IHNlc3Npb25EZXNjcmliZSB8fCAn5L2g5aW977yM5oiR5piv5aSp5py6QUnliqnmiYsnIH19PC9oMj5cbiAgICAgICAgICA8cD7miJHlj6/ku6XluK7kvaDmjqjojZDor77nqIvjgIHop6PnrZTlrabkuaDpl67popjvvIzor7fpmo/ml7blkJHmiJHmj5Dpl648L3A+XG4gICAgICAgICAgPGRpdiBjbGFzcz1cImhvdC1leGFtcGxlc1wiIHYtaWY9XCJob3RFeGFtcGxlcy5sZW5ndGhcIj5cbiAgICAgICAgICAgIDxkaXYgY2xhc3M9XCJleGFtcGxlcy10aXRsZVwiPueDremXqOmXrumimDwvZGl2PlxuICAgICAgICAgICAgPGRpdlxuICAgICAgICAgICAgICB2LWZvcj1cIihleGFtcGxlLCBpZHgpIGluIGhvdEV4YW1wbGVzXCJcbiAgICAgICAgICAgICAgOmtleT1cImlkeFwiXG4gICAgICAgICAgICAgIGNsYXNzPVwiZXhhbXBsZS1pdGVtXCJcbiAgICAgICAgICAgICAgQGNsaWNrPVwic2VuZEV4YW1wbGUoZXhhbXBsZS5kZXNjcmliZSlcIlxuICAgICAgICAgICAgPlxuICAgICAgICAgICAgICA8c3BhbiBjbGFzcz1cImV4YW1wbGUtaWNvblwiPvCfkqw8L3NwYW4+XG4gICAgICAgICAgICAgIDxkaXYgY2xhc3M9XCJleGFtcGxlLXRleHRcIj5cbiAgICAgICAgICAgICAgICA8ZGl2IGNsYXNzPVwiZXhhbXBsZS10aXRsZVwiPnt7IGV4YW1wbGUudGl0bGUgfX08L2Rpdj5cbiAgICAgICAgICAgICAgICA8ZGl2IGNsYXNzPVwiZXhhbXBsZS1kZXNjXCI+e3sgZXhhbXBsZS5kZXNjcmliZSB9fTwvZGl2PlxuICAgICAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgICAgIDwvZGl2PlxuICAgICAgICAgIDwvZGl2PlxuICAgICAgICA8L2Rpdj5cblxuICAgICAgICA8IS0tIOa2iOaBr+WIl+ihqCAtLT5cbiAgICAgICAgPGRpdlxuICAgICAgICAgIHYtZm9yPVwiKG1zZywgaWR4KSBpbiBtZXNzYWdlc1wiXG4gICAgICAgICAgOmtleT1cImlkeFwiXG4gICAgICAgICAgY2xhc3M9XCJtZXNzYWdlLXJvd1wiXG4gICAgICAgICAgOmNsYXNzPVwiaXNVc2VyTXNnKG1zZykgPyAndXNlci1yb3cnIDogJ2FpLXJvdydcIlxuICAgICAgICA+XG4gICAgICAgICAgPGRpdiBjbGFzcz1cIm1lc3NhZ2UtYXZhdGFyXCIgdi1pZj1cIiFpc1VzZXJNc2cobXNnKVwiPvCfpJY8L2Rpdj5cbiAgICAgICAgICA8ZGl2IGNsYXNzPVwibWVzc2FnZS1idWJibGVcIiA6Y2xhc3M9XCJpc1VzZXJNc2cobXNnKSA/ICd1c2VyLWJ1YmJsZScgOiAnYWktYnViYmxlJ1wiPlxuICAgICAgICAgICAgPGRpdiBjbGFzcz1cIm1lc3NhZ2UtdGV4dFwiIHYtaHRtbD1cImZvcm1hdENvbnRlbnQobXNnLmNvbnRlbnQpXCI+PC9kaXY+XG4gICAgICAgICAgICA8ZGl2IGNsYXNzPVwibWVzc2FnZS1wYXJhbXNcIiB2LWlmPVwibXNnLnBhcmFtcyAmJiBPYmplY3Qua2V5cyhtc2cucGFyYW1zKS5sZW5ndGhcIj5cbiAgICAgICAgICAgICAgPGRpdiBjbGFzcz1cInBhcmFtcy1sYWJlbFwiPuW3peWFt+iwg+eUqOe7k+aenO+8mjwvZGl2PlxuICAgICAgICAgICAgICA8ZGl2IHYtZm9yPVwiKHZhbCwga2V5KSBpbiBtc2cucGFyYW1zXCIgOmtleT1cImtleVwiIGNsYXNzPVwicGFyYW0tY2hpcFwiPlxuICAgICAgICAgICAgICAgIDxzcGFuIGNsYXNzPVwicGFyYW0ta2V5XCI+e3sga2V5IH19PC9zcGFuPlxuICAgICAgICAgICAgICAgIDxzcGFuIGNsYXNzPVwicGFyYW0tdmFsXCI+e3sgdmFsIH19PC9zcGFuPlxuICAgICAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgICAgIDwvZGl2PlxuICAgICAgICAgIDwvZGl2PlxuICAgICAgICAgIDxkaXYgY2xhc3M9XCJtZXNzYWdlLWF2YXRhciB1c2VyLWF2YXRhclwiIHYtaWY9XCJpc1VzZXJNc2cobXNnKVwiPvCfkaQ8L2Rpdj5cbiAgICAgICAgPC9kaXY+XG5cbiAgICAgICAgPCEtLSDmtYHlvI/ovpPlh7ogLS0+XG4gICAgICAgIDxkaXYgdi1pZj1cInN0cmVhbWluZ1wiIGNsYXNzPVwibWVzc2FnZS1yb3cgYWktcm93XCI+XG4gICAgICAgICAgPGRpdiBjbGFzcz1cIm1lc3NhZ2UtYXZhdGFyXCI+8J+kljwvZGl2PlxuICAgICAgICAgIDxkaXYgY2xhc3M9XCJtZXNzYWdlLWJ1YmJsZSBhaS1idWJibGVcIj5cbiAgICAgICAgICAgIDxkaXYgY2xhc3M9XCJtZXNzYWdlLXRleHRcIiB2LWh0bWw9XCJmb3JtYXRDb250ZW50KHN0cmVhbWluZ1RleHQpXCI+PC9kaXY+XG4gICAgICAgICAgICA8c3BhbiBjbGFzcz1cImN1cnNvci1ibGlua1wiPnw8L3NwYW4+XG4gICAgICAgICAgPC9kaXY+XG4gICAgICAgIDwvZGl2PlxuXG4gICAgICAgIDwhLS0g5Yqg6L295LitIC0tPlxuICAgICAgICA8ZGl2IHYtaWY9XCJsb2FkaW5nTWVzc2FnZXNcIiBjbGFzcz1cImxvYWRpbmctbXNnXCI+5Yqg6L295raI5oGv5LitLi4uPC9kaXY+XG4gICAgICA8L2Rpdj5cblxuICAgICAgPCEtLSDlupXpg6jovpPlhaXljLogLS0+XG4gICAgICA8ZGl2IGNsYXNzPVwiY2hhdC1pbnB1dC1hcmVhXCI+XG4gICAgICAgIDwhLS0g5qih5p2/5b+r5o235pON5L2cIC0tPlxuICAgICAgICA8ZGl2IGNsYXNzPVwidGVtcGxhdGUtYmFyXCIgdi1pZj1cInRlbXBsYXRlcy5sZW5ndGhcIj5cbiAgICAgICAgICA8c3BhblxuICAgICAgICAgICAgdi1mb3I9XCIodHBsLCBpZHgpIGluIHRlbXBsYXRlc1wiXG4gICAgICAgICAgICA6a2V5PVwiaWR4XCJcbiAgICAgICAgICAgIGNsYXNzPVwidGVtcGxhdGUtY2hpcFwiXG4gICAgICAgICAgICBAY2xpY2s9XCJzZW5kRXhhbXBsZSh0cGwucHJvbXB0KVwiXG4gICAgICAgICAgPnt7IHRwbC5sYWJlbCB9fTwvc3Bhbj5cbiAgICAgICAgPC9kaXY+XG5cbiAgICAgICAgPGRpdiBjbGFzcz1cImlucHV0LXdyYXBwZXJcIj5cbiAgICAgICAgICA8dGV4dGFyZWFcbiAgICAgICAgICAgIHJlZj1cImlucHV0UmVmXCJcbiAgICAgICAgICAgIHYtbW9kZWw9XCJpbnB1dFRleHRcIlxuICAgICAgICAgICAgY2xhc3M9XCJjaGF0LWlucHV0XCJcbiAgICAgICAgICAgIHBsYWNlaG9sZGVyPVwi6L6T5YWl5L2g55qE6Zeu6aKYLi4uXCJcbiAgICAgICAgICAgIHJvd3M9XCIxXCJcbiAgICAgICAgICAgIEBrZXlkb3duLmVudGVyLmV4YWN0LnByZXZlbnQ9XCJoYW5kbGVTZW5kXCJcbiAgICAgICAgICAgIEBpbnB1dD1cImF1dG9SZXNpemVcIlxuICAgICAgICAgID48L3RleHRhcmVhPlxuICAgICAgICAgIDxkaXYgY2xhc3M9XCJpbnB1dC1hY3Rpb25zXCI+XG4gICAgICAgICAgICA8YnV0dG9uXG4gICAgICAgICAgICAgIHYtaWY9XCJzdHJlYW1pbmdcIlxuICAgICAgICAgICAgICBjbGFzcz1cImJ0bi1zdG9wXCJcbiAgICAgICAgICAgICAgQGNsaWNrPVwiaGFuZGxlU3RvcFwiXG4gICAgICAgICAgICA+5YGc5q2i55Sf5oiQPC9idXR0b24+XG4gICAgICAgICAgICA8YnV0dG9uXG4gICAgICAgICAgICAgIHYtZWxzZVxuICAgICAgICAgICAgICBjbGFzcz1cImJ0bi1zZW5kXCJcbiAgICAgICAgICAgICAgOmRpc2FibGVkPVwiIWlucHV0VGV4dC50cmltKClcIlxuICAgICAgICAgICAgICBAY2xpY2s9XCJoYW5kbGVTZW5kXCJcbiAgICAgICAgICAgID7lj5HpgIE8L2J1dHRvbj5cbiAgICAgICAgICA8L2Rpdj5cbiAgICAgICAgPC9kaXY+XG4gICAgICAgIDxkaXYgY2xhc3M9XCJpbnB1dC1kaXNjbGFpbWVyXCI+5YaF5a6555SxQUnnlJ/miJDvvIzku4Xkvpvlj4LogIM8L2Rpdj5cbiAgICAgIDwvZGl2PlxuICAgIDwvZGl2PlxuICA8L2Rpdj5cbjwvdGVtcGxhdGU+XG5cbjxzY3JpcHQgc2V0dXA+XG5pbXBvcnQgeyByZWYsIG9uTW91bnRlZCwgbmV4dFRpY2sgfSBmcm9tICd2dWUnXG5pbXBvcnQgeyBFbE1lc3NhZ2UsIEVsTWVzc2FnZUJveCB9IGZyb20gJ2VsZW1lbnQtcGx1cydcbmltcG9ydCB7IHVzZVN0cmVhbUNoYXQgfSBmcm9tICdAL2NvbXBvc2FibGVzL3VzZVN0cmVhbUNoYXQnXG5pbXBvcnQge1xuICBnZXRIb3RFeGFtcGxlcywgZ2V0U2Vzc2lvbk1lc3NhZ2VzLFxuICBnZXRIaXN0b3J5U2Vzc2lvbnMsIGRlbGV0ZUhpc3RvcnlTZXNzaW9uLCBnZXRUZW1wbGF0ZXNcbn0gZnJvbSAnQC9hcGkvYWlnYydcblxuLy8g5L6n6L655qCPXG5jb25zdCBzaWRlYmFyQ29sbGFwc2VkID0gcmVmKGZhbHNlKVxuXG4vLyDmtYHlvI/ogYrlpKkgY29tcG9zYWJsZVxuY29uc3Qge1xuICBzZXNzaW9uSWQ6IGN1cnJlbnRTZXNzaW9uSWQsXG4gIG1lc3NhZ2VzLFxuICBzdHJlYW1pbmcsXG4gIHN0cmVhbWluZ1RleHQsXG4gIGVuc3VyZVNlc3Npb24sXG4gIHNlbmRNZXNzYWdlLFxuICBhYm9ydFN0cmVhbSxcbiAgZm9ybWF0Q29udGVudCxcbiAgc2Nyb2xsVG9Cb3R0b206IGNvbXBvc2FibGVTY3JvbGxUb0JvdHRvbVxufSA9IHVzZVN0cmVhbUNoYXQoKVxuXG4vLyDkvJror53nm7jlhbNcbmNvbnN0IGN1cnJlbnRTZXNzaW9uVGl0bGUgPSByZWYoJycpXG5jb25zdCBzZXNzaW9uRGVzY3JpYmUgPSByZWYoJycpXG5jb25zdCBsb2FkaW5nTWVzc2FnZXMgPSByZWYoZmFsc2UpXG5cbi8vIOi+k+WFpVxuY29uc3QgaW5wdXRUZXh0ID0gcmVmKCcnKVxuY29uc3QgaW5wdXRSZWYgPSByZWYobnVsbClcblxuLy8g5rua5Yqo6YCC6YWN5ZmoXG5jb25zdCBzY3JvbGxUb0JvdHRvbSA9ICgpID0+IGNvbXBvc2FibGVTY3JvbGxUb0JvdHRvbShtZXNzYWdlc0NvbnRhaW5lcilcblxuLy8g54Ot6Zeo56S65L6LICYg5qih5p2/XG5jb25zdCBob3RFeGFtcGxlcyA9IHJlZihbXSlcbmNvbnN0IHRlbXBsYXRlcyA9IHJlZihbXSlcblxuLy8g5Y6G5Y+y5Lya6K+dXG5jb25zdCBoaXN0b3J5R3JvdXBzID0gcmVmKFtdKVxuXG4vLyDmtojmga/lrrnlmahcbmNvbnN0IG1lc3NhZ2VzQ29udGFpbmVyID0gcmVmKG51bGwpXG5cbm9uTW91bnRlZChhc3luYyAoKSA9PiB7XG4gIGF3YWl0IGxvYWRIaXN0b3J5KClcbiAgYXdhaXQgbG9hZEhvdEV4YW1wbGVzKClcbiAgYXdhaXQgbG9hZFRlbXBsYXRlcygpXG59KVxuXG4vLyDliqDovb3ng63pl6jnpLrkvotcbmNvbnN0IGxvYWRIb3RFeGFtcGxlcyA9IGFzeW5jICgpID0+IHtcbiAgdHJ5IHtcbiAgICBjb25zdCByZXMgPSBhd2FpdCBnZXRIb3RFeGFtcGxlcygzKVxuICAgIGlmIChyZXMuY29kZSA9PT0gMjAwKSB7XG4gICAgICBob3RFeGFtcGxlcy52YWx1ZSA9IHJlcy5kYXRhIHx8IFtdXG4gICAgfVxuICB9IGNhdGNoIChlKSB7IC8qIGlnbm9yZSAqLyB9XG59XG5cbi8vIOWKoOi9veaooeadv1xuY29uc3QgbG9hZFRlbXBsYXRlcyA9IGFzeW5jICgpID0+IHtcbiAgdHJ5IHtcbiAgICBjb25zdCByZXMgPSBhd2FpdCBnZXRUZW1wbGF0ZXMoKVxuICAgIGlmIChyZXMuY29kZSA9PT0gMjAwICYmIHJlcy5kYXRhKSB7XG4gICAgICBjb25zdCB0cGwgPSByZXMuZGF0YVxuICAgICAgY29uc3QgbGlzdCA9IFtdXG4gICAgICBpZiAodHBsLmFzc29jaWF0aW9uYWxXb3JkKSBsaXN0LnB1c2goeyBsYWJlbDogJ+iBlOaDs+ivjScsIHByb21wdDogdHBsLmFzc29jaWF0aW9uYWxXb3JkIH0pXG4gICAgICBpZiAodHBsLmhlbHBlZFdyaXRlKSBsaXN0LnB1c2goeyBsYWJlbDogJ+W4ruaIkeWGmScsIHByb21wdDogdHBsLmhlbHBlZFdyaXRlIH0pXG4gICAgICBpZiAodHBsLmNvbnRpbnVlZFdyaXRlKSBsaXN0LnB1c2goeyBsYWJlbDogJ+e7p+e7reWGmScsIHByb21wdDogdHBsLmNvbnRpbnVlZFdyaXRlIH0pXG4gICAgICBpZiAodHBsLnBvbGlzaCkgbGlzdC5wdXNoKHsgbGFiZWw6ICfmtqboibInLCBwcm9tcHQ6IHRwbC5wb2xpc2ggfSlcbiAgICAgIGlmICh0cGwuc3RyZWFtbGluZSkgbGlzdC5wdXNoKHsgbGFiZWw6ICfnsr7nroAnLCBwcm9tcHQ6IHRwbC5zdHJlYW1saW5lIH0pXG4gICAgICB0ZW1wbGF0ZXMudmFsdWUgPSBsaXN0XG4gICAgfVxuICB9IGNhdGNoIChlKSB7IC8qIGlnbm9yZSAqLyB9XG59XG5cbi8vIOWKoOi9veWOhuWPsuS8muivnVxuY29uc3QgbG9hZEhpc3RvcnkgPSBhc3luYyAoKSA9PiB7XG4gIHRyeSB7XG4gICAgY29uc3QgcmVzID0gYXdhaXQgZ2V0SGlzdG9yeVNlc3Npb25zKClcbiAgICBpZiAocmVzLmNvZGUgPT09IDIwMCAmJiByZXMuZGF0YSkge1xuICAgICAgY29uc3QgZGF0YSA9IHJlcy5kYXRhXG4gICAgICBjb25zdCBncm91cHMgPSBbXVxuICAgICAgLy8gZGF0YSBpcyBNYXA8U3RyaW5nLCBMaXN0PENoYXRTZXNzaW9uVk8+PlxuICAgICAgZm9yIChjb25zdCBbbGFiZWwsIHNlc3Npb25zXSBvZiBPYmplY3QuZW50cmllcyhkYXRhKSkge1xuICAgICAgICBpZiAoc2Vzc2lvbnMgJiYgc2Vzc2lvbnMubGVuZ3RoKSB7XG4gICAgICAgICAgZ3JvdXBzLnB1c2goeyBsYWJlbCwgc2Vzc2lvbnMgfSlcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgaGlzdG9yeUdyb3Vwcy52YWx1ZSA9IGdyb3Vwc1xuICAgIH1cbiAgfSBjYXRjaCAoZSkgeyAvKiBpZ25vcmUgKi8gfVxufVxuXG4vLyDliKTmlq3mmK/lkKbmmK/nlKjmiLfmtojmga8gKOWFvOWuueWtl+espuS4suS4juaemuS4vuWAvClcbmNvbnN0IGlzVXNlck1zZyA9IChtc2cpID0+IHtcbiAgcmV0dXJuIG1zZy50eXBlID09PSAnVVNFUicgfHwgbXNnLnR5cGUgPT09ICd1c2VyJyB8fCBtc2cudHlwZSA9PT0gMSB8fCBtc2cudHlwZSA9PT0gJzEnXG59XG5cbi8vIOaWsOW7uuS8muivnVxuY29uc3QgaGFuZGxlTmV3U2Vzc2lvbiA9IGFzeW5jICgpID0+IHtcbiAgdHJ5IHtcbiAgICBpZiAoc3RyZWFtaW5nLnZhbHVlKSB7XG4gICAgICBhYm9ydFN0cmVhbSgpXG4gICAgfVxuICAgIG1lc3NhZ2VzLnZhbHVlID0gW11cbiAgICBzdHJlYW1pbmdUZXh0LnZhbHVlID0gJydcbiAgICBjb25zdCBzZXNzaW9uID0gYXdhaXQgZW5zdXJlU2Vzc2lvbigzKVxuICAgIGN1cnJlbnRTZXNzaW9uVGl0bGUudmFsdWUgPSBzZXNzaW9uLnRpdGxlIHx8ICfmlrDlr7nor50nXG4gICAgc2Vzc2lvbkRlc2NyaWJlLnZhbHVlID0gc2Vzc2lvbi5kZXNjcmliZSB8fCAnJ1xuICAgIGhvdEV4YW1wbGVzLnZhbHVlID0gc2Vzc2lvbi5leGFtcGxlcyB8fCBbXVxuICAgIGF3YWl0IGxvYWRIaXN0b3J5KClcbiAgICBzY3JvbGxUb0JvdHRvbSgpXG4gIH0gY2F0Y2ggKGUpIHtcbiAgICBFbE1lc3NhZ2UoeyBtZXNzYWdlOiAn5Yib5bu65Lya6K+d5aSx6LSlJywgdHlwZTogJ2Vycm9yJyB9KVxuICB9XG59XG5cbi8vIOWIh+aNouS8muivnVxuY29uc3Qgc3dpdGNoU2Vzc2lvbiA9IGFzeW5jIChzZXNzaW9uSWQpID0+IHtcbiAgaWYgKHNlc3Npb25JZCA9PT0gY3VycmVudFNlc3Npb25JZC52YWx1ZSkgcmV0dXJuXG4gIGlmIChzdHJlYW1pbmcudmFsdWUpIHtcbiAgICBoYW5kbGVTdG9wKClcbiAgfVxuICBjdXJyZW50U2Vzc2lvbklkLnZhbHVlID0gc2Vzc2lvbklkXG4gIGN1cnJlbnRTZXNzaW9uVGl0bGUudmFsdWUgPSAnJ1xuICBzZXNzaW9uRGVzY3JpYmUudmFsdWUgPSAnJ1xuICBtZXNzYWdlcy52YWx1ZSA9IFtdXG4gIHN0cmVhbWluZ1RleHQudmFsdWUgPSAnJ1xuICBsb2FkaW5nTWVzc2FnZXMudmFsdWUgPSB0cnVlXG4gIHRyeSB7XG4gICAgY29uc3QgcmVzID0gYXdhaXQgZ2V0U2Vzc2lvbk1lc3NhZ2VzKHNlc3Npb25JZClcbiAgICBpZiAocmVzLmNvZGUgPT09IDIwMCAmJiByZXMuZGF0YSkge1xuICAgICAgbWVzc2FnZXMudmFsdWUgPSByZXMuZGF0YS5tYXAobSA9PiAoe1xuICAgICAgICB0eXBlOiBtLnR5cGUsXG4gICAgICAgIGNvbnRlbnQ6IG0uY29udGVudCxcbiAgICAgICAgcGFyYW1zOiBtLnBhcmFtc1xuICAgICAgfSkpXG4gICAgfVxuICB9IGNhdGNoIChlKSB7XG4gICAgRWxNZXNzYWdlKHsgbWVzc2FnZTogJ+WKoOi9vea2iOaBr+Wksei0pScsIHR5cGU6ICdlcnJvcicgfSlcbiAgfSBmaW5hbGx5IHtcbiAgICBsb2FkaW5nTWVzc2FnZXMudmFsdWUgPSBmYWxzZVxuICAgIGF3YWl0IG5leHRUaWNrKClcbiAgICBzY3JvbGxUb0JvdHRvbSgpXG4gIH1cbn1cblxuLy8g5Yig6Zmk5Lya6K+dXG5jb25zdCBoYW5kbGVEZWxldGVTZXNzaW9uID0gYXN5bmMgKHNlc3Npb25JZCkgPT4ge1xuICB0cnkge1xuICAgIGF3YWl0IEVsTWVzc2FnZUJveC5jb25maXJtKCfnoa7lrprliKDpmaTor6XkvJror53lkJfvvJ8nLCAn5o+Q56S6Jywge1xuICAgICAgY29uZmlybUJ1dHRvblRleHQ6ICfnoa7lrponLFxuICAgICAgY2FuY2VsQnV0dG9uVGV4dDogJ+WPlua2iCcsXG4gICAgICB0eXBlOiAnd2FybmluZydcbiAgICB9KVxuICAgIGF3YWl0IGRlbGV0ZUhpc3RvcnlTZXNzaW9uKHNlc3Npb25JZClcbiAgICBpZiAoY3VycmVudFNlc3Npb25JZC52YWx1ZSA9PT0gc2Vzc2lvbklkKSB7XG4gICAgICBjdXJyZW50U2Vzc2lvbklkLnZhbHVlID0gJydcbiAgICAgIGN1cnJlbnRTZXNzaW9uVGl0bGUudmFsdWUgPSAnJ1xuICAgICAgc2Vzc2lvbkRlc2NyaWJlLnZhbHVlID0gJydcbiAgICAgIG1lc3NhZ2VzLnZhbHVlID0gW11cbiAgICAgIHN0cmVhbWluZ1RleHQudmFsdWUgPSAnJ1xuICAgIH1cbiAgICBhd2FpdCBsb2FkSGlzdG9yeSgpXG4gICAgRWxNZXNzYWdlKHsgbWVzc2FnZTogJ+WIoOmZpOaIkOWKnycsIHR5cGU6ICdzdWNjZXNzJyB9KVxuICB9IGNhdGNoIChlKSB7IC8qIGNhbmNlbGxlZCBvciBlcnJvciAqLyB9XG59XG5cbi8vIOWPkemAgeekuuS+iy/mqKHmnb/pl67pophcbmNvbnN0IHNlbmRFeGFtcGxlID0gKHRleHQpID0+IHtcbiAgaW5wdXRUZXh0LnZhbHVlID0gdGV4dFxuICBoYW5kbGVTZW5kKClcbn1cblxuLy8g5Y+R6YCB5raI5oGvXG5jb25zdCBoYW5kbGVTZW5kID0gYXN5bmMgKCkgPT4ge1xuICBjb25zdCB0ZXh0ID0gaW5wdXRUZXh0LnZhbHVlLnRyaW0oKVxuICBpZiAoIXRleHQgfHwgc3RyZWFtaW5nLnZhbHVlKSByZXR1cm5cbiAgaW5wdXRUZXh0LnZhbHVlID0gJydcblxuICBpZiAoIWN1cnJlbnRTZXNzaW9uSWQudmFsdWUpIHtcbiAgICB0cnkge1xuICAgICAgY29uc3Qgc2Vzc2lvbiA9IGF3YWl0IGVuc3VyZVNlc3Npb24oMylcbiAgICAgIGN1cnJlbnRTZXNzaW9uVGl0bGUudmFsdWUgPSBzZXNzaW9uLnRpdGxlIHx8ICfmlrDlr7nor50nXG4gICAgICBzZXNzaW9uRGVzY3JpYmUudmFsdWUgPSBzZXNzaW9uLmRlc2NyaWJlIHx8ICcnXG4gICAgICBob3RFeGFtcGxlcy52YWx1ZSA9IHNlc3Npb24uZXhhbXBsZXMgfHwgW11cbiAgICAgIGF3YWl0IGxvYWRIaXN0b3J5KClcbiAgICB9IGNhdGNoIChlKSB7XG4gICAgICBFbE1lc3NhZ2UoeyBtZXNzYWdlOiAn5Yib5bu65Lya6K+d5aSx6LSlJywgdHlwZTogJ2Vycm9yJyB9KVxuICAgICAgcmV0dXJuXG4gICAgfVxuICB9XG5cbiAgbWVzc2FnZXMudmFsdWUucHVzaCh7IHR5cGU6ICdVU0VSJywgY29udGVudDogdGV4dCB9KVxuICBzY3JvbGxUb0JvdHRvbSgpXG4gIHN0cmVhbWluZy52YWx1ZSA9IHRydWVcbiAgc3RyZWFtaW5nVGV4dC52YWx1ZSA9ICcnXG5cbiAgYXdhaXQgc2VuZE1lc3NhZ2UodGV4dClcbiAgYXdhaXQgbG9hZEhpc3RvcnkoKVxuICBzY3JvbGxUb0JvdHRvbSgpXG59XG5cbi8vIOWBnOatoueUn+aIkFxuY29uc3QgaGFuZGxlU3RvcCA9ICgpID0+IHtcbiAgYWJvcnRTdHJlYW0oKVxufVxuXG4vLyDoh6rliqjosIPmlbTovpPlhaXmoYbpq5jluqZcbmNvbnN0IGF1dG9SZXNpemUgPSAoKSA9PiB7XG4gIGNvbnN0IGVsID0gaW5wdXRSZWYudmFsdWVcbiAgaWYgKGVsKSB7XG4gICAgZWwuc3R5bGUuaGVpZ2h0ID0gJ2F1dG8nXG4gICAgZWwuc3R5bGUuaGVpZ2h0ID0gTWF0aC5taW4oZWwuc2Nyb2xsSGVpZ2h0LCAxNTApICsgJ3B4J1xuICB9XG59XG48L3NjcmlwdD5cblxuPHN0eWxlIGxhbmc9XCJzY3NzXCIgc3JjPVwiLi9pbmRleC5zY3NzXCI+PC9zdHlsZT5cbiJdfQ==