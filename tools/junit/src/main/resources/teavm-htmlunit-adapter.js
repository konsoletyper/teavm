var $rt_decodeStack;

function runMain(argument, stackDecoder, callback) {
    $rt_decodeStack = stackDecoder;
    main(argument !== null ? [argument] : [], function(result) {
        var message = {};
        if (result instanceof Error) {
            makeErrorMessage(message, result);
        } else {
            message.status = "ok";
        }
        callback.complete(message);
    });

    function makeErrorMessage(message, e) {
        message.status = "exception";
        var je = main.javaException(e);
        if (je) {
            message.className = je.constructor.name;
            message.message = je.getMessage();
        } else {
            message.className = Object.getPrototypeOf(e).name;
            message.message = e.message;
        }
        message.exception = e;
        message.stack = e.stack;
    }
}