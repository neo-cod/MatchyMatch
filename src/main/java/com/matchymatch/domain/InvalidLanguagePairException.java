package com.matchymatch.domain;

// InvalidLanguagePairException sınıfı: Oyun oturumu başlatılırken geçersiz bir dil çifti seçildiğinde
// (örneğin Uygulama Dili ile Öğrenilecek Dil aynı olduğunda veya desteklenmeyen bir dil girildiğinde)
// fırlatılan özel kontrol edilen (checked) istisna (Exception) sınıfıdır.
public class InvalidLanguagePairException extends Exception {

    // Hata mesajı alan kurucu metot (Constructor)
    public InvalidLanguagePairException(String message) {
        super(message);
    }

    // Hata mesajı ve alt sebep (Throwable cause) alan kurucu metot
    public InvalidLanguagePairException(String message, Throwable cause) {
        super(message, cause);
    }
}
