package org.qiyi.basecore.algorithm;

import org.qiyi.basecore.utils.ExceptionUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES 是一种可逆加密算法，对用户的敏感信息加密处理 对原始数据进行AES加密后，转换成16进制
 * 
 * @author:songguobin
 * @date:2014-8-20
 * @time:下午3:14:00
 */
//try
public class AESAlgorithm {
	/*
	 * 加密用的Key 可以用26个字母和数字组成 此处使用AES-128-CBC加密模式，key需要为16位。
	 */
	private static String sKey = "3e3acd08bb05bb1d";
	private static String ivParameter = "bb05bb1d3e3acd08";

	/**
	 * 加密算法
	 * 
	 * @date:2014-8-20
	 * @time:下午3:16:22
	 */
	public static String encrypt(String deviceId) {
		try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] raw = sKey.getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            // 使用CBC模式，需要一个向量iv，可增加加密算法的强度
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(deviceId.getBytes());
			return bytesToHexString(encrypted);
		} catch (Exception e) {
			ExceptionUtils.printStackTrace(e);
		}

		return null;
	}

	/**
	 * 解密算法
	 * 
	 * @date:2014-8-20
	 * @time:下午3:15:26
	 */
	public static String decrypt(String sSrc) {
		try {
			byte[] raw = sKey.getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(hexStringToBytes(sSrc));
            return new String(original);
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * 字节转16进制
	 * 
	 * @date:2014-8-20
	 * @time:下午3:16:52
	 */
	public static String bytesToHexString(byte[] src) {
		if (src == null || src.length <= 0) {
			return null;
		}
        StringBuilder stringBuilder = new StringBuilder("");
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * 16进制转二进制
	 * 
	 * @date:2014-8-20
	 * @time:下午3:17:08
	 */
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | (charToByte(hexChars[pos + 1]) & 0xff));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

}
