package com.victor.MovieDownloader.service;

import com.victor.MovieDownloader.exception.InvalidUrlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlValidationServiceTest {

    private final UrlValidationService service = new UrlValidationService();

    @Test
    void rejectsBlankUrl() {
        assertThrows(InvalidUrlException.class, () -> service.validate(""));
        assertThrows(InvalidUrlException.class, () -> service.validate(null));
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertThrows(InvalidUrlException.class, () -> service.validate("ftp://example.com/file"));
        assertThrows(InvalidUrlException.class, () -> service.validate("file:///etc/passwd"));
        assertThrows(InvalidUrlException.class, () -> service.validate("javascript:alert(1)"));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(InvalidUrlException.class, () -> service.validate("not a url"));
    }

    @Test
    void rejectsLoopbackAndPrivateAddresses() {
        assertThrows(InvalidUrlException.class, () -> service.validate("http://127.0.0.1/video"));
        assertThrows(InvalidUrlException.class, () -> service.validate("http://localhost/video"));
        assertThrows(InvalidUrlException.class, () -> service.validate("http://10.0.0.5/video"));
        assertThrows(InvalidUrlException.class, () -> service.validate("http://192.168.1.10/video"));
        assertThrows(InvalidUrlException.class, () -> service.validate("http://169.254.169.254/latest/meta-data"));
    }

    @Test
    void acceptsPublicHttpsUrl() {
        assertDoesNotThrow(() -> service.validate("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }
}
