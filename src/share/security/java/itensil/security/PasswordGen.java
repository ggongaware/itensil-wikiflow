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
package itensil.security;

import itensil.util.Check;

import java.security.SecureRandom;

/**
 * @author ggongaware@itensil.com
 *
 */
public class PasswordGen {

    protected static char [] CHAR_CONSONANTS =  {
            'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'm',
            'n', 'p', 'q', 'r', 's', 't', 'v', 'w', 'x',
            'y', 'z' // no 'l'
        };

    protected static char [] CHAR_VOWELS = {
            'a', 'e', 'i', 'o' // no 'u' to avoid "fuk" and "suk"
        };

    protected static char [] CHAR_NUMBERS = {
        '-', '2', '3', '4', '5', '6', '7', '8', '9', '-' // no '0' or '1'
        };

    protected static char [] CHAR_NUMBERS2 = {
        '2', '3', '4', '5', '6', '7', '8', '9', // no '0' or '1'
        };

    public static final int MIN_LENGTH = 5;
    public static final int MAX_LENGTH = 20;

    protected static SecureRandom sRand = new SecureRandom();

    /**
     * Check for empty and out of bounds passwords
     *
     * @param password
     * @return true=OK
     */
    public static boolean checkPassword(String password) {
        if (Check.isEmpty(password)) {
            return false;
        }
        return password.length() >= 5 && password.length() <= 20;
    }

    /**
     * Generates a new 8 character password
     *
     * @return pass
     */
    public static String generatePassword() {

        byte rbs[] = new byte[9];
        sRand.nextBytes(rbs);

        char [] pass = new char[8];

        pass[0] = CHAR_CONSONANTS[Math.abs((int)rbs[1]) % CHAR_CONSONANTS.length];
        pass[1] = CHAR_VOWELS[Math.abs((int)rbs[2]) % CHAR_VOWELS.length];
        pass[2] = CHAR_CONSONANTS[Math.abs((int)rbs[3]) % CHAR_CONSONANTS.length];

        // three formats
        // dig-zop9
        switch (Math.abs((int)rbs[0]) % 3) {

            case 0: // dig-zop9
                pass[3] = CHAR_NUMBERS[Math.abs((int)rbs[4]) % CHAR_NUMBERS.length];
                pass[4] = CHAR_CONSONANTS[Math.abs((int)rbs[5]) % CHAR_CONSONANTS.length];
                pass[5] = CHAR_VOWELS[Math.abs((int)rbs[6]) % CHAR_VOWELS.length];
                pass[6] = CHAR_CONSONANTS[Math.abs((int)rbs[7]) % CHAR_CONSONANTS.length];
                pass[7] = CHAR_NUMBERS2[Math.abs((int)rbs[8]) % CHAR_NUMBERS2.length];
                break;

            case 1: // dig-9zop
                pass[3] = CHAR_NUMBERS[Math.abs((int)rbs[4]) % CHAR_NUMBERS.length];
                pass[4] = CHAR_NUMBERS2[Math.abs((int)rbs[5]) % CHAR_NUMBERS2.length];
                pass[5] = CHAR_CONSONANTS[Math.abs((int)rbs[6]) % CHAR_CONSONANTS.length];
                pass[6] = CHAR_VOWELS[Math.abs((int)rbs[7]) % CHAR_VOWELS.length];
                pass[7] = CHAR_CONSONANTS[Math.abs((int)rbs[8]) % CHAR_CONSONANTS.length];
                break;

            case 2: // digzop-9
            default:
                pass[3] = CHAR_CONSONANTS[Math.abs((int)rbs[4]) % CHAR_CONSONANTS.length];
                pass[4] = CHAR_VOWELS[Math.abs((int)rbs[5]) % CHAR_VOWELS.length];
                pass[5] = CHAR_CONSONANTS[Math.abs((int)rbs[6]) % CHAR_CONSONANTS.length];
                pass[6] = CHAR_NUMBERS[Math.abs((int)rbs[7]) % CHAR_NUMBERS.length];
                pass[7] = CHAR_NUMBERS2[Math.abs((int)rbs[8]) % CHAR_NUMBERS2.length];
                break;
        }
        return new String(pass);
    }

    public static void main(String args[]) {

        // test some
        System.out.println(generatePassword());
        System.out.println(generatePassword());
        System.out.println(generatePassword());
        System.out.println(generatePassword());
    }

}
