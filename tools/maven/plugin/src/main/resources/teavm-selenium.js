var runtimeSource = arguments[0]
var testSource = arguments[1]
var adapterSource = arguments[2]
var seleniumCallback = arguments[arguments.length - 1]

var iframe = document.createElement("iframe")
document.body.appendChild(iframe)
var doc = iframe.contentDocument

window.jsErrors = []
window.onerror = reportError
iframe.contentWindow.onerror = reportError

loadScripts([ runtimeSource, adapterSource, testSource ])
window.addEventListener("message", handleMessage)

function handleMessage(event) {
    window.removeEventListener("message", handleMessage)
    document.body.removeChild(iframe)
    seleniumCallback(event.data)
}

function loadScript(script, callback) {
    callback()
}

function loadScripts(scripts) {
    for (var i = 0; i < scripts.length; ++i) {
        var elem = doc.createElement("script")
        elem.type = "text/javascript"
        doc.head.appendChild(elem)
        elem.text = scripts[i]
    }
}
function reportError(error, url, line) {
    window.jsErrors.push(error + " at " + line)
}
function report(error) {
    window.jsErrors.push(error)
}
function globalEval(window, arg) {
    eval.apply(window, [arg])
}