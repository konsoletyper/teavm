// @flow

/*::declare function strace<T>(imports: T, no_trace: Array<String>): T*/

export function strace(imports, no_trace) {
    return new Proxy(imports, {
        get(target, prop, receiver) {
            let res = Reflect.get(...arguments);
            if (no_trace.includes(prop)) {
                return res;
            }
            return function(...args) {
                console.log(prop, "(", ...args, ")");
                return Reflect.apply(res, receiver, args);
            }
        }
    });
}
