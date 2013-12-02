currentTestReportBody = null;

runTestCase = function(instance, methodName, realMethodName, expectedExceptions) {
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
        if (expectedExceptions.length > 0) {
            statusCell.appendChild(document.createTextNode("expected exception not thrown"));
        } else {
            statusCell.appendChild(document.createTextNode("ok"));
        }
    } catch (e) {
        if (isExpectedException(e, expectedExceptions)) {
            statusCell.appendChild(document.createTextNode("ok"));
        } else {
            statusCell.appendChild(document.createTextNode("unexpected exception"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(e.stack));
            exceptionCell.appendChild(exceptionText);
        }
    }
}

isExpectedException = function(e, expectedExceptions) {
    if (e.$javaException !== undefined) {
        for (var i = 0; i < expectedExceptions.length; ++i) {
            if (expectedExceptions[i] === e.$javaException.$class) {
                return true;
            }
        }
    }
    return false;
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