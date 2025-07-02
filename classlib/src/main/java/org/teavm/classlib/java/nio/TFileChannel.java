package org.teavm.classlib.java.nio.channels;

public abstract class TFileChannel implements TChannel {
  /**
   * Returns the current size of this channel's file.
   */
  public abstract long size();
}
