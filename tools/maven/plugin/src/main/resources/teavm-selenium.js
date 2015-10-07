var runtimeSource = arguments[0]
var testSource = arguments[1]
var adapterSource = arguments[2]

var iframe = document.createElement("iframe")
document.appendChild(iframe)
var doc = iframe.contentDocument

loadScripts([ runtimeSource, adapterSource, testSource ], runTest)
window.addEventListener("message", handleMessage)

function handleMessage(event) {
    window.removeEventListener("message", handleMessage)
    callback(JSON.stringify(message.data))
}

var handler = window.addEventListener("message", function(event) {
    window.removeEventListener
})

function loadScript(script, callback) {
    var elem = doc.createElement("script")
    elem.setAttribute("type", "text/javascript")
    elem.appendChild(doc.createTextNode(runtimeSource))
    elem.onload = function() {
        callback()
    }
    doc.body.appendChild(script)
}

function loadScripts(scripts, callback, index) {
    index = index || 0
    loadScript(scripts[i], function() {
        if (++index == scripts.length) {
            callback()
        } else {
            loadScripts(scripts, callback, index)
        }
    })
}