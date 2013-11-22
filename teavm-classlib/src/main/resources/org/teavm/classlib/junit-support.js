JUnitAssertionFailure = function() {}
JUnitAssertionFailure.prototype = new Error();

currentTestReportBody = null;

runTestCase = function(instance, methodName, realMethodName) {
    var row = document.createElement("tr");
    currentTestReportBody.appendChild(row);
    var nameCell = document.createElement("td");
    row.appendChild(nameCell);
    nameCell.appendChild(document.createTextNode(methodName));
    var statusCell = document.createElement("td");
    row.appendChild(statusCell);
    var exceptionCell = document.createElement("td");
    row.appendChild(exceptionCell);
    try {
        instance[realMethodName]();
        statusCell.appendChild(document.createTextNode("ok"));
    } catch (e) {
        if (e instanceof JUnitAssertionFailure) {
            statusCell.appendChild(document.createTextNode("assertion failed"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(e.stack));
            exceptionCell.appendChild(exceptionText);
        } else {
            statusCell.appendChild(document.createTextNode("unexpected exception"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(e.stack));
            exceptionCell.appendChild(exceptionText);
        }
    }
}

testClass = function(className, classTests) {
    var table = document.createElement("table");
    document.body.appendChild(table);
    var caption = document.createElement("caption");
    table.appendChild(caption);
    caption.appendChild(document.createTextNode(className));
    var tbody = document.createElement("tbody");
    table.appendChild(tbody);
    currentTestReportBody = tbody;
    classTests();
    currentTestReportBody = null;
}