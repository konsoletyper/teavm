let $rt_stdoutBuffer = "";
function $rt_putStdoutCustom(msg) {
    let index = 0;
    while (true) {
        let next = msg.indexOf('\n', index);
        if (next < 0) {
            break;
        }
        let line = $rt_stdoutBuffer + msg.substring(index, next);
        let lineElem = document.createElement("div");
        let stdoutElem = document.getElementById("stdout");
        lineElem.appendChild(document.createTextNode(line));
        stdoutElem.appendChild(lineElem);
        stdoutElem.scrollTop = stdoutElem.scrollHeight;
        $rt_stdoutBuffer = "";
        index = next + 1;
    }
    $rt_stdoutBuffer += msg.substring(index);
}
this.$rt_putStdoutCustom = $rt_putStdoutCustom;