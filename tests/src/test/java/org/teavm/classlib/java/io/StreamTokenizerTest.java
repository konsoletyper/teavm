/*
 *  Copyright 2017 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class StreamTokenizerTest {
    private StreamTokenizer st;

    @Test
    @SuppressWarnings("deprecation")
    public void constructorLjava_io_InputStream() throws IOException {
        st = new StreamTokenizer(new StringReader("/comments\n d 8 'h'"));

        assertEquals(StreamTokenizer.TT_WORD, st.nextToken(), "the next token returned should be the letter d");
        assertEquals("d", st.sval, "the next token returned should be the letter d");

        assertEquals(StreamTokenizer.TT_NUMBER, st.nextToken(), "the next token returned should be the digit 8");
        assertEquals(8.0, st.nval, 0.0001, "the next token returned should be the digit 8");

        assertEquals(39, st.nextToken(), "the next token returned should be the quote character");
        assertEquals("h", st.sval, "the next token returned should be the quote character");
    }

    @Test
    public void constructorLjava_io_Reader() throws IOException {
        setTest("/testing\n d 8 'h' ");
        assertEquals(StreamTokenizer.TT_WORD, st.nextToken(),
                "the next token returned should be the letter d skipping the comments");
        assertEquals("d", st.sval, "the next token returned should be the letter d");

        assertEquals(StreamTokenizer.TT_NUMBER, st.nextToken(), "the next token returned should be the digit 8");
        assertEquals(8.0, st.nval, 0.001, "the next token returned should be the digit 8");

        assertEquals(39, st.nextToken(), "the next token returned should be the quote character");
        assertEquals("h", st.sval, "the next token returned should be the quote character");
    }

    @Test
    public void commentCharI() throws IOException {
        setTest("*comment \n / 8 'h' ");
        st.ordinaryChar('/');
        st.commentChar('*');
        assertEquals(47, st.nextToken(),
                "nextToken() did not return the character / skiping the comments starting with *");
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER && st.nval == 8.0,
                "the next token returned should be the digit 8");
        assertTrue(st.nextToken() == 39 && st.sval.equals("h"),
                "the next token returned should be the quote character");
    }

    @Test
    public void eolIsSignificantZ() throws IOException {
        setTest("d 8\n");
        // by default end of line characters are not significant
        assertTrue(st.nextToken() == StreamTokenizer.TT_WORD && st.sval.equals("d"), "nextToken did not return d");
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER && st.nval == 8.0, "nextToken did not return 8");
        assertTrue(st.nextToken() == StreamTokenizer.TT_EOF, "nextToken should be the end of file");
        setTest("d\n");
        st.eolIsSignificant(true);
        // end of line characters are significant
        assertTrue(st.nextToken() == StreamTokenizer.TT_WORD && st.sval.equals("d"), "nextToken did not return d");
        assertTrue(st.nextToken() == StreamTokenizer.TT_EOL, "nextToken is the end of line");
    }

    @Test
    public void lineno() throws IOException {
        setTest("d\n 8\n");
        assertEquals(1, st.lineno(), "the lineno should be 1");
        st.nextToken();
        st.nextToken();
        assertEquals(2, st.lineno(), "the lineno should be 2");
        st.nextToken();
        assertEquals(3, st.lineno(), "the next line no should be 3");
    }

    @Test
    public void lowerCaseModeZ() throws Exception {
        // SM.
        setTest("HELLOWORLD");
        st.lowerCaseMode(true);

        st.nextToken();
        assertEquals("helloworld", st.sval, "sval not converted to lowercase.");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void nextToken() throws IOException {
        // SM.
        setTest("\r\n/* fje fje 43.4 f \r\n f g */  456.459 \r\nHello  / \t\r\n \r\n \n \r \257 Hi \'Hello World\'");
        st.ordinaryChar('/');
        st.slashStarComments(true);
        st.nextToken();
        assertTrue(st.ttype == StreamTokenizer.TT_NUMBER, "Wrong Token type1: " + (char) st.ttype);
        st.nextToken();
        assertTrue(st.ttype == StreamTokenizer.TT_WORD, "Wrong Token type2: " + st.ttype);
        st.nextToken();
        assertTrue(st.ttype == '/', "Wrong Token type3: " + st.ttype);
        st.nextToken();
        assertTrue(st.ttype == StreamTokenizer.TT_WORD, "Wrong Token type4: " + st.ttype);
        st.nextToken();
        assertTrue(st.ttype == StreamTokenizer.TT_WORD, "Wrong Token type5: " + st.ttype);
        st.nextToken();
        assertTrue(st.ttype == '\'', "Wrong Token type6: " + st.ttype);
        assertTrue(st.sval.equals("Hello World"), "Wrong Token type7: " + st.ttype);
        st.nextToken();
        assertTrue(st.ttype == -1, "Wrong Token type8: " + st.ttype);

        StreamTokenizer s = new StreamTokenizer(new StringReader("hello\n\n\n"));
        s.eolIsSignificant(true);
        assertTrue(s.nextToken() == StreamTokenizer.TT_WORD && s.sval.equals("hello"), "Wrong token 1,1");
        assertTrue(s.nextToken() == '\n', "Wrong token 1,2");
        assertTrue(s.nextToken() == '\n', "Wrong token 1,3");
        assertTrue(s.nextToken() == '\n', "Wrong token 1,4");
        assertTrue(s.nextToken() == StreamTokenizer.TT_EOF, "Wrong token 1,5");
        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader("\n \r\n#"));
        tokenizer.ordinaryChar('\n'); // make \n ordinary
        tokenizer.eolIsSignificant(true);
        assertTrue(tokenizer.nextToken() == '\n', "Wrong token 2,1");
        assertTrue(tokenizer.nextToken() == '\n', "Wrong token 2,2");
        assertEquals('#', tokenizer.nextToken(), "Wrong token 2,3");
    }

    @Test
    public void ordinaryCharI() throws IOException {
        // SM.
        setTest("Ffjein 893");
        st.ordinaryChar('F');
        st.nextToken();
        assertTrue(st.ttype == 'F', "OrdinaryChar failed." + (char) st.ttype);
    }

    @Test
    public void ordinaryCharsII() throws IOException {
        setTest("azbc iof z 893");
        st.ordinaryChars('a', 'z');
        assertEquals('a', st.nextToken(), "OrdinaryChars failed.");
        assertEquals('z', st.nextToken(), "OrdinaryChars failed.");
    }

    @Test
    public void parseNumbers() throws IOException {
        // SM
        setTest("9.9 678");
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER, "Base behavior failed.");
        st.ordinaryChars('0', '9');
        assertEquals('6', st.nextToken(), "setOrdinary failed.");
        st.parseNumbers();
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER, "parseNumbers failed.");
    }

    @Test
    public void pushBack() throws IOException {
        // SM.
        setTest("Hello 897");
        st.nextToken();
        st.pushBack();
        assertTrue(st.nextToken() == StreamTokenizer.TT_WORD, "PushBack failed.");
    }

    @Test
    public void quoteCharI() throws IOException {
        // SM
        setTest("<Hello World<    HelloWorldH");
        st.quoteChar('<');
        assertEquals('<', st.nextToken(), "QuoteChar failed.");
        assertEquals("Hello World", st.sval, "QuoteChar failed.");
        st.quoteChar('H');
        st.nextToken();
        assertEquals("elloWorld", st.sval, "QuoteChar failed for word.");
    }

    @Test
    public void resetSyntax() throws IOException {
        // SM
        setTest("H 9\' ello World");
        st.resetSyntax();
        assertTrue(st.nextToken() == 'H', "resetSyntax failed1." + (char) st.ttype);
        assertTrue(st.nextToken() == ' ', "resetSyntax failed1." + (char) st.ttype);
        assertTrue(st.nextToken() == '9', "resetSyntax failed2." + (char) st.ttype);
        assertTrue(st.nextToken() == '\'', "resetSyntax failed3." + (char) st.ttype);
    }

    @Test
    public void slashSlashCommentsZ() throws IOException {
        // SM.
        setTest("// foo \r\n /fiji \r\n -456");
        st.ordinaryChar('/');
        st.slashSlashComments(true);
        assertEquals('/', st.nextToken(), "Test failed.");
        assertTrue(st.nextToken() == StreamTokenizer.TT_WORD, "Test failed.");
    }
    
    @Test
    public void slashSlashComments_withSSOpen() throws IOException {
        Reader reader = new StringReader("t // t t t");

        StreamTokenizer st = new StreamTokenizer(reader);
        st.slashSlashComments(true);

        assertEquals(StreamTokenizer.TT_WORD, st.nextToken());
        assertEquals(StreamTokenizer.TT_EOF, st.nextToken());
    }

    @Test
    public void slashSlashComments_withSSOpen_NoComment() throws IOException {
        Reader reader = new StringReader("// t");

        StreamTokenizer st = new StreamTokenizer(reader);
        st.slashSlashComments(true);
        st.ordinaryChar('/');

        assertEquals(StreamTokenizer.TT_EOF, st.nextToken());
    }
    
    @Test
    public void slashSlashComments_withSSClosed() throws IOException {
        Reader reader = new StringReader("// t");

        StreamTokenizer st = new StreamTokenizer(reader);
        st.slashSlashComments(false);
        st.ordinaryChar('/');

        assertEquals('/', st.nextToken());
        assertEquals('/', st.nextToken());
        assertEquals(StreamTokenizer.TT_WORD, st.nextToken());
    }
    
    @Test
    public void slashStarCommentsZ() throws IOException {
        setTest("/* foo \r\n /fiji \r\n*/ -456");
        st.ordinaryChar('/');
        st.slashStarComments(true);
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER, "Test failed.");
    }

    @Test
    public void slashStarComments_withSTOpen() throws IOException {
        Reader reader = new StringReader("t /* t */ t");

        StreamTokenizer st = new StreamTokenizer(reader);
        st.slashStarComments(true);

        assertEquals(StreamTokenizer.TT_WORD, st.nextToken());
        assertEquals(StreamTokenizer.TT_WORD, st.nextToken());
        assertEquals(StreamTokenizer.TT_EOF, st.nextToken());
    }

    @Test
    public void slashStarComments_withSTClosed() throws IOException {
        Reader reader = new StringReader("t /* t */ t");

        StreamTokenizer st = new StreamTokenizer(reader);
        st.slashStarComments(false);

        assertEquals(StreamTokenizer.TT_WORD, st.nextToken());
        assertEquals(StreamTokenizer.TT_EOF, st.nextToken());
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void test_toString() throws IOException {
        setTest("ABC Hello World");
        st.nextToken();
        assertEquals("Token[ABC], line 1", st.toString(), "toString failed.");

        // Regression test for HARMONY-4070
        byte[] data = new byte[] { (byte) '-' };
        StreamTokenizer tokenizer = new StreamTokenizer(new ByteArrayInputStream(data));
        tokenizer.nextToken();
        String result = tokenizer.toString();
        assertEquals("Token['-'], line 1", result);
    }

    @Test
    public void whitespaceCharsII() throws IOException {
        setTest("azbc iof z 893");
        st.whitespaceChars('a', 'z');
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER, "OrdinaryChar failed.");
    }

    @Test
    public void wordCharsII() throws IOException {
        setTest("A893 -9B87");
        st.wordChars('0', '9');
        assertTrue(st.nextToken() == StreamTokenizer.TT_WORD, "WordChar failed1.");
        assertEquals("A893", st.sval, "WordChar failed2.");
        assertTrue(st.nextToken() == StreamTokenizer.TT_NUMBER, "WordChar failed3.");
        st.nextToken();
        assertEquals("B87", st.sval, "WordChar failed4.");

        setTest("    Hello World");
        st.wordChars(' ', ' ');
        st.nextToken();
        assertEquals("Hello World", st.sval, "WordChars failed for whitespace.");

        setTest("    Hello World\r\n  \'Hello World\' Hello\' World");
        st.wordChars(' ', ' ');
        st.wordChars('\'', '\'');
        st.nextToken();
        assertTrue(st.sval.equals("Hello World"), "WordChars failed for whitespace: " + st.sval);
        st.nextToken();
        assertTrue(st.sval.equals("\'Hello World\' Hello\' World"), "WordChars failed for quote1: " + st.sval);
    }

    private void setTest(String s) {
        st = new StreamTokenizer(new StringReader(s));
    }
}
