/**
 * Represents a class model that is alternative to {@link java.lang.reflection} package.
 * Model is suitable for representing classes that are not in class path. Also
 * it allows to disassemble method bodies into three-address code that is very
 * close to JVM bytecode (see {@link org.teavm.instructions}.
 *
 * <p>The entry point is some implementation of {@link MutableClassHolderSource} class.
 *
 */
package org.teavm.model;