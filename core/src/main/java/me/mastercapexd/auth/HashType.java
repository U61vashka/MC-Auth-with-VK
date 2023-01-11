package me.mastercapexd.auth;

import org.mindrot.jbcrypt.BCrypt;

import me.mastercapexd.auth.utils.HashUtils;

public enum HashType {
    MD5 {
        @Override
        public String hash(String string) {
            return HashUtils.hashText(string, HashUtils.getMD5());
        }
    }, SHA256 {
        @Override
        public String hash(String string) {
            return HashUtils.hashText(string, HashUtils.getSHA256());
        }
    }, BCRYPT {
        @Override
        public String hash(String string) {
            return BCrypt.hashpw(string, BCrypt.gensalt());
        }

        @Override
        public boolean checkHash(String string, String hash) {
            return BCrypt.checkpw(string, hash);
        }
    };

    public abstract String hash(String string);

    public boolean checkHash(String string, String hash) {
        if (string == null || hash == null)
            return false;
        return hash(string).equals(hash);
    }
}