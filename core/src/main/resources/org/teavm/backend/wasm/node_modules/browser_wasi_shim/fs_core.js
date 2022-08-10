import * as wasi from "./wasi_defs.js";

export class File {
    data = null;

    constructor(data) {
        console.log(data);
        this.data = new Uint8Array(data);
    }

    get size() {
        return this.data.byteLength;
    }

    stat() {
        return new wasi.Filestat(wasi.FILETYPE_REGULAR_FILE, this.size);
    }

    truncate() {
        this.data = new Uint8Array([]);
    }
}

export class Directory {
    contents = null;

    constructor(contents) {
        this.contents = contents;
    }

    stat() {
        return new wasi.Filestat(wasi.FILETYPE_DIRECTORY, 0);
    }

    get_entry_for_path(path) {
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

    create_entry_for_path(path) {
        let entry = this;
        let components = path.split("/").filter((component) => component != "/");
        for (let i in components) {
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
