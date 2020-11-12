package ch.ubique.notifyme.sdk.backend.ws;

import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass;
import ch.ubique.notifyme.sdk.backend.model.SeedMessageOuterClass.SeedMessage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.goterl.lazycode.lazysodium.SodiumJava;
import com.goterl.lazycode.lazysodium.interfaces.Box;
import com.goterl.lazycode.lazysodium.utils.LibraryLoadingException;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class SodiumWrapper {

	private static final Logger logger = LoggerFactory.getLogger(SodiumWrapper.class);

	public final byte[] sk;
	public final byte[] pk;
	private final SodiumJava sodium;

	public SodiumWrapper(String skHex, String pkHex) {
		try {
			this.sk = Hex.decodeHex(skHex);
		} catch (DecoderException e) {
			logger.error("unable to parse sk hexstring", e);
			throw new RuntimeException(e);
		}
		try {
			this.pk = Hex.decodeHex(pkHex);
		} catch (DecoderException e) {
			logger.error("unable to parse pk hexstring", e);
			throw new RuntimeException(e);
		}
		// Do custom loading for the libsodium lib, as it does not work out of the box
		// with spring boot bundled jars. To get a path to the full file, we copy
		// libsodium to a tmpfile and give that absolute path to lazysodium
		try {
			InputStream in = getClass().getClassLoader().getResourceAsStream("libsodium/" + getSodiumPathInResources());
			File libTmpFile = File.createTempFile("libsodium", null);
			Files.copy(in, libTmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			in.close();
			this.sodium = new SodiumJava(libTmpFile.getAbsolutePath());
		} catch (Exception e) {
			logger.error("unable to load libsodium", e);
			throw new RuntimeException(e);
		}
	}

	public byte[] decryptQrTrace(byte[] ctx) throws InvalidProtocolBufferException {
		byte[] msg = new byte[ctx.length - Box.SEALBYTES];
		int result = sodium.crypto_box_seal_open(msg, ctx, ctx.length, pk, sk);
		SeedMessage seed = SeedMessageOuterClass.SeedMessage.parseFrom(msg);
		logger.debug(result + " msg: " + seed.toString());
		byte[] newPk = new byte[64];
		byte[] newSk = new byte[64];
		byte[] msgSHA256 = new byte[32];
		sodium.crypto_hash_sha256(msgSHA256, msg, msg.length);
		sodium.crypto_sign_seed_keypair(newPk, newSk, msgSHA256);
		logger.debug(newSk.toString());
		return newSk;
	}

	/**
	 * Returns the absolute path to sodium library inside JAR (beginning with '/'),
	 * e.g. /linux/libsodium.so.
	 */
	private static String getSodiumPathInResources() {
		boolean is64Bit = Native.POINTER_SIZE == 8;
		if (Platform.isWindows()) {
			if (is64Bit) {
				return getPath("windows64", "libsodium.dll");
			} else {
				return getPath("windows", "libsodium.dll");
			}
		}
		if (Platform.isARM()) {
			return getPath("armv6", "libsodium.so");
		}
		if (Platform.isLinux()) {
			if (is64Bit) {
				return getPath("linux64", "libsodium.so");
			} else {
				return getPath("linux", "libsodium.so");
			}
		}
		if (Platform.isMac()) {
			return getPath("mac", "libsodium.dylib");
		}

		String message = String.format("Unsupported platform: %s/%s", System.getProperty("os.name"),
				System.getProperty("os.arch"));
		throw new LibraryLoadingException(message);
	}

	private static String getPath(String folder, String name) {
		String separator = "/";
		return folder + separator + name;
	}
}
