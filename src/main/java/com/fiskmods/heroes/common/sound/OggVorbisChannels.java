package com.fiskmods.heroes.common.sound;

/** Reads only the channel count from the first Ogg/Vorbis identification packet. */
public final class OggVorbisChannels
{
    private OggVorbisChannels() {}

    /** Returns zero for a truncated, non-Vorbis or unsupported header. */
    public static int read(byte[] header)
    {
        if (header.length < 28 || header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S'
                || header[4] != 0 || (header[5] & 2) == 0) return 0;
        int segments = header[26] & 255;
        int offset = 27 + segments;
        if (segments == 0 || (header[27] & 255) < 30 || offset + 30 > header.length) return 0;
        if (header[offset] != 1 || header[offset + 1] != 'v' || header[offset + 2] != 'o'
                || header[offset + 3] != 'r' || header[offset + 4] != 'b'
                || header[offset + 5] != 'i' || header[offset + 6] != 's'
                || header[offset + 7] != 0 || header[offset + 8] != 0
                || header[offset + 9] != 0 || header[offset + 10] != 0) return 0;
        return header[offset + 11] & 255;
    }
}
