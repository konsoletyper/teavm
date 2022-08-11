// @flow

import * as wasi from "./wasi_defs.js";

export class File {
    /*:: data: Uint8Array*/;

    constructor(data/*: ArrayBuffer | Uint8Array*/) {
        console.log(data);
        this.data = new Uint8Array(data);
    }

    get size()/*: number*/ {
        return this.data.byteLength;
    }

    stat()/*: wasi.Filestat*/ {
        return new wasi.Filestat(wasi.FILETYPE_REGULAR_FILE, this.size);
    }

    truncate() {
        this.data = new Uint8Array([]);
    }
}

export class Directory {
    /*:: contents: { [string]: File | Directory } */;

    constructor(contents/*: { [string]: File | Directory }*/) {
        this.contents = contents;
    }

    stat()/*: wasi.Filestat*/ {
        return new wasi.Filestat(wasi.FILETYPE_DIRECTORY, 0);
    }

    get_entry_for_path(path/*: string*/)/*: File | Directory | null*/ {
        let entry = this;
        for (let component of path.split("/")) {
            if (component == "") break;
            if (entry.contents[component] != undefined) {
                entry = entry.contents[component];
            } else {
                console.log(component);
                return null;
            }
        }
        return entry;
    }

    create_entry_for_path(path/*: string*/)/*: File | Directory*/ {
        // FIXME fix type errors
        let entry = this;
        let components/*: Array<string>*/ = path.split("/").filter((component) => component != "/");
        for (let i = 0; i < components.length; i++) {
            let component = components[i];
            if (entry.contents[component] != undefined) {
                entry = entry.contents[component];
            } else {
                console.log("create", component);
                if (i == components.length - 1) {
                    entry.contents[component] = new File(new ArrayBuffer(0));
                } else {
                    entry.contents[component] = new Directory({});
                }
                entry = entry.contents[component];
            }
        }
        return entry;
    }
}
