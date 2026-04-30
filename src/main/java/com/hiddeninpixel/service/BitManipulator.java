// BitManipulator handles all the raw binary operations in this project.
// This is a helper/utility class. It does NOT know anything about images or steganography.
// Its only job is to deal with bits and bytes at the lowest level.
// Keeping this code separate means the algorithm classes stay cleaner and easier to read.
//
// Quick binary recap:
// Every color channel (Red, Green, Blue) is a number from 0 to 255.
// In binary, 255 looks like: 11111111 (eight 1s).
// The "Least Significant Bit" (LSB) is the rightmost bit.
// Changing it moves the color value by just 1, which is completely invisible to the eye.
package com.hiddeninpixel.service;

public class BitManipulator {

    // Takes one pixel's packed RGB integer and splits it into three separate channel values.
    // Java stores a pixel as one big int like 0x00RRGGBB.
    // We use bit-shifting and masking to peel out each channel.
    //
    // Example: pixel = 0x00FF8040
    // Red   = (0x00FF8040 >> 16) & 0xFF = 0xFF = 255
    // Green = (0x00FF8040 >> 8)  & 0xFF = 0x80 = 128
    // Blue  = (0x00FF8040)       & 0xFF = 0x40 = 64
    public int[] splitRGB(int rgb) {
        return new int[] {
            (rgb >> 16) & 0xFF,  // shift 16 bits right to get Red, then mask to 8 bits
            (rgb >> 8) & 0xFF,   // shift 8 bits right to get Green, then mask to 8 bits
            rgb & 0xFF           // no shift needed for Blue, just mask to 8 bits
        };
    }

    // Takes a three-element array [R, G, B] and packs them back into one integer.
    // This is the reverse of splitRGB.
    // We shift Red left 16 bits and Green left 8 bits so they sit in the right positions.
    public int mergeRGB(int[] channels) {
        return (channels[0] << 16)  // move Red to the top 8 bits
             | (channels[1] << 8)   // move Green to the middle 8 bits
             | channels[2];         // Blue stays in the bottom 8 bits
    }

    // Embeds a single bit (0 or 1) into the least significant bit of a channel value.
    // Step 1: Clear the LSB of channelValue using 0xFE (binary: 11111110).
    //         The AND with 0xFE forces the last bit to 0, leaving all other bits alone.
    // Step 2: OR in our new bit at the last position.
    // Example: channelValue = 200 (11001000), bit = 1
    //          (200 & 0xFE) = 200 (11001000), then | 1 = 201 (11001001)
    public int embedLSB(int channelValue, int bit) {
        return (channelValue & 0xFE) | (bit & 1);
    }

    // Reads the least significant bit from a channel value.
    // AND with 1 (binary: 00000001) masks everything away except the very last bit.
    // Result is either 0 or 1.
    public int extractLSB(int channelValue) {
        return channelValue & 1;
    }
}
