public class DiffieHellman {

    private static final int PRIME_NUMBER = 23;
    private static final int GENERATOR = 9;

    private static final int ALICE_PRIVATE_KEY = 4;
    private static final int BOB_PRIVATE_KEY = 3;

    private static final int ALICE_PUBLIC_KEY = generatePublicKey(ALICE_PRIVATE_KEY);
    private static final int BOB_PUBLIC_KEY = generatePublicKey(BOB_PRIVATE_KEY);

    public static void main(String[] args) {

        String message = "Good Morning!";

        String cipherText = encryptMessage(message);                //ɿϧϧ΄ĠʵϧЂϞαϞΟĩ
        String decryptedMessage = decryptMessage(cipherText);       //Good Morning!

    }

    private static String encryptMessage(String message) {

        int sharedKey = generateSharedKey(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        StringBuilder cipherText = new StringBuilder();
        for(int i=0; i<message.length(); i++) {
            cipherText.append(encryptCharacter(message.charAt(i), sharedKey));
        }

        return cipherText.toString();
    }

    private static String decryptMessage(String cipherText) {

        int sharedKey = generateSharedKey(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);

        StringBuilder decryptedMessage = new StringBuilder();
        for(int i=0; i<cipherText.length(); i++) {
            decryptedMessage.append(decryptCharacter(cipherText.charAt(i), sharedKey));
        }

        return decryptedMessage.toString();
    }


    private static char encryptCharacter(char character, int sharedKey) {
        return (char) (character * sharedKey);
    }

    private static char decryptCharacter(char character, int sharedKey) {
        return (char) (character / sharedKey);
    }

    private static int generatePublicKey(int privateKey) {
        return (int) (Math.pow(GENERATOR, privateKey) % PRIME_NUMBER);
    }

    private static int generateSharedKey(int ownPrivateKey, int receivedPublicKey) {
        return (int) (Math.pow(receivedPublicKey, ownPrivateKey) % PRIME_NUMBER);
    }

}
