let $rt_stdoutBuffer = "";
function $rt_putStdoutCustom(str) {
    let index = 0;
    while (true) {
        let next = msg.indexOf('\n', index);
        if (next < 0) {
            break;
        }
        let line = buffer + msg.substring(index, next);
        let lineElem = document.createElement("div");
        let stdoutElem = document.getElementById("stdout");
        lineElem.appendChild(document.createTextNode(line));
        stdoutElem.appendChild(lineElem);
        buffer = "";
        index = next + 1;
    }
}
this.$rt_putStdoutCustom = $rt_putStdoutCustom;