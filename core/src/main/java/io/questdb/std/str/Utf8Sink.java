/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.std.str;

import io.questdb.std.Numbers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Utf8Sink extends CharSink<Utf8Sink> {

    default Utf8Sink escapeJsonStr(@NotNull CharSequence cs) {
        return escapeJsonStr(cs, 0, cs.length());
    }

    default Utf8Sink escapeJsonStr(@NotNull CharSequence cs, int lo, int hi) {
        int i = lo;
        while (i < hi) {
            char c = cs.charAt(i++);
            if (c < 32) {
                escapeJsonStrChar(c);
            } else if (c < 128) {
                switch (c) {
                    case '\"':
                    case '\\':
                        putAscii('\\');
                        // intentional fall through
                    default:
                        putAscii(c);
                        break;
                }
            } else {
                i = Utf8s.encodeUtf16Char(this, cs, hi, i, c);
            }
        }
        return this;
    }

    default Utf8Sink escapeJsonStr(Utf8Sequence utf8) {
        int i = 0;
        final int hi = utf8.size();
        while (i < hi) {
            char c = (char) utf8.byteAt(i++);
            if (c > 0 && c < 32) {
                escapeJsonStrChar(c);
            } else if (c > 0 && c < 128) {
                switch (c) {
                    case '\"':
                    case '\\':
                        putAscii('\\');
                        // intentional fall through
                    default:
                        putAscii(c);
                        break;
                }
            } else {
                put((byte) c);
            }
        }
        return this;
    }

    default void escapeJsonStrChar(char c) {
        switch (c) {
            case '\b':
                putAscii("\\b");
                break;
            case '\f':
                putAscii("\\f");
                break;
            case '\n':
                putAscii("\\n");
                break;
            case '\r':
                putAscii("\\r");
                break;
            case '\t':
                putAscii("\\t");
                break;
            default:
                putAscii("\\u00");
                put(c >> 4);
                putAscii(Numbers.hexDigits[c & 15]);
                break;
        }
    }

    @Override
    default int getEncoding() {
        return CharSinkEncoding.UTF8;
    }

    /**
     * Writes out bytes (8bits) to physical storage verbatim. This method is not commonly implemented on
     * {@link CharSink}. For writing ASCII (8-byte) characters please see {@link #putAscii(char)}
     *
     * @param b byte value
     * @return this sink for daisy-chaining
     */
    Utf8Sink put(byte b);

    /**
     * Encodes the given char sequence from UTF-16 to UTF-8 and writes it to the sink.
     */
    default Utf8Sink put(@Nullable CharSequence cs) {
        if (cs != null) {
            put(cs, 0, cs.length());
        }
        return this;
    }

    /**
     * Encodes the given UTF-16 char to UTF-8 and writes it to the sink.
     */
    default Utf8Sink put(char c) {
        if (c < 128) {
            putAscii(c);
        } else if (c < 2048) {
            put((byte) (192 | c >> 6)).put((byte) (128 | c & 63));
        } else if (Character.isSurrogate(c)) {
            putAscii('?');
        } else {
            put((byte) (224 | c >> 12)).put((byte) (128 | c >> 6 & 63)).put((byte) (128 | c & 63));
        }
        return this;
    }

    default Utf8Sink put(@Nullable DirectUtf8Sequence dus) {
        if (dus != null) {
            putUtf8(dus.lo(), dus.hi());
        }
        return this;
    }

    /**
     * Encodes the given segment of a char sequence from UTF-16 to UTF-8 and writes it to the sink.
     */
    default Utf8Sink put(@NotNull CharSequence cs, int lo, int hi) {
        int i = lo;
        while (i < hi) {
            char c = cs.charAt(i++);
            if (c < 128) {
                putAscii(c);
            } else {
                i = Utf8s.encodeUtf16Char(this, cs, hi, i, c);
            }
        }
        return this;
    }

    @Override
    default Utf8Sink putAscii(@Nullable CharSequence cs) {
        if (cs != null) {
            int l = cs.length();
            for (int i = 0; i < l; i++) {
                putAscii(cs.charAt(i));
            }
        }
        return this;
    }

    @Override
    default Utf8Sink putAscii(char c) {
        // todo: have function that accepts ascii byte
        return put((byte) c);
    }

    default Utf8Sink putQuote() {
        putAscii('"');
        return this;
    }

    default Utf8Sink putQuoted(@NotNull CharSequence cs) {
        putAscii('\"').put(cs).putAscii('\"');
        return this;
    }
}
