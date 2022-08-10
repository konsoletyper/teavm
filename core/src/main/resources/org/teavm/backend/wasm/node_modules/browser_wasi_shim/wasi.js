import * as wasi from "./wasi_defs.js";

export default class WASI {
    args = [];
    env = {};
    fds = [];
    inst = null;

    start(instance) {
        this.inst = instance;
        instance.exports._start();
    }

    constructor(args, env, fds) {
        this.args = args;
        this.env = env;
        this.fds = fds;
        let self = this;
        this.wasiImport = {
            args_sizes_get(argc, argv_buf_size) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("args_sizes_get(", argc, ", ", argv_buf_size, ")");
                buffer.setUint32(argc, self.args.length, true);
                let buf_size = 0;
                for (let arg of self.args) {
                    buf_size += arg.length + 1;
                }
                buffer.setUint32(argv_buf_size, buf_size, true);
                console.log(buffer.getUint32(argc, true), buffer.getUint32(argv_buf_size, true));
                return 0;
            },
            args_get(argv, argv_buf) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("args_get(", argv, ", ", argv_buf, ")");
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

            environ_sizes_get(environ_count, environ_size) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("environ_sizes_get(", environ_count, ", ", environ_size, ")");
                buffer.setUint32(environ_count, self.env.length, true);
                let buf_size = 0;
                for (let environ of self.env) {
                    buf_size += environ.length + 1;
                }
                buffer.setUint32(environ_size, buf_size, true);
                console.log(buffer.getUint32(environ_count, true), buffer.getUint32(environ_size, true));
                return 0;
            },
            environ_get(environ, environ_buf) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("environ_get(", environ, ", ", environ_buf, ")");
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

            clock_res_get(id, res_ptr) {
                throw "unimplemented";
            },
            clock_time_get(id, precision, time) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("clock_time_get(", id, ", ", precision, ", ", time, ")");
                buffer.setBigUint64(time, 0n, true);
                return 0;
            },

            fd_advise(fd, offset, len, advice) {
                console.log("fd_advise(", fd, ", ", offset, ", ", len, ", ", advice, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_advise(offset, len, advice);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_allocate(fd, offset, len) {
                console.log("fd_allocate(", fd, ", ", offset, ", ", len, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_allocate(offset, len);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_close(fd) {
                console.log("fd_close(", fd, ")");
                if (self.fds[fd] != undefined) {
                    let ret = self.fds[fd].fd_close();
                    self.fds[fd] = undefined;
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_datasync(fd) {
                console.log("fd_datasync(", fd, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_datasync();
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_fdstat_get(fd, fdstat_ptr) {
                console.log("fd_fdstat_get(", fd, ", ", fdstat_ptr, ")");
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
            fd_fdstat_set_flags(fd, flags) {
                console.log("fd_fdstat_set_flags(", fd, ", ", flags, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_fdstat_set_flags(flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_fdstat_set_rights(fd, rights) {
                console.log("fd_fdstat_set_rights(", fd, ", ", rights, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_fdstat_set_rights(rights);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_filestat_get(fd, filestat_ptr) {
                console.log("fd_filestat_get(", fd, ", ", filestat_ptr, ")");
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
            fd_filestat_set_size(fd, size) {
                console.log("fd_filestat_set_size(", fd, ", ", size, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_filestat_set_size(size);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_filestat_set_times(fd, atim, mtim, fst_flags) {
                console.log("fd_filestat_set_times(", fd, ", ", atim, ", ", mtim, ", ", fst_flags, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_filestat_set_times(atim, mtim, fst_flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_pread(fd, iovs_ptr, iovs_len, offset, nread_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                //console.log("fd_pread(", fd, ", ", iovs_ptr, ", ", iovs_len, ", ", offset, ", ", nread_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Iovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nread } = self.fds[fd].fd_pread(buffer8, iovecs, offset);
                    buffer.setUint32(nread_ptr, nread, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_prestat_get(fd, buf_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("fd_prestat_get(", fd, ", ", buf_ptr, ")");
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
            fd_prestat_dir_name(fd, path_ptr, path_len) {
                // FIXME don't ignore path_len
                console.log("fd_prestat_dir_name(", fd, ", ", path_ptr, ", ", path_len, ")");
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
            fd_pwrite(fd, iovs_ptr, iovs_len, offset, nwritten_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                //console.log("fd_pwrite(", fd, ", ", iovs_ptr, ", ", iovs_len, ", ", offset, ", ", nwritten_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Ciovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nwritten } = self.fds[fd].fd_pwrite(buffer8, iovecs, offset);
                    buffer.setUint32(nwritten_ptr, nwritten, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_read(fd, iovs_ptr, iovs_len, nread_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                //console.log("fd_read(", fd, ", ", iovs_ptr, ", ", iovs_len, ", ", nread_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Iovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nread } = self.fds[fd].fd_read(buffer8, iovecs);
                    buffer.setUint32(nread_ptr, nread, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_readdir(fd, buf, buf_len, cookie, bufused_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("fd_readdir(", fd, ", ", buf, ", ", buf_len, ", ", cookie, ", ", bufused_ptr, ")");
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
            fd_renumber(fd, to) {
                console.log("fd_renumber(", fd, ", ", to, ")");
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
            fd_seek(fd, offset, whence, offset_out_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("fd_seek(", fd, ", ", offset, ", ", whence, ", ", offset_out_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let { ret, offset_out } = self.fds[fd].fd_seek(offset, whence);
                    buffer.setUint32(offset_out_ptr, offset_out, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_sync(fd) {
                console.log("fd_sync(", fd, ")");
                if (self.fds[fd] != undefined) {
                    return self.fds[fd].fd_sync();
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_tell(fd, offset_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                console.log("fd_tell(", fd, ", ", offset_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let { ret, offset } = self.fds[fd].fd_tell();
                    buffer.setUint32(offset_ptr, offset, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            fd_write(fd, iovs_ptr, iovs_len, nwritten_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                //console.log("fd_write(", fd, ", ", iovs_ptr, ", ", iovs_len, ", ", nwritten_ptr, ")");
                if (self.fds[fd] != undefined) {
                    let iovecs = wasi.Ciovec.read_bytes_array(buffer, iovs_ptr, iovs_len);
                    let { ret, nwritten } = self.fds[fd].fd_write(buffer8, iovecs);
                    buffer.setUint32(nwritten_ptr, nwritten, true);
                    return ret;
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_create_directory(fd, path_ptr, path_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("path_create_directory(", fd, ", ", path_ptr, ", ", path_len, ")");
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_create_directory(path);
                }
            },
            path_filestat_get(fd, flags, path_ptr, path_len, filestat_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log(
                    "path_filestat_get(", fd, ", ", flags, ", ", path_ptr, ", ", path_len, ", ",
                    filestat_ptr, ")"
                );
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
            path_filestat_set_times(fd, flags, path_ptr, path_len, atim, mtim, fst_flags) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log(
                    "path_filestat_set_times(", fd, ", ", flags, ", ", path_ptr, ", ", path_len,
                    ", ", atim, ", ", mtim, ", ", fst_flags, ")"
                );
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_filestat_set_times(flags, path, atim, mtim, fst_flags);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_link(old_fd, old_flags, old_path_ptr, old_path_len, new_fd, new_path_ptr, new_path_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log(
                    "path_link(", old_fd, ", ", old_flags, ", ", old_path_ptr, ", ", old_path_len,
                    ", ", new_fd, ", ", new_path_ptr, ", ", new_path_len, ")"
                );
                if (self.fds[old_fd] != undefined && self.fds[new_fs] != undefined) {
                    let old_path = new TextDecoder("utf-8").decode(buffer8.slice(old_path_ptr, old_path_ptr + old_path_len));
                    let new_path = new TextDecoder("utf-8").decode(buffer8.slice(new_path_ptr, new_path_ptr + new_path_len));
                    return self.fds[new_fd].path_link(old_fd, old_flags, old_path, new_path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_open(fd, dirflags, path_ptr, path_len, oflags, fs_rights_base, fs_rights_inheriting, fd_flags, opened_fd_ptr) {
                console.log(
                    "path_open(", fd, ", ", dirflags, ", ", path_ptr, ", ", path_len, ", ", oflags,
                    ", ", fs_rights_base, ", ", fs_rights_inheriting, ", ", fd_flags, ", ",
                    opened_fd_ptr, ")"
                );
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
            path_readlink(fd, path_ptr, path_len, buf_ptr, buf_len, nread_ptr) {
                let buffer = new DataView(self.inst.exports.memory.buffer);
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log(
                    "path_readlink(", fd, ", ", path_ptr, ", ", path_len, ", ", buf_ptr, ", ",
                    buf_len, ", ", nread_ptr, ")"
                );
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
            path_remove_directory(fd, path_ptr, path_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("path_remove_directory(", fd, ", ", path_ptr, ", ", path_len, ")");
                if (self.fds[fd] != undefined) {
                    let path = new TextDecoder("utf-8").decode(buffer8.slice(path_ptr, path_ptr + path_len));
                    return self.fds[fd].path_remove_directory(path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_rename(fd, old_path_ptr, old_path_len, new_fd, new_path_ptr, new_path_len) {
                throw "FIXME what is the best abstraction for this?";
            },
            path_symlink(old_path_ptr, old_path_len, fd, new_path_ptr, new_path_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log(
                    "path_symlink(", old_path_ptr, ", ", old_path_len, ", ", fd, ", ", new_path_ptr,
                    ", ", new_path_len, ")"
                );
                if (self.fds[fd] != undefined) {
                    let old_path = new TextDecoder("utf-8").decode(buffer8.slice(old_path_ptr, old_path_ptr + old_path_len));
                    let new_path = new TextDecoder("utf-8").decode(buffer8.slice(new_path_ptr, new_path_ptr + new_path_len));
                    return self.fds[new_fd].path_symlink(old_path, new_path);
                } else {
                    return wasi.ERRNO_BADF;
                }
            },
            path_unlink_file(fd, path_ptr, path_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("path_unlink_file(", fd, ", ", path_ptr, ", ", path_len, ")");
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
            proc_exit(exit_code) {
                throw "exit with exit code " + exit_code;
            },
            proc_raise(sig) {
                throw "raised signal " + sig;
            },
            sched_yield() {
                console.log("sched_yield()");
            },
            random_get(buf, buf_len) {
                let buffer8 = new Uint8Array(self.inst.exports.memory.buffer);
                console.log("random_get(", buf, ", ", buf_len, ")");
                for (let i = 0; i < buf_len; i++) {
                    buffer8[buf + i] = (Math.random() * 256) | 0;
                }
            },
            sock_recv(fd, ri_data, ri_flags) {
                throw "sockets not supported";
            },
            sock_send(fd, si_data, si_flags) {
                throw "sockets not supported";
            },
            sock_shutdown(fd, how) {
                throw "sockets not supported";
            }
        };
    }
}
