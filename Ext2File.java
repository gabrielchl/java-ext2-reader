public class Ext2File {
    /**
     * Reads at most length bytes starting at byte offset startByte from start
     * of file. Byte 0 is the first byte in the file.
     * startByte must be such that, 0 ≤ startByte < file.size or an exception
     * should be raised.
     * If there are fewer than length bytes remaining these will be read and a
     * smaller number of bytes than requested will be returned.
     *
     * @param   startByte   Byte offset from start of file
     * @param   length      Length of file
     * @return  Byte array of requested file
     */
    public byte[] read(long startByte, long length) {

    }

    /**
     * Reads at most length bytes starting at current position in the file.
     * If the current position is set beyond the end of the file, and exception
     * should be raised.
     * If there are fewer than length bytes remaining these will be read and a
     * smaller number of bytes than requested will be returned.
     *
     * @param   length      Length of file
     * @return  Byte array of requested file
     */
    public byte[] read(long length) {

    }

    /**
     * Move to byte position in file.
     * Setting position to 0L will move to the start of the file. Note, it is
     * legal to seek beyond the end of the file; if writing were supported,
     * this is how holes are created.
     *
     * @param   position    Positoin to move to in file
     */
    public void seek(long position) {

    }

    /**
     * Returns current position in file, i.e. the byte offset from the start of
     * the file. The file position will be zero when the file is first opened
     * and will advance by the number of bytes read with every call to one of
     * the read( ) routines.
     *
     * @return  Current position in file
     */
    public long position() {

    }

    /**
     * Returns size of file as specified in filesystem.
     *
     * @return  Size of file
     */
    public long size() {

    }
}
