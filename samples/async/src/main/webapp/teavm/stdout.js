var $rt_stdoutBuffer = "";
function $rt_putStdoutCustom(ch) {
    if (ch === 0xA) {
        var lineElem = document.createElement("div");
        var stdoutElem = document.getElementById("stdout");
        lineElem.appendChild(document.createTextNode($rt_stdoutBuffer));
        stdoutElem.appendChild(lineElem);
        $rt_stdoutBuffer = "";
        stdoutElem.scrollTop = stdoutElem.scrollHeight;
    } else {
        $rt_stdoutBuffer += String.fromCharCode(ch);
    }
} 
