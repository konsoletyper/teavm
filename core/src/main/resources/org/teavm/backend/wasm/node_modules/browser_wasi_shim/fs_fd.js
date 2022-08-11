// @flow

import * as wasi from "./wasi_defs.js";
import { File, Directory } from "./fs_core.js";
import { Fd } from "./fd.js";

export class OpenFile extends Fd {
    /*:: file: File*/;
    file_pos/*: number*/ = 0;

    constructor(file/*: File*/) {
        super();
        this.file = file;
    }

    fd_fdstat_get()/*: { ret: number, fdstat: wasi.Fdstat | null }*/ {
        return { ret: 0, fdstat: new wasi.Fdstat(wasi.FILETYPE_REGULAR_FILE, 0) };
    }

    fd_read(view8/*: Uint8Array*/, iovs/*: Array<wasi.Iovec>*/)/*: { ret: number, nread: number }*/ {
        let nread = 0;
        for (let iovec of iovs) {
            if (this.file_pos < this.file.data.byteLength) {
                let slice = this.file.data.slice(this.file_pos, this.file_pos + iovec.buf_len);
                view8.set(slice, iovec.buf);
                this.file_pos += slice.length;
                nread += slice.length;
            } else {
                break;
            }
        }
        return { ret: 0, nread };
    }

    fd_seek(offset/*: number*/, whence/*: number*/)/*: {ret: number, offset: number }*/ {
        let calculated_offset;
        switch (whence) {
            case wasi.WHENCE_SET:
                calculated_offset = offset;
                break;
            case wasi.WHENCE_CUR:
                calculated_offset = this.file_pos + offset;
                break;
            case wasi.WHENCE_END:
                calculated_offset = this.file.data.byteLength + offset;
                break;
            default:
                return { ret: wasi.ERRNO_INVAL, offset: 0 };
        }

        if (calculated_offset < 0) {
            return { ret: wasi.ERRNO_INVAL, offset: 0 };
        }

        this.file_pos = calculated_offset;
        return { ret: 0, offset: calculated_offset };
    }

    fd_write(view8/*: Uint8Array*/, iovs/*: Array<wasi.Ciovec>*/)/*: { ret: number, nwritten: number }*/ {
        let nwritten = 0;
        for (let iovec of iovs) {
            let buffer = view8.slice(iovec.buf, iovec.buf + iovec.buf_len);
            if (this.file_pos + buffer.byteLength > this.file.size) {
                let old = this.file.data;
                this.file.data = new Uint8Array(this.file_pos + buffer.byteLength);
                this.file.data.set(old);
            }
            this.file.data.set(
                buffer.slice(
                    0,
                    this.file.size - this.file_pos,
                ), this.file_pos
            );
            this.file_pos += buffer.byteLength;
            nwritten += iovec.buf_len;
        }
        return { ret: 0, nwritten };
    }

    fd_filestat_get()/*: { ret: number, stat: wasi.Filestat }*/ {
        return { ret: 0, stat: this.file.stat() };
    }
}

export class OpenDirectory extends Fd {
    /*:: dir: Directory*/;

    constructor(dir/*: Directory */) {
        super();
        this.dir = dir;
    }

    fd_fdstat_get()/*: { ret: number, fdstat: wasi.Fdstat | null }*/ {
        return { ret: 0, fdstat: new wasi.Fdstat(wasi.FILETYPE_DIRECTORY, 0) };
    }

    fd_readdir_single(cookie/*: BigInt*/)/*: { ret: number, dirent: wasi.Dirent | null }*/ {
        console.log(cookie, Object.keys(this.dir.contents).slice(Number(cookie)));
        if (cookie >= BigInt(Object.keys(this.dir.contents).length)) {
            return { ret: 0, dirent: null };
        }

        let name = Object.keys(this.dir.contents)[Number(cookie)];
        let entry = this.dir.contents[name];
        let encoded_name = new TextEncoder("utf-8").encode(name);

        return { ret: 0, dirent: new wasi.Dirent(cookie + 1n, name, entry.stat().filetype) };
    }

    path_filestat_get(flags/*: number*/, path/*: string*/)/*: { ret: number, filestat: wasi.Filestat | null }*/ {
        let entry = this.dir.get_entry_for_path(path);
        if (entry == null) {
            return { ret: -1, filestat: null };
        }
        return { ret: 0, filestat: entry.stat() };
    }

    path_open(
        dirflags/*: number*/,
        path/*: string*/,
        oflags/*: number*/,
        fs_rights_base/*: BigInt*/,
        fs_rights_inheriting/*: BigInt*/,
        fd_flags/*: number*/
    )/*: { ret: number, fd_obj: Fd | null }*/ {
        let entry = this.dir.get_entry_for_path(path);
        if (entry == null) {
            if ((oflags & wasi.OFLAGS_CREAT) == wasi.OFLAGS_CREAT) {
                entry = this.dir.create_entry_for_path(path);
            } else {
                return { ret: -1, fd_obj: null };
            }
        } else if ((oflags & wasi.OFLAGS_EXCL) == wasi.OFLAGS_EXCL) {
            return { ret: -1, fd_obj: null };
        }
        if ((oflags & wasi.OFLAGS_DIRECTORY) == wasi.OFLAGS_DIRECTORY && entry.stat().filetype != wasi.FILETYPE_DIRECTORY) {
            return { ret: -1, fd_obj: null };
        }
        if ((oflags & wasi.OFLAGS_TRUNC) == wasi.OFLAGS_TRUNC) {
            entry.truncate();
        }
        // FIXME handle this more elegantly
        if (entry instanceof File) {
            return { ret: 0, fd_obj: new OpenFile(entry) };
        } else if (entry instanceof Directory) {
            return { ret: 0, fd_obj: new OpenDirectory(entry) };
        } else {
            throw "dir entry neither file nor dir";
        }
    }
}

export class PreopenDirectory extends OpenDirectory {
    /*:: prestat_name: Uint8Array*/;

    constructor(name/*: string*/, contents/*: { [string]: File | Directory }*/) {
        super(new Directory(contents));
        this.prestat_name = new TextEncoder("utf-8").encode(name);
    }

    fd_prestat_get()/*: { ret: number, prestat: wasi.Prestat }*/ {
        return {
            ret: 0, prestat: wasi.Prestat.dir(this.prestat_name.length)
        };
    }

    fd_prestat_dir_name()/*: { ret: number, prestat_dir_name: Uint8Array }*/ {
        return {
            ret: 0, prestat_dir_name: this.prestat_name
        };
    }
}
