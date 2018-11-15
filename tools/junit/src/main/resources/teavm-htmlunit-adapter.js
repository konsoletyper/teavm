function runMain(callback) {
    main([], function(result) {
        var message = {};
        if (result instanceof Error) {
            makeErrorMessage(message, result);
        } else {
            message.status = "ok";
        }
        callback.complete(JSON.stringify(message));
    });

    function makeErrorMessage(message, e) {
        message.status = "exception";
        var stack = "";
        if (e.$javaException && e.$javaException.constructor.$meta) {
            stack = e.$javaException.constructor.$meta.name + ": ";
            stack += e.$javaException.getMessage() || "";
            stack += "\n";
        }
        message.stack = stack + e.stack;
    }
}