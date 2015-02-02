package tsv2json;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SimHash {
  
  /**
   * Generate 64 bit simhash for a string
   * 
   * @param s
   * @return
   */
  public static long simHash64(String s) {
    long result = 0;
    int[] bitVector = new int[64];

    String[] words = s.split("[\\s()\\-\\/]+");
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
//      long hash = md5Hash64(word);
      long hash = fvnHash64(word);
      for (int i = 0; i < bitVector.length; i++) {
        bitVector[i] += (hash & 1) == 1 ? 1 : -1;
        hash = hash >> 1;
      }
    }
    
    for (int i = 0; i < bitVector.length; i++) {
      result = result << 1;
      if (bitVector[i] > 0) {
        result += 1;
      }
    }
    
    return result;
  }
  
  /**
   * Count different bits between two numbers
   * 
   * @param a
   * @param b
   * @return
   */
  public static int hammingDistance(long a, long b) {
    int dist = 0;
    a = a ^ b;
    while (a != 0) {
      a &= a - 1;
      dist++;
    }
    return dist;
  }
  
  /**
   * Generate 64 bit FVN hash for a string
   * @param s
   * @return
   */
  public static long fvnHash64(String s) {
    long basis = 0xcbf29ce484222325L;
    long prime = 0x100000001b3L;
    for (int i = 0; i < s.length(); i++) {
      basis ^= s.charAt(i);
      basis *= prime;
    }
    return basis;
  }
    
  public static BigInteger md5Hash64(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    digest.update(str.getBytes("utf-8"));
    return new BigInteger(1, digest.digest());
  }
}
