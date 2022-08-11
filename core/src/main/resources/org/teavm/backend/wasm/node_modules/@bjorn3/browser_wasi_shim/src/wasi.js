// @flow

import * as wasi from "./wasi_defs.js";
import { Fd } from "./fd.js";

export default class WASI {
    args/*: Array<string>*/ = [];
    env/*: Array<string>*/ = [];
    fds/*: Array<Fd>*/ = [];
    inst/*: { +exports: { memory: WebAssembly$Memory } }*/;
    wasiImport/*: { [string]: (...args: Array<any>) => mixed }*/;

    start(instance/*: { exports: { memory: WebAssembly$Memory, _start: () => mixed } }*/) {
        this.inst = instance;
        instance.exports._start();
    }

    constructor(args/*: Array<string>*/, env/*: Array<string>*/, fds/*: Array<Fd>*/) {
        this.args = args;
        this.env = env;
        this.fds = fds;
        let self = this;
        this.wasiImport = {
            args_sizes_get(argc/*: number*/, argv_buf_size/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                buffer.setUint32(argc, self.args.length, true);
                let buf_size = 0;
                for (let arg of self.args) {
                    buf_size += arg.length + 1;
                }
                buffer.setUint32(argv_buf_size, buf_size, true);
                console.log(buffer.getUint32(argc, true), buffer.getUint32(argv_buf_size, true));
                return 0;
            },
            args_get(argv/*: number*/, argv_buf/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                let orig_argv_buf = argv_buf;
                for (let i = 0; i < self.args.length; i++) {
                    buffer.setUint32(argv, argv_buf, true);
                    argv += 4;
                    let arg = new TextEncoder("utf-8").encode(self.args[i]);
                    buffer8.set(arg, argv_buf);
                    buffer.setUint8(argv_buf + arg.length, 0);
                    argv_buf += arg.length + 1;
                }
                console.log(new TextDecoder("utf-8").decode(buffer8.slice(orig_argv_buf, argv_buf)));
                return 0;
            },

            environ_sizes_get(environ_count/*: number*/, environ_size/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                buffer.setUint32(environ_count, self.env.length, true);
                let buf_size = 0;
                for (let environ of self.env) {
                    buf_size += environ.length + 1;
                }
                buffer.setUint32(environ_size, buf_size, true);
                console.log(buffer.getUint32(environ_count, true), buffer.getUint32(environ_size, true));
                return 0;
            },
            environ_get(environ/*: number*/, environ_buf/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                let orig_environ_buf = environ_buf;
                for (let i = 0; i < env.length; i++) {
                    buffer.setUint32(environ, environ_buf, true);
                    environ += 4;
                    let e = new TextEncoder("utf-8").encode(env[i]);
                    buffer8.set(e, environ_buf);
                    buffer.setUint8(environ_buf + e.length, 0);
                    environ_buf += e.length + 1;
                }
                console.log(new TextDecoder("utf-8").decode(buffer8.slice(orig_environ_buf, environ_buf)));
                return 0;
            },

            clock_res_get(id/*: number*/, res_ptr/*: number*/)/*: number*/ {
                throw "unimplemented";
            },
            clock_time_get(id/*: number*/, precision/*: BigInt*/, time/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                if (id === wasi.CLOCKID_REALTIME) {
                    buffer.setBigUint64(time, BigInt(new Date().getTime()) * 1000000n, true);
                } else {
                    // TODO
                    buffer.setBigUint64(time, 0n, true);
                }
                return 0;
            },

            fd_advise(fd/*: number*/, offset/*: BigInt*/, len/*: BigInt*/, advice/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_advise(offset, len, advice);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_allocate(fd/*: number*/, offset/*: BigInt*/, len/*: BigInt*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_allocate(offset, len);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_close(fd/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    let ret = self.fds[fd].fd_close();
                    self.fds[fd] = undefined;
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_datasync(fd/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_datasync();
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_fdstat_get(fd/*: number*/, fdstat_ptr/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    let { ret, fdstat } = self.fds[fd].fd_fdstat_get();
                    if (fdstat != null) {
                        fdstat.write_bytes(new DataView(self.inst.exports.memory.buffer), fdstat_ptr);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_fdstat_set_flags(fd/*: number*/, flags/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_fdstat_set_flags(flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_fdstat_set_rights(fd/*: number*/, fs_rights_base/*: BigInt*/, fs_rights_inheriting/*: BigInt*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_fdstat_set_rights(fs_rights_base, fs_rights_inheriting);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_filestat_get(fd/*: number*/, filestat_ptr/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    let { ret, filestat } = self.fds[fd].fd_filestat_get();
                    if (filestat != null) {
                        filestat.write_bytes(new DataView(self.inst.exports.memory.buffer), filestat_ptr);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_filestat_set_size(fd/*: number*/, size/*: BigInt*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_filestat_set_size(size);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_filestat_set_times(fd/*: number*/, atim/*: BigInt*/, mtim/*: BigInt*/, fst_flags/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_filestat_set_times(atim, mtim, fst_flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_pread(fd/*: number*/, iovs_ptr/*: number*/, iovs_len/*: number*/, offset/*: BigInt*/, nread_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Iovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nread } = self.fds[fd].fd_pread(buffer8, iovecs, offset);
                    buffer.setUint32(nread_ptr, nread, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_prestat_get(fd/*: number*/, buf_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let { ret, prestat } = self.fds[fd].fd_prestat_get();
                    if (prestat != null) {
                        prestat.write_bytes(buffer, buf_ptr);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_prestat_dir_name(fd/*: number*/, path_ptr/*: number*/, path_len/*: number*/)/*: number*/ {
                // FIXME don't ignore path_len
                if (self.fds[fd] != undefined) {
                    let { ret, prestat_dir_name } = self.fds[fd].fd_prestat_dir_name();
                    if (prestat_dir_name != null) {
                        let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                        buffer8.set(prestat_dir_name, path_ptr);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_pwrite(fd/*: number*/, iovs_ptr/*: number*/, iovs_len/*: number*/, offset/*: number*/, nwritten_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Ciovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nwritten } = self.fds[fd].fd_pwrite(buffer8, iovecs, offset);
                    buffer.setUint32(nwritten_ptr, nwritten, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_read(fd/*: number*/, iovs_ptr/*: number*/, iovs_len/*: number*/, nread_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Iovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nread } = self.fds[fd].fd_read(buffer8, iovecs);
                    buffer.setUint32(nread_ptr, nread, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_readdir(fd/*: number*/, buf/*: number*/, buf_len/*: number*/, cookie/*: BigInt*/, bufused_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let bufused = 0;

                    while (true) {
                        let { ret, dirent } = self.fds[fd].fd_readdir_single(cookie);
                        if (ret != 0) {
                            buffer.setUint32(bufused_ptr, bufused, true);
                            return ret;
                        }
                        if (dirent == null) {
                            break;
                        }
                        let offset = dirent.length();

                        if ((buf_len - bufused) < offset) {
                            break;
                        }

                        dirent.write_bytes(buffer, buffer8, buf);
                        buf += offset;
                        bufused += offset;
                        cookie = dirent.d_next;
                    }

                    buffer.setUint32(bufused_ptr, bufused, true);
                    return 0;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_renumber(fd/*: number*/, to/*: number*/) {
                if (self.fds[fd] != undefined && self.fds[to] != undefined) {
                    let ret = self.fds[to].fd_close();
                    if (ret != 0) {
                        return ret;
                    }
                    self.fds[to] = self.fds[fd];
                    self.fds[fd] = undefined;
                    return 0;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_seek(fd/*: number*/, offset/*: number*/, whence/*: number*/, offset_out_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let { ret, offset_out } = self.fds[fd].fd_seek(offset, whence);
                    buffer.setUint32(offset_out_ptr, offset_out, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_sync(fd/*: number*/)/*: number*/ {
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_sync();
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_tell(fd/*: number*/, offset_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let { ret, offset } = self.fds[fd].fd_tell();
                    buffer.setUint32(offset_ptr, offset, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_write(fd/*: number*/, iovs_ptr/*: number*/, iovs_len/*: number*/, nwritten_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Ciovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nwritten } = self.fds[fd].fd_write(buffer8, iovecs);
                    buffer.setUint32(nwritten_ptr, nwritten, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_create_directory(fd/*: number*/, path_ptr/*: number*/, path_len/*: number*/)/*: number*/ {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_create_directory(path);
                }
            },
            path_filestat_get(fd/*: number*/, flags/*: number*/, path_ptr/*: number*/, path_len/*: number*/, filestat_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    let { ret, filestat } = self.fds[fd].path_filestat_get(flags, path);
                    if (filestat != null) {
                        filestat.write_bytes(buffer, filestat_ptr);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_filestat_set_times(fd/*: number*/, flags/*: number*/, path_ptr/*: number*/, path_len/*: number*/, atim, mtim, fst_flags) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_filestat_set_times(flags, path, atim, mtim, fst_flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_link(old_fd/*: number*/, old_flags, old_path_ptr/*: number*/, old_path_len/*: number*/, new_fd/*: number*/, new_path_ptr/*: number*/, new_path_len/*: number*/)/*: number*/ {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[old_fd] != undefined && self.fds[new_fd] != undefined) {
                    let old_path = new TextDecoder("utf-8").decode(buffer8.slice(old_path_ptr, old_path_ptr + old_path_len));
                    let new_path = new TextDecoder("utf-8").decode(buffer8.slice(new_path_ptr, new_path_ptr + new_path_len));
                    return self.fds[new_fd].path_link(old_fd, old_flags, old_path, new_path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_open(fd/*: number*/, dirflags, path_ptr/*: number*/, path_len/*: number*/, oflags, fs_rights_base, fs_rights_inheriting, fd_flags, opened_fd_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    console.log(path);
                    let { ret, fd_obj } = self.fds[fd].path_open(dirflags, path, oflags, fs_rights_base, fs_rights_inheriting, fd_flags);
                    if (ret != 0) {
                        return ret;
                    }
                    // FIXME use first free fd
                    self.fds.push(fd_obj);
                    let opened_fd = self.fds.length - 1;
                    buffer.setUint32(opened_fd_ptr, opened_fd, true);
                    return 0;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_readlink(fd/*: number*/, path_ptr/*: number*/, path_len/*: number*/, buf_ptr/*: number*/, buf_len/*: number*/, nread_ptr/*: number*/)/*: number*/ {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    console.log(path);
                    let { ret, data } = self.fds[fd].path_readlink(path);
                    if (data != null) {
                        if (data.length > buf_len) {
                            buffer.setUint32(nread_ptr, 0, true);
                            return wasi.ERRNO_BADF;
                        }
                        buffer8.set(data, buf_ptr);
                        buffer.setUint32(nread_ptr, data.length, true);
                    }
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_remove_directory(fd/*: number*/, path_ptr/*: number*/, path_len/*: number*/)/*: number*/ {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_remove_directory(path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_rename(fd/*: number*/, old_path_ptr/*: number*/, old_path_len/*: number*/, new_fd/*: number*/, new_path_ptr/*: number*/, new_path_len/*: number*/)/*: number*/ {
                throw "FIXME what is the best abstraction for this?";
            },
            path_symlink(old_path_ptr/*: number*/, old_path_len/*: number*/, fd/*: number*/, new_path_ptr/*: number*/, new_path_len/*: number*/)/*: number*/ {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let old_path = new TextDecoder("utf-8").decode(buffer8.slice(old_path_ptr, old_path_ptr + old_path_len));
                    let new_path = new TextDecoder("utf-8").decode(buffer8.slice(new_path_ptr, new_path_ptr + new_path_len));
                    return self.fds[fd].path_symlink(old_path, new_path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_unlink_file(fd/*: number*/, path_ptr/*: number*/, path_len/*: number*/)/*: number*/ {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_unlink_file(path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            poll_oneoff(in_, out, nsubscriptions) {
                throw "async io not supported";
            },
            proc_exit(exit_code/*: number*/) {
                throw "exit with exit code " + exit_code;
            },
            proc_raise(sig/*: number*/) {
                throw "raised signal " + sig;
            },
            sched_yield() {},
            random_get(buf/*: number*/, buf_len/*: number*/) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                for (let i = 0; i < buf_len; i++) {
                    buffer8[buf + i] = (Math.random() * 256) | 0;
                }
            },
            sock_recv(fd/*: number*/, ri_data, ri_flags) {
                throw "sockets not supported";
            },
            sock_send(fd/*: number*/, si_data, si_flags) {
                throw "sockets not supported";
            },
            sock_shutdown(fd/*: number*/, how) {
                throw "sockets not supported";
            }
        };
    }
}
