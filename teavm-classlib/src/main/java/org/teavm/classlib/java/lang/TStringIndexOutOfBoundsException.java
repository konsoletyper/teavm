package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TStringIndexOutOfBoundsException extends TIndexOutOfBoundsException {
    private static final long serialVersionUID = 6706349858694463085L;

    public TStringIndexOutOfBoundsException() {
        super();
    }

    public TStringIndexOutOfBoundsException(TString message) {
        super(message);
    }

    public TStringIndexOutOfBoundsException(int index) {
        super(new TStringBuilder().append(TString.wrap("String index out of bounds: ")).append(index).toString0());
    }
}
