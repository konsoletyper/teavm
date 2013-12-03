currentTestReportBody = null;
currentTimeSpent = 0;
totalTimeSpent = 0;
currentMethodCount = 0;

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
    var timeCell = document.createElement("td");
    row.appendChild(timeCell);
    var startTime = new Date().getTime();
    var endTime;
    try {
        instance[realMethodName]();
        endTime = new Date().getTime();
        if (expectedExceptions.length > 0) {
            statusCell.appendChild(document.createTextNode("expected exception not thrown"));
            statusCell.style.color = 'yellow';
        } else {
            statusCell.appendChild(document.createTextNode("ok"));
            statusCell.style.color = 'green';
        }
    } catch (e) {
        endTime = new Date().getTime();
        if (isExpectedException(e, expectedExceptions)) {
            statusCell.appendChild(document.createTextNode("ok"));
            statusCell.style.color = 'green';
        } else {
            statusCell.appendChild(document.createTextNode("unexpected exception"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(e.stack));
            exceptionCell.appendChild(exceptionText);
            statusCell.style.color = 'red';
        }
    }
    ++currentMethodCount;
    var timeSpent = (endTime - startTime) / 1000;
    currentTimeSpent += timeSpent;
    timeCell.appendChild(document.createTextNode(timeSpent.toFixed(3)));
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
    currentTimeSpent = 0;
    currentMethodCount = 0;
    var table = document.createElement("table");
    document.body.appendChild(table);
    var caption = document.createElement("caption");
    table.appendChild(caption);
    caption.appendChild(document.createTextNode(className));

    var head = document.createElement("thead");
    table.appendChild(head);
    var headRow = document.createElement("tr");
    head.appendChild(headRow);
    var headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Method"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Result"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Exception"));
    headCell = document.createElement("th");
    headRow.appendChild(headCell);
    headCell.appendChild(document.createTextNode("Time spent, s"));

    var tbody = document.createElement("tbody");
    table.appendChild(tbody);
    currentTestReportBody = tbody;
    classTests();

    var foot = document.createElement("tfoot");
    table.appendChild(foot);
    var footRow = document.createElement("tr");
    foot.appendChild(footRow);
    var footName = document.createElement("td");
    footRow.appendChild(footName);
    footName.appendChild(document.createTextNode("---"));
    var footMethods = document.createElement("td");
    footRow.appendChild(footMethods);
    footMethods.appendChild(document.createTextNode(currentMethodCount));
    var footSpace = document.createElement("td");
    footRow.appendChild(footSpace);
    footSpace.appendChild(document.createTextNode("---"));
    var footTime = document.createElement("td");
    footRow.appendChild(footTime);
    footTime.appendChild(document.createTextNode(currentTimeSpent.toFixed(3)));
    totalTimeSpent += currentTimeSpent;

    currentTestReportBody = null;
}