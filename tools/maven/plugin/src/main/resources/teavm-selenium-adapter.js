JUnitClient.run = function() {
    var handler = window.addEventListener("message", $rt_threadStarter(function() {
        var thread = $rt_nativeThread();
        var instance;
        var ptr = 0;
        var message;
        if (thread.isResuming()) {
            ptr = thread.pop();
            instance = thread.pop();
        }
        loop: while (true) { switch (ptr) {
        case 0:
            instance = new TestClass();
            ptr = 1;
        case 1:
            try {
                initInstance(instance);
            } catch (e) {
                message = {};
                JUnitClient.makeErrorMessage(message, e);
                break loop;
            }
            if (thread.isSuspending()) {
                thread.push(instance);
                thread.push(ptr);
                return;
            }
            ptr = 2;
        case 2:
            try {
                runTest(instance);
            } catch (e) {
                message = {};
                JUnitClient.makeErrorMessage(message, e);
                break loop;
            }
            if (thread.isSuspending()) {
                thread.push(instance);
                thread.push(ptr);
                return;
            }
            message = {};
            message.status = "ok";
            break loop;
        }}
        window.parent.postMessage(JSON.stringify(message), "*");
    }));
}