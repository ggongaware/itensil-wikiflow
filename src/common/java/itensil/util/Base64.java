/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.util;

import itensil.io.FixedByteArrayOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Base64 encoding and decoding.
 * 
 * 
 * Based on ostermillerutils which was released under GNU
 * Copyright (C) 2001-2002 Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Java+Utilities
 * 
 * Lot's removed... added ALT support
 *  
 * ALT Support:
 * Takes the base 64 encoding and replaces '+' with '$'
 * and '/' with '_' so the resulted string can be used to
 * generate database table names, LDAP names, filenames,
 * and java/c variable names.
 * 
 * Source to be included with distributions.
 * 
 */
public class Base64 {

	/**
	 * Symbol that represents the end of an input stream
	 */
	private static final int END_OF_INPUT = -1;

	/**
	 * A character that is not a valid base 64 character.
	 */
	private static final int NON_BASE_64 = -1;

	/**
	 * A character that is not a valid base 64 character.
	 */
	private static final int NON_BASE_64_WHITESPACE = -2;

	/**
	 * A character that is not a valid base 64 character.
	 */
	private static final int NON_BASE_64_PADDING = -3;
	
	protected static final byte[] base64Chars = {
		'A','B','C','D','E','F','G','H',
		'I','J','K','L','M','N','O','P',
		'Q','R','S','T','U','V','W','X',
		'Y','Z','a','b','c','d','e','f',
		'g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v',
		'w','x','y','z','0','1','2','3',
		'4','5','6','7','8','9','+','/',
	};
	
	protected static final byte[] base64CharsAlt = {
		'A','B','C','D','E','F','G','H',
		'I','J','K','L','M','N','O','P',
		'Q','R','S','T','U','V','W','X',
		'Y','Z','a','b','c','d','e','f',
		'g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v',
		'w','x','y','z','0','1','2','3',
		'4','5','6','7','8','9','$','_',
	};

	/**
	 * Reverse lookup table for the Base64 alphabet.
	 * reversebase64Chars[byte] gives n for the nth Base64
	 * character or negative if a character is not a Base64 character.
	 */
	protected static final byte[] reverseBase64Chars = new byte[0x100];
	static {
		// Fill in NON_BASE_64 for all characters to start with
		for (int i = 0; i < reverseBase64Chars.length; i++) {
			reverseBase64Chars[i] = NON_BASE_64;
		}
		// For characters that are base64Chars, adjust
		// the reverse lookup table.
		for (byte i = 0; i < base64Chars.length; i++) {
			reverseBase64Chars[base64Chars[i]] = i;
		}

		reverseBase64Chars[' '] = NON_BASE_64_WHITESPACE;
		reverseBase64Chars['\n'] = NON_BASE_64_WHITESPACE;
		reverseBase64Chars['\r'] = NON_BASE_64_WHITESPACE;
		reverseBase64Chars['\t'] = NON_BASE_64_WHITESPACE;
		reverseBase64Chars['\f'] = NON_BASE_64_WHITESPACE;
		reverseBase64Chars['='] = NON_BASE_64_PADDING;

		// Alt support
		reverseBase64Chars['$'] = (byte) (base64CharsAlt.length - 2);
		reverseBase64Chars['_'] = (byte) (base64CharsAlt.length - 1);
	}

	/**
	 * Encode a String in Base64.
	 * The String is converted to and from bytes according to the platform's
	 * default character encoding.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param string The data to encode.
	 * @return An encoded String.
	 */
	public static String encode(String string) {
		return new String(encode(string.getBytes()));
	}

	/**
	 * Encode a String in Base64.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param string The data to encode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return An encoded String.
	 */
	public static String encode(String string, String enc)
		throws UnsupportedEncodingException {
		return new String(encode(string.getBytes(enc)), enc);
	}
	
	/**
	 * Encode a String in Base64.
	 * The String is converted to and from bytes according to the platform's
	 * default character encoding.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param string The data to encode.
	 * @param useAlt Whether to use an alternate character set
	 * @return An encoded String.
	 */
	public static String encode(String string, boolean useAlt) {
		return new String(encode(string.getBytes(), useAlt));
	}
	
	/**
	 * Encode a String in Base64.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param string The data to encode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @param useAlt Whether to use an alternate character set
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return An encoded String.
	 */
	public static String encode(String string, String enc, boolean useAlt)
		throws UnsupportedEncodingException {
		return new String(encode(string.getBytes(enc), useAlt), enc);
	}

	/**
	 * Encode bytes in Base64.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param bytes The data to encode.
	 * @return Encoded bytes.
	 */
	public static byte[] encode(byte[] bytes) {
		return encode(bytes, false);
	}
			
	/**
	 * Encode bytes in Base64.
	 * No line breaks or other white space are inserted into the encoded data.
	 *
	 * @param bytes The data to encode.
	 * @param useAlt Whether to use an alternate character set
	 * @return Encoded bytes.
	 */
	public static byte[] encode(byte[] bytes, boolean useAlt) {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// calculate the length of the resulting output.
		// in general it will be 4/3 the size of the input
		// but the input length must be divisible by three.
		// If it isn't the next largest size that is divisible
		// by three is used.
		int mod;
		int length = bytes.length;
		if ((mod = length % 3) != 0) {
			length += 3 - mod;
		}
		length = length * 4 / 3;
		ByteArrayOutputStream out = new ByteArrayOutputStream(length);
		try {
			encode(in, out, false, useAlt);
		} catch (IOException x) {
			// This can't happen.
			// The input and output streams were constructed
			// on memory structures that don't actually use IO.
			throw new RuntimeException(x.getMessage());
		}
		return out.toByteArray();
	}

	/**
	 * Encode this file in Base64.
	 * Line breaks will be inserted every 76 characters.
	 *
	 * @param fIn File to be encoded (will be overwritten).
	 * @throws IOException if an input or output error occurs.
	 */
	public static void encode(File fIn) throws IOException {
		encode(fIn, fIn, true);
	}

	/**
	 * Encode this file in Base64.
	 *
	 * @param fIn File to be encoded (will be overwritten).
	 * @param lineBreaks  Whether to insert line breaks every 76 characters in the output.
	 * @throws IOException if an input or output error occurs.
	 *
	 * @since ostermillerutils 1.00.00
	 */
	public static void encode(File fIn, boolean lineBreaks)
		throws IOException {
		encode(fIn, fIn, lineBreaks);
	}

	/**
	 * Encode this file in Base64.
	 * Line breaks will be inserted every 76 characters.
	 *
	 * @param fIn File to be encoded.
	 * @param fOut File to which the results should be written (may be the same as fIn).
	 * @throws IOException if an input or output error occurs.
	 */
	public static void encode(File fIn, File fOut) throws IOException {
		encode(fIn, fOut, true);
	}

	/**
	 * Encode this file in Base64.
	 *
	 * @param fIn File to be encoded.
	 * @param fOut File to which the results should be written (may be the same as fIn).
	 * @param lineBreaks  Whether to insert line breaks every 76 characters in the output.
	 * @throws IOException if an input or output error occurs.
	 *
	 */
	public static void encode(File fIn, File fOut, boolean lineBreaks)
		throws IOException {
		File temp = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fIn));
			temp = File.createTempFile("Base64", null, null);
			out = new BufferedOutputStream(new FileOutputStream(temp));
			encode(in, out, lineBreaks);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			if (fOut.isFile()) {
				fOut.delete();
			}
			temp.renameTo(fOut);
		} finally {
			if (in != null) {
				in.close();
				in = null;
			}
			if (out != null) {
				out.flush();
				out.close();
				out = null;
			}
		}
	}

	/**
	 * Encode data from the InputStream to the OutputStream in Base64.
	 * Line breaks are inserted every 76 characters in the output.
	 *
	 * @param in Stream from which to read data that needs to be encoded.
	 * @param out Stream to which to write encoded data.
	 * @throws IOException if there is a problem reading or writing.
	 */
	public static void encode(InputStream in, OutputStream out)
		throws IOException {
		encode(in, out, true);
	}
	
	/**
	 * Encode data from the InputStream to the OutputStream in Base64.
	 * Line breaks are inserted every 76 characters in the output.
	 *
	 * @param in Stream from which to read data that needs to be encoded.
	 * @param out Stream to which to write encoded data.
	 * @param lineBreaks Whether to insert line breaks every 76 characters in the output.
	 * @throws IOException if there is a problem reading or writing.
	 */
	public static void encode(InputStream in, OutputStream out, boolean lineBreaks)
		throws IOException {
		encode(in, out, lineBreaks, false);
	}

	/**
	 * Encode data from the InputStream to the OutputStream in Base64.
	 *
	 * @param in Stream from which to read data that needs to be encoded.
	 * @param out Stream to which to write encoded data.
	 * @param lineBreaks Whether to insert line breaks every 76 characters in the output.
	 * @param useAlt Whether to use an alternate character set
	 * @throws IOException if there is a problem reading or writing.
	 */
	public static void encode(
		InputStream in,
		OutputStream out,
		boolean lineBreaks,
		boolean useAlt)
		throws IOException {
		// Base64 encoding converts three bytes of input to
		// four bytes of output
		int[] inBuffer = new int[3];
		int lineCount = 0;
		byte[] base64Chs;
		if (useAlt) {
			base64Chs = base64CharsAlt;
		} else {
			base64Chs = base64Chars;
		}
		boolean done = false;
		while (!done && (inBuffer[0] = in.read()) != END_OF_INPUT) {
			// Fill the buffer
			inBuffer[1] = in.read();
			inBuffer[2] = in.read();

			// Calculate the out Buffer
			// The first byte of our in buffer will always be valid
			// but we must check to make sure the other two bytes
			// are not END_OF_INPUT before using them.
			// The basic idea is that the three bytes get split into
			// four bytes along these lines:
			//      [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
			// [xxAAAAAA] [xxBBBBBB] [xxCCCCCC] [xxDDDDDD]
			// bytes are considered to be zero when absent.
			// the four bytes are then mapped to common ASCII symbols

			// A's: first six bits of first byte
			out.write(base64Chs[inBuffer[0] >> 2]);
			if (inBuffer[1] != END_OF_INPUT) {
				// B's: last two bits of first byte, first four bits of second byte
				out.write(
					base64Chs[((inBuffer[0] << 4) & 0x30)
						| (inBuffer[1] >> 4)]);
				if (inBuffer[2] != END_OF_INPUT) {
					// C's: last four bits of second byte, first two bits of third byte
					out.write(
						base64Chs[((inBuffer[1] << 2) & 0x3c)
							| (inBuffer[2] >> 6)]);
					// D's: last six bits of third byte
					out.write(base64Chs[inBuffer[2] & 0x3F]);
				} else {
					// C's: last four bits of second byte
					out.write(base64Chs[((inBuffer[1] << 2) & 0x3c)]);
					// an equals sign for a character that is not a Base64 character
					out.write('=');
					done = true;
				}
			} else {
				// B's: last two bits of first byte
				out.write(base64Chs[((inBuffer[0] << 4) & 0x30)]);
				// an equal signs for characters that is not a Base64 characters
				out.write('=');
				out.write('=');
				done = true;
			}
			lineCount += 4;
			if (lineBreaks && lineCount >= 76) {
				out.write('\n');
				lineCount = 0;
			}
		}
		if (lineBreaks && lineCount >= 1) {
			out.write('\n');
			lineCount = 0;
		}
		out.flush();
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * The String is converted to and from bytes according to the platform's
	 * default character encoding.
	 *
	 * @param string The data to decode.
	 * @return A decoded String.
	 */
	public static String decode(String string) {
		return new String(decode(string.getBytes()));
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return A decoded String.
	 */
	public static String decode(String string, String enc)
		throws UnsupportedEncodingException {
		return new String(decode(string.getBytes(enc)), enc);
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param encIn Character encoding to use when converting input to bytes (should not matter because Base64 data is designed to survive most character encodings)
	 * @param encOut Character encoding to use when converting decoded bytes to output.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return A decoded String.
	 */
	public static String decode(String string, String encIn, String encOut)
		throws UnsupportedEncodingException {
		return new String(decode(string.getBytes(encIn)), encOut);
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * The String is converted to and from bytes according to the platform's
	 * default character encoding.
	 *
	 * @param string The data to decode.
	 * @return A decoded String.
	 */
	public static String decodeToString(String string) {
		return new String(decode(string.getBytes()));
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return A decoded String.
	 */
	public static String decodeToString(String string, String enc)
		throws UnsupportedEncodingException {
		return new String(decode(string.getBytes(enc)), enc);
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param encIn Character encoding to use when converting input to bytes (should not matter because Base64 data is designed to survive most character encodings)
	 * @param encOut Character encoding to use when converting decoded bytes to output.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return A decoded String.
	 */
	public static String decodeToString(
		String string,
		String encIn,
		String encOut)
		throws UnsupportedEncodingException {
		return new String(decode(string.getBytes(encIn)), encOut);
	}

	/**
	 * Decode a Base64 encoded String to an OutputStream.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * The String is converted from bytes according to the platform's
	 * default character encoding.
	 *
	 * @param string The data to decode.
	 * @param out Stream to which to write decoded data.
	 * @throws IOException if an IO error occurs.
	 */
	public static void decodeToStream(String string, OutputStream out)
		throws IOException {
		decode(new ByteArrayInputStream(string.getBytes()), out);
	}

	/**
	 * Decode a Base64 encoded String to an OutputStream.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @param out Stream to which to write decoded data.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @throws IOException if an IO error occurs.
	 */
	public static void decodeToStream(
		String string,
		String enc,
		OutputStream out)
		throws UnsupportedEncodingException, IOException {
		decode(new ByteArrayInputStream(string.getBytes(enc)), out);
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * The String is converted from bytes according to the platform's
	 * default character encoding.
	 *
	 * @param string The data to decode.
	 * @return decoded data.
	 */
	public static byte[] decodeToBytes(String string) {
		return decode(string.getBytes());
	}

	/**
	 * Decode a Base64 encoded String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param string The data to decode.
	 * @param enc Character encoding to use when converting from bytes.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return decoded data.
	 */
	public static byte[] decodeToBytes(String string, String enc)
		throws UnsupportedEncodingException {
		return decode(string.getBytes(enc));
	}

	/**
	 * Decode Base64 encoded bytes.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * The String is converted to bytes according to the platform's
	 * default character encoding.
	 *
	 * @param bytes The data to decode.
	 * @return A decoded String.
	 */
	public static String decodeToString(byte[] bytes) {
		return new String(decode(bytes));
	}

	/**
	 * Decode Base64 encoded bytes.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param bytes The data to decode.
	 * @param enc Character encoding to use when converting to and from bytes.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 * @return A decoded String.
	 */
	public static String decodeToString(byte[] bytes, String enc)
		throws UnsupportedEncodingException {
		return new String(decode(bytes), enc);
	}

	/**
	 * Decode Base64 encoded bytes.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param bytes The data to decode.
	 * @return Decoded bytes.
	 */
	public static byte[] decodeToBytes(byte[] bytes) {
		return decode(bytes);
	}

	/**
	 * Decode Base64 encoded bytes.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param bytes The data to decode.
	 * @return Decoded bytes.
	 */
	public static byte[] decode(byte[] bytes) {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// calculate the length of the resulting output.
		// in general it will be at most 3/4 the size of the input
		// but the input length must be divisible by four.
		// If it isn't the next largest size that is divisible
		// by four is used.
		int mod;
		int length = bytes.length;
		if ((mod = length % 4) != 0) {
			length += 4 - mod;
		}
		length = length * 3 / 4;
		FixedByteArrayOutputStream out = new FixedByteArrayOutputStream(length);
		try {
			decode(in, out, false);
		} catch (IOException x) {
			// This can't happen.
			// The input and output streams were constructed
			// on memory structures that don't actually use IO.
			throw new RuntimeException(x.getMessage());
		}
        
        // if array exact size, don't copy
        if (out.size() == length) {
            return out.getBytes();
        } else {
            return out.toByteArray();
        }		
	}

	/**
	 * Decode Base64 encoded bytes to the an OutputStream.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param bytes The data to decode.
	 * @param out Stream to which to write decoded data.
	 * @throws IOException if an IO error occurs.
	 * @return Decoded bytes.
	 */
	public static void decode(byte[] bytes, OutputStream out)
		throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		decode(in, out, false);
	}

	/**
	 * Decode Base64 encoded bytes to the an OutputStream.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param bytes The data to decode.
	 * @param out Stream to which to write decoded data.
	 * @throws IOException if an IO error occurs.
	 * @return Decoded bytes.
	 *
	 */
	public static void decodeToStream(byte[] bytes, OutputStream out)
		throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		decode(in, out, false);
	}

	/**
	 * Decode Base64 encoded data from one file to the other.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param fIn File to be decoded (will be overwritten).
	 * @throws IOException if an IO error occurs.
	 */
	public static void decode(File fIn) throws IOException {
		decode(fIn, fIn, true);
	}

	/**
	 * Decode Base64 encoded data from one file to the other.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param fIn File to be decoded (will be overwritten).
	 * @param throwExceptions Whether to throw exceptions when unexpected data is encountered.
	 * @throws IOException if an IO error occurs.
	 */
	public static void decode(File fIn, boolean throwExceptions)
		throws IOException {
		decode(fIn, fIn, throwExceptions);
	}

	/**
	 * Decode Base64 encoded data from one file to the other.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param fIn File to be decoded.
	 * @param fOut File to which the results should be written (may be the same as fIn).
	 * @throws IOException if an IO error occurs.
	 */
	public static void decode(File fIn, File fOut) throws IOException {
		decode(fIn, fOut, true);
	}

	/**
	 * Decode Base64 encoded data from one file to the other.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param fIn File to be decoded.
	 * @param fOut File to which the results should be written (may be the same as fIn).
	 * @param throwExceptions Whether to throw exceptions when unexpected data is encountered.
	 * @throws IOException if an IO error occurs.
	 */
	public static void decode(File fIn, File fOut, boolean throwExceptions)
		throws IOException {
		File temp = null;
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new BufferedInputStream(new FileInputStream(fIn));
			temp = File.createTempFile("Base64", null, null);
			out = new BufferedOutputStream(new FileOutputStream(temp));
			decode(in, out, throwExceptions);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
			if (fOut.isFile()) {
				fOut.delete();
			}
			temp.renameTo(fOut);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException ignore) {
					if (throwExceptions)
						throw ignore;
				}
				in = null;
			}
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException ignore) {
					if (throwExceptions)
						throw ignore;
				}
				out = null;
			}
		}
	}

	/**
	 * Reads the next (decoded) Base64 character from the input stream.
	 * Non Base64 characters are skipped.
	 *
	 * @param in Stream from which bytes are read.
	 * @param throwExceptions Throw an exception if an unexpected character is encountered.
	 * @return the next Base64 character from the stream or -1 if there are no more Base64 characters on the stream.
	 * @throws IOException if an IO Error occurs.
	 */
	private static final int readBase64(
		InputStream in,
		boolean throwExceptions)
		throws IOException {
		int read;
		int numPadding = 0;
		do {
			read = in.read();
			if (read == END_OF_INPUT)
				return END_OF_INPUT;
			read = reverseBase64Chars[(byte) read];
			if (throwExceptions
				&& (read == NON_BASE_64
					|| (numPadding > 0 && read > NON_BASE_64))) {
				throw new IOException("unexpected base64 data ");
			}
			if (read == NON_BASE_64_PADDING) {
				numPadding++;
			}
		} while (read <= NON_BASE_64);
		return read;
	}

	/**
	 * Decode Base64 encoded data from the InputStream to a byte array.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param in Stream from which to read data that needs to be decoded.
	 * @return decoded data.
	 * @throws IOException if an IO error occurs.
	 */
	public static byte[] decodeToBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		decode(in, out, false);
		return out.toByteArray();
	}

	/**
	 * Decode Base64 encoded data from the InputStream to a String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 * Bytes are converted to characters in the output String according to the platform's
	 * default character encoding.
	 *
	 * @param in Stream from which to read data that needs to be decoded.
	 * @return decoded data.
	 * @throws IOException if an IO error occurs.
	 */
	public static String decodeToString(InputStream in) throws IOException {
		return new String(decodeToBytes(in));
	}

	/**
	 * Decode Base64 encoded data from the InputStream to a String.
	 * Characters that are not part of the Base64 alphabet are ignored
	 * in the input.
	 *
	 * @param in Stream from which to read data that needs to be decoded.
	 * @param enc Character encoding to use when converting bytes to characters.
	 * @return decoded data.
	 * @throws IOException if an IO error occurs.Throws:
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 */
	public static String decodeToString(InputStream in, String enc)
		throws IOException {
		return new String(decodeToBytes(in), enc);
	}

	/**
	 * Decode Base64 encoded data from the InputStream to the OutputStream.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param in Stream from which to read data that needs to be decoded.
	 * @param out Stream to which to write decoded data.
	 * @throws IOException if an IO error occurs.
	 * @throws Base64DecodingException if unexpected data is encountered.
	 */
	public static void decode(InputStream in, OutputStream out)
		throws IOException {
		decode(in, out, true);
	}

	/**
	 * Decode Base64 encoded data from the InputStream to the OutputStream.
	 * Characters in the Base64 alphabet, white space and equals sign are
	 * expected to be in urlencoded data.  The presence of other characters
	 * could be a sign that the data is corrupted.
	 *
	 * @param in Stream from which to read data that needs to be decoded.
	 * @param out Stream to which to write decoded data.
	 * @param throwExceptions Whether to throw exceptions when unexpected data is encountered.
	 * @throws IOException if an IO error occurs.
	 */
	public static void decode(
		InputStream in,
		OutputStream out,
		boolean throwExceptions)
		throws IOException {
		// Base64 decoding converts four bytes of input to three bytes of output
		int[] inBuffer = new int[4];

		// read bytes unmapping them from their ASCII encoding in the process
		// we must read at least two bytes to be able to output anything
		boolean done = false;
		while (!done
			&& (inBuffer[0] = readBase64(in, throwExceptions)) != -1
			&& (inBuffer[1] = readBase64(in, throwExceptions)) != -1) {
			// Fill the buffer
			inBuffer[2] = readBase64(in, throwExceptions);
			inBuffer[3] = readBase64(in, throwExceptions);

			// Calculate the output
			// The first two bytes of our in buffer will always be valid
			// but we must check to make sure the other two bytes
			// are not END_OF_INPUT before using them.
			// The basic idea is that the four bytes will get reconstituted
			// into three bytes along these lines:
			// [xxAAAAAA] [xxBBBBBB] [xxCCCCCC] [xxDDDDDD]
			//      [AAAAAABB] [BBBBCCCC] [CCDDDDDD]
			// bytes are considered to be zero when absent.

			// six A and two B
			out.write(inBuffer[0] << 2 | inBuffer[1] >> 4);
			if (inBuffer[2] != -1) {
				// four B and four C
				out.write(inBuffer[1] << 4 | inBuffer[2] >> 2);
				if (inBuffer[3] != -1) {
					// two C and six D
					out.write(inBuffer[2] << 6 | inBuffer[3]);
				} else {
					done = true;
				}
			} else {
				done = true;
			}
		}
		out.flush();
	}

	/**
	 * Determines if the byte array is in base64 format.
	 * <p>
	 * Data will be considered to be in base64 format if it contains
	 * only base64 characters and whitespace with equals sign padding
	 * on the end so that the number of base64 characters is divisible
	 * by four.
	 * <p>
	 * It is possible for data to be in base64 format but for it to not
	 * meet these stringent requirements.  It is also possible for data
	 * to meet these requirements even though decoding it would not make
	 * any sense.  This method should be used as a guide but it is not
	 * authoritative because of the possibility of these false positives
	 * and false negatives.
	 * <p>
	 * Additionally, extra data such as headers or footers may throw
	 * this method off the scent and cause it to return false.
	 *
	 * @param bytes data that could be in base64 format.
	 *
	 */
	public static boolean isBase64(byte[] bytes) {
		try {
			return isBase64(new ByteArrayInputStream(bytes));
		} catch (IOException x) {
			// This can't happen.
			// The input and output streams were constructed
			// on memory structures that don't actually use IO.
			return false;
		}
	}

	/**
	 * Determines if the String is in base64 format.
	 * The String is converted to and from bytes according to the platform's
	 * default character encoding.
	 * <p>
	 * Data will be considered to be in base64 format if it contains
	 * only base64 characters and whitespace with equals sign padding
	 * on the end so that the number of base64 characters is divisible
	 * by four.
	 * <p>
	 * It is possible for data to be in base64 format but for it to not
	 * meet these stringent requirements.  It is also possible for data
	 * to meet these requirements even though decoding it would not make
	 * any sense.  This method should be used as a guide but it is not
	 * authoritative because of the possibility of these false positives
	 * and false negatives.
	 * <p>
	 * Additionally, extra data such as headers or footers may throw
	 * this method off the scent and cause it to return false.
	 *
	 * @param string String that may be in base64 format.
	 * @return Best guess as to whether the data is in base64 format.
	 *
	 */
	public static boolean isBase64(String string) {
		return isBase64(string.getBytes());
	}

	/**
	 * Determines if the String is in base64 format.
	 * <p>
	 * Data will be considered to be in base64 format if it contains
	 * only base64 characters and whitespace with equals sign padding
	 * on the end so that the number of base64 characters is divisible
	 * by four.
	 * <p>
	 * It is possible for data to be in base64 format but for it to not
	 * meet these stringent requirements.  It is also possible for data
	 * to meet these requirements even though decoding it would not make
	 * any sense.  This method should be used as a guide but it is not
	 * authoritative because of the possibility of these false positives
	 * and false negatives.
	 * <p>
	 * Additionally, extra data such as headers or footers may throw
	 * this method off the scent and cause it to return false.
	 *
	 * @param string String that may be in base64 format.
	 * @param enc Character encoding to use when converting to bytes.
	 * @return Best guess as to whether the data is in base64 format.
	 * @throws UnsupportedEncodingException if the character encoding specified is not supported.
	 */
	public static boolean isBase64(String string, String enc)
		throws UnsupportedEncodingException {
		return isBase64(string.getBytes(enc));
	}

	/**
	 * Determines if the File is in base64 format.
	 * <p>
	 * Data will be considered to be in base64 format if it contains
	 * only base64 characters and whitespace with equals sign padding
	 * on the end so that the number of base64 characters is divisible
	 * by four.
	 * <p>
	 * It is possible for data to be in base64 format but for it to not
	 * meet these stringent requirements.  It is also possible for data
	 * to meet these requirements even though decoding it would not make
	 * any sense.  This method should be used as a guide but it is not
	 * authoritative because of the possibility of these false positives
	 * and false negatives.
	 * <p>
	 * Additionally, extra data such as headers or footers may throw
	 * this method off the scent and cause it to return false.
	 *
	 * @param fIn File that may be in base64 format.
	 * @return Best guess as to whether the data is in base64 format.
	 * @throws IOException if an IO error occurs.
	 *
	 */
	public static boolean isBase64(File fIn) throws IOException {
		return isBase64(new BufferedInputStream(new FileInputStream(fIn)));
	}

	/**
	 * Reads data from the stream and determines if it is
	 * in base64 format.
	 * <p>
	 * Data will be considered to be in base64 format if it contains
	 * only base64 characters and whitespace with equals sign padding
	 * on the end so that the number of base64 characters is divisible
	 * by four.
	 * <p>
	 * It is possible for data to be in base64 format but for it to not
	 * meet these stringent requirements.  It is also possible for data
	 * to meet these requirements even though decoding it would not make
	 * any sense.  This method should be used as a guide but it is not
	 * authoritative because of the possibility of these false positives
	 * and false negatives.
	 * <p>
	 * Additionally, extra data such as headers or footers may throw
	 * this method off the scent and cause it to return false.
	 *
	 * @param in Stream from which to read data to be tested.
	 * @return Best guess as to whether the data is in base64 format.
	 * @throws IOException if an IO error occurs.
	 *
	 */
	public static boolean isBase64(InputStream in) throws IOException {
		long numBase64Chars = 0;
		int numPadding = 0;
		int read;

		while ((read = in.read()) != -1) {
			read = reverseBase64Chars[read];
			if (read == NON_BASE_64) {
				return false;
			} else if (read == NON_BASE_64_WHITESPACE) {
			} else if (read == NON_BASE_64_PADDING) {
				numPadding++;
				numBase64Chars++;
			} else if (numPadding > 0) {
				return false;
			} else {
				numBase64Chars++;
			}
		}
		if (numBase64Chars == 0)
			return false;
		if (numBase64Chars % 4 != 0)
			return false;
		return true;
	}
}