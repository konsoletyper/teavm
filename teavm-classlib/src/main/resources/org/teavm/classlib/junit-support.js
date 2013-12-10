currentTestReportBody = null;
currentTimeSpent = 0;
totalTimeSpent = 0;
currentMethodCount = 0;
currentStatusCell = null;
currentExceptionCell = null;
currentTimeCell = null;
currentStartTime = 0;
currentExpectedExceptions = [];
currentFrame = null;

window.addEventListener("message", function(event) {
    endTime = new Date().getTime();
    var message = JSON.parse(event.data);
    if (message.status == "ok") {
        if (currentExpectedExceptions.length > 0) {
            currentStatusCell.appendChild(document.createTextNode("expected exception not thrown"));
            currentStatusCell.style.color = 'yellow';
        } else {
            currentStatusCell.appendChild(document.createTextNode("ok"));
            currentStatusCell.style.color = 'green';
        }
    } else if (message.status == "exception") {
        if (isExpectedException(e)) {
            currentStatusCell.appendChild(document.createTextNode("ok"));
            currentStatusCell.style.color = 'green';
        } else {
            currentStatusCell.appendChild(document.createTextNode("unexpected exception"));
            var exceptionText = document.createElement("pre");
            exceptionText.appendChild(document.createTextNode(e.stack));
            currentExceptionCell.appendChild(exceptionText);
            currentStatusCell.style.color = 'red';
        }
    }
    ++currentMethodCount;
    var timeSpent = (endTime - currentStartTime) / 1000;
    currentTimeSpent += timeSpent;
    currentTimeCell.appendChild(document.createTextNode(timeSpent.toFixed(3)));
    document.body.removeChild(currentFrame);
}, false);

runTestCase = function(methodName, path, expectedExceptions) {
    var row = document.createElement("tr");
    currentTestReportBody.appendChild(row);
    var nameCell = document.createElement("td");
    row.appendChild(nameCell);
    nameCell.appendChild(document.createTextNode(methodName));
    currentStatusCell = document.createElement("td");
    row.appendChild(currentStatusCell);
    currentExceptionCell = document.createElement("td");
    row.appendChild(currentExceptionCell);
    currentTimeCell = document.createElement("td");
    row.appendChild(currentTimeCell);
    currentStartTime = new Date().getTime();
    currentExpectedExceptions = expectedExceptions;
    var frame = document.createElement("iframe");
    cirremtFrame = frame;
    document.body.appendChild(frame);
    var frameDoc = frame.contentWindow.document;
    var frameScript = frameDoc.createElement("script");
    frameScript.src = path;
    frameDoc.body.appendChild(frameScript);
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

isExpectedException = function(e) {
    if (e.javaException !== undefined) {
        for (var i = 0; i < currentExpectedExceptions.length; ++i) {
            if (currentExpectedExceptions[i] === e.javaException) {
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