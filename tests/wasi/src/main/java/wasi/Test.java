package wasi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.teavm.interop.Export;

public class Test {
    public static void main(String[] args) {
        for (int i = 0; i < args.length; ++i) {
            System.out.print(args[i]);
            if (i < args.length - 1) {
                System.out.print(" ");
            }
        }
    }

    private static void doThrow(String message) throws Exception {
        throw new Exception(message);
    }

    @Export(name = "env")
    public static void env() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String var1 = string.substring(0, index);
        String var2 = string.substring(index + 1);
        System.out.print(System.getenv(var1));
        System.out.print(System.getenv(var2));
    }

    @Export(name = "catch")
    public static void doCatch() {
        try {
            doThrow(readString());
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }
    }

    @Export(name = "epoch")
    public static void epoch() {
        System.out.print(System.currentTimeMillis());
    }

    @Export(name = "stdin")
    public static void stdin() throws IOException {
        byte[] buffer = new byte[256];
        int count = 0;
        while ((count = System.in.read(buffer, 0, buffer.length)) != -1) {
            System.out.write(buffer, 0, count);
        }
    }

    private static String readString(InputStream in) throws IOException {
        byte[] buffer = new byte[256];
        int offset = 0;
        int count = 0;
        while ((count = in.read(buffer, offset, buffer.length - offset)) != -1) {
            offset += count;

            if (offset >= buffer.length) {
                throw new IOException("buffer overflow");
            }
        }

        return new String(buffer, 0, offset, StandardCharsets.UTF_8).trim();
    }

    private static String readString() throws IOException {
        return readString(System.in);
    }

    @Export(name = "mkdirs")
    public static void mkdirs() throws IOException {
        if (!new File(readString()).mkdirs()) {
            throw new AssertionError();
        }
    }

    @Export(name = "create")
    public static void create() throws IOException {
        if (!new File(readString()).createNewFile()) {
            throw new AssertionError();
        }
    }

    @Export(name = "create_already_exists")
    public static void createAlreadyExists() throws IOException {
        if (new File(readString()).createNewFile()) {
            throw new AssertionError();
        }
    }

    @Export(name = "write")
    public static void write() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String path = string.substring(0, index);
        String message = string.substring(index + 1);
        try (PrintStream out = new PrintStream(new FileOutputStream(path))) {
            out.print(message);
        }
    }

    @Export(name = "read")
    public static void read() throws IOException {
        try (InputStream in = new FileInputStream(readString())) {
            System.out.println(readString(in));
        }
    }

    @Export(name = "seek")
    public static void seek() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String path = string.substring(0, index);
        long position = Long.parseLong(string.substring(index + 1));
        RandomAccessFile file = new RandomAccessFile(path, "r");
        long length = file.length();
        file.seek(position);
        if (position != file.getFilePointer()) {
            throw new AssertionError();
        }
        byte[] buffer = new byte[(int) (length - position)];
        file.readFully(buffer);
        System.out.write(buffer);
    }

    @Export(name = "resize")
    public static void resize() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String path = string.substring(0, index);
        long length = Long.parseLong(string.substring(index + 1));
        new RandomAccessFile(path, "rw").setLength(length);
    }

    @Export(name = "length")
    public static void length() throws IOException {
        System.out.println(new File(readString()).length());
    }

    @Export(name = "rename")
    public static void rename() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String oldPath = string.substring(0, index);
        String newPath = string.substring(index + 1);
        if (!new File(oldPath).renameTo(new File(newPath))) {
            throw new AssertionError();
        }
    }

    @Export(name = "delete")
    public static void delete() throws IOException {
        if (!new File(readString()).delete()) {
            throw new AssertionError();
        }
    }

    @Export(name = "list")
    public static void list() throws IOException {
        File[] files = new File(readString()).listFiles();
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; ++i) {
            names[i] = files[i].getName();
        }
        Arrays.sort(names);

        for (int i = 0; i < names.length; ++i) {
            System.out.print(names[i]);
            if (i < names.length - 1) {
                System.out.print(" ");
            }
        }
    }

    @Export(name = "mtime")
    public static void mtime() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String path = string.substring(0, index);
        long time = Long.parseLong(string.substring(index + 1));
        if (!new File(path).setLastModified(time)) {
            throw new AssertionError();
        }
        if (time != new File(path).lastModified()) {
            throw new AssertionError();
        }
    }

    @Export(name = "bad_mkdirs")
    public static void badMkdirs() throws IOException {
        if (new File(readString()).mkdirs()) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }

    @Export(name = "bad_create")
    public static void badCreate() throws IOException {
        try {
            new File(readString()).createNewFile();
            throw new AssertionError();
        } catch (IOException e) {
            System.out.print("SUCCESS");
        }
    }

    @Export(name = "bad_write")
    public static void badWrite() throws IOException {
        try {
            new FileOutputStream(readString());
            throw new AssertionError();
        } catch (FileNotFoundException e) {
            System.out.print("SUCCESS");
        }
    }

    @Export(name = "bad_read")
    public static void badRead() throws IOException {
        try {
            new FileInputStream(readString());
            throw new AssertionError();
        } catch (FileNotFoundException e) {
            System.out.print("SUCCESS");
        }
    }

    @Export(name = "bad_random_access")
    public static void badRandomAccess() throws IOException {
        try {
            new RandomAccessFile(readString(), "r");
            throw new AssertionError();
        } catch (FileNotFoundException e) {
            System.out.print("SUCCESS");
        }
    }

    @Export(name = "bad_length")
    public static void badLength() throws IOException {
        if (new File(readString()).length() != 0) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }

    @Export(name = "bad_rename")
    public static void badRename() throws IOException {
        String string = readString();
        int index = string.indexOf(':');
        String oldPath = string.substring(0, index);
        String newPath = string.substring(index + 1);
        if (new File(oldPath).renameTo(new File(newPath))) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }

    @Export(name = "bad_delete")
    public static void badDelete() throws IOException {
        if (new File(readString()).delete()) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }

    @Export(name = "bad_list")
    public static void badList() throws IOException {
        if (new File(readString()).listFiles() != null) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }

    @Export(name = "bad_mtime")
    public static void badMtime() throws IOException {
        if (new File(readString()).lastModified() != 0) {
            throw new AssertionError();
        }
        System.out.print("SUCCESS");
    }
}
