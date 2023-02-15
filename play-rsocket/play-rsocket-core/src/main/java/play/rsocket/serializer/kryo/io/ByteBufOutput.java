/* Copyright (c) 2008-2022, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package play.rsocket.serializer.kryo.io;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.KryoBufferOverflowException;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * An {@link Output} that uses a ByteBuffer rather than a byte[].
 * <p>
 * Note that the byte[] {@link #getBuffer() buffer} is not used. Code taking an Output and expecting the byte[] to be used may not
 * work correctly.
 *
 * @author Roman Levenstein <romixlev@gmail.com>
 * @author Nathan Sweet
 */
public class ByteBufOutput extends Output {

    protected ByteBuf byteBuf;

    /**
     * Creates an uninitialized Output, {@link #setBuffer(ByteBuf)} must be called before the Output is used.
     */
    public ByteBufOutput() {
    }

    /**
     * Creates a new Output for writing to a direct {@link ByteBuffer}.
     *
     * @param bufferSize The size of the buffer. An exception is thrown if more bytes than this are written and {@link #flush()}
     *                   does not empty the buffer.
     */
    public ByteBufOutput(int bufferSize) {
        this(bufferSize, bufferSize);
    }

    /**
     * Creates a new Output for writing to a direct ByteBuffer.
     *
     * @param bufferSize    The initial size of the buffer.
     * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
     *                      maxBufferSize and an exception is thrown. Can be -1 for no maximum.
     */
    public ByteBufOutput(int bufferSize, int maxBufferSize) {
        if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
        this.capacity = bufferSize;
        this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(bufferSize);
    }

    /**
     * Creates a new Output for writing to a ByteBuffer.
     */
    public ByteBufOutput(ByteBuf buffer) {
        setBuffer(buffer);
    }

    /**
     * Creates a new Output for writing to a ByteBuffer.
     *
     * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
     *                      maxBufferSize and an exception is thrown. Can be -1 for no maximum.
     */
    public ByteBufOutput(ByteBuf buffer, int maxBufferSize) {
        setBuffer(buffer, maxBufferSize);
    }

    /**
     * @see Output#Output(OutputStream)
     */
    public ByteBufOutput(OutputStream outputStream) {
        this(4096, 4096);
        if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
        this.outputStream = outputStream;
    }

    /**
     * @see Output#Output(OutputStream, int)
     */
    public ByteBufOutput(OutputStream outputStream, int bufferSize) {
        this(bufferSize, bufferSize);
        if (outputStream == null) throw new IllegalArgumentException("outputStream cannot be null.");
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
     *
     * @see #getByteBuf()
     * @deprecated
     */
    @Deprecated
    public byte[] getBuffer() {
        throw new UnsupportedOperationException("This buffer does not used a byte[], see #getByteBuffer().");
    }

    /**
     * Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
     *
     * @see #getByteBuf()
     * @deprecated
     */
    @Deprecated
    public void setBuffer(byte[] buffer) {
        throw new UnsupportedOperationException("This buffer does not used a byte[], see #setByteBuffer(ByteBuffer).");
    }

    /**
     * Throws {@link UnsupportedOperationException} because this output uses a ByteBuffer, not a byte[].
     *
     * @see #getByteBuf()
     * @deprecated
     */
    @Deprecated
    public void setBuffer(byte[] buffer, int maxBufferSize) {
        throw new UnsupportedOperationException("This buffer does not used a byte[], see #setByteBuffer(ByteBuffer).");
    }

    /**
     * Allocates a new direct ByteBuffer with the specified bytes and sets it as the new buffer.
     *
     * @see #setBuffer(ByteBuf)
     */
    public void setBuffer(byte[] bytes, int offset, int count) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buffer.writeBytes(bytes, offset, count);
        setBufferPosition(buffer, 0);
        setBufferLimit(buffer, bytes.length);
        setBuffer(buffer);
    }

    /**
     * Sets a new buffer to write to. The max size is the buffer's length.
     *
     * @see #setBuffer(ByteBuf, int)
     */
    public void setBuffer(ByteBuf buffer) {
        setBuffer(buffer, buffer.capacity());
    }

    /**
     * Sets a new buffer to write to. The bytes are not copied, the old buffer is discarded and the new buffer used in its place.
     * The position and capacity are set to match the specified buffer. The total is reset. The
     * {@link #setOutputStream(OutputStream) OutputStream} is set to null.
     *
     * @param maxBufferSize If {@link #flush()} does not empty the buffer, the buffer is doubled as needed until it exceeds
     *                      maxBufferSize and an exception is thrown. Can be -1 for no maximum.
     */
    public void setBuffer(ByteBuf buffer, int maxBufferSize) {
        if (buffer == null) throw new IllegalArgumentException("buffer cannot be null.");
        if (maxBufferSize < -1) throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
        this.byteBuf = buffer;
        this.maxCapacity = maxBufferSize == -1 ? Util.maxArraySize : maxBufferSize;
        capacity = buffer.capacity();
        position = buffer.writerIndex();
        total = 0;
        outputStream = null;
    }

    /**
     * Returns the buffer. The bytes between zero and {@link #position()} are the data that has been written.
     */
    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    public byte[] toBytes() {
        byte[] newBuffer = new byte[position];
        setBufferPosition(byteBuf, 0);
        byteBuf.getBytes(byteBuf.readerIndex(), newBuffer, 0, position);
        return newBuffer;
    }

    public void setPosition(int position) {
        this.position = position;
        setBufferPosition(byteBuf, position);
    }

    public void reset() {
        super.reset();
        setBufferPosition(byteBuf, 0);
    }

    private int getBufferPosition(ByteBuf buffer) {
        return buffer.writerIndex();
    }

    private void setBufferPosition(ByteBuf buffer, int newPosition) {
        buffer.writerIndex(newPosition);
    }

    private void setBufferLimit(ByteBuf buffer, int length) {
        buffer.capacity(length);
    }

    protected boolean require(int required) throws KryoException {
        if (capacity - position >= required) return false;
        flush();
        if (capacity - position >= required) return true;
        if (required > maxCapacity - position) {
            if (required > maxCapacity)
                throw new KryoBufferOverflowException("Buffer overflow. Max capacity: " + maxCapacity + ", required: " + required);
            throw new KryoBufferOverflowException("Buffer overflow. Available: " + (maxCapacity - position) + ", required: " + required);
        }
        if (capacity == 0) capacity = 16;
        do {
            capacity = Math.min(capacity * 2, maxCapacity);
        } while (capacity - position < required);
        ByteBuf newBuffer = !byteBuf.isDirect() ? ByteBufAllocator.DEFAULT.buffer(capacity) : ByteBufAllocator.DEFAULT.directBuffer(capacity);
        setBufferPosition(byteBuf, 0);
        setBufferLimit(byteBuf, position);
        newBuffer.writeBytes(byteBuf, byteBuf.readableBytes());
        byteBuf = newBuffer;
        return true;
    }

    // OutputStream:

    public void flush() throws KryoException {
        if (outputStream == null) return;
        try {
            byte[] tmp = new byte[position];
            setBufferPosition(byteBuf, 0);
            byteBuf.readBytes(tmp);
            setBufferPosition(byteBuf, 0);
            outputStream.write(tmp, 0, position);
        } catch (IOException ex) {
            throw new KryoException(ex);
        }
        total += position;
        position = 0;
    }

    public void close() throws KryoException {
        flush();
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void write(int value) throws KryoException {
        if (position == capacity) require(1);
        byteBuf.writeByte((byte) value);
        position++;
    }

    public void write(byte[] bytes) throws KryoException {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
        writeBytes(bytes, 0, bytes.length);
    }

    public void write(byte[] bytes, int offset, int length) throws KryoException {
        writeBytes(bytes, offset, length);
    }

    // byte:

    public void writeByte(byte value) throws KryoException {
        if (position == capacity) require(1);
        byteBuf.writeByte(value);
        position++;
    }

    public void writeByte(int value) throws KryoException {
        if (position == capacity) require(1);
        byteBuf.writeByte((byte) value);
        position++;
    }

    public void writeBytes(byte[] bytes) throws KryoException {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bytes, int offset, int count) throws KryoException {
        if (bytes == null) throw new IllegalArgumentException("bytes cannot be null.");
        int copyCount = Math.min(capacity - position, count);
        while (true) {
            byteBuf.writeBytes(bytes, offset, copyCount);
            position += copyCount;
            count -= copyCount;
            if (count == 0) return;
            offset += copyCount;
            copyCount = Math.min(capacity, count);
            require(copyCount);
        }
    }

    // int:

    public void writeInt(int value) throws KryoException {
        require(4);
        position += 4;
        ByteBuf byteBuffer = this.byteBuf;
        byteBuffer.writeByte((byte) value);
        byteBuffer.writeByte((byte) (value >> 8));
        byteBuffer.writeByte((byte) (value >> 16));
        byteBuffer.writeByte((byte) (value >> 24));
    }

    public int writeVarInt(int value, boolean optimizePositive) throws KryoException {
        if (!optimizePositive) value = (value << 1) ^ (value >> 31);
        if (value >>> 7 == 0) {
            if (position == capacity) require(1);
            position++;
            byteBuf.writeByte((byte) value);
            return 1;
        }
        if (value >>> 14 == 0) {
            require(2);
            position += 2;
            byteBuf.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuf.writeByte((byte) (value >>> 7));
            return 2;
        }
        if (value >>> 21 == 0) {
            require(3);
            position += 3;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14));
            return 3;
        }
        if (value >>> 28 == 0) {
            require(4);
            position += 4;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21));
            return 4;
        }
        require(5);
        position += 5;
        ByteBuf byteBuffer = this.byteBuf;
        byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
        byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 28));
        return 5;
    }

    public int writeVarIntFlag(boolean flag, int value, boolean optimizePositive) throws KryoException {
        if (!optimizePositive) value = (value << 1) ^ (value >> 31);
        int first = (value & 0x3F) | (flag ? 0x80 : 0); // Mask first 6 bits, bit 8 is the flag.
        if (value >>> 6 == 0) {
            if (position == capacity) require(1);
            byteBuf.writeByte((byte) first);
            position++;
            return 1;
        }
        if (value >>> 13 == 0) {
            require(2);
            position += 2;
            byteBuf.writeByte((byte) (first | 0x40)); // Set bit 7.
            byteBuf.writeByte((byte) (value >>> 6));
            return 2;
        }
        if (value >>> 20 == 0) {
            require(3);
            position += 3;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) (first | 0x40)); // Set bit 7.
            byteBuffer.writeByte((byte) ((value >>> 6) | 0x80)); // Set bit 8.
            byteBuffer.writeByte((byte) (value >>> 13));
            return 3;
        }
        if (value >>> 27 == 0) {
            require(4);
            position += 4;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) (first | 0x40)); // Set bit 7.
            byteBuffer.writeByte((byte) ((value >>> 6) | 0x80)); // Set bit 8.
            byteBuffer.writeByte((byte) ((value >>> 13) | 0x80)); // Set bit 8.
            byteBuffer.writeByte((byte) (value >>> 20));
            return 4;
        }
        require(5);
        position += 5;
        ByteBuf byteBuffer = this.byteBuf;
        byteBuffer.writeByte((byte) (first | 0x40)); // Set bit 7.
        byteBuffer.writeByte((byte) ((value >>> 6) | 0x80)); // Set bit 8.
        byteBuffer.writeByte((byte) ((value >>> 13) | 0x80)); // Set bit 8.
        byteBuffer.writeByte((byte) ((value >>> 20) | 0x80)); // Set bit 8.
        byteBuffer.writeByte((byte) (value >>> 27));
        return 5;
    }

    // long:

    public void writeLong(long value) throws KryoException {
        require(8);
        position += 8;
        ByteBuf byteBuffer = this.byteBuf;
        byteBuffer.writeByte((byte) value);
        byteBuffer.writeByte((byte) (value >>> 8));
        byteBuffer.writeByte((byte) (value >>> 16));
        byteBuffer.writeByte((byte) (value >>> 24));
        byteBuffer.writeByte((byte) (value >>> 32));
        byteBuffer.writeByte((byte) (value >>> 40));
        byteBuffer.writeByte((byte) (value >>> 48));
        byteBuffer.writeByte((byte) (value >>> 56));
    }

    public int writeVarLong(long value, boolean optimizePositive) throws KryoException {
        if (!optimizePositive) value = (value << 1) ^ (value >> 63);
        if (value >>> 7 == 0) {
            if (position == capacity) require(1);
            position++;
            byteBuf.writeByte((byte) value);
            return 1;
        }
        if (value >>> 14 == 0) {
            require(2);
            position += 2;
            byteBuf.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuf.writeByte((byte) (value >>> 7));
            return 2;
        }
        if (value >>> 21 == 0) {
            require(3);
            position += 3;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14));
            return 3;
        }
        if (value >>> 28 == 0) {
            require(4);
            position += 4;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21));
            return 4;
        }
        if (value >>> 35 == 0) {
            require(5);
            position += 5;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 28));
            return 5;
        }
        if (value >>> 42 == 0) {
            require(6);
            position += 6;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 28 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 35));
            return 6;
        }
        if (value >>> 49 == 0) {
            require(7);
            position += 7;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 28 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 35 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 42));
            return 7;
        }
        if (value >>> 56 == 0) {
            require(8);
            position += 8;
            ByteBuf byteBuffer = this.byteBuf;
            byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
            byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 28 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 35 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 42 | 0x80));
            byteBuffer.writeByte((byte) (value >>> 49));
            return 8;
        }
        require(9);
        position += 9;
        ByteBuf byteBuffer = this.byteBuf;
        byteBuffer.writeByte((byte) ((value & 0x7F) | 0x80));
        byteBuffer.writeByte((byte) (value >>> 7 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 14 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 21 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 28 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 35 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 42 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 49 | 0x80));
        byteBuffer.writeByte((byte) (value >>> 56));
        return 9;
    }

    // float:

    public void writeFloat(float value) throws KryoException {
        require(4);
        ByteBuf byteBuffer = this.byteBuf;
        position += 4;
        int intValue = Float.floatToIntBits(value);
        byteBuffer.writeByte((byte) intValue);
        byteBuffer.writeByte((byte) (intValue >> 8));
        byteBuffer.writeByte((byte) (intValue >> 16));
        byteBuffer.writeByte((byte) (intValue >> 24));
    }

    // double:

    public void writeDouble(double value) throws KryoException {
        require(8);
        position += 8;
        ByteBuf byteBuffer = this.byteBuf;
        long longValue = Double.doubleToLongBits(value);
        byteBuffer.writeByte((byte) longValue);
        byteBuffer.writeByte((byte) (longValue >>> 8));
        byteBuffer.writeByte((byte) (longValue >>> 16));
        byteBuffer.writeByte((byte) (longValue >>> 24));
        byteBuffer.writeByte((byte) (longValue >>> 32));
        byteBuffer.writeByte((byte) (longValue >>> 40));
        byteBuffer.writeByte((byte) (longValue >>> 48));
        byteBuffer.writeByte((byte) (longValue >>> 56));
    }

    // short:

    public void writeShort(int value) throws KryoException {
        require(2);
        position += 2;
        byteBuf.writeByte((byte) value);
        byteBuf.writeByte((byte) (value >>> 8));
    }

    // char:

    public void writeChar(char value) throws KryoException {
        require(2);
        position += 2;
        byteBuf.writeByte((byte) value);
        byteBuf.writeByte((byte) (value >>> 8));
    }

    // boolean:

    public void writeBoolean(boolean value) throws KryoException {
        if (position == capacity) require(1);
        byteBuf.writeByte((byte) (value ? 1 : 0));
        position++;
    }

    // String:

    public void writeString(String value) throws KryoException {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }
        int charCount = value.length();
        if (charCount == 0) {
            writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
            return;
        }
        // Detect ASCII.
        outer:
        if (charCount > 1 && charCount <= 32) {
            for (int i = 0; i < charCount; i++)
                if (value.charAt(i) > 127) break outer;
            if (capacity - position < charCount) writeAscii_slow(value, charCount);
            else {
                for (int i = 0, n = value.length(); i < n; ++i)
                    byteBuf.writeByte((byte) value.charAt(i));
                position += charCount;
            }
            byteBuf.setByte(position - 1, (byte) (byteBuf.getByte(position - 1) | 0x80));
            return;
        }
        writeVarIntFlag(true, charCount + 1, true);
        int charIndex = 0;
        if (capacity - position >= charCount) {
            // Try to write 7 bit chars.
            ByteBuf byteBuffer = this.byteBuf;
            while (true) {
                int c = value.charAt(charIndex);
                if (c > 127) break;
                byteBuffer.writeByte((byte) c);
                charIndex++;
                if (charIndex == charCount) {
                    position = getBufferPosition(byteBuffer);
                    return;
                }
            }
            position = getBufferPosition(byteBuffer);
        }
        if (charIndex < charCount) writeUtf8_slow(value, charCount, charIndex);
    }

    public void writeAscii(String value) throws KryoException {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }
        int charCount = value.length();
        if (charCount == 0) {
            writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
            return;
        }
        if (capacity - position < charCount) writeAscii_slow(value, charCount);
        else {
            ByteBuf byteBuffer = this.byteBuf;
            for (int i = 0, n = value.length(); i < n; ++i)
                byteBuffer.writeByte((byte) value.charAt(i));
            position += charCount;
        }
        byteBuf.setByte(position - 1, (byte) (byteBuf.getByte(position - 1) | 0x80)); // Bit 8 means end of ASCII.
    }

    private void writeUtf8_slow(String value, int charCount, int charIndex) {
        for (; charIndex < charCount; charIndex++) {
            if (position == capacity) require(Math.min(capacity, charCount - charIndex));
            position++;
            int c = value.charAt(charIndex);
            if (c <= 0x007F) byteBuf.writeByte((byte) c);
            else if (c > 0x07FF) {
                byteBuf.writeByte((byte) (0xE0 | c >> 12 & 0x0F));
                require(2);
                position += 2;
                byteBuf.writeByte((byte) (0x80 | c >> 6 & 0x3F));
                byteBuf.writeByte((byte) (0x80 | c & 0x3F));
            } else {
                byteBuf.writeByte((byte) (0xC0 | c >> 6 & 0x1F));
                if (position == capacity) require(1);
                position++;
                byteBuf.writeByte((byte) (0x80 | c & 0x3F));
            }
        }
    }

    private void writeAscii_slow(String value, int charCount) throws KryoException {
        ByteBuf buffer = this.byteBuf;
        int charIndex = 0;
        int charsToWrite = Math.min(charCount, capacity - position);
        while (charIndex < charCount) {
            ByteBufUtil.writeUtf8(buffer, value, charIndex, charIndex + charsToWrite);
            charIndex += charsToWrite;
            position += charsToWrite;
            charsToWrite = Math.min(charCount - charIndex, capacity);
            if (require(charsToWrite)) buffer = this.byteBuf;
        }
    }

    // Primitive arrays:

    public void writeInts(int[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 2) {
            require(count << 2);
            ByteBuf byteBuffer = this.byteBuf;
            for (int n = offset + count; offset < n; offset++) {
                int value = array[offset];
                byteBuffer.writeByte((byte) value);
                byteBuffer.writeByte((byte) (value >> 8));
                byteBuffer.writeByte((byte) (value >> 16));
                byteBuffer.writeByte((byte) (value >> 24));
            }
            position = getBufferPosition(byteBuffer);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeInt(array[offset]);
        }
    }

    public void writeLongs(long[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 3) {
            require(count << 3);
            ByteBuf byteBuffer = this.byteBuf;
            for (int n = offset + count; offset < n; offset++) {
                long value = array[offset];
                byteBuffer.writeByte((byte) value);
                byteBuffer.writeByte((byte) (value >>> 8));
                byteBuffer.writeByte((byte) (value >>> 16));
                byteBuffer.writeByte((byte) (value >>> 24));
                byteBuffer.writeByte((byte) (value >>> 32));
                byteBuffer.writeByte((byte) (value >>> 40));
                byteBuffer.writeByte((byte) (value >>> 48));
                byteBuffer.writeByte((byte) (value >>> 56));
            }
            position = getBufferPosition(byteBuffer);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeLong(array[offset]);
        }
    }

    public void writeFloats(float[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 2) {
            require(count << 2);
            ByteBuf byteBuffer = this.byteBuf;
            for (int n = offset + count; offset < n; offset++) {
                int value = Float.floatToIntBits(array[offset]);
                byteBuffer.writeByte((byte) value);
                byteBuffer.writeByte((byte) (value >> 8));
                byteBuffer.writeByte((byte) (value >> 16));
                byteBuffer.writeByte((byte) (value >> 24));
            }
            position = getBufferPosition(byteBuffer);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeFloat(array[offset]);
        }
    }

    public void writeDoubles(double[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 3) {
            require(count << 3);
            ByteBuf byteBuffer = this.byteBuf;
            for (int n = offset + count; offset < n; offset++) {
                long value = Double.doubleToLongBits(array[offset]);
                byteBuffer.writeByte((byte) value);
                byteBuffer.writeByte((byte) (value >>> 8));
                byteBuffer.writeByte((byte) (value >>> 16));
                byteBuffer.writeByte((byte) (value >>> 24));
                byteBuffer.writeByte((byte) (value >>> 32));
                byteBuffer.writeByte((byte) (value >>> 40));
                byteBuffer.writeByte((byte) (value >>> 48));
                byteBuffer.writeByte((byte) (value >>> 56));
            }
            position = getBufferPosition(byteBuffer);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeDouble(array[offset]);
        }
    }

    public void writeShorts(short[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 1) {
            require(count << 1);
            for (int n = offset + count; offset < n; offset++) {
                int value = array[offset];
                byteBuf.writeByte((byte) value);
                byteBuf.writeByte((byte) (value >>> 8));
            }
            position = getBufferPosition(byteBuf);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeShort(array[offset]);
        }
    }

    public void writeChars(char[] array, int offset, int count) throws KryoException {
        if (capacity >= count << 1) {
            require(count << 1);
            for (int n = offset + count; offset < n; offset++) {
                int value = array[offset];
                byteBuf.writeByte((byte) value);
                byteBuf.writeByte((byte) (value >>> 8));
            }
            position = getBufferPosition(byteBuf);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeChar(array[offset]);
        }
    }

    public void writeBooleans(boolean[] array, int offset, int count) throws KryoException {
        if (capacity >= count) {
            require(count);
            for (int n = offset + count; offset < n; offset++)
                byteBuf.writeByte(array[offset] ? (byte) 1 : 0);
            position = getBufferPosition(byteBuf);
        } else {
            for (int n = offset + count; offset < n; offset++)
                writeBoolean(array[offset]);
        }
    }
}
